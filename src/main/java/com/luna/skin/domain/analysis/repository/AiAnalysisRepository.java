package com.luna.skin.domain.analysis.repository;

import com.luna.skin.domain.analysis.entity.AiAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AiAnalysisRepository extends JpaRepository<AiAnalysis, Long> {
}
