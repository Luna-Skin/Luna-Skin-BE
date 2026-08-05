package com.luna.skin.domain.cycle.service;

import com.luna.skin.domain.analysis.entity.AiAnalysis;
import com.luna.skin.domain.analysis.repository.AiAnalysisRepository;
import com.luna.skin.domain.cycle.dto.response.CycleAndAnalysisDateResponse;
import com.luna.skin.domain.cycle.dto.response.CycleCommentResponse;
import com.luna.skin.domain.cycle.dto.response.CycleResponse;
import com.luna.skin.domain.cycle.entity.CyclePhase;
import com.luna.skin.domain.cycle.entity.MenstruationCycle;
import com.luna.skin.domain.cycle.enums.PhaseType;
import com.luna.skin.domain.cycle.exception.CycleErrorCode;
import com.luna.skin.domain.cycle.repository.CyclePhaseRepository;
import com.luna.skin.domain.cycle.repository.MenstruationCycleRepository;
import com.luna.skin.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CycleService {

    private final CyclePhaseRepository cyclePhaseRepository;
    private final MenstruationCycleRepository menstruationCycleRepository;
    private final AiAnalysisRepository aiAnalysisRepository;


    public CycleAndAnalysisDateResponse getCyclePhaseAtMonth(Long currentUserId, int year, int month) {

        log.info("[월별 주기 단계 및 분석 여부 조회] currentUserId = {}, year = {}, month = {}", currentUserId, year, month);

        // 캘린더 월 1일
        LocalDate startDate = LocalDate.of(year, month, 1);
        //캘린더 월 말
        LocalDate endDate = YearMonth.of(year, month).atEndOfMonth();

        // 실제 CyclePhase 조회
        List<CycleResponse> actualResponses = cyclePhaseRepository
                .findByCyclePhaseAtMonth(currentUserId, startDate, endDate)
                .stream().map(CycleResponse::from).toList();

        // 마지막 주기 기반으로 예측 계산
        MenstruationCycle lastCycle = menstruationCycleRepository
                .findByLatestMenstruationCycle(currentUserId)
                .orElseThrow(() -> {
                    log.warn("[월별 주기 단계 및 분석 여부 조회] 마지막 주기를 찾을 수 없습니다. currentUserId = {}", currentUserId);
                    return new CustomException(CycleErrorCode.CYCLE_NOT_FOUND);
                });

        int cycleLength = lastCycle.getPredictedCycleLength();
        int periodDuration = lastCycle.getPeriodDuration();

        // 마지막 실제 주기 다음 예측 시작일부터 월말까지 겹치는 모든 예측 주기 수집
        List<CycleResponse> predictedResponses = new ArrayList<>();

        // 다음 주기 시작일(생리 시작일)
        LocalDate predictedStart = lastCycle.getCycleStartDate().plusDays(cycleLength);

        // 예측 시작일 <= 월말
        // 예측 시작일이 월 말을 넘어가면 해당 달에 속한 주기가 아님
        while (!predictedStart.isAfter(endDate)) {
            predictedResponses.addAll(
                    calculatePredictedPhases(predictedStart, cycleLength, periodDuration, startDate, endDate)
            );
            predictedStart = predictedStart.plusDays(cycleLength);
        }

        // 실제 + 예측 합산 후 날짜순 정렬
        List<CycleResponse> combined = Stream.concat(actualResponses.stream(), predictedResponses.stream())
                .sorted(Comparator.comparing(CycleResponse::getStartDate))
                .toList();

        List<AiAnalysis> aiAnalysisAtMonth = aiAnalysisRepository
                .findAiAnalysisAtMonth(currentUserId, startDate, endDate);

        List<Integer> analysisDates = aiAnalysisAtMonth.stream()
                .map(an -> an.getTodaySkin().getLogDate().getDayOfMonth())
                .toList();

        log.info("[월별 주기 단계 및 분석 여부 조회] 조회 완료 - 실제 {}개, 예측 {}개", actualResponses.size(), predictedResponses.size());
        return CycleAndAnalysisDateResponse.of(combined, analysisDates);
    }

    // 주기 단계 예측일 계산 매서드
    private List<CycleResponse> calculatePredictedPhases(
            LocalDate cycleStart, int cycleLength, int periodDuration,
            LocalDate monthStart, LocalDate monthEnd) {

        // 배란기 시작일
        LocalDate ovulationStart = cycleStart.plusDays((long) cycleLength / 2 - 1);

        // 예측 생리기
        CycleResponse predictedMenstruation = CycleResponse.predicted(PhaseType.MENSTRUATION,
                cycleStart,
                cycleStart.plusDays(periodDuration - 1));

        // 예측 난포기
        CycleResponse predictedFollicular = CycleResponse.predicted(PhaseType.FOLLICULAR,
                cycleStart.plusDays(periodDuration),
                ovulationStart.minusDays(1));

        // 예측 배란기
        CycleResponse predictedOvulation = CycleResponse.predicted(PhaseType.OVULATION,
                ovulationStart,
                ovulationStart.plusDays(2));

        // 예측 황체기
        CycleResponse predictedLuteal = CycleResponse.predicted(PhaseType.LUTEAL,
                ovulationStart.plusDays(3),
                cycleStart.plusDays(cycleLength - 1));

        List<CycleResponse> phases = List.of(
                predictedMenstruation,
                predictedFollicular,
                predictedOvulation,
                predictedLuteal);

        // 요청 월과 겹치는 단계만 필터링
        return phases.stream()
                .filter(p -> !p.getStartDate().isAfter(monthEnd) && !p.getEndDate().isBefore(monthStart))
                .toList();
    }

    // 주기별 코멘트
    public CycleCommentResponse getCycleComment(Long currentUserId) {

        LocalDate now = LocalDate.now();
        log.info("[주기별 코멘트] now = {}", now);

        CyclePhase cyclePhaseAtNow = cyclePhaseRepository.findByCyclePhaseAtNow(currentUserId, now)
                .orElseThrow(() -> {
                    log.warn("[주기별 코멘트] 주기 정보를 찾을 수 없습니다.");
                    return new CustomException(CycleErrorCode.CYCLE_NOT_FOUND);
                });


        return CycleCommentResponse.of(cyclePhaseAtNow);

    }
}
