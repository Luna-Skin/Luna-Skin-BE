package com.luna.skin.infra.openai;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class OpenAiSkinAnalysisResult {

  @JsonProperty("overall_score")
  private Integer overallScore;

  @JsonProperty("trouble")
  private Integer trouble;

  @JsonProperty("sebum")
  private Integer sebum;

  @JsonProperty("dullness")
  private Integer dullness;

  @JsonProperty("moisture")
  private Integer moisture;

  @JsonProperty("elasticity")
  private Integer elasticity;

  @JsonProperty("ai_comment")
  private String aiComment;

  @JsonProperty("phase_comment")
  private String phaseComment;

  public OpenAiSkinAnalysisResult withOverallScore(int overallScore) {
    this.overallScore = overallScore;
    return this;
  }
}
