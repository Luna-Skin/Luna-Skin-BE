package com.luna.skin.global.exception;


import com.luna.skin.global.response.BaseResponse;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CustomException.class)
    public ResponseEntity<BaseResponse<Object>> handleCustomException(CustomException e) {
        ErrorCode errorCode = e.getErrorCode();
        log.warn("CustomException 발생: {} - {}", errorCode.getCode(), errorCode.getMessage());
        return ResponseEntity
                .status(errorCode.getStatus())
                .body(BaseResponse.fail(errorCode, e.getData()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<BaseResponse<Object>> handleMethodArgumentNotValid(MethodArgumentNotValidException e) {
        List<FieldErrorDetail> fieldErrors = e.getBindingResult().getFieldErrors().stream()
                .map(FieldErrorDetail::from)
                .toList();
        log.warn("ValidationException 발생: {}", fieldErrors);

        return ResponseEntity
                .status(CommonErrorCode.INVALID_REQUEST.getStatus())
                .body(BaseResponse.fail(CommonErrorCode.INVALID_REQUEST, fieldErrors));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<BaseResponse<Object>> handleConstraintViolation(ConstraintViolationException e) {
        List<FieldErrorDetail> fieldErrors = e.getConstraintViolations().stream()
                .map(v -> {
                    String field = v.getPropertyPath().toString();
                    field = field.contains(".") ? field.substring(field.lastIndexOf('.') + 1) : field;
                    return new FieldErrorDetail(field, v.getMessage());
                })
                .toList();
        log.warn("ConstraintViolationException 발생: {}", fieldErrors);
        return ResponseEntity
                .status(CommonErrorCode.INVALID_REQUEST.getStatus())
                .body(BaseResponse.fail(CommonErrorCode.INVALID_REQUEST, fieldErrors));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<BaseResponse<Object>> handleException(Exception e) {
        log.error("UnhandledException 발생: {}", e.getMessage(), e);
        return ResponseEntity
                .status(CommonErrorCode.INTERNAL_SERVER_ERROR.getStatus())
                .body(BaseResponse.fail(CommonErrorCode.INTERNAL_SERVER_ERROR));
    }

    private record FieldErrorDetail(String field, String reason) {
        static FieldErrorDetail from(FieldError fieldError) {
            return new FieldErrorDetail(fieldError.getField(), fieldError.getDefaultMessage());
        }
    }
}
