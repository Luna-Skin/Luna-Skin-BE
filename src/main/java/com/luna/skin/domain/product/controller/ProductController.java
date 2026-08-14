package com.luna.skin.domain.product.controller;

import com.luna.skin.domain.product.dto.response.ProductRecommendResponse;
import com.luna.skin.domain.product.service.ProductService;
import com.luna.skin.global.response.BaseResponse;
import com.luna.skin.global.security.CurrentUserProvider;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Product", description = "제품 추천 API")
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

  private final ProductService productService;
  private final CurrentUserProvider currentUserProvider;

  @Operation(summary = "피부 맞춤 제품 추천", description = "특정 날짜의 피부 분석 결과를 기반으로 제품 2개를 추천하는 API")
  @GetMapping("/recommend")
  public ResponseEntity<BaseResponse<List<ProductRecommendResponse>>> recommend(
      @Parameter(description = "분석 날짜", example = "2026-08-14") @RequestParam LocalDate date) {
    Long userId = currentUserProvider.getCurrentUserId();
    return ResponseEntity.ok(BaseResponse.success(productService.recommend(userId, date)));
  }
}
