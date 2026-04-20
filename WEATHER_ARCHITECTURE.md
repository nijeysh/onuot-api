# Weather Provider Architecture

## 1. 개요

날씨 데이터를 외부 API로부터 수집하여 통합된 형태로 제공하는 멀티 Provider 아키텍처.
현재는 기상청(KMA)만 연동되어 있으며, 이후 에어코리아, OpenWeatherMap 등을 추가할 수 있도록 설계.

### 설계 목표
- **Provider 추상화**: 각 날씨 API를 독립적인 Provider로 캡슐화
- **데이터 정규화**: Provider별 상이한 응답 포맷을 통일된 내부 모델로 변환
- **자동 등록**: `@Component`만 붙이면 `WeatherService`에 자동 주입 — 코드 수정 불필요
- **MSA Ready**: weather 도메인 전체를 독립 서비스로 추출 가능

---

## 2. 패키지 구조

```
com.onuot.api.modules.weather
├── api/
│   └── WeatherController.java              # GET /api/weather 엔드포인트
├── application/
│   ├── WeatherService.java                 # Provider 조합 + 캐싱 오케스트레이션
│   ├── WeatherCacheService.java            # Redis 캐시 R/W
│   └── dto/
│       ├── WeatherRequest.java
│       └── WeatherResponse.java            # record 기반, 프론트엔드 WeatherData와 1:1 매칭
├── domain/
│   ├── WeatherProvider.java                # 공통 인터페이스 (outbound port)
│   ├── WeatherProviderType.java            # enum: KMA, AIRKOREA, OPENWEATHERMAP
│   ├── WeatherDataCapability.java          # enum: CURRENT, HOURLY_FORECAST, DAILY_FORECAST, AIR_QUALITY, UV_INDEX
│   └── model/                             # 정규화된 내부 도메인 모델
│       ├── NormalizedWeatherData.java      # 통합 모델 (current + hourly + daily + air + uv)
│       ├── CurrentWeather.java
│       ├── HourlyForecast.java
│       ├── DailyForecast.java
│       ├── AirQuality.java
│       ├── WeatherLocation.java
│       └── WeatherCondition.java           # enum: CLEAR, CLOUDY, RAIN, SNOW 등
└── infrastructure/
    ├── config/
    │   └── WeatherProviderConfig.java      # @ConfigurationProperties(prefix = "weather")
    └── provider/
        └── kma/                            # 기상청 Provider (현재 유일한 구현체)
            ├── KmaWeatherProvider.java     # WeatherProvider 구현체
            ├── KmaApiClient.java           # 기상청 API 호출 (초단기실황, 단기예보)
            ├── KmaGridConverter.java       # 위경도 → nx/ny 격자 변환 (Lambert Conformal Conic)
            └── dto/
                └── KmaApiResponse.java     # 기상청 raw response
```

---

## 3. 핵심 인터페이스

### WeatherProvider

모든 날씨 API Provider가 구현하는 공통 인터페이스.

```java
public interface WeatherProvider {

    /** Provider 식별자 */
    WeatherProviderType getType();

    /** 이 Provider가 제공 가능한 데이터 종류 */
    Set<WeatherDataCapability> getCapabilities();

    /** 날씨 데이터 조회 — 자기 capability에 해당하는 필드만 채워서 반환 */
    NormalizedWeatherData fetch(double latitude, double longitude);
}
```

### WeatherProviderType

```java
public enum WeatherProviderType {
    KMA,              // 기상청 (구현 완료)
    AIRKOREA,         // 에어코리아 (미구현)
    OPENWEATHERMAP    // OpenWeatherMap (미구현)
}
```

### WeatherDataCapability

```java
public enum WeatherDataCapability {
    CURRENT,           // 현재 날씨
    HOURLY_FORECAST,   // 시간별 예보 (24시간)
    DAILY_FORECAST,    // 일별 예보 (7일)
    AIR_QUALITY,       // 대기질 (PM10, PM2.5)
    UV_INDEX           // 자외선 지수
}
```

---

## 4. 데이터 모델

### 내부 정규화 모델 (model/)

각 Provider의 raw 응답을 변환하여 저장하는 통일된 모델.

```java
// NormalizedWeatherData — 모든 Provider 결과를 merge하는 통합 모델
public record NormalizedWeatherData(
    WeatherLocation location,
    CurrentWeather current,
    List<HourlyForecast> hourly,    // 최대 24개
    List<DailyForecast> daily,      // 최대 7개
    AirQuality airQuality,          // nullable
    Double uvIndex,                 // nullable
    LocalDateTime updatedAt
) {
    public NormalizedWeatherData mergeWith(NormalizedWeatherData other) { ... }
}
```

