package com.luna.skin.domain.analysis.repository;

import com.luna.skin.domain.analysis.entity.AiAnalysis;
import com.luna.skin.domain.analysis.entity.DetailedSkinAnalysis;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DetailedSkinAnalysisRepository extends JpaRepository<DetailedSkinAnalysis, Long> {
  Optional<DetailedSkinAnalysis> findByAiAnalysis(AiAnalysis aiAnalysis);

  List<DetailedSkinAnalysis> findAllByAiAnalysisIn(List<AiAnalysis> analyses);
}
