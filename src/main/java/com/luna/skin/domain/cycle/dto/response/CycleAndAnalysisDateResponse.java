package com.luna.skin.domain.cycle.dto.response;

import com.luna.skin.domain.analysis.dto.response.AnalysisResponse;
import com.luna.skin.domain.analysis.entity.AiAnalysis;
import com.luna.skin.domain.cycle.entity.CyclePhase;
import lombok.*;

import java.util.List;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PRIVATE)
public class CycleAndAnalysisDateResponse {

    private List<CycleResponse> cycleResponses;
    private List<AnalysisResponse> analyses;;

    // 실제데이터 반환용
    public static CycleAndAnalysisDateResponse from(List<CyclePhase> cyclePhaseAtMonth, List<AiAnalysis> aiAnalysisAtMonth) {
        List<CycleResponse> cycleResponses = cyclePhaseAtMonth.stream().map(CycleResponse::from).toList();

        List<AnalysisResponse> analysisDates = aiAnalysisAtMonth.stream()
                .map(AnalysisResponse::of)
                .toList();

        return CycleAndAnalysisDateResponse.builder()
                .cycleResponses(cycleResponses)
                .analyses(analysisDates)
                .build();
    }

    // 예측 데이터 반환용
    public static CycleAndAnalysisDateResponse fromPredicted(List<CycleResponse> predicted) {
        return CycleAndAnalysisDateResponse.builder()
                .cycleResponses(predicted)
                .analyses(List.of())
                .build();
    }

    // 실제 + 예측 혼합 반환용
    public static CycleAndAnalysisDateResponse of(List<CycleResponse> cycleResponses, List<AiAnalysis> aiAnalysisAtMonth) {


        List<AnalysisResponse> analysisDates = aiAnalysisAtMonth.stream()
                .map(AnalysisResponse::of)
                .toList();

        return CycleAndAnalysisDateResponse.builder()
                .cycleResponses(cycleResponses)
                .analyses(analysisDates)
                .build();
    }

}
