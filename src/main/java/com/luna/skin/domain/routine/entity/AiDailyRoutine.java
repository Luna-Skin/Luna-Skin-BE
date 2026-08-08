package com.luna.skin.domain.routine.entity;

import com.luna.skin.domain.analysis.entity.AiAnalysis;
import com.luna.skin.domain.cycle.enums.PhaseType;
import com.luna.skin.domain.routine.enums.RoutineCategory;
import com.luna.skin.domain.user.entity.User;
import com.luna.skin.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

import static com.luna.skin.domain.routine.enums.RoutineCategory.*;
import static com.luna.skin.domain.routine.enums.RoutineCategory.SKINCARE;

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
                                    PhaseType phaseType, List<RoutineContent> routineContents) {

        RoutineContent skincareContent = null;
        RoutineContent actionContent = null;
        RoutineContent exerciseContent = null;

        for (RoutineContent r : routineContents) {
            if (r.getCategory() == SKINCARE) {
                skincareContent = r;
            } else if (r.getCategory() == ACTION) {
                actionContent = r;
            } else if (r.getCategory() == EXERCISE) {
                exerciseContent = r;
            }
        }

        // ★ 빌더에 각각의 Content 변수들을 연결해 줍니다.
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
