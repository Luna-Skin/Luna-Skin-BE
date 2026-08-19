package com.luna.skin.domain.analysis.service;

import com.luna.skin.domain.analysis.dto.response.CycleDetailResponse;
import com.luna.skin.domain.analysis.dto.response.LifestyleInsightResponse;
import com.luna.skin.domain.analysis.dto.response.TroubleTimelineResponse;
import com.luna.skin.domain.analysis.entity.AiAnalysis;
import com.luna.skin.domain.analysis.entity.DetailedSkinAnalysis;
import com.luna.skin.domain.analysis.repository.AiAnalysisRepository;
import com.luna.skin.domain.analysis.repository.DetailedSkinAnalysisRepository;
import com.luna.skin.domain.cycle.entity.CyclePhase;
import com.luna.skin.domain.cycle.entity.MenstruationCycle;
import com.luna.skin.domain.cycle.enums.PhaseType;
import com.luna.skin.domain.cycle.repository.CyclePhaseRepository;
import com.luna.skin.domain.cycle.repository.MenstruationCycleRepository;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumMap;
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
  private final CyclePhaseRepository cyclePhaseRepository;

  private static final List<String> BAD_FOODS =
      List.of("SPICY_FOOD", "CAFFEINE", "HIGH_FAT", "SUGAR", "SODA", "ALCOHOL");

  // ======================== Public API ========================

  /**
   * [트러블 타임라인 조회]
   * 생리 시작일 기준 D-14 ~ D+14 구간의 트러블 점수를 누적 평균으로 계산하여
   * 주기별 트러블 패턴과 집중 구간(peak)을 반환한다.
   *
   * - 미관측 날짜는 null 처리 (0점 포함 시 평균 왜곡 방지)
   * - 전체 평균 초과 구간 중 가장 긴 연속 구간을 peak로 판정
   * - 결과는 캐싱되며 새 분석 저장 시 evict
   *
   * @param userId 조회할 사용자 식별자
   * @return 트러블 타임라인 응답 DTO (cycleCount, patternComment, troubleTimeline, peakRange)
   */
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

    Map<Integer, List<Integer>> troubleByDay = new HashMap<>();
    for (int d = -14; d <= 14; d++) troubleByDay.put(d, new ArrayList<>());

    LocalDate minDate = cycles.get(0).getCycleStartDate().minusDays(14);
    LocalDate maxDate = cycles.get(cycles.size() - 1).getCycleStartDate().plusDays(14);

    Map<LocalDate, DetailedSkinAnalysis> detailByDate = fetchDetailByDate(userId, minDate, maxDate);

    for (MenstruationCycle cycle : cycles) {
      LocalDate startDate = cycle.getCycleStartDate();
      for (int d = -14; d <= 14; d++) {
        DetailedSkinAnalysis detail = detailByDate.get(startDate.plusDays(d));
        if (detail != null && detail.getTrouble() != null) {
          troubleByDay.get(d).add(detail.getTrouble());
        }
      }
    }

    List<TroubleTimelineResponse.TroublePoint> timeline = buildTimeline(troubleByDay);
    double overallAvg = calcOverallAvg(timeline);
    int[] peak = findPeakRange(timeline, overallAvg);
    int peakStart = peak[0], peakEnd = peak[1], maxLen = peak[2];

    String patternComment = maxLen > 0
        ? "생리 D" + (peakStart >= 0 ? "+" + peakStart : peakStart) + "부터 트러블이 증가해요."
        : "아직 트러블 패턴을 분석하기에 데이터가 부족해요.";

    TroubleTimelineResponse.PeakRange peakRange = maxLen > 0
        ? TroubleTimelineResponse.PeakRange.builder()
        .startDay(peakStart).endDay(peakEnd).label("트러블 집중 구간").build()
        : null;

    return TroubleTimelineResponse.builder()
        .cycleCount(cycles.size())
        .patternComment(patternComment)
        .troubleTimeline(timeline)
        .peakRange(peakRange)
        .build();
  }

  /**
   * [단계별 피부 비교 조회]
   * 생리기 / 배란기 / 황체기 3단계별로 피부 측정값(트러블, 피지, 칙칙함, 수분, 탄력)의
   * 누적 평균을 계산하여 반환한다.
   *
   * - FOLLICULAR(여포기) 제외 — 분석 의미 있는 3단계만 대상
   * - 전체 phase 날짜 범위를 한 번에 조회하여 N+1 방지
   * - 데이터 없는 메트릭은 null 반환
   *
   * @param userId 조회할 사용자 식별자
   * @return 단계별 피부 비교 응답 DTO (phases: phase, label, metrics)
   */
  @Cacheable(value = "cycleDetail", key = "#userId")
  public CycleDetailResponse getCycleDetail(Long userId) {
    List<PhaseType> targetPhases = List.of(
        PhaseType.MENSTRUATION, PhaseType.OVULATION, PhaseType.LUTEAL
    );

    List<CyclePhase> cyclePhases = cyclePhaseRepository
        .findAllByUserIdAndPhaseTypes(userId, targetPhases);
    if (cyclePhases.isEmpty()) {
      return CycleDetailResponse.builder().phases(List.of()).build();
    }

    LocalDate minDate = cyclePhases.stream().map(CyclePhase::getStartDate).min(Comparator.naturalOrder()).orElseThrow();
    LocalDate maxDate = cyclePhases.stream().map(CyclePhase::getEndDate).max(Comparator.naturalOrder()).orElseThrow();

    Map<LocalDate, DetailedSkinAnalysis> detailByDate = fetchDetailByDate(userId, minDate, maxDate);

    Map<PhaseType, List<DetailedSkinAnalysis>> detailsByPhase = new EnumMap<>(PhaseType.class);
    for (PhaseType pt : targetPhases) detailsByPhase.put(pt, new ArrayList<>());

    for (CyclePhase cp : cyclePhases) {
      LocalDate d = cp.getStartDate();
      while (!d.isAfter(cp.getEndDate())) {
        DetailedSkinAnalysis detail = detailByDate.get(d);
        if (detail != null) detailsByPhase.get(cp.getPhaseType()).add(detail);
        d = d.plusDays(1);
      }
    }

    List<CycleDetailResponse.PhaseDetail> phases = targetPhases.stream()
        .map(pt -> CycleDetailResponse.PhaseDetail.builder()
            .phase(pt.name())
            .label(phaseLabel(pt))
            .metrics(toAverageMetrics(detailsByPhase.get(pt)))
            .build())
        .collect(Collectors.toList());

    return CycleDetailResponse.builder().phases(phases).build();
  }

  /**
   * [생활습관 영향 분석 조회]
   * 수면 6시간 / 수분 1.5L / 운동 30분 / 식단 기준치로 판단한다.
   *
   * - impactType/impactLabel은 항상 같은 방향을 가리킨다.
   *   양쪽 그룹 다 있으면 실측 평균 차이의 부호로 type을 정하고, 차이가 5 이하면 label만 "-"로 표시.
   *   한쪽 그룹만 있으면 기준치 방향(불량군 있으면 negative/↑, 양호군만 있으면 positive/↓) 그대로 사용.
   * - 두 그룹 모두 데이터가 없으면(해당 항목 미기록) 그 요인은 응답에서 제외
   * - 식단 null은 양호군/불량군 모두 제외 (통계 왜곡 방지)
   * - 결과는 캐싱되며 새 분석 저장 시 evict
   *
   * @param userId 조회할 사용자 식별자
   * @return 생활습관 영향 분석 응답 DTO (factors: condition, impactType, impactLabel)
   */
  @Cacheable(value = "lifestyleInsight", key = "#userId")
  public LifestyleInsightResponse getLifestyleInsight(Long userId) {
    List<AiAnalysis> analyses = aiAnalysisRepository.findAllByUserIdWithTodaySkin(userId);
    if (analyses.isEmpty()) {
      return LifestyleInsightResponse.builder().factors(List.of()).build();
    }

    Map<Long, DetailedSkinAnalysis> detailMap = detailedSkinAnalysisRepository
        .findAllByAiAnalysisIn(analyses).stream()
        .collect(Collectors.toMap(d -> d.getAiAnalysis().getAnalysisId(), d -> d));

    List<LifestyleInsightResponse.LifestyleFactor> factors = new ArrayList<>();

    addFactorIfSignificant(factors, analyses, detailMap,
        a -> a.getTodaySkin().getSleepTime() != null && a.getTodaySkin().getSleepTime() < 6,
        a -> a.getTodaySkin().getSleepTime() != null && a.getTodaySkin().getSleepTime() >= 6,
        DetailedSkinAnalysis::getTrouble,
        "수면 6시간 미만", "트러블 ↑", "트러블 ↓", "트러블 -");

    addFactorIfSignificant(factors, analyses, detailMap,
        a -> a.getTodaySkin().getWaterIntake() != null && a.getTodaySkin().getWaterIntake() < 1.5,
        a -> a.getTodaySkin().getWaterIntake() != null && a.getTodaySkin().getWaterIntake() >= 1.5,
        DetailedSkinAnalysis::getMoisture,
        "수분 섭취 부족", "건조도 ↑", "건조도 ↓", "건조도 -");

    addFactorIfSignificant(factors, analyses, detailMap,
        a -> a.getTodaySkin().getExerciseTime() != null && a.getTodaySkin().getExerciseTime() < 30,
        a -> a.getTodaySkin().getExerciseTime() != null && a.getTodaySkin().getExerciseTime() >= 30,
        DetailedSkinAnalysis::getDullness,
        "운동 부족", "칙칙함 ↑", "칙칙함 ↓", "칙칙함 -");

    addFactorIfSignificant(factors, analyses, detailMap,
        a -> hasBadFood(a.getTodaySkin().getDietType()),
        a -> hasGoodFood(a.getTodaySkin().getDietType()),
        DetailedSkinAnalysis::getTrouble,
        "자극적 식단", "트러블 ↑", "트러블 ↓", "트러블 -");

    return LifestyleInsightResponse.builder().factors(factors).build();
  }

  // ======================== Calculations ========================

  /** 날짜 범위의 DetailedSkinAnalysis를 날짜 기준 맵으로 반환 */
  private Map<LocalDate, DetailedSkinAnalysis> fetchDetailByDate(Long userId, LocalDate from, LocalDate to) {
    List<AiAnalysis> analyses = aiAnalysisRepository.findAllByUserIdAndLogDateBetween(userId, from, to);
    return detailedSkinAnalysisRepository.findAllByAiAnalysisIn(analyses).stream()
        .collect(Collectors.toMap(d -> d.getAiAnalysis().getTodaySkin().getLogDate(), d -> d));
  }

  private List<TroubleTimelineResponse.TroublePoint> buildTimeline(Map<Integer, List<Integer>> troubleByDay) {
    List<TroubleTimelineResponse.TroublePoint> timeline = new ArrayList<>();
    for (int d = -14; d <= 14; d++) {
      List<Integer> scores = troubleByDay.get(d);
      Double avg = scores.isEmpty() ? null : scores.stream().mapToInt(Integer::intValue).average().orElse(0);
      timeline.add(TroubleTimelineResponse.TroublePoint.builder().dayFromStart(d).troubleIndex(avg).build());
    }
    return timeline;
  }

  private double calcOverallAvg(List<TroubleTimelineResponse.TroublePoint> timeline) {
    return timeline.stream()
        .map(TroubleTimelineResponse.TroublePoint::getTroubleIndex)
        .filter(Objects::nonNull)
        .mapToDouble(Double::doubleValue)
        .average().orElse(0);
  }

  /** @return [peakStart, peakEnd, maxLen] */
  private int[] findPeakRange(List<TroubleTimelineResponse.TroublePoint> timeline, double threshold) {
    int peakStart = 0, peakEnd = 0, maxLen = 0, curStart = 0, curLen = 0;
    for (TroubleTimelineResponse.TroublePoint p : timeline) {
      if (p.getTroubleIndex() != null && p.getTroubleIndex() > threshold) {
        if (curLen == 0) curStart = p.getDayFromStart();
        curLen++;
        if (curLen > maxLen) { maxLen = curLen; peakStart = curStart; peakEnd = p.getDayFromStart(); }
      } else {
        curLen = 0;
      }
    }
    return new int[]{peakStart, peakEnd, maxLen};
  }

  private Integer roundToInt(Double value) {
    return value != null ? (int) Math.round(value) : null;
  }

  private CycleDetailResponse.Metrics toAverageMetrics(List<DetailedSkinAnalysis> details) {
    return CycleDetailResponse.Metrics.builder()
        .trouble(roundToInt(avgMetricValue(details, d -> d.getTrouble() != null ? d.getTrouble().doubleValue() : null)))
        .sebum(roundToInt(avgMetricValue(details, d -> d.getSebum() != null ? d.getSebum().doubleValue() : null)))
        .dullness(roundToInt(avgMetricValue(details, d -> d.getDullness() != null ? d.getDullness().doubleValue() : null)))
        .moisture(roundToInt(avgMetricValue(details, d -> d.getMoisture() != null ? d.getMoisture().doubleValue() : null)))
        .elasticity(roundToInt(avgMetricValue(details, d -> d.getElasticity() != null ? d.getElasticity().doubleValue() : null)))
        .build();
  }

  private String phaseLabel(PhaseType pt) {
    return switch (pt) {
      case MENSTRUATION -> "생리기";
      case OVULATION -> "배란기";
      case LUTEAL -> "황체기";
      default -> pt.name();
    };
  }

  private boolean hasBadFood(String diet) {
    if (diet == null) return false;
    return Arrays.stream(diet.split(",")).anyMatch(BAD_FOODS::contains);
  }

  private boolean hasGoodFood(String diet) {
    if (diet == null) return false;
    return Arrays.stream(diet.split(",")).noneMatch(BAD_FOODS::contains);
  }

  // 실측 평균 차이가 이 이하면 "비슷함"으로 보고 라벨을 "-"로 표시 (AnalysisService.calcChange와 동일 기준)
  private static final int SIMILAR_THRESHOLD = 5;

  private void addFactorIfSignificant(
      List<LifestyleInsightResponse.LifestyleFactor> factors,
      List<AiAnalysis> analyses,
      Map<Long, DetailedSkinAnalysis> detailMap,
      Predicate<AiAnalysis> badCondition,
      Predicate<AiAnalysis> goodCondition,
      Function<DetailedSkinAnalysis, Integer> metric,
      String condition,
      String negativeLabel,
      String positiveLabel,
      String similarLabel) {

    Optional<Double> badAvg = avgMetric(analyses.stream().filter(badCondition).collect(Collectors.toList()), detailMap, metric);
    Optional<Double> goodAvg = avgMetric(analyses.stream().filter(goodCondition).collect(Collectors.toList()), detailMap, metric);

    // 해당 항목을 한 번도 기록 안 했으면(양쪽 다 데이터 없음) 요인 자체를 응답에서 제외
    if (badAvg.isEmpty() && goodAvg.isEmpty()) return;

    // impactType과 impactLabel이 항상 같은 방향을 가리키도록, 양쪽 다 있으면 실측 평균 차이의
    // 부호로 type을 정하고(작은 차이라도), 한쪽만 있으면 그 기준치 방향을 그대로 따른다.
    boolean isNegative = (badAvg.isPresent() && goodAvg.isPresent())
        ? badAvg.get() > goodAvg.get()
        : badAvg.isPresent();
    String impactType = isNegative ? "negative" : "positive";

    // impactLabel: 양쪽 다 있으면 차이가 5 이하일 때만 "비슷함(-)"으로, 그 외엔 위 type과 같은 방향의 화살표
    String impactLabel;
    if (badAvg.isPresent() && goodAvg.isPresent() && Math.abs(badAvg.get() - goodAvg.get()) <= SIMILAR_THRESHOLD) {
      impactLabel = similarLabel;
    } else {
      impactLabel = isNegative ? negativeLabel : positiveLabel;
    }

    factors.add(LifestyleInsightResponse.LifestyleFactor.builder()
        .condition(condition)
        .impactType(impactType)
        .impactLabel(impactLabel)
        .build());
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

  private Double avgMetricValue(List<DetailedSkinAnalysis> details, Function<DetailedSkinAnalysis, Double> extractor) {
    return details.stream()
        .map(extractor)
        .filter(Objects::nonNull)
        .mapToDouble(Double::doubleValue)
        .average()
        .stream().boxed().findFirst().orElse(null);
  }
}