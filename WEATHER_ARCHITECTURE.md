# Weather Provider Architecture

## 1. 개요

날씨 데이터를 여러 외부 API(기상청, 에어코리아, OpenWeatherMap 등)로부터 수집하여 통합된 형태로 제공하는 멀티 Provider 아키텍처.

### 설계 목표
- **Provider 추상화**: 각 날씨 API를 독립적인 Provider로 캡슐화 (Strategy/Adapter 패턴)
- **데이터 정규화**: Provider별 상이한 응답 포맷을 통일된 내부 모델로 변환
- **Capability 기반 조합**: 각 Provider가 제공 가능한 데이터 종류를 선언하고, 설정에 따라 최적 조합
- **Fallback 전략**: Provider 장애 시 자동으로 대체 Provider 사용
- **MSA Ready**: weather 도메인 전체를 독립 서비스로 추출 가능

---

## 2. 패키지 구조

```
com.onuot.api.domain.weather
├── controller/
│   └── WeatherController.java              # GET /api/weather 엔드포인트
├── service/
│   ├── WeatherAggregationService.java      # Provider 조합 + 캐싱 오케스트레이션
│   └── WeatherCacheService.java            # Redis 캐시 R/W
├── provider/
│   ├── WeatherProvider.java                # 공통 인터페이스
│   ├── WeatherProviderType.java            # enum: KMA, AIRKOREA, OPENWEATHERMAP
│   ├── WeatherDataCapability.java          # enum: CURRENT, HOURLY_FORECAST, DAILY_FORECAST, AIR_QUALITY, UV_INDEX
│   ├── kma/                                # 기상청 Provider
│   │   ├── KmaWeatherProvider.java         # WeatherProvider 구현체
│   │   ├── KmaApiClient.java              # 기상청 API 호출 (초단기실황, 단기예보)
│   │   ├── KmaGridConverter.java          # 위경도 → nx/ny 격자 변환 (Lambert Conformal Conic)
│   │   └── dto/
│   │       ├── KmaUltraShortResponse.java # 초단기실황 raw response
│   │       └── KmaShortForecastResponse.java # 단기예보 raw response
│   ├── airkorea/                           # 에어코리아 Provider
│   │   ├── AirKoreaWeatherProvider.java
│   │   ├── AirKoreaApiClient.java         # 근접측정소 + 대기질 조회
│   │   └── dto/
│   │       └── AirKoreaResponse.java
│   └── openweathermap/                     # OpenWeatherMap Provider
│       ├── OwmWeatherProvider.java
│       ├── OwmApiClient.java              # One Call API 3.0
│       └── dto/
│           └── OwmOneCallResponse.java
├── model/                                  # 정규화된 내부 도메인 모델
│   ├── NormalizedWeatherData.java         # 통합 모델 (current + hourly + daily + air + uv)
│   ├── CurrentWeather.java
│   ├── HourlyForecast.java
│   ├── DailyForecast.java
│   ├── AirQuality.java
│   ├── WeatherLocation.java
│   └── WeatherCondition.java              # enum: CLEAR, CLOUDY, RAIN, SNOW 등
├── dto/
│   ├── WeatherRequest.java
│   └── WeatherResponse.java              # record 기반, 프론트엔드 WeatherData와 1:1 매칭
└── config/
    └── WeatherProviderConfig.java         # @ConfigurationProperties(prefix = "weather")
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
    KMA,              // 기상청
    AIRKOREA,         // 에어코리아 (한국환경공단)
    OPENWEATHERMAP    // OpenWeatherMap
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
) {}

// CurrentWeather
public record CurrentWeather(
    double temperature,
    double feelsLike,
    int humidity,
    double windSpeed,
    WeatherCondition condition,
    String description
) {}

// HourlyForecast
public record HourlyForecast(
    LocalDateTime time,
    double temperature,
    WeatherCondition condition,
    double precipitation
) {}

// DailyForecast
public record DailyForecast(
    LocalDate date,
    double minTemp,
    double maxTemp,
    WeatherCondition condition,
    double precipitation
) {}

// AirQuality
public record AirQuality(
    int pm10,
    int pm25
) {}

// WeatherLocation
public record WeatherLocation(
    double latitude,
    double longitude,
    String city,
    String district
) {}
```

### WeatherCondition (통합 날씨 상태)

