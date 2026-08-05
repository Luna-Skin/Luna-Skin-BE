package com.luna.skin.domain.cycle.dto.response;

import com.luna.skin.domain.analysis.entity.AiAnalysis;
import com.luna.skin.domain.cycle.entity.CyclePhase;
import lombok.*;

import java.util.List;
import java.util.stream.Stream;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PRIVATE)
public class CycleAndAnalysisDateResponse {

    private List<CycleResponse> cycleResponses;
    private List<Integer> analysisDates;;

    public static CycleAndAnalysisDateResponse from(List<CyclePhase> cyclePhaseAtMonth, List<AiAnalysis> aiAnalysisAtMonth) {
        List<CycleResponse> cycleResponses = cyclePhaseAtMonth.stream().map(CycleResponse::from).toList();

        List<Integer> analysisDates = aiAnalysisAtMonth.stream()
                .map(an -> an.getTodaySkin().getLogDate().getDayOfMonth())
                .toList();

        return CycleAndAnalysisDateResponse.builder()
                .cycleResponses(cycleResponses)
                .analysisDates(analysisDates)
                .build();
    }

}
