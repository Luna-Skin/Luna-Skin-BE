package com.luna.skin.domain.routine.exception;

import com.luna.skin.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum RoutineErrorCode implements ErrorCode {

    AI_ROUTINE_GENERATION_FAILED("ROUTINE_500", "AI 루틴 생성 및 응답 파싱에 실패했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
    ROUTINE_NOT_FOUND("ROUTINE_404", "해당 날짜의 루틴을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    ROUTINE_ALREADY_EXISTS("ROUTINE_400", "이미 해당 날짜의 루틴이 존재합니다.", HttpStatus.BAD_REQUEST),

    ROUTINE_CONTENT_NOT_FOUND("ROUTINE_404_2", "해당 루틴 항목을 찾을 수 없습니다.", HttpStatus.NOT_FOUND);

    private final String code;
    private final String message;
    private final HttpStatus status;
}