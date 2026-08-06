package com.luna.skin.domain.cycle.dto.response;

import com.luna.skin.domain.cycle.entity.CyclePhase;
import com.luna.skin.domain.cycle.enums.PhaseType;
import lombok.*;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PRIVATE)
public class CycleCommentResponse {

    private PhaseType phaseType;
    private String comment;

    public static CycleCommentResponse of(CyclePhase cyclePhaseAtNow) {
        return CycleCommentResponse.builder()
                .phaseType(cyclePhaseAtNow.getPhaseType())
                .comment(cyclePhaseAtNow.getPhaseType().getComment())
                .build();
    }

    public static CycleCommentResponse ofPredicted(PhaseType phaseType) {
        return CycleCommentResponse.builder()
                .phaseType(phaseType)
                .comment(phaseType.getComment())
                .build();
    }

}
