# Onuot API - Architecture

## 프로젝트 개요

날씨 정보 조회, 날씨 기반 옷 추천, 캘린더 기능을 제공하는 REST API 서버.
현재 모놀리식 구조이며, 각 바운디드 컨텍스트가 독립 마이크로서비스로 추출될 수 있도록 설계.

## 기술 스택

| 구분 | 기술 |
|------|------|
| Framework | Spring Boot 4.0.4 |
| Language | Java 25 |
| Build | Gradle 9.4 |
| Database | PostgreSQL |
| Cache | Redis |
| Auth | JWT (Access/Refresh Token) |
| API Client | RestClient (Spring 6) |
| Frontend | React (별도 레포) |

---

## 아키텍처 원칙

### DDD 레이어드 아키텍처

각 바운디드 컨텍스트는 다음 레이어 구조를 따른다:

```
api/            ─── HTTP 진입점 (Controller)
    │
application/   ─── 유스케이스 오케스트레이션 (Service, DTO)
    │
domain/        ─── 핵심 도메인 (Entity, Repository interface, Value Object)
    │
infrastructure/ ─ 기술 구현체 (외부 API 어댑터, 설정) — weather만 해당
```

**의존성 방향**: `api → application → domain ← infrastructure`

- `domain/`: 외부 기술에 의존하지 않음 (`global.common.BaseEntity` 제외)
- `application/`: `domain`만 import, `infrastructure` import 금지
- `infrastructure/`: `domain`만 import
- `api/`: `application`만 import
- **컨텍스트 간 직접 import 금지** — ID 참조만 허용

### MSA 대비 설계

각 최상위 패키지(`member/`, `calendar/`, `outfit/`, `weather/`)가 미래 마이크로서비스 단위:
1. 해당 컨텍스트 패키지 전체를 새 Spring Boot 프로젝트로 복사
2. 패키지 prefix: `com.onuot.api.{context}` → `com.onuot.{context}`
3. `global/` → `onuot-common` 라이브러리로 추출 후 공유

---

## 패키지 구조

`global/`은 기술적 공통 관심사(Cross-cutting concerns), `modules/`는 바운디드 컨텍스트 묶음으로 명확히 분리된다.

```
com.onuot.api
├── global/                             # 공통 모듈 (future: onuot-common)
│   ├── config/
│   │   ├── SecurityConfig
│   │   ├── RedisConfig
│   │   ├── RestClientConfig
│   │   ├── CorsConfig
│   │   └── JpaAuditingConfig
│   ├── auth/
│   │   ├── jwt/
│   │   │   ├── JwtTokenProvider
│   │   │   └── JwtAuthenticationFilter
│   │   └── dto/TokenResponse
│   ├── exception/
│   │   ├── GlobalExceptionHandler
│   │   ├── ErrorCode
│   │   └── BusinessException
│   └── common/
│       ├── BaseEntity
│       └── ApiResponse
│
└── modules/                            # 바운디드 컨텍스트 (각각 future: 독립 서비스)
    ├── member/                         # future: member-service
    │   ├── domain/
    │   │   ├── Member.java             # JPA @Entity
    │   │   └── MemberRepository.java   # Spring Data JPA
    │   ├── application/
    │   │   ├── MemberService.java
    │   │   └── dto/
    │   │       ├── SignUpRequest.java
    │   │       ├── LoginRequest.java
    │   │       └── MemberResponse.java
    │   └── api/
    │       └── MemberController.java
    │
    ├── calendar/                       # future: calendar-service
    │   ├── domain/
    │   │   ├── CalendarEvent.java
    │   │   └── CalendarEventRepository.java
    │   ├── application/
    │   │   ├── CalendarService.java
    │   │   └── dto/
    │   │       ├── CalendarEventRequest.java
    │   │       └── CalendarEventResponse.java
    │   └── api/
    │       └── CalendarController.java
    │
    ├── outfit/                         # future: weather-service 포함
    │   ├── domain/
    │   │   ├── OutfitRecommendation.java
    │   │   └── OutfitRecommendationRepository.java
    │   ├── application/
    │   │   ├── OutfitService.java
    │   │   └── dto/OutfitResponse.java
    │   └── api/
    │       └── OutfitController.java
    │
    └── weather/                        # future: weather-service
        ├── domain/                     # 도메인 핵심 — 외부 기술 의존 없음
        │   ├── WeatherProvider.java    # outbound port interface
        │   ├── WeatherProviderType.java
        │   ├── WeatherDataCapability.java
        │   └── model/
        │       ├── NormalizedWeatherData.java
        │       ├── CurrentWeather.java
        │       ├── HourlyForecast.java
        │       ├── DailyForecast.java
        │       ├── AirQuality.java
        │       ├── WeatherLocation.java
        │       └── WeatherCondition.java
        ├── application/
        │   ├── WeatherAggregationService.java
        │   ├── WeatherCacheService.java
        │   └── dto/
        │       ├── WeatherRequest.java
        │       └── WeatherResponse.java
        ├── infrastructure/             # 기술 구현체 — adapters
        │   ├── config/
        │   │   └── WeatherProviderConfig.java
        │   └── provider/
        │       ├── kma/
        │       │   ├── KmaWeatherProvider.java
        │       │   ├── KmaApiClient.java
        │       │   ├── KmaGridConverter.java
        │       │   └── dto/KmaApiResponse.java
        │       ├── airkorea/
        │       │   ├── AirKoreaWeatherProvider.java
        │       │   ├── AirKoreaApiClient.java
        │       │   └── dto/
        │       │       ├── AirKoreaResponse.java
        │       │       └── AirKoreaNearbyStationResponse.java
        │       └── openweathermap/
        │           ├── OwmWeatherProvider.java
        │           ├── OwmApiClient.java
        │           └── dto/OwmOneCallResponse.java
        └── api/
            └── WeatherController.java
```

