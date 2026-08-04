package com.luna.skin.domain.skin.repository;

import com.luna.skin.domain.skin.entity.TodaySkin;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface TodaySkinRepository extends JpaRepository<TodaySkin, Long> {

    Optional<TodaySkin> findByUserUserIdAndLogDate(Long userId, LocalDate logDate);
}
