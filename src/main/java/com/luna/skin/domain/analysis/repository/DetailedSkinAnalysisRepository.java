package com.luna.skin.domain.analysis.repository;

import com.luna.skin.domain.analysis.entity.DetailedSkinAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DetailedSkinAnalysisRepository extends JpaRepository<DetailedSkinAnalysis, Long> {

    Optional<DetailedSkinAnalysis> findByAiAnalysisAnalysisId(Long analysisId);
}
