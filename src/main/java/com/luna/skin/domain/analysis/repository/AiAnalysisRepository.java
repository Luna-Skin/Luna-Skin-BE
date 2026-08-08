package com.luna.skin.domain.analysis.repository;

import com.luna.skin.domain.analysis.entity.AiAnalysis;
import org.springframework.cglib.core.Local;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AiAnalysisRepository extends JpaRepository<AiAnalysis, Long> {

    @Query("select an from AiAnalysis an " +
        "join fetch an.todaySkin ts " +
        "where ts.user.userId = :currentUserId " +
        "and ts.logDate between :startDate and :endDate " +
        "order by ts.logDate")
    List<AiAnalysis> findAiAnalysisAtMonth(
        @Param("currentUserId") Long currentUserId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    // 기간 내에 가장 최근 투데이 스킨을 조회
    @Query("select an from AiAnalysis an " +
            "join fetch an.todaySkin ts " +
            "where ts.user.userId = :currentUserId " +
            "and ts.logDate between :startDate and :endDate " +
            "order by ts.logDate desc limit 1")
    Optional<AiAnalysis> findByUserIdAndBetweenDate(
            @Param("currentUserId") Long currentUserId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
            );

    Optional<AiAnalysis> findByTodaySkinUserUserIdAndTodaySkinLogDate(Long userId, LocalDate logDate);

    @Query("select an from AiAnalysis an join fetch an.todaySkin ts where ts.user.userId = :userId")
    List<AiAnalysis> findAllByUserIdWithTodaySkin(@Param("userId") Long userId);

    @Query("select an from AiAnalysis an join fetch an.todaySkin ts " +
        "where ts.user.userId = :userId " +
        "and ts.logDate between :startDate and :endDate")
    List<AiAnalysis> findAllByUserIdAndLogDateBetween(
        @Param("userId") Long userId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate);
}