package com.luna.skin.domain.routine.repository;

import com.luna.skin.domain.routine.entity.AiDailyRoutine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;

public interface AiDailyRoutineRepository extends JpaRepository<AiDailyRoutine, Long> {


    // userId, targetDate 가지고 오늘 해당하는 데일리 루틴 조회
    Optional<AiDailyRoutine> findByUserUserIdAndTargetDate(Long userUserId, LocalDate targetDate);


    @Modifying(clearAutomatically = true)
    @Query("delete from AiDailyRoutine ad " +
            "where ad.user.userId = :currentUserId " +
            "and ad.targetDate = :targetDate")
    void deleteByUserIdAndTargetDate(
            @Param("currentUserId") Long currentUserId,
            @Param("targetDate") LocalDate targetDate
    );
}