```java
public enum WeatherCondition {
    CLEAR,           // 맑음
    PARTLY_CLOUDY,   // 구름 조금
    CLOUDY,          // 흐림
    OVERCAST,        // 완전 흐림
    RAIN,            // 비
    HEAVY_RAIN,      // 폭우
    SNOW,            // 눈
    SLEET,           // 진눈깨비
    FOG,             // 안개
    THUNDERSTORM,    // 뇌우
    UNKNOWN          // 알 수 없음
}
```

각 Provider는 자체 날씨 코드를 이 enum에 매핑:
- **KMA**: PTY(강수형태) 코드 → 0:CLEAR, 1:RAIN, 2:SLEET, 3:SNOW, 4:RAIN / SKY 코드 → 1:CLEAR, 3:PARTLY_CLOUDY, 4:CLOUDY
- **OWM**: Weather ID → 200~:THUNDERSTORM, 300~:RAIN, 500~:RAIN, 600~:SNOW, 700~:FOG, 800:CLEAR, 801~:CLOUDY

### WeatherResponse DTO (프론트엔드 매칭)

React 프론트엔드의 `WeatherData` TypeScript 타입과 1:1 대응.

```java
public record WeatherResponse(
    LocationDto location,
    CurrentDto current,
    List<HourlyDto> hourly,
    List<DailyDto> daily,
    String updatedAt               // ISO 8601
) {
    public record LocationDto(
        double latitude, double longitude,
        String city, String district
    ) {}

    public record CurrentDto(
        double temperature, double feelsLike,
        int humidity, double windSpeed,
        String condition, String description,
        Integer pm10, Integer pm25, Double uvIndex
    ) {}

    public record HourlyDto(
        String time, double temperature,
        String condition, double precipitation
    ) {}

    public record DailyDto(
        String date, double minTemp, double maxTemp,
        String condition, double precipitation
    ) {}
}
```

---

## 5. Provider별 상세

### 5-1. 기상청 (KMA)

**Capabilities**: `CURRENT`, `HOURLY_FORECAST`, `DAILY_FORECAST`

**사용 API**:
| API | 용도 | 엔드포인트 |
|-----|------|-----------|
| 초단기실황 | 현재 날씨 | `getUltraSrtNcst` |
| 단기예보 | 시간별/일별 예보 | `getVilageFcst` |

**좌표 변환**: 기상청은 위경도가 아닌 격자 좌표(nx, ny)를 사용.
- Lambert Conformal Conic 투영법으로 변환
- `KmaGridConverter.toGrid(lat, lon)` → `Grid(nx, ny)`
- 예: 서울시청 (37.5665, 126.9780) → (60, 127)

**응답 매핑**:
```
기상청 카테고리 → 내부 모델
─────────────────────────────
T1H (기온)      → CurrentWeather.temperature
REH (습도)      → CurrentWeather.humidity
WSD (풍속)      → CurrentWeather.windSpeed
PTY (강수형태)   → WeatherCondition
SKY (하늘상태)   → WeatherCondition (PTY=0일 때)
RN1 (1시간 강수량) → HourlyForecast.precipitation
TMN/TMX (최저/최고) → DailyForecast.minTemp/maxTemp
```

### 5-2. 에어코리아 (AirKorea)

**Capabilities**: `AIR_QUALITY`

**사용 API**:
| API | 용도 |
|-----|------|
| 근접측정소 목록 조회 | 위경도 → 가장 가까운 측정소 이름 |
| 측정소별 실시간 측정정보 | 측정소 이름 → PM10, PM2.5 등 |

**호출 흐름**:
1. `getMsrstnAcctoRltmMesureDnsty` 또는 근접측정소 조회 API로 가까운 측정소 확인
2. 해당 측정소의 실시간 대기질 데이터 조회
3. PM10, PM2.5 값을 `AirQuality` 모델에 매핑

### 5-3. OpenWeatherMap (OWM)

**Capabilities**: `CURRENT`, `HOURLY_FORECAST`, `DAILY_FORECAST`, `AIR_QUALITY`, `UV_INDEX`

**사용 API**: One Call API 3.0
- 단일 호출로 current + hourly(48h) + daily(8d) + alerts 제공
- 별도 Air Pollution API로 대기질 조회
- UV Index는 current 응답에 포함

