package com.luna.skin.domain.product.dto.response;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "추천 제품 응답")
public class ProductRecommendResponse {

  @Schema(description = "제품 ID", example = "1")
  private Long productId;

  @Schema(description = "제품명", example = "비자 트러블 토너")
  private String prodName;

  @Schema(description = "효과", example = "트러블 완화")
  private String ingredient;

  @Schema(description = "구매 링크")
  private String purchaseUrl;
}