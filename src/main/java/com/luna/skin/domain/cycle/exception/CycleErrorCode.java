package com.luna.skin.domain.cycle.exception;

import com.luna.skin.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum CycleErrorCode implements ErrorCode {
    CYCLE_NOT_FOUND("CYCLE_404", "생리 주기 정보를 찾을 수 없습니다.", HttpStatus.NOT_FOUND);

    private final String code;
    private final String message;
    private final HttpStatus status;
}
