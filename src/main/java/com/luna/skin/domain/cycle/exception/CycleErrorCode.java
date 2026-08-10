package com.luna.skin.domain.cycle.exception;

import com.luna.skin.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum CycleErrorCode implements ErrorCode {
    CYCLE_NOT_FOUND("CYCLE_404", "생리 주기 정보를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    INVALID_CYCLE_LENGTH("CYCLE_400", "생리 주기가 너무 짧습니다.", HttpStatus.BAD_REQUEST),
    INVALID_START_DATE("CYCLE_400_2", "유효하지 않은 생리 시작일입니다.", HttpStatus.BAD_REQUEST),
    START_AND_END_DATE_CANNOT_BE_SAME("CYCLE_400_3", "시작일과 종료일은 같을 수 없습니다.", HttpStatus.BAD_REQUEST),
    INVALID_CYCLE_PERIOD_RANGE("CYCLE_400_4", "생리 기간은 생리 주기보다 길 수 없습니다.", HttpStatus.BAD_REQUEST);

    private final String code;
    private final String message;
    private final HttpStatus status;
}
