# Onuot API - Architecture

## 프로젝트 개요

날씨 정보 조회, 날씨 기반 옷 추천, 캘린더 기능을 제공하는 REST API 서버.
현재 모놀리식 구조이며, 추후 MSA로 전환 예정.

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

## 패키지 구조

```
com.onuot.api
├── global/                          # 공통 모듈 (추후 common-lib으로 분리)
│   ├── config/                      # 설정 클래스
│   │   ├── SecurityConfig           # Spring Security + JWT 설정
│   │   ├── RedisConfig              # Redis 설정
│   │   ├── RestClientConfig         # 외부 API 호출용 RestClient
│   │   ├── CorsConfig               # CORS 설정 (React 연동)
│   │   └── JpaAuditingConfig        # JPA Auditing 활성화
│   ├── auth/                        # 인증/인가
│   │   ├── jwt/
│   │   │   ├── JwtTokenProvider     # JWT 토큰 생성/검증
│   │   │   └── JwtAuthenticationFilter  # 요청별 JWT 검증 필터
│   │   └── dto/
│   │       └── TokenResponse        # 토큰 응답 DTO
│   ├── exception/                   # 예외 처리
│   │   ├── GlobalExceptionHandler   # 전역 예외 핸들러
│   │   ├── ErrorCode                # 에러코드 enum
│   │   └── BusinessException        # 비즈니스 예외
│   └── common/                      # 공통 클래스
│       ├── BaseEntity               # createdAt, updatedAt
│       └── ApiResponse              # 공통 API 응답 포맷
│
├── domain/
│   ├── member/                      # 회원 도메인
│   │   ├── controller/              # 회원가입, 로그인, 내 정보 조회
│   │   ├── service/                 # 비즈니스 로직
│   │   ├── repository/              # JPA Repository
│   │   ├── entity/                  # Member 엔티티
│   │   └── dto/                     # 요청/응답 DTO
│   │
│   ├── weather/                     # 날씨 도메인
│   │   ├── controller/              # 날씨 조회 API
│   │   ├── service/                 # 날씨 서비스 (캐싱 포함)
│   │   ├── client/                  # 기상청 외부 API 클라이언트
│   │   └── dto/                     # 날씨 DTO
│   │
│   ├── outfit/                      # 옷 추천 도메인
│   │   ├── controller/              # 옷 추천 API
│   │   ├── service/                 # 기온 기반 추천 로직
│   │   ├── repository/              # 추천 데이터 조회
│   │   ├── entity/                  # OutfitRecommendation 엔티티
│   │   └── dto/                     # 추천 응답 DTO
│   │
│   └── calendar/                    # 캘린더 도메인
│       ├── controller/              # 일정 CRUD API
│       ├── service/                 # 일정 관리 로직
│       ├── repository/              # 일정 데이터 조회
│       ├── entity/                  # CalendarEvent 엔티티
│       └── dto/                     # 일정 요청/응답 DTO
│
└── OnuotApiApplication.java
```

## 설계 원칙

### 도메인 간 의존성 규칙 (MSA 전환 대비)
- **도메인 간 직접 Entity 참조 금지**: 다른 도메인의 Entity를 직접 참조하지 않고, ID(Long)만 보관
  - 예: `CalendarEvent.memberId` (Long) - Member 엔티티를 직접 참조하지 않음
- **도메인 간 호출은 서비스 인터페이스를 통해서만**: 추후 Feign Client / gRPC로 교체 가능
- **global 패키지는 도메인 비의존적**: 어떤 도메인도 import하지 않음

### 공통 응답 포맷
```json
{
  "success": true,
  "data": { ... },
  "message": null
}
```

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

## MSA 전환 계획

### 서비스 분리 단위

```
[API Gateway]
    ├── member-service     ← domain.member + global.auth
    ├── weather-service    ← domain.weather + domain.outfit
    ├── calendar-service   ← domain.calendar
    └── chat-service       ← 별도 WebFlux 프로젝트 (신규)
```

### 공통 모듈
- `global.*` → **onuot-common** 라이브러리로 추출
  - ApiResponse, BaseEntity, ErrorCode, BusinessException
  - JWT 관련 공통 코드

### 서비스 간 통신
- **동기**: Spring Cloud OpenFeign / gRPC
- **비동기**: Apache Kafka / RabbitMQ (채팅 서비스 연동 시)

### 채팅 서비스 (WebFlux)
- 별도 Spring WebFlux 프로젝트로 생성
- WebSocket + Reactive 기반
- member-service와 연동하여 사용자 인증
- 메시지 브로커(Kafka/RabbitMQ)를 통해 다른 서비스와 비동기 통신

## 환경 설정

### 필수 환경 변수
| 변수명 | 설명 | 기본값 |
|--------|------|--------|
| `DB_USERNAME` | PostgreSQL 사용자명 | onuot |
| `DB_PASSWORD` | PostgreSQL 비밀번호 | onuot |
| `REDIS_HOST` | Redis 호스트 | localhost |
| `REDIS_PORT` | Redis 포트 | 6379 |
| `JWT_SECRET` | JWT 서명 키 (256비트 이상) | 개발용 기본값 |
| `WEATHER_API_BASE_URL` | 기상청 API URL | - |
| `WEATHER_API_SERVICE_KEY` | 기상청 API 키 | - |
