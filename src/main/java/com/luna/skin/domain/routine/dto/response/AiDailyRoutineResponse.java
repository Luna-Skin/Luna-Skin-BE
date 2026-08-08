package com.luna.skin.domain.routine.dto.response;

import com.luna.skin.domain.cycle.enums.PhaseType;
import com.luna.skin.domain.routine.entity.AiDailyRoutine;
import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Getter
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
public class AiDailyRoutineResponse {

    private PhaseType phaseType;
    private LocalDate targetDate;
    private List<AiRoutineContentResponse> routines;


    public static AiDailyRoutineResponse from(AiDailyRoutine routine) {
        return AiDailyRoutineResponse.builder()
                .phaseType(routine.getPhaseType())
                .targetDate(routine.getTargetDate())
                .routines(List.of(
                        AiRoutineContentResponse.from(routine.getSkincareContent()),
                        AiRoutineContentResponse.from(routine.getActionContent()),
                        AiRoutineContentResponse.from(routine.getExerciseContent())
                ))
                .build();
    }
}
