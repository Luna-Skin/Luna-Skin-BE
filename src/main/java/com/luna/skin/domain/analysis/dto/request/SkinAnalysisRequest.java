package com.luna.skin.domain.analysis.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.luna.skin.domain.skin.enums.DietType;
import com.luna.skin.domain.skin.enums.SkinStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Schema(description = "피부 분석 요청")
public class SkinAnalysisRequest {

  @Schema(description = "업로드된 사진 URL (정면)", example = "/files/analysis/uuid.png")
  @JsonProperty("imageUrl")
  @NotBlank(message = "정면 사진은 필수입니다.")
  private String imageUrl;

  @Schema(description = "왼쪽 측면 사진 URL", nullable = true)
  @JsonProperty("leftImageUrl")
  private String leftImageUrl;

  @Schema(description = "오른쪽 측면 사진 URL", nullable = true)
  @JsonProperty("rightImageUrl")
  private String rightImageUrl;

  @Schema(description = "수면 시간 (시간)", example = "7.5")
  private Double sleepTime;

  @Schema(description = "식단 유형 (복수 선택)", example = "[\"DAIRY\", \"CAFFEINE\"]")
  private List<DietType> dietType;

  @Schema(description = "수분 섭취 (L)", example = "0.5")
  private Double waterIntake;

  @Schema(description = "운동 시간 (분)", example = "90")
  private Integer exerciseTime;

  @Schema(description = "피부 상태", example = "OILY")
  private SkinStatus skinStatus;

}
