package com.luna.skin.domain.cycle.repository;

import com.luna.skin.domain.cycle.entity.CyclePhase;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface CyclePhaseRepository extends JpaRepository<CyclePhase, Long> {
}
