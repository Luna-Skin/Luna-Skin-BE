package com.luna.skin.domain.routine.dto.response;

import com.luna.skin.domain.routine.entity.RoutineContent;
import com.luna.skin.domain.routine.enums.RoutineCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AiRoutineContentResponse {

    private RoutineCategory routineCategory;
    private String content;

    public static AiRoutineContentResponse from(RoutineContent routineContent) {
        return new AiRoutineContentResponse(
                routineContent.getCategory(),
                routineContent.getContent()
        );
    }
}