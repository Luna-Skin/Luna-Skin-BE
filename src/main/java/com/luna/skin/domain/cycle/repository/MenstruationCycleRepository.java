package com.luna.skin.domain.cycle.repository;

import com.luna.skin.domain.cycle.entity.CyclePhase;
import com.luna.skin.domain.cycle.entity.MenstruationCycle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface MenstruationCycleRepository extends JpaRepository<MenstruationCycle, Long> {

    // 사용자의 마지막 MenstruationCycle 조회
    @Query("select mc from MenstruationCycle mc " +
            "where mc.user.userId = :currentUserId " +
            "order by mc.cycleStartDate desc limit 1")
    Optional<MenstruationCycle> findByLatestMenstruationCycle(
            @Param("currentUserId") Long currentUserId
    );

    // 사용자의 마지막 주기 하나 전 MenstruationCycle 조회
    @Query("select mc from MenstruationCycle mc " +
            "where mc.user.userId = :currentUserId " +
            "order by mc.cycleStartDate desc limit 1 offset 1")
    Optional<MenstruationCycle> findBySecondLatestMenstruationCycle(
            @Param("currentUserId") Long currentUserId
    );

    // 종료일과 가장 가까운 시작일이있는 주기 조회
    @Query("select mc from MenstruationCycle mc " +
            "where mc.user.userId = :currentUserId " +
            "and mc.cycleStartDate <= :endDate " +
            "order by  mc.cycleStartDate desc limit 1")
    Optional<MenstruationCycle> findByMenstruationCycleWithEndDate(
            @Param("currentUserId") Long currentUserId,
            @Param("endDate")LocalDate endDate);

    // targetStartDate 와 가장 가까운 이전 주기 조회
    @Query("select mc from MenstruationCycle mc " +
            "where mc.user.userId = :currentUserId " +
            "and mc.cycleStartDate <= :targetStartDate " +
            "and mc.menstruationCycleId != :targetId " +
            "order by mc.cycleStartDate desc limit 1")
    Optional<MenstruationCycle> findByMenstruationCycleBeforeTarget(
            @Param("currentUserId") Long currentUserId,
            @Param("targetStartDate") LocalDate targetStartDate,
            @Param("targetId") Long targetId
    );

    @Query("select mc from MenstruationCycle mc where mc.user.userId = :userId order by mc.cycleStartDate asc")
    List<MenstruationCycle> findAllByUserId(@Param("userId") Long userId);
}
