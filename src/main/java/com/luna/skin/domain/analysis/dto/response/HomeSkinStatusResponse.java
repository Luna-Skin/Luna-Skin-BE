package com.luna.skin.domain.analysis.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "홈 화면 오늘의 피부 상태 응답")
public class HomeSkinStatusResponse {

  @Schema(description = "피부 상태 (모름/나쁨/보통/좋음)", example = "보통")
  private String skinStatus;

  @Schema(description = "AI 코멘트 (기록 없으면 null)", nullable = true)
  private String aiComment;
}