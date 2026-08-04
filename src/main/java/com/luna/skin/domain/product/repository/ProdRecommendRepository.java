package com.luna.skin.domain.product.repository;

import com.luna.skin.domain.product.entity.ProdRecommend;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProdRecommendRepository extends JpaRepository<ProdRecommend, Long> {

    List<ProdRecommend> findByAiAnalysisAnalysisId(Long analysisId);
}
