package com.luna.skin.domain.analysis.controller;

import com.luna.skin.domain.analysis.dto.response.CycleDetailResponse;
import com.luna.skin.domain.analysis.dto.response.LifestyleInsightResponse;
import com.luna.skin.domain.analysis.dto.response.TroubleTimelineResponse;
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

  @Operation(summary = "트러블 지수 타임라인", description = "누적 트러블 점수를 생리 D-14 ~ D+14 기간동안 분석하는 API")
  @GetMapping("/trouble-timeline")
  public ResponseEntity<BaseResponse<TroubleTimelineResponse>> getTroubleTimeline() {
    Long userId = currentUserProvider.getCurrentUserId();
    return ResponseEntity.ok(BaseResponse.success(
        insightService.getTroubleTimeline(userId)));
  }

  @Operation(summary = "주기 단계 별 피부 세부 지표", description = "누적 피부 점수를 주기 단계별로 비교하는 API")
  @GetMapping("/cycle-detail")
  public ResponseEntity<BaseResponse<CycleDetailResponse>> getCycleDetail() {
    Long userId = currentUserProvider.getCurrentUserId();
    return  ResponseEntity.ok(BaseResponse.success(insightService.getCycleDetail(userId)));
  }

  @Operation(summary = "생활습관 영향 분석", description = "생활습관 기반으로 피부 영향 분석하는 API")
  @GetMapping("/lifestyle")
  public ResponseEntity<BaseResponse<LifestyleInsightResponse>> getLifestyleInsight() {
    Long userId = currentUserProvider.getCurrentUserId();
    return ResponseEntity.ok(BaseResponse.success(
        insightService.getLifestyleInsight(userId)));
  }
}
