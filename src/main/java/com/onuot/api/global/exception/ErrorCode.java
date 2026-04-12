package com.onuot.api.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // Common (CMM)
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "CMM_001", "잘못된 입력값입니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "CMM_002", "서버 내부 오류가 발생했습니다."),

    // Auth (AUTH)
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH_001", "유효하지 않은 토큰입니다."),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH_002", "만료된 토큰입니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "AUTH_003", "인증이 필요합니다."),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "AUTH_004", "접근 권한이 없습니다."),

    // Member (MBR)
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "MBR_001", "이미 존재하는 이메일입니다."),
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "MBR_002", "회원을 찾을 수 없습니다."),
    INVALID_PASSWORD(HttpStatus.BAD_REQUEST, "MBR_003", "비밀번호가 일치하지 않습니다."),

    // Weather (WTH)
    WEATHER_API_ERROR(HttpStatus.SERVICE_UNAVAILABLE, "WTH_001", "날씨 API 호출에 실패했습니다."),
    WEATHER_PROVIDER_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "WTH_002", "날씨 제공자가 응답하지 않습니다."),
    WEATHER_ALL_PROVIDERS_FAILED(HttpStatus.SERVICE_UNAVAILABLE, "WTH_003", "모든 날씨 제공자 호출에 실패했습니다."),

    // Outfit (OFT)
    // 추후 추가

    // Calendar (CAL)
    CALENDAR_EVENT_NOT_FOUND(HttpStatus.NOT_FOUND, "CAL_001", "캘린더 일정을 찾을 수 없습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
