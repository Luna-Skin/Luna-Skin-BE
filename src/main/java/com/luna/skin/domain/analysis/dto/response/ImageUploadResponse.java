package com.luna.skin.domain.analysis.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
@Schema(description = "이미지 반환 응답")
public class ImageUploadResponse {
  @Schema(description = "정면 사진 URL")
  private String imageUrl;

  @Schema(description = "왼쪽 측면 사진 URL", nullable = true)
  private String leftImageUrl;

  @Schema(description = "오른쪽 측면 사진 URL", nullable = true)
  private String rightImageUrl;
}
