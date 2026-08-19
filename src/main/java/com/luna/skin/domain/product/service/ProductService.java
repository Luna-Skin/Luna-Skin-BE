package com.luna.skin.domain.product.service;

import com.luna.skin.domain.analysis.entity.AiAnalysis;
import com.luna.skin.domain.analysis.entity.DetailedSkinAnalysis;
import com.luna.skin.domain.analysis.repository.AiAnalysisRepository;
import com.luna.skin.domain.analysis.repository.DetailedSkinAnalysisRepository;
import com.luna.skin.domain.product.dto.response.ProductRecommendResponse;
import com.luna.skin.domain.product.entity.ProdRecommend;
import com.luna.skin.domain.product.entity.Product;
import com.luna.skin.domain.product.exception.ProductErrorCode;
import com.luna.skin.domain.product.repository.ProdRecommendRepository;
import com.luna.skin.domain.product.repository.ProductRepository;
import com.luna.skin.global.exception.CustomException;
import com.luna.skin.infra.openai.service.OpenAiAnalysisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

  private final OpenAiAnalysisService openAiAnalysisService;
  private final AiAnalysisRepository aiAnalysisRepository;
  private final DetailedSkinAnalysisRepository detailedSkinAnalysisRepository;
  private final ProductRepository productRepository;
  private final ProdRecommendRepository prodRecommendRepository;

  /**
   * 같은 분석(analysis_id)에 대해 최초 한 번만 OpenAI+랜덤으로 추천을 계산하고,
   * 그 결과를 prod_recommend에 저장해둔다. 재요청 시엔 저장된 결과를 그대로 반환해서
   * 새로고침할 때마다 추천 제품이 바뀌지 않게 한다.
   */
  @Transactional
  public List<ProductRecommendResponse> recommend(Long userId, LocalDate date) {
    AiAnalysis aiAnalysis = aiAnalysisRepository
        .findByTodaySkinUserUserIdAndTodaySkinLogDate(userId, date)
        .orElseThrow(() -> new CustomException(ProductErrorCode.PRODUCT_NOT_FOUND));

    List<ProdRecommend> saved = prodRecommendRepository.findAllByAiAnalysis(aiAnalysis);
    if (!saved.isEmpty()) {
      return saved.stream().map(pr -> toResponse(pr.getProduct())).collect(Collectors.toList());
    }

    DetailedSkinAnalysis detail = detailedSkinAnalysisRepository.findByAiAnalysis(aiAnalysis)
        .orElseThrow(() -> new CustomException(ProductErrorCode.PRODUCT_NOT_FOUND));

    List<String> ingredients = openAiAnalysisService.recommendIngredients(
        detail.getTrouble(), detail.getSebum(), detail.getDullness(),
        detail.getMoisture(), detail.getElasticity());

    List<Product> selected = new ArrayList<>();
    for (String ingredient : ingredients) {
      productRepository.findRandomByIngredient(ingredient).ifPresent(selected::add);
      if (selected.size() == 2) break;
    }

    List<ProdRecommend> toSave = selected.stream()
        .map(p -> ProdRecommend.builder().aiAnalysis(aiAnalysis).product(p).build())
        .collect(Collectors.toList());
    prodRecommendRepository.saveAll(toSave);

    return selected.stream().map(this::toResponse).collect(Collectors.toList());
  }

  private ProductRecommendResponse toResponse(Product p) {
    return ProductRecommendResponse.builder()
        .productId(p.getProductId())
        .prodName(p.getProdName())
        .ingredient(p.getIngredient())
        .purchaseUrl(p.getPurchaseUrl())
        .build();
  }
}