package com.luna.skin.domain.user.exception;

import com.luna.skin.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum UserErrorCode implements ErrorCode {

    USER_NOT_FOUND("USER_404", "사용자를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    INVALID_SKIN_TYPE("USER_400_1", "유효하지 않은 피부 타입입니다.", HttpStatus.BAD_REQUEST),
    INVALID_SKIN_CONCERN("USER_400_2", "유효하지 않은 피부 고민입니다.", HttpStatus.BAD_REQUEST);

    private final String code;
    private final String message;
    private final HttpStatus status;
}
