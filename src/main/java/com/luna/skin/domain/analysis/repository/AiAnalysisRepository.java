package com.luna.skin.domain.analysis.repository;

import com.luna.skin.domain.analysis.entity.AiAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AiAnalysisRepository extends JpaRepository<AiAnalysis, Long> {

    // 해당 달에 이루어진 분석 조회
    @Query("select an from AiAnalysis an " +
            "where an.todaySkin.user.userId = :currentUserId " +
            "and an.todaySkin.logDate between :startDate and :endDate " +
            "order by an.todaySkin.logDate")
    List<AiAnalysis> findAiAnalysisAtMonth(
            @Param("currentUserId") Long currentUserId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
}
