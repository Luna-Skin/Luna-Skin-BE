package com.luna.skin.domain.cycle.service;

import com.luna.skin.domain.analysis.entity.AiAnalysis;
import com.luna.skin.domain.analysis.repository.AiAnalysisRepository;
import com.luna.skin.domain.cycle.dto.request.CycleInfoUpdateRequest;
import com.luna.skin.domain.cycle.dto.response.CycleAndAnalysisDateResponse;
import com.luna.skin.domain.cycle.dto.response.CycleCommentResponse;
import com.luna.skin.domain.cycle.dto.response.CycleInfoResponse;
import com.luna.skin.domain.cycle.dto.response.CycleResponse;
import com.luna.skin.domain.cycle.entity.CyclePhase;
import com.luna.skin.domain.cycle.entity.MenstruationCycle;
import com.luna.skin.domain.cycle.enums.PhaseType;
import com.luna.skin.domain.cycle.exception.CycleErrorCode;
import com.luna.skin.domain.cycle.repository.CyclePhaseRepository;
import com.luna.skin.domain.cycle.repository.MenstruationCycleRepository;
import com.luna.skin.domain.routine.dto.response.AiDailyRoutineResponse;
import com.luna.skin.domain.routine.entity.AiDailyRoutine;
import com.luna.skin.domain.routine.repository.AiDailyRoutineRepository;
import com.luna.skin.domain.user.entity.User;
import com.luna.skin.domain.user.exception.UserErrorCode;
import com.luna.skin.domain.user.repository.UserRepository;
import com.luna.skin.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CycleService {

    private final CyclePhaseRepository cyclePhaseRepository;
    private final MenstruationCycleRepository menstruationCycleRepository;
    private final UserRepository userRepository;
    private final AiAnalysisRepository aiAnalysisRepository;
    private final AiDailyRoutineRepository aiDailyRoutineRepository;
    private final CyclePhasePredictor cyclePhasePredictor;


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

        // 마지마 주기 길이
        int lastCycleLength = lastCycle.getPredictedCycleLength();

        // 사용자가 설정한 주기, 기간
        int nextCycleLength = lastCycle.getUser().getDefaultCycleLength();
        int periodDuration = lastCycle.getUser().getDefaultPeriodDuration();

        // 마지막 실제 주기 다음 예측 시작일부터 월말까지 겹치는 모든 예측 주기 수집
        List<CycleResponse> predictedResponses = new ArrayList<>();

        // 다음 주기 시작일(생리 시작일)
        LocalDate predictedStart = lastCycle.getCycleStartDate().plusDays(lastCycleLength);

        // 예측 시작일 <= 월말
        // 예측 시작일이 월 말을 넘어가면 해당 달에 속한 주기가 아님
        while (!predictedStart.isAfter(endDate)) {
            predictedResponses.addAll(
                    calculatePredictedPhases(predictedStart, nextCycleLength, periodDuration, startDate, endDate)
            );
            predictedStart = predictedStart.plusDays(nextCycleLength);
        }

        // 실제 + 예측 합산 후 날짜순 정렬
        List<CycleResponse> combined = Stream.concat(actualResponses.stream(), predictedResponses.stream())
                .sorted(Comparator.comparing(CycleResponse::getStartDate))
                .toList();

        List<AiAnalysis> aiAnalysisAtMonth = aiAnalysisRepository
                .findAiAnalysisAtMonth(currentUserId, startDate, endDate);

        log.info("[월별 주기 단계 및 분석 여부 조회] 조회 완료 - 실제 {}개, 예측 {}개", actualResponses.size(), predictedResponses.size());
        return CycleAndAnalysisDateResponse.of(combined, aiAnalysisAtMonth);
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

        // 실제 데이터 조회
        // 실제 주기 데이터가 있다면 진짜 주기 단계에 맞는 코멘트 리턴
        Optional<CyclePhase> cyclePhaseAtNow = cyclePhaseRepository.findByCyclePhaseAtNow(currentUserId, now);
        if (cyclePhaseAtNow.isPresent()) {
            return CycleCommentResponse.of(cyclePhaseAtNow.get());
        }

        // 실제 데이터 없으면 예측단계 값으로
        log.info("[주기별 코멘트] 실제 데이터 없음, 예측으로 폴백");

        MenstruationCycle lastCycle = menstruationCycleRepository
                .findByLatestMenstruationCycle(currentUserId)
                .orElseThrow(() -> {
                    log.warn("[주기별 코멘트] 마지막 주기를 찾을 수 없습니다. currentUserId = {}", currentUserId);
                    return new CustomException(CycleErrorCode.CYCLE_NOT_FOUND);
                });

        // 예측 단계 반환
        PhaseType predictedPhase = cyclePhasePredictor.predict(lastCycle, now).getPhaseType();

        return CycleCommentResponse.ofPredicted(predictedPhase);
    }

    // 생리 시작일 기록
    @CacheEvict(value = "cycleDetail", key = "#currentUserId")
    @Transactional
    public void startMenstruation(Long currentUserId, LocalDate startDate) {

        log.info("[생리 시작일 기록] currentUserId = {}, startDate = {}", currentUserId, startDate);

        // 미래 날짜 차단
        if (startDate.isAfter(LocalDate.now())) {
            log.warn("[생리 시작일 기록] 아직 다가오지 않은 날짜엔 시작 할 수 없습니다. ");
            throw new CustomException(CycleErrorCode.INVALID_START_DATE);
        }

        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> {
                    log.warn("[생리 시작일 기록] 사용자를 찾을 수 없습니다. currentUserId = {}", currentUserId);
                    return new CustomException(UserErrorCode.USER_NOT_FOUND);
                });

        //마지막 주기 조회
        MenstruationCycle lastCycle = menstruationCycleRepository
                .findByLatestMenstruationCycle(currentUserId)
                .orElseThrow(() -> {
                    log.warn("[생리 시작일 기록] 마지막 주기를 찾을 수 없습니다. currentUserId = {}", currentUserId);
                    return new CustomException(CycleErrorCode.CYCLE_NOT_FOUND);
                });


        Optional<MenstruationCycle> targetMenstruation = cyclePhaseRepository
                .findByMenstruationCycleWithStartDate(currentUserId, startDate, PhaseType.MENSTRUATION);

        // 기존에 있던 주기의 시작일을 변경 하는 경우
        if(targetMenstruation.isPresent()) {

            // 기존 주기 조회
            MenstruationCycle menstruationCycle = targetMenstruation.get();

            // 기존 주기의 생리 종료일
            LocalDate existingMenstruationEnd = menstruationCycle.getCycleStartDate()
                    .plusDays(menstruationCycle.getPeriodDuration() - 1);

            // 시작일 = 종료일 체크
            if (!startDate.isBefore(existingMenstruationEnd)) {
                log.warn("[생리 시작일 기록] 시작일과 종료일은 같을 수 없습니다. startDate = {}", startDate);
                throw new CustomException(CycleErrorCode.START_AND_END_DATE_CANNOT_BE_SAME);
            }

            // 시작일 갱신
            menstruationCycle.updateStartDate(startDate);

            // target 주기 이전의 가장 최근 주기 업데이트
            menstruationCycleRepository.findByMenstruationCycleBeforeTarget(currentUserId, startDate, menstruationCycle.getMenstruationCycleId())
                    .ifPresent(prevCycle -> {

                        int between = (int) ChronoUnit.DAYS
                                .between(prevCycle.getCycleStartDate(), startDate);

                        // 주기 길이 갱신
                        prevCycle.updateActualCycle(startDate.minusDays(1), between);
                        // 주기 단계 재계산
                        cyclePhaseRepository.deleteAllByMenstruationCycle(prevCycle);
                        cyclePhaseRepository.saveAll(CyclePhase.of(prevCycle));
                    });

            // 기존 주기의 시작일 업데이트
            cyclePhaseRepository.deleteAllByMenstruationCycle(menstruationCycle);
            cyclePhaseRepository.saveAll(CyclePhase.of(menstruationCycle));

            // 시작일 변동시 오늘 데일리 루틴도 지움
            aiDailyRoutineRepository.deleteByUserIdAndTargetDate(currentUserId, LocalDate.now());

            return;
        }

        // 새로운 주기를 만드는 경우

        // 새로운 주기와 이전 주기의 차이( lastCycle의 실 주기 )
        int actualCycleLength = (int) ChronoUnit.DAYS
                .between(lastCycle.getCycleStartDate(), startDate);


        // 새로운 주기 생성
        MenstruationCycle newCycle = MenstruationCycle.of(
                user, startDate,
                startDate.plusDays(user.getDefaultCycleLength() - 1),
                user.getDefaultCycleLength(),
                user.getDefaultPeriodDuration()
        );

        // 새로운 주기가 생성 됐으므로 기존 주기 종료 및 갱신
        lastCycle.updateActualCycle(startDate.minusDays(1), actualCycleLength);

        cyclePhaseRepository.deleteAllByMenstruationCycle(lastCycle);
        cyclePhaseRepository.saveAll(CyclePhase.of(lastCycle));

        // 저장
        menstruationCycleRepository.save(newCycle);
        cyclePhaseRepository.saveAll(CyclePhase.of(newCycle));

        // 오늘 생성된 데일리 루틴도 지움
        aiDailyRoutineRepository.deleteByUserIdAndTargetDate(currentUserId, LocalDate.now());
    }

    @CacheEvict(value = "cycleDetail", key = "#currentUserId")
    @Transactional
    public void endMenstruation(Long currentUserId, LocalDate endDate) {

        log.info("[생리 종료일 기록] currentUserId = {}, endDate = {}", currentUserId, endDate);

        // 미래시점 차단
        if (endDate.isAfter(LocalDate.now())) {
            log.warn("[생리 종료일 기록] 아직 다가오지 않은 날짜엔 시작 할 수 없습니다. ");
            throw new CustomException(CycleErrorCode.INVALID_START_DATE);
        }

        // 종료일과 짝인 시작일 주기 조회
        MenstruationCycle targetMenstruation = menstruationCycleRepository.findByMenstruationCycleWithEndDate(currentUserId, endDate)
                .orElseThrow(() -> {
                    log.warn("[생리 종료일 기록] 시작일을 먼저 입력해야합니다.");
                    return new CustomException(CycleErrorCode.CYCLE_NOT_FOUND);
                });

        // 시작일 = 종료일
        if(targetMenstruation.getCycleStartDate().isEqual(endDate)) {
            log.warn("[생리 종료일 기록] 시작일과 종료일은 같을 수 없습니다. endDate = {}", endDate);
            throw new CustomException(CycleErrorCode.START_AND_END_DATE_CANNOT_BE_SAME);
        }

        // 종료일 갱신으로 인한 생리기간 업데이트
        targetMenstruation.updateEndDate(endDate);
        // 주기 단계 갱신
        cyclePhaseRepository.deleteAllByMenstruationCycle(targetMenstruation);
        cyclePhaseRepository.saveAll(CyclePhase.of(targetMenstruation));

        // 종료 변동시 오늘 데일리 루틴도 지움
        aiDailyRoutineRepository.deleteByUserIdAndTargetDate(currentUserId, LocalDate.now());
    }

    public CycleInfoResponse getCycleInfo(Long currentUserId) {

        log.info("[주기 정보 조회] currentUserId = {}", currentUserId);

        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> {
                    log.warn("[주기 정보 조회] 해당 유저를 찾을 수 없습니다,currentUserId = {}", currentUserId);
                    return new CustomException(UserErrorCode.USER_NOT_FOUND);
                });

        return CycleInfoResponse.from(user);
    }

    @Transactional
    public void changeCycleInfo(Long currentUserId, CycleInfoUpdateRequest request) {

        log.info("[생리정보수정] currentUserId = {}, duration = {}, cycleLength = {}", currentUserId, request.getPeriodDuration(), request.getCycleLength());

        // 생리 기간은 주기보단 짧아야 함.
        if(request.getCycleLength() < request.getPeriodDuration()) {
            throw new CustomException(CycleErrorCode.INVALID_CYCLE_PERIOD_RANGE);
        }

        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> {
                    log.warn("[생리정보수정] 해당 유저를 찾을 수 없습니다,currentUserId = {}", currentUserId);
                    return new CustomException(UserErrorCode.USER_NOT_FOUND);
                });

        // 주기, 기간 업데이트
        user.updateCycleSettings(request.getPeriodDuration(), request.getCycleLength());
    }
}
