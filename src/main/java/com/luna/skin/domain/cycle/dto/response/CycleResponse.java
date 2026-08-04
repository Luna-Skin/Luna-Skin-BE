package com.luna.skin.domain.cycle.dto.response;

import com.luna.skin.domain.cycle.entity.CyclePhase;
import com.luna.skin.domain.cycle.enums.PhaseType;
import lombok.*;

import java.time.LocalDate;
import java.util.PrimitiveIterator;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PRIVATE)
public class CycleResponse {
    private PhaseType phaseType;
    private LocalDate startDate;
    private LocalDate endDate;

    public static CycleResponse from(CyclePhase cyclePhase) {
        return CycleResponse.builder()
                .phaseType(cyclePhase.getPhaseType())
                .startDate(cyclePhase.getStartDate())
                .endDate(cyclePhase.getEndDate())
                .build();
    }
}
