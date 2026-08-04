package com.luna.skin.domain.routine.repository;

import com.luna.skin.domain.routine.entity.AiDailyRoutine;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface AiDailyRoutineRepository extends JpaRepository<AiDailyRoutine, Long> {
}
