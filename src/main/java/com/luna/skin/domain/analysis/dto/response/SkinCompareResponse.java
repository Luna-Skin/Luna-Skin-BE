package com.luna.skin.domain.analysis.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter @Builder
@Schema(description = "피부 비교 응답")
public class SkinCompareResponse {

  @Schema(description = "기준 날짜 A 분석 결과")
  private DaySnapshot dateA;

  @Schema(description = "비교 날짜 B 분석 결과")
  private DaySnapshot dateB;

  @Schema(description = "항목별 변화 방향")
  private MetricChanges changes;

  @Schema(description = "두 날짜를 종합한 AI 분석 코멘트", nullable = true)
  private String aiComment;

  @Getter @Builder
  @Schema(description = "날짜별 피부 스냅샷")
  public static class DaySnapshot {
    @Schema(description = "날짜", example = "2026-08-01")
    private String date;

    @Schema(description = "피부 사진 URL", nullable = true)
    private String imageUrl;

    @Schema(description = "종합 점수", example = "56")
    private int overallScore;

    @Schema(description = "피부 상태", example = "나쁨")
    private String skinStatus;

    @Schema(description = "항목별 점수")
    private Metrics metrics;
  }

  @Getter @Builder
  @Schema(description = "항목별 점수")
  public static class Metrics {
    @Schema(description = "트러블 점수", example = "90")
    private int trouble;

    @Schema(description = "유분 점수", example = "45")
    private int sebum;

    @Schema(description = "칙칙함 점수", example = "35")
    private int dullness;

    @Schema(description = "수분 점수", example = "60")
    private int moisture;

    @Schema(description = "탄력 점수", example = "50")
    private int elasticity;
  }

  @Getter
  @Builder
  @Schema(description = "항목별 변화 방향")
  public static class MetricChanges {
    @Schema(description = "트러블 변화", example = "IMPROVED", allowableValues = {"IMPROVED", "SIMILAR", "WORSENED"})
    private String trouble;

    @Schema(description = "유분 변화", example = "WORSENED", allowableValues = {"IMPROVED", "SIMILAR", "WORSENED"})
    private String sebum;

    @Schema(description = "칙칙함 변화", example = "SIMILAR", allowableValues = {"IMPROVED", "SIMILAR", "WORSENED"})
    private String dullness;

    @Schema(description = "수분 변화", example = "IMPROVED", allowableValues = {"IMPROVED", "SIMILAR", "WORSENED"})
    private String moisture;

    @Schema(description = "탄력 변화", example = "SIMILAR", allowableValues = {"IMPROVED", "SIMILAR", "WORSENED"})
    private String elasticity;
  }
}