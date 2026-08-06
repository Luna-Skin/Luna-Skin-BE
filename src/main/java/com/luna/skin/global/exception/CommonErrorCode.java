package com.luna.skin.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum CommonErrorCode implements ErrorCode {
    INVALID_REQUEST("COMMON_400", "잘못된 요청입니다.", HttpStatus.BAD_REQUEST),
    UNAUTHORIZED("AUTH_401", "로그인이 필요합니다.", HttpStatus.UNAUTHORIZED),
    FORBIDDEN("AUTH_403", "접근할 수 없는 항목입니다.", HttpStatus.FORBIDDEN),
    NOT_FOUND("COMMON_404", "정보를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    ALREADY_PROCESSED("COMMON_409", "이미 처리된 요청입니다.", HttpStatus.CONFLICT),
    INTERNAL_SERVER_ERROR("COMMON_500", "서버 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
    FILE_TOO_LARGE("COMMON_413", "파일 크기는 10MB를 초과할 수 없습니다.", HttpStatus.BAD_REQUEST);

    private final String code;
    private final String message;
    private final HttpStatus status;
}