package com.luna.skin.domain.analysis.repository;

import com.luna.skin.domain.analysis.entity.AiAnalysis;
import com.luna.skin.domain.analysis.entity.DetailedSkinAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DetailedSkinAnalysisRepository extends JpaRepository<DetailedSkinAnalysis, Long> {


    // 분석에 맞는 상세 지표를 조회
    Optional<DetailedSkinAnalysis> findByAiAnalysis(AiAnalysis aiAnalysis);
}
