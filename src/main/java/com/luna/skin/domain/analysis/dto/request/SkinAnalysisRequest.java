package com.luna.skin.domain.analysis.dto.request;

import com.luna.skin.domain.skin.enums.DietType;
import com.luna.skin.domain.skin.enums.ExerciseTime;
import com.luna.skin.domain.skin.enums.SkinStatus;
import io.swagger.v3.oas.annotations.media.Schema;
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

  @Schema(description = "업로드된 사진 URL", example = "/files/analysis/uuid.png")
  private String imageUrl;

  @Schema(description = "수면 시간 (시간)", example = "7")
  private Integer sleepTime;

  @Schema(description = "수분 섭취 (잔)", example = "8")
  private Integer waterIntake;

  @Schema(description = "식단 유형 (복수 선택)", example = "[\"DAIRY\", \"CAFFEINE\"]")
  private List<DietType> dietType;

  @Schema(description = "운동량", example = "THIRTY_M")
  private ExerciseTime exerciseTime;

  @Schema(description = "피부 상태", example = "OILY")
  private SkinStatus skinStatus;

}
