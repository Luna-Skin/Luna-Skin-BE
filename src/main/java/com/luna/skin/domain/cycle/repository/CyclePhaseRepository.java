package com.luna.skin.domain.cycle.repository;

import com.luna.skin.domain.cycle.entity.CyclePhase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface CyclePhaseRepository extends JpaRepository<CyclePhase, Long> {

    // 월별 주기 단계 조회
    @Query("select cp from CyclePhase cp " +
            "join cp.menstruationCycle mc " +
            "where mc.user.userId = :currentUserId " +
            "and cp.startDate <= :endDate and cp.endDate >= :startDate")
    List<CyclePhase> findByCyclePhaseAtMonth(
            @Param("currentUserId") Long currentUserId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

}
