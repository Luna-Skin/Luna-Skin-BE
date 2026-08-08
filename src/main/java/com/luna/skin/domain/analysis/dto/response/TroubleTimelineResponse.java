package com.luna.skin.domain.analysis.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "트러블 지수 타임라인 응답")
public class TroubleTimelineResponse {

  @Schema(description = "누적 주기 수")
  private int cycleCount;

  @Schema(description = "트러블 패턴 코멘트")
  private String patternComment;

  @Schema(description = "D-14 ~ D+14 트러블 지수")
  private List<TroublePoint> troubleTimeline;

  @Schema(description = "트러블 집중 구간")
  private PeakRange peakRange;

  @Getter
  @Builder
  public static class TroublePoint {
    @Schema(description = "생리 시작일 기준 일수", example = "-5")
    private int dayFromStart;

    @Schema(description = "트러블 지수", example = "72")
    private double troubleIndex;
  }

  @Getter
  @Builder
  public static class PeakRange {
    @Schema(description = "집중 구간 시작일", example = "-5")
    private int startDay;

    @Schema(description = "집중 구간 종료일", example = "-1")
    private int endDay;

    @Schema(description = "라벨", example = "트러블 집중 구간")
    private String label;
  }
}