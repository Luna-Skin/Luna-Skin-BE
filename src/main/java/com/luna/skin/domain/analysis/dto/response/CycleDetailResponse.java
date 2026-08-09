package com.luna.skin.domain.analysis.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Builder;
import lombok.Getter;
import org.springframework.data.geo.Metrics;

@Getter
@Builder
@Schema(description = "주기별 피부 응답")
public class CycleDetailResponse {
  @Schema(description = "단계별 피부 데이터")
  private List<PhaseDetail> phases;

  @Getter
  @Builder
  public static class PhaseDetail {

    @Schema(description = "주기 단계", example = "MENSTRUATION")
    private String phase;

    @Schema(description = "단계 라벨", example = "생리기")
    private String label;

    @Schema(description = "세부 피부 지표 평균")
    private Metrics metrics;
  }

  @Getter
  @Builder
  public static class Metrics {
    private Integer trouble;
    private Integer sebum;
    private Integer dullness;
    private Integer moisture;
    private Integer elasticity;
  }
}
