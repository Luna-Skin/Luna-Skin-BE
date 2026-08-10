package com.luna.skin.domain.analysis.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "생활습관 영향 분석 응답")
public class LifestyleInsightResponse {

  @Schema(description = "생활습관 영향 목록")
  private List<LifestyleFactor> factors;

  @Getter
  @Builder
  @Schema(description = "생활습관 영향 항목")
  public static class LifestyleFactor {

    @Schema(description = "조건 설명", example = "수면 6시간 미만")
    private String condition;

    @Schema(description = "영향 유형 (positive/negative)", example = "negative")
    private String impactType;

    @Schema(description = "영향 설명", example = "트러블 ↑")
    private String impactLabel;
  }
}
