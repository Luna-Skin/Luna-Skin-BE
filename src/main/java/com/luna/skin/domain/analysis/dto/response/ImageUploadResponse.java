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
  private String imageUrl;
}
