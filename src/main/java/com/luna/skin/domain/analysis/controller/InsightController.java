package com.luna.skin.domain.analysis.controller;

import com.luna.skin.domain.analysis.dto.response.LifestyleInsightResponse;
import com.luna.skin.domain.analysis.service.InsightService;
import com.luna.skin.global.response.BaseResponse;
import com.luna.skin.global.security.CurrentUserProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Insight", description = "인사이트 API")
@RestController
@RequestMapping("/api/analyses")
@RequiredArgsConstructor
public class InsightController {

  private final InsightService insightService;
  private final CurrentUserProvider currentUserProvider;

  @Operation(summary = "생활습관 영향 분석(인사이트)", description = "생활습관 기반으로 피부 영향 분석하는 API")
  @GetMapping("/lifestyle")
  public ResponseEntity<BaseResponse<LifestyleInsightResponse>> getLifestyleInsight() {
    return ResponseEntity.ok(BaseResponse.success(
        insightService.getLifestyleInsight(currentUserProvider.getCurrentUserId())));
  }

}