**응답 매핑**:
```
OWM 필드 → 내부 모델
─────────────────────────────
current.temp        → CurrentWeather.temperature
current.feels_like  → CurrentWeather.feelsLike
current.humidity    → CurrentWeather.humidity
current.wind_speed  → CurrentWeather.windSpeed
current.uvi         → uvIndex
current.weather[0]  → WeatherCondition (id 기반 매핑)
hourly[0..23]       → List<HourlyForecast>
daily[0..6]         → List<DailyForecast>
```

---

## 6. 데이터 흐름도

```
[React Frontend]
    │  GET /api/weather?latitude=37.5665&longitude=126.9780
    ▼
[WeatherController]
    │  aggregationService.getWeather(lat, lon)
    ▼
[WeatherAggregationService]
    │
    ├─ 1. Redis 캐시 확인
    │     key: "weather:37.57:126.98" (소수점 2자리 truncate, ~1.1km 정밀도)
    │     캐시 hit → 즉시 반환
    │
    ├─ 2. Strategy 기반 Provider 선택
    │     config.strategy에서 capability별 provider 결정
    │     예: CURRENT→KMA, AIR_QUALITY→AIRKOREA, UV_INDEX→OWM
    │
    ├─ 3. CompletableFuture로 병렬 호출
    │     ┌─────────────────────┬──────────────────────┬─────────────────────┐
    │     │  KmaWeatherProvider │ AirKoreaProvider     │ OwmWeatherProvider  │
    │     │                     │                      │                     │
    │     │  KmaGridConverter   │ 근접측정소 조회       │ One Call API 호출   │
    │     │  toGrid(lat,lon)    │ → 대기질 조회        │                     │
    │     │  → 초단기실황 호출   │                      │                     │
    │     │  → 단기예보 호출     │                      │                     │
    │     │  → NormalizedData   │ → NormalizedData     │ → NormalizedData    │
    │     │  (current,hourly,   │ (airQuality)         │ (uvIndex)           │
    │     │   daily)            │                      │                     │
    │     └─────────────────────┴──────────────────────┴─────────────────────┘
    │
    ├─ 4. 결과 Merge
    │     KMA의 current/hourly/daily + AirKorea의 airQuality + OWM의 uvIndex
    │     → NormalizedWeatherData (전체 필드 채움)
    │
    ├─ 5. Redis 캐시 저장
    │     TTL: current 30분, forecast 1시간
    │
    └─ 6. WeatherResponse.from(merged) → 프론트엔드 포맷 변환
         ▼
    [ApiResponse.ok(weatherResponse)] → JSON
```

---

## 7. 설정 (application.yml)

```yaml
weather:
  cache:
    current-ttl: 1800         # 현재 날씨 캐시 TTL (초) — 30분
    forecast-ttl: 3600        # 예보 캐시 TTL (초) — 1시간

  providers:
    kma:
      enabled: true
      base-url: ${KMA_API_BASE_URL:https://apis.data.go.kr/1360000/VilageFcstInfoService_2.0}
      service-key: ${KMA_API_SERVICE_KEY:}
    airkorea:
      enabled: true
      base-url: ${AIRKOREA_API_BASE_URL:https://apis.data.go.kr/B552584/ArpltnInforInqireSvc}
      service-key: ${AIRKOREA_API_SERVICE_KEY:}
    openweathermap:
      enabled: false          # 기본 비활성화, fallback 또는 UV용으로 활성화
      base-url: https://api.openweathermap.org/data/3.0
      api-key: ${OWM_API_KEY:}

  strategy:                   # capability별 primary provider 지정
    current: KMA
    hourly-forecast: KMA
    daily-forecast: KMA
    air-quality: AIRKOREA
    uv-index: OPENWEATHERMAP
    fallback: OPENWEATHERMAP  # primary 실패 시 대체 provider
```

### Provider 추가 방법

1. `WeatherProviderType` enum에 새 값 추가
2. `WeatherProvider` 인터페이스 구현체 작성
3. `application.yml`에 provider 설정 추가
4. strategy에서 해당 capability에 새 provider 지정

---

## 8. 에러 처리 & Fallback

### ErrorCode 추가

