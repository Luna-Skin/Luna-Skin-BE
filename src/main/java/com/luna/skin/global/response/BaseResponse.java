package com.luna.skin.global.response;


import com.luna.skin.global.exception.ErrorCode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Schema(description = "공통 API 응답")
public class BaseResponse<T> {

    @Schema(description = "요청 성공 여부", example = "true")
    private final boolean success;

    @Schema(description = "응답 코드", example = "COMMON_200")
    private final String code;

    @Schema(description = "응답 메시지", example = "요청에 성공했습니다.")
    private final String message;

    @Schema(description = "응답 데이터")
    private final T data;

    public static <T> BaseResponse<T> success(T data) {
        return new BaseResponse<>(true, "COMMON_200", "요청에 성공했습니다.", data);
    }

    public static <T> BaseResponse<T> success(T data, String message) {
        return new BaseResponse<>(true, "COMMON_200", message, data);
    }

    public static <T> BaseResponse<T> success(String code, String message, T data) {
        return new BaseResponse<>(true, code, message, data);
    }

    public static BaseResponse<Object> fail(ErrorCode errorCode) {
        return new BaseResponse<>(false, errorCode.getCode(), errorCode.getMessage(), null);
    }

    public static BaseResponse<Object> fail(ErrorCode errorCode, Object data) {
        return new BaseResponse<>(false, errorCode.getCode(), errorCode.getMessage(), data);
    }
}
