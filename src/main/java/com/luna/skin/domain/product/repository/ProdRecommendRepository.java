package com.luna.skin.domain.product.repository;

import com.luna.skin.domain.analysis.entity.AiAnalysis;
import com.luna.skin.domain.product.entity.ProdRecommend;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProdRecommendRepository extends JpaRepository<ProdRecommend, Long> {

    // 이미 저장된 추천 결과가 있는지 조회 (같은 분석에 재요청 시 재사용)
    List<ProdRecommend> findAllByAiAnalysis(AiAnalysis aiAnalysis);
}
