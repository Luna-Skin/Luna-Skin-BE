package com.luna.skin.domain.chat.exception;

import com.luna.skin.domain.chat.controller.ChatController;
import com.luna.skin.global.exception.CommonErrorCode;
import com.luna.skin.global.response.BaseResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

// GlobalExceptionHandler보다 먼저 평가되어야 하므로 HIGHEST_PRECEDENCE로 지정
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = ChatController.class)
public class ChatRequestExceptionHandler {

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<BaseResponse<Object>> handleHttpMessageNotReadable(HttpMessageNotReadableException e) {
        log.warn("[Chat] 요청 본문을 읽을 수 없습니다: {}", e.getMessage());
        return ResponseEntity
                .status(CommonErrorCode.INVALID_REQUEST.getStatus())
                .body(BaseResponse.fail(CommonErrorCode.INVALID_REQUEST));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<BaseResponse<Object>> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        log.warn("[Chat] 요청 파라미터 타입이 올바르지 않습니다: {} - {}", e.getName(), e.getMessage());
        return ResponseEntity
                .status(CommonErrorCode.INVALID_REQUEST.getStatus())
                .body(BaseResponse.fail(CommonErrorCode.INVALID_REQUEST));
    }
}
