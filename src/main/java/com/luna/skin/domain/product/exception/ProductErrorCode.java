package com.luna.skin.domain.product.exception;

import com.luna.skin.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ProductErrorCode implements ErrorCode {
  PRODUCT_NOT_FOUND("PRODUCT_404", "추천할 제품을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
  RECOMMEND_FAILED("PRODUCT_500", "제품 추천 중 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR);

  private final String code;
  private final String message;
  private final HttpStatus status;
}