package com.luna.skin.domain.product.service;

import com.luna.skin.domain.analysis.entity.DetailedSkinAnalysis;
import com.luna.skin.domain.analysis.exception.AnalysisErrorCode;
import com.luna.skin.domain.analysis.repository.AiAnalysisRepository;
import com.luna.skin.domain.analysis.repository.DetailedSkinAnalysisRepository;
import com.luna.skin.domain.product.dto.response.ProductRecommendResponse;
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

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

  private final OpenAiAnalysisService openAiAnalysisService;
  private final AiAnalysisRepository aiAnalysisRepository;
  private final DetailedSkinAnalysisRepository detailedSkinAnalysisRepository;
  private final ProductRepository productRepository;

  public List<ProductRecommendResponse> recommend(Long userId, LocalDate date) {
    DetailedSkinAnalysis detail = aiAnalysisRepository
        .findByTodaySkinUserUserIdAndTodaySkinLogDate(userId, date)
        .flatMap(detailedSkinAnalysisRepository::findByAiAnalysis)
        .orElseThrow(() -> new CustomException(AnalysisErrorCode.ANALYSIS_NOT_FOUND));

    List<String> ingredients = openAiAnalysisService.recommendIngredients(
        detail.getTrouble(), detail.getSebum(), detail.getDullness(),
        detail.getMoisture(), detail.getElasticity());

    List<ProductRecommendResponse> result = new ArrayList<>();
    for (String ingredient : ingredients) {
      productRepository.findRandomByIngredient(ingredient)
          .map(p -> ProductRecommendResponse.builder()
              .productId(p.getProductId())
              .prodName(p.getProdName())
              .ingredient(p.getIngredient())
              .purchaseUrl(p.getPurchaseUrl())
              .build())
          .ifPresent(result::add);
      if (result.size() == 2) break;
    }
    return result;
  }
}