---

## API 엔드포인트

### 인증 (공개)
| Method | URL | 설명 |
|--------|-----|------|
| POST | `/api/members/signup` | 회원가입 |
| POST | `/api/auth/login` | 로그인 → JWT 발급 |

### 회원 (인증 필요)
| Method | URL | 설명 |
|--------|-----|------|
| GET | `/api/members/me` | 내 정보 조회 |

### 날씨 (인증 필요)
| Method | URL | 설명 |
|--------|-----|------|
| GET | `/api/weather?latitude={lat}&longitude={lon}` | 날씨 조회 |

### 옷 추천 (인증 필요)
| Method | URL | 설명 |
|--------|-----|------|
| GET | `/api/outfits/recommend?temperature={temp}` | 기온 기반 옷 추천 |

### 캘린더 (인증 필요)
| Method | URL | 설명 |
|--------|-----|------|
| POST | `/api/calendar/events` | 일정 생성 |
| GET | `/api/calendar/events?startDate={}&endDate={}` | 기간별 일정 조회 |
| PUT | `/api/calendar/events/{eventId}` | 일정 수정 |
| DELETE | `/api/calendar/events/{eventId}` | 일정 삭제 |

---

## 공통 응답 포맷

```json
{
  "success": true,
  "data": { ... },
  "message": null
}
```

---

## MSA 전환 계획

### 서비스 분리 단위

```
[API Gateway]
    ├── member-service     ← modules/member/ + global.auth
    ├── weather-service    ← modules/weather/ + modules/outfit/
    ├── calendar-service   ← modules/calendar/
    └── chat-service       ← 별도 WebFlux 프로젝트 (신규)
```

### 공통 모듈 (`onuot-common`으로 추출)
- `ApiResponse`, `BaseEntity`, `BusinessException`, `ErrorCode`
- `JwtTokenProvider`, `JwtAuthenticationFilter`

### 서비스별 유지 (추출하지 않음)
- `SecurityConfig`, `CorsConfig`, `JpaAuditingConfig`, `RedisConfig`, `RestClientConfig`

### 서비스 간 통신
- **동기**: Spring 6 `@HttpExchange` + Resilience4j circuit breaker
- **비동기**: Kafka 이벤트 (채팅 서비스 연동 시)

---

## 날씨 Provider 아키텍처

날씨 도메인은 멀티 Provider 패턴을 사용. 상세 내용은 [WEATHER_ARCHITECTURE.md](./WEATHER_ARCHITECTURE.md) 참조.

### Provider별 Capability

| Provider | CURRENT | HOURLY | DAILY | AIR_QUALITY | UV_INDEX |
|----------|---------|--------|-------|-------------|----------|
| KMA (기상청) | ✓ | ✓ | ✓ | - | - |
| AirKorea (에어코리아) | - | - | - | ✓ | - |
| OpenWeatherMap | ✓ | ✓ | ✓ | ✓ | ✓ |

### 기본 전략 (application.yml)
```yaml
weather.strategy:
  current: KMA
  hourly-forecast: KMA
  daily-forecast: KMA
  air-quality: AIRKOREA
  uv-index: OPENWEATHERMAP
  fallback: OPENWEATHERMAP
```

---

## 환경 변수

| 변수명 | 설명 |
|--------|------|
| `DB_USERNAME` | PostgreSQL 사용자명 |
| `DB_PASSWORD` | PostgreSQL 비밀번호 |
| `REDIS_HOST` | Redis 호스트 |
| `REDIS_PORT` | Redis 포트 |
| `JWT_SECRET` | JWT 서명 키 (256비트 이상) |
| `KMA_API_BASE_URL` | 기상청 API URL |
| `KMA_API_SERVICE_KEY` | 기상청 API 서비스 키 |
| `AIRKOREA_API_BASE_URL` | 에어코리아 API URL |
| `AIRKOREA_API_SERVICE_KEY` | 에어코리아 API 서비스 키 |
| `OWM_API_KEY` | OpenWeatherMap API 키 |