### WeatherCondition (통합 날씨 상태)

```java
public enum WeatherCondition {
    CLEAR, PARTLY_CLOUDY, CLOUDY, OVERCAST,
    RAIN, HEAVY_RAIN, SNOW, SLEET, FOG, THUNDERSTORM, UNKNOWN
}
```

KMA 매핑:
- PTY 코드 → 0:CLEAR, 1:RAIN, 2:SLEET, 3:SNOW, 4:RAIN
- SKY 코드 → 1:CLEAR, 3:PARTLY_CLOUDY, 4:CLOUDY (PTY=0일 때만)

---

## 5. Provider 상세

### 기상청 (KMA)

**Capabilities**: `CURRENT`, `HOURLY_FORECAST`, `DAILY_FORECAST`

**사용 API**:
| API | 용도 | 엔드포인트 |
|-----|------|-----------|
| 초단기실황 | 현재 날씨 | `getUltraSrtNcst` |
| 단기예보 | 시간별/일별 예보 | `getVilageFcst` |

**좌표 변환**: 위경도 → 격자 좌표(nx, ny)
- Lambert Conformal Conic 투영법
- `KmaGridConverter.toGrid(lat, lon)` → `Grid(nx, ny)`
- 예: 서울시청 (37.5665, 126.9780) → (60, 127)

**응답 매핑**:
```
T1H (기온)           → CurrentWeather.temperature
REH (습도)           → CurrentWeather.humidity
WSD (풍속)           → CurrentWeather.windSpeed
PTY (강수형태)        → WeatherCondition
SKY (하늘상태)        → WeatherCondition (PTY=0일 때)
RN1 (1시간 강수량)    → HourlyForecast.precipitation
TMN/TMX (최저/최고)   → DailyForecast.minTemp/maxTemp
```

---

## 6. 데이터 흐름도

```
[React Frontend]
    │  GET /api/weather?latitude=37.5665&longitude=126.9780
    ▼
[WeatherController]
    │  weatherService.getWeather(lat, lon)
    ▼
[WeatherService]
    │
    ├─ 1. Redis 캐시 확인
    │     key: "weather:37.57:126.98" (소수점 2자리 truncate)
    │     캐시 hit → 즉시 반환
    │
    ├─ 2. 등록된 모든 Provider 순회 (현재: KmaWeatherProvider만)
    │     Provider 호출 실패 시 → 해당 Provider 건너뜀 (warn 로그)
    │     모든 Provider 실패 시 → BusinessException(WTH_003)
    │
    ├─ 3. NormalizedWeatherData.mergeWith()로 결과 병합
    │
    ├─ 4. Redis 캐시 저장 (TTL: 30분)
    │
    └─ 5. WeatherResponse.from(data) → JSON 응답
```

---

## 7. 설정 (application.yml)

```yaml
weather:
  cache:
    current-ttl: 1800    # 캐시 TTL (초) — 30분
    forecast-ttl: 3600   # 예보 캐시 TTL (초) — 1시간
  providers:
    kma:
      enabled: true
      base-url: ${KMA_API_BASE_URL:https://apis.data.go.kr/1360000/VilageFcstInfoService_2.0}
      service-key: ${KMA_API_SERVICE_KEY:}
```

---

## 8. Provider 추가 방법

```
1. WeatherProviderType enum에 새 값 추가 (이미 AIRKOREA, OPENWEATHERMAP 있음)
2. infrastructure/provider/{name}/ 아래 구현체 작성
   - WeatherProvider 인터페이스 구현
   - @Component 추가
3. application.yml providers.{name} 설정 추가
→ WeatherService 수정 불필요 — Spring이 자동으로 List<WeatherProvider>에 주입
```

---

## 9. 에러 처리

```java
// ErrorCode (Weather)
WEATHER_ALL_PROVIDERS_FAILED(HttpStatus.SERVICE_UNAVAILABLE, "WTH_003", "모든 날씨 제공자 호출에 실패했습니다.")
```

개별 Provider 호출 실패 시 → warn 로그 후 해당 Provider 결과 제외  
모든 Provider 실패 시 → `BusinessException(WEATHER_ALL_PROVIDERS_FAILED)` throw

---

## 10. MSA 전환 포인트

| 항목 | 설명 |
|------|------|
| DB 비의존 | weather 도메인은 PostgreSQL 미사용, Redis만 사용 |
| 도메인 격리 | 다른 도메인과 Entity 참조 없음 |
| 자체 완결 | controller → service → provider 체인이 독립적 |
| 설정 분리 | `weather.*` 설정이 별도 namespace |
