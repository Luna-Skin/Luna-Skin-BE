package com.luna.skin.domain.routine.entity;

import com.luna.skin.domain.analysis.entity.AiAnalysis;
import com.luna.skin.domain.cycle.enums.PhaseType;
import com.luna.skin.domain.user.entity.User;
import com.luna.skin.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "ai_daily_routine")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiDailyRoutine extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "routine_id")
    private Long routineId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "analysis_id")
    private AiAnalysis aiAnalysis;

    @Column(name = "target_date", nullable = false)
    private LocalDate targetDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "phase_type", nullable = false)
    private PhaseType phaseType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "skincare_content_id")
    private RoutineContent skincareContent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "action_content_id")
    private RoutineContent actionContent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exercise_content_id")
    private RoutineContent exerciseContent;

    public static AiDailyRoutine of(User user, AiAnalysis aiAnalysis, LocalDate targetDate,
                                    PhaseType phaseType, RoutineContent skincareContent,
                                    RoutineContent actionContent, RoutineContent exerciseContent) {
        return AiDailyRoutine.builder()
                .user(user)
                .aiAnalysis(aiAnalysis)
                .targetDate(targetDate)
                .phaseType(phaseType)
                .skincareContent(skincareContent)
                .actionContent(actionContent)
                .exerciseContent(exerciseContent)
                .build();
    }
}
