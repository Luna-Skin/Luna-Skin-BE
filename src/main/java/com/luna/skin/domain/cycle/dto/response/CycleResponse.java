package com.luna.skin.domain.cycle.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.luna.skin.domain.cycle.entity.CyclePhase;
import com.luna.skin.domain.cycle.enums.PhaseType;
import lombok.*;

import java.time.LocalDate;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PRIVATE)
public class CycleResponse {
    private PhaseType phaseType;
    private LocalDate startDate;
    private LocalDate endDate;

    @JsonProperty("isPredicted")
    private boolean isPredicted;

    // 실제 데이터
    public static CycleResponse from(CyclePhase cyclePhase) {
        return CycleResponse.builder()
                .phaseType(cyclePhase.getPhaseType())
                .startDate(cyclePhase.getStartDate())
                .endDate(cyclePhase.getEndDate())
                .isPredicted(false)
                .build();
    }

    // 예측 데이터
    public static CycleResponse predicted(PhaseType phaseType, LocalDate startDate, LocalDate endDate) {
        return CycleResponse.builder()
                .phaseType(phaseType)
                .startDate(startDate)
                .endDate(endDate)
                .isPredicted(true)
                .build();
    }
}
