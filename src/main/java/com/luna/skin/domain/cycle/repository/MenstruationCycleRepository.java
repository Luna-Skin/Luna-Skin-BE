package com.luna.skin.domain.cycle.repository;

import com.luna.skin.domain.cycle.entity.MenstruationCycle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MenstruationCycleRepository extends JpaRepository<MenstruationCycle, Long> {

    List<MenstruationCycle> findByUserUserIdOrderByCreatedAtDesc(Long userId);
}
