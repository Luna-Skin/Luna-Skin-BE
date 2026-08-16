package com.luna.skin.domain.cycle.repository;

import com.luna.skin.domain.cycle.entity.CyclePhase;
import com.luna.skin.domain.cycle.entity.MenstruationCycle;
import com.luna.skin.domain.cycle.enums.PhaseType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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


    // 오늘 주기 단계 조회
    @Query("select cp from CyclePhase cp " +
            "where cp.menstruationCycle.user.userId = :currentUserId " +
            "and :now between cp.startDate and cp.endDate")
    Optional<CyclePhase> findByCyclePhaseAtNow(
            @Param("currentUserId") Long currentUserId,
            @Param("now") LocalDate now
    );

    // 해당 주기의 단계 모두 삭제
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from  CyclePhase cp where " +
            "cp.menstruationCycle =:menstruationCycle")
    void deleteAllByMenstruationCycle(MenstruationCycle menstruationCycle);

    // 시작일과 짝을 이룰수있는 endDate가 있는 주기 조회
    @Query("select cp.menstruationCycle from CyclePhase cp " +
            "where cp.menstruationCycle.user.userId = :currentUserId " +
            "and cp.phaseType = :phaseType " +
            "and :startDate <= cp.endDate " +
            "order by cp.endDate asc limit 1")
    Optional<MenstruationCycle> findByMenstruationCycleWithStartDate(
            @Param("currentUserId") Long currentUserId,
            @Param("startDate")LocalDate startDate,
            @Param("phaseType") PhaseType phaseType);

    @Query("select cp from CyclePhase cp join cp.menstruationCycle mc " +
        "where mc.user.userId = :userId " +
        "and cp.phaseType in :phaseTypes")
    List<CyclePhase> findAllByUserIdAndPhaseTypes(
        @Param("userId") Long userId,
        @Param("phaseTypes") List<PhaseType> phaseTypes);

}
