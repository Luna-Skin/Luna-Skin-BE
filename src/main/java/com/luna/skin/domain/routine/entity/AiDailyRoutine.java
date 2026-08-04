package com.luna.skin.domain.routine.entity;

import com.luna.skin.domain.analysis.entity.AiAnalysis;
import com.luna.skin.domain.cycle.enums.PhaseType;
import com.luna.skin.domain.user.entity.User;
import com.luna.skin.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "ai_daily_routine")
@Getter
@NoArgsConstructor
public class AiDailyRoutine extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "routine_id")
    private Long routineId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "analysis_id", nullable = false)
    private AiAnalysis aiAnalysis;

    @Column(name = "target_date", nullable = false)
    private LocalDate targetDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "phase_type", nullable = false)
    private PhaseType phaseType;

    @Column(name = "skincare_routine", nullable = false, columnDefinition = "TEXT")
    private String skincareRoutine;

    @Column(name = "action_content", nullable = false, columnDefinition = "TEXT")
    private String actionContent;

    @Column(name = "exercise_routine", columnDefinition = "TEXT")
    private String exerciseRoutine;
}
