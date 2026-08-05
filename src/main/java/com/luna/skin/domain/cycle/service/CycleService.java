package com.luna.skin.domain.cycle.service;

import com.luna.skin.domain.analysis.entity.AiAnalysis;
import com.luna.skin.domain.analysis.repository.AiAnalysisRepository;
import com.luna.skin.domain.cycle.dto.response.CycleAndAnalysisDateResponse;
import com.luna.skin.domain.cycle.entity.CyclePhase;
import com.luna.skin.domain.cycle.repository.CyclePhaseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CycleService {

    private final CyclePhaseRepository cyclePhaseRepository;
    private final AiAnalysisRepository aiAnalysisRepository;


    public CycleAndAnalysisDateResponse getCyclePhaseAtMonth(Long currentUserId, int year, int month) {

        log.info("[월별 주기 단계 및 분석 여부 조회] currentUserId = {}, year = {}, month = {}", currentUserId, year, month);

        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = YearMonth.of(year, month).atEndOfMonth();

        List<CyclePhase> cyclePhaseAtMonth = cyclePhaseRepository
                .findByCyclePhaseAtMonth(currentUserId, startDate, endDate);

        List<AiAnalysis> aiAnalysisAtMonth = aiAnalysisRepository
                .findAiAnalysisAtMonth(currentUserId, startDate, endDate);

        log.info("[월별 주기 단계 및 분석 여부 조회] 조회 완료");
        return CycleAndAnalysisDateResponse.from(cyclePhaseAtMonth, aiAnalysisAtMonth);
    }
}