```java
// Weather (WTH)
WEATHER_API_ERROR(HttpStatus.SERVICE_UNAVAILABLE, "WTH_001", "날씨 API 호출에 실패했습니다."),
WEATHER_PROVIDER_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "WTH_002", "날씨 제공자가 응답하지 않습니다."),
WEATHER_ALL_PROVIDERS_FAILED(HttpStatus.SERVICE_UNAVAILABLE, "WTH_003", "모든 날씨 제공자 호출에 실패했습니다."),
```

### Fallback 전략

```
Provider 호출 실패 시:
├── 필수 데이터 (CURRENT, HOURLY_FORECAST, DAILY_FORECAST)
│   ├── primary provider 실패 → fallback provider 시도
│   └── fallback도 실패 → BusinessException(WTH_003) throw
│
└── 선택 데이터 (AIR_QUALITY, UV_INDEX)
    ├── provider 실패 → 해당 필드 null 반환
    └── 프론트엔드에서 graceful degradation 처리
```

### 타임아웃 설정

- 각 Provider API 호출: 5초 타임아웃
- 전체 aggregation: 10초 타임아웃
- 캐시 조회: 1초 타임아웃 (캐시 장애 시 API 직접 호출)

---

## 9. MSA 전환 포인트

### 현재 구조의 MSA 장점

| 항목 | 설명 |
|------|------|
| DB 비의존 | weather 도메인은 PostgreSQL 미사용, Redis만 사용 |
| 도메인 격리 | 다른 도메인과 Entity 참조 없음, ID만 사용 |
| 자체 완결 | controller → service → provider 체인이 독립적 |
| 설정 분리 | weather.* 설정이 별도 namespace |

### 분리 시 작업

```
현재 모놀리스                         MSA
─────────────                      ─────
domain/weather/ ──────────────→    weather-service (Spring Boot)
domain/outfit/  ──────────────→    weather-service에 포함
global/common/  ──────────────→    onuot-common (shared library)
global/config/RestClientConfig ──→ weather-service 내부 config
global/config/RedisConfig ────────→ weather-service 내부 config
```

### 서비스 간 통신 (MSA 전환 후)

- **member-service → weather-service**: 사용자 위치 기반 날씨 조회 (OpenFeign / gRPC)
- **calendar-service → weather-service**: 캘린더 날짜별 날씨 정보 (OpenFeign / gRPC)
- **outfit 추천**: weather-service 내부에서 직접 호출 (같은 서비스)

---

## 10. 구현 순서

### Phase 1: Foundation
- [ ] `model/` 패키지 — WeatherCondition, CurrentWeather, HourlyForecast, DailyForecast, AirQuality, WeatherLocation, NormalizedWeatherData
- [ ] `provider/` — WeatherProvider 인터페이스, WeatherProviderType, WeatherDataCapability enum
- [ ] `config/WeatherProviderConfig.java` — @ConfigurationProperties
- [ ] `service/WeatherCacheService.java` — Redis 캐시
- [ ] `dto/WeatherResponse.java` — record 기반 재작성

### Phase 2: KMA Provider
- [ ] `kma/KmaGridConverter.java` — 위경도 → 격자 변환
- [ ] `kma/dto/` — 기상청 raw response DTO
- [ ] `kma/KmaApiClient.java` — RestClient로 기상청 API 호출
- [ ] `kma/KmaWeatherProvider.java` — WeatherProvider 구현

### Phase 3: AirKorea Provider
- [ ] `airkorea/dto/AirKoreaResponse.java`
- [ ] `airkorea/AirKoreaApiClient.java`
- [ ] `airkorea/AirKoreaWeatherProvider.java`

### Phase 4: OpenWeatherMap Provider
- [ ] `openweathermap/dto/OwmOneCallResponse.java`
- [ ] `openweathermap/OwmApiClient.java`
- [ ] `openweathermap/OwmWeatherProvider.java`

### Phase 5: Aggregation + Controller
- [ ] `service/WeatherAggregationService.java` — 기존 WeatherService 대체
- [ ] `controller/WeatherController.java` 수정
- [ ] 기존 `WeatherApiClient.java`, `WeatherService.java` 삭제
- [ ] `application.yml` 설정 추가
- [ ] `ErrorCode.java`에 WTH_002, WTH_003 추가

### Phase 6: 문서 업데이트
- [ ] `ARCHITECTURE.md`에 날씨 Provider 아키텍처 참조 추가
