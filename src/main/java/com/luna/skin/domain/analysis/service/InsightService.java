package com.luna.skin.domain.analysis.service;

import com.luna.skin.domain.analysis.dto.response.LifestyleInsightResponse;
import com.luna.skin.domain.analysis.dto.response.TroubleTimelineResponse;
import com.luna.skin.domain.analysis.entity.AiAnalysis;
import com.luna.skin.domain.analysis.entity.DetailedSkinAnalysis;
import com.luna.skin.domain.analysis.repository.AiAnalysisRepository;
import com.luna.skin.domain.analysis.repository.DetailedSkinAnalysisRepository;
import com.luna.skin.domain.cycle.entity.MenstruationCycle;
import com.luna.skin.domain.cycle.repository.MenstruationCycleRepository;
import com.luna.skin.domain.skin.enums.ExerciseTime;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InsightService {

  private final AiAnalysisRepository aiAnalysisRepository;
  private final DetailedSkinAnalysisRepository detailedSkinAnalysisRepository;
  private final MenstruationCycleRepository menstruationCycleRepository;

  @Cacheable(value = "troubleTimeline", key = "#userId")
  public TroubleTimelineResponse getTroubleTimeline(Long userId) {
    List<MenstruationCycle> cycles = menstruationCycleRepository.findAllByUserId(userId);

    if (cycles.isEmpty()) {
      return TroubleTimelineResponse.builder()
          .cycleCount(0)
          .patternComment("생리 주기 데이터가 없어요.")
          .troubleTimeline(List.of())
          .build();
    }

    // dayFromStart별 trouble 점수 누적
    Map<Integer, List<Integer>> troubleByDay = new HashMap<>();
    for (int d = -14; d <= 14; d++) {
      troubleByDay.put(d, new ArrayList<>());
    }

    for (MenstruationCycle cycle : cycles) {
      LocalDate startDate = cycle.getCycleStartDate();
      for (int d = -14; d <= 14; d++) {
        LocalDate targetDate = startDate.plusDays(d);
        aiAnalysisRepository.findByTodaySkinUserUserIdAndTodaySkinLogDate(userId, targetDate)
            .ifPresent(ai -> {
              detailedSkinAnalysisRepository.findByAiAnalysis(ai)
                  .ifPresent(detail -> {
                    if (detail.getTrouble() != null) {
                      troubleByDay.get((int) ChronoUnit.DAYS.between(startDate, targetDate))
                          .add(detail.getTrouble());
                    }
                  });
            });
      }
    }

    // 평균 계산
    List<TroubleTimelineResponse.TroublePoint> timeline = new ArrayList<>();
    for (int d = -14; d <= 14; d++) {
      List<Integer> scores = troubleByDay.get(d);
      double avg = scores.isEmpty() ? 0 :
          scores.stream().mapToInt(Integer::intValue).average().orElse(0);
      timeline.add(TroubleTimelineResponse.TroublePoint.builder()
          .dayFromStart(d)
          .troubleIndex(avg)
          .build());
    }

    // 전체 평균
    double overallAvg = timeline.stream()
        .mapToDouble(TroubleTimelineResponse.TroublePoint::getTroubleIndex)
        .average().orElse(0);

    // peakRange: 평균보다 높은 연속 구간 중 가장 긴 구간
    int peakStart = 0, peakEnd = 0, maxLen = 0;
    int curStart = 0, curLen = 0;
    for (TroubleTimelineResponse.TroublePoint p : timeline) {
      if (p.getTroubleIndex() > overallAvg) {
        if (curLen == 0) curStart = p.getDayFromStart();
        curLen++;
        if (curLen > maxLen) {
          maxLen = curLen;
          peakStart = curStart;
          peakEnd = p.getDayFromStart();
        }
      } else {
        curLen = 0;
      }
    }

    String patternComment = maxLen > 0
        ? "생리 D" + peakStart + "부터 트러블이 증가해요."
        : "아직 트러블 패턴을 분석하기에 데이터가 부족해요.";

    TroubleTimelineResponse.PeakRange peakRange = maxLen > 0
        ? TroubleTimelineResponse.PeakRange.builder()
        .startDay(peakStart)
        .endDay(peakEnd)
        .label("트러블 집중 구간")
        .build()
        : null;

    return TroubleTimelineResponse.builder()
        .cycleCount(cycles.size())
        .patternComment(patternComment)
        .troubleTimeline(timeline)
        .peakRange(peakRange)
        .build();
  }

  @Cacheable(value = "lifestyleInsight", key = "#userId")
  public LifestyleInsightResponse getLifestyleInsight(Long userId) {
    List<AiAnalysis> analyses = aiAnalysisRepository.findAllByUserIdWithTodaySkin(userId);

    if (analyses.isEmpty()) {
      return LifestyleInsightResponse.builder().factors(List.of()).build();
    }

    Map<Long, DetailedSkinAnalysis> detailMap = detailedSkinAnalysisRepository
        .findAllByAiAnalysisIn(analyses)
        .stream()
        .collect(Collectors.toMap(d -> d.getAiAnalysis().getAnalysisId(), d -> d));

    List<LifestyleInsightResponse.LifestyleFactor> factors = new ArrayList<>();

    // 수면: 6시간 미만 vs 이상
    addFactorIfSignificant(factors, analyses, detailMap,
        a -> a.getTodaySkin().getSleepTime() != null && a.getTodaySkin().getSleepTime() < 6,
        a -> a.getTodaySkin().getSleepTime() != null && a.getTodaySkin().getSleepTime() >= 6,
        DetailedSkinAnalysis::getTrouble,
        "수면 6시간 미만", "트러블 ↑", "트러블 ↓");

    // 수분: 7잔 미만 vs 이상
    addFactorIfSignificant(factors, analyses, detailMap,
        a -> a.getTodaySkin().getWaterIntake() != null && a.getTodaySkin().getWaterIntake() < 7,
        a -> a.getTodaySkin().getWaterIntake() != null && a.getTodaySkin().getWaterIntake() >= 7,
        DetailedSkinAnalysis::getMoisture,
        "수분 섭취 부족", "건조도 ↑", "건조도 ↓");

    // 운동: 없음 vs 있음
    addFactorIfSignificant(factors, analyses, detailMap,
        a -> a.getTodaySkin().getExerciseTime() == ExerciseTime.ZERO_M,
        a -> a.getTodaySkin().getExerciseTime() != null && a.getTodaySkin().getExerciseTime() != ExerciseTime.ZERO_M,
        DetailedSkinAnalysis::getDullness,
        "운동 부족", "칙칙함 ↑", "칙칙함 ↓");

    // 자극적 식단 조건 수정 - null이면 양쪽 다 제외
    List<String> badFoods = List.of("SPICY_FOOD", "CAFFEINE", "HIGH_FAT", "SUGAR", "SODA", "ALCOHOL");
    addFactorIfSignificant(factors, analyses, detailMap,
        a -> {
          String diet = a.getTodaySkin().getDietType();
          if (diet == null) return false;
          return Arrays.stream(diet.split(",")).anyMatch(badFoods::contains);
        },
        a -> {
          String diet = a.getTodaySkin().getDietType();
          if (diet == null) return false;  // null이면 양호군에도 미포함
          return Arrays.stream(diet.split(",")).noneMatch(badFoods::contains);
        },
        DetailedSkinAnalysis::getTrouble,
        "자극적 식단", "트러블 ↑", "트러블 ↓");

    return LifestyleInsightResponse.builder().factors(factors).build();
  }

  private void addFactorIfSignificant(
      List<LifestyleInsightResponse.LifestyleFactor> factors,
      List<AiAnalysis> analyses,
      Map<Long, DetailedSkinAnalysis> detailMap,
      Predicate<AiAnalysis> badCondition,
      Predicate<AiAnalysis> goodCondition,
      Function<DetailedSkinAnalysis, Integer> metric,
      String condition,
      String negativeLabel,
      String positiveLabel) {

    Optional<Double> badAvg = avgMetric(analyses.stream().filter(badCondition).collect(Collectors.toList()), detailMap, metric);
    Optional<Double> goodAvg = avgMetric(analyses.stream().filter(goodCondition).collect(Collectors.toList()), detailMap, metric);

    if (badAvg.isEmpty() || goodAvg.isEmpty()) return;
    if (badAvg.get().equals(goodAvg.get())) return;

    if (badAvg.get() > goodAvg.get()) {
      factors.add(LifestyleInsightResponse.LifestyleFactor.builder()
          .condition(condition)
          .impactType("negative")
          .impactLabel(negativeLabel)
          .build());
    } else {
      factors.add(LifestyleInsightResponse.LifestyleFactor.builder()
          .condition(condition)
          .impactType("positive")
          .impactLabel(positiveLabel)
          .build());
    }
  }

  private Optional<Double> avgMetric(List<AiAnalysis> list, Map<Long, DetailedSkinAnalysis> detailMap,
      Function<DetailedSkinAnalysis, Integer> getter) {
    OptionalDouble result = list.stream()
        .map(a -> detailMap.get(a.getAnalysisId()))
        .filter(Objects::nonNull)
        .map(getter)
        .filter(Objects::nonNull)
        .mapToInt(Integer::intValue)
        .average();
    return result.isPresent() ? Optional.of(result.getAsDouble()) : Optional.empty();
  }

}