package com.luna.skin.domain.cycle.repository;

import com.luna.skin.domain.cycle.entity.MenstruationCycle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MenstruationCycleRepository extends JpaRepository<MenstruationCycle, Long> {

    // 사용자의 마지막 MenstruationCycle 조회
    @Query("select mc from MenstruationCycle mc " +
            "where mc.user.userId = :currentUserId " +
            "order by mc.createdAt desc limit 1")
    Optional<MenstruationCycle> findByLatestMenstruationCycle(
            @Param("currentUserId") Long currentUserId
    );

    // 사용자의 마지막 주기 하나 전 MenstruationCycle 조회
    @Query("select mc from MenstruationCycle mc " +
            "where mc.user.userId = :currentUserId " +
            "order by mc.createdAt desc limit 1 offset 1")
    Optional<MenstruationCycle> findBySecondLatestMenstruationCycle(
            @Param("currentUserId") Long currentUserId
    );
}
