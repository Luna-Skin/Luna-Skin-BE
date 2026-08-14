package com.luna.skin.domain.analysis.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "피부 분석 응답")
public class SkinAnalysisResponse {

  @Schema(description = "분석 ID", example = "1")
  private Long analysisId;

  @Schema(description = "정면 사진 URL", nullable = true)
  private String imageUrl;

  @Schema(description = "왼쪽 측면 사진 URL", nullable = true)
  private String leftImageUrl;

  @Schema(description = "오른쪽 측면 사진 URL", nullable = true)
  private String rightImageUrl;

  @Schema(description = "분석 날짜", example = "2026-08-08")
  private String date;

  @Schema(description = "종합 점수 (0~100)", example = "72")
  private Integer overallScore;

  @Schema(description = "피부 상태 (좋음/보통/나쁨)", example = "보통")
  private String skinStatus;

  @Schema(description = "생리 주기 단계", example = "LUTEAL")
  private String cyclePhase;

  @Schema(description = "주기 단계 코멘트")
  private String phaseComment;

  @Schema(description = "AI 분석 코멘트")
  private String aiComment;

  @Schema(description = "세부 피부 지표")
  private DetailedMetrics detailedMetrics;

  @Getter
  @Builder
  @Schema(description = "세부 피부 지표")
  public static class DetailedMetrics {

    @Schema(description = "트러블 점수", example = "45")
    private Integer trouble;

    @Schema(description = "유분 점수", example = "90")
    private Integer sebum;

    @Schema(description = "칙칙함 점수", example = "65")
    private Integer dullness;

    @Schema(description = "수분 점수", example = "20")
    private Integer moisture;

    @Schema(description = "탄력 점수", example = "50")
    private Integer elasticity;
  }
}