package com.luna.skin.domain.analysis.dto.response;

import com.luna.skin.domain.analysis.entity.AiAnalysis;
import lombok.*;

import java.time.LocalDate;

@Getter
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder(access =AccessLevel.PROTECTED)
public class AnalysisResponse {

    private Long analysisId;
    private LocalDate date;

    public static AnalysisResponse of(AiAnalysis analysis) {
        return AnalysisResponse.builder()
                .analysisId(analysis.getAnalysisId())
                .date(analysis.getTodaySkin().getLogDate())
                .build();
    }

}
