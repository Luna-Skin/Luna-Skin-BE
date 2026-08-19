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
import com.luna.skin.domain.skin.entity.TodaySkin;
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
   * - AI가 주는 trouble 원점수는 "높을수록 건강함"(0개=100점)이라, 트러블이 심한 정도를 보여주는
   *   troubleIndex는 (100 - trouble)로 뒤집어서 계산한다 (값이 높을수록 트러블이 심함).
   * - 매일 기록을 전제로 하지 않으므로, 특정 day의 값은 그 날짜 하나가 아니라 앞뒤
   *   {@link #TIMELINE_WINDOW}일(기본 ±2일)을 같이 묶어 이동평균으로 완만하게 계산한다.
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
          // AI가 주는 trouble 원점수는 "높을수록 건강함"이라, 트러블이 심한 정도로 보여주려면 뒤집어야 함
          troubleByDay.get(d).add(100 - detail.getTrouble());
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
   * - trouble/sebum/dullness는 AI 원점수(높을수록 좋음)를 (100 - 점수)로 뒤집어서 "심한 정도"로 반환.
   *   moisture/elasticity는 원래부터 높을수록 좋은 의미라 그대로 반환.
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
   * 생활습관이 하나라도 기록된 가장 최근 분석을 "오늘"로 잡고(진짜 최신 기록이라도 습관 입력이
   * 아예 없으면 건너뜀), 그 이전 전체 기록의 평균("baseline")과 비교해서 baseline보다 뚜렷하게
   * (5 이상) 나빠지거나 좋아진 피부 지표를 찾는다. 트러블/건조도/칙칙함은 각각 최대 1개 factor로만
   * 나가며, 그 지표에 매칭되는 습관은 오늘 그 지표 방향에 해당하는 습관들(나빠졌으면 오늘 기준치
   * 미달 습관, 좋아졌으면 기준치 충족 습관) 중 이 유저의 과거 기록에서 그 지표와 가장 상관관계가
   * 컸던(양호군·불량군 평균 차이가 가장 큰) 습관 하나로 고른다.
   *
   * - 습관이 기록된 분석이 없으면(전부 미입력) 빈 배열
   * - 비교할 과거 기록(baseline)이 없으면(분석이 1건 이하) 빈 배열
   * - 지표가 뚜렷하게 변하지 않았거나, 매칭할 습관 근거가 없으면 그 지표는 factor로 안 나감
   * - 결과는 캐싱되며 새 분석 저장 시 evict
   *
   * @param userId 조회할 사용자 식별자
   * @return 생활습관 영향 분석 응답 DTO (factors: condition, impactType, impactLabel)
   */
  @Cacheable(value = "lifestyleInsight", key = "#userId")
  public LifestyleInsightResponse getLifestyleInsight(Long userId) {
    List<AiAnalysis> analyses = aiAnalysisRepository.findAllByUserIdWithTodaySkin(userId);
    if (analyses.size() < 2) {
      return LifestyleInsightResponse.builder().factors(List.of()).build();
    }

    // "오늘"은 생활습관이 하나라도 기록된 가장 최근 분석으로 잡는다.
    // 진짜 최신 기록에 습관 입력이 아예 없으면(분석만 하고 습관은 안 채운 날) 매칭할 게 없으니,
    // 습관이 있는 가장 최근 기록으로 대신 비교한다.
    AiAnalysis latest = analyses.stream()
        .filter(a -> hasAnyHabitData(a.getTodaySkin()))
        .max(Comparator.comparing(a -> a.getTodaySkin().getLogDate()))
        .orElse(null);
    if (latest == null) {
      return LifestyleInsightResponse.builder().factors(List.of()).build();
    }

    Map<Long, DetailedSkinAnalysis> detailMap = detailedSkinAnalysisRepository
        .findAllByAiAnalysisIn(analyses).stream()
        .collect(Collectors.toMap(d -> d.getAiAnalysis().getAnalysisId(), d -> d));

    DetailedSkinAnalysis latestDetail = detailMap.get(latest.getAnalysisId());
    if (latestDetail == null) {
      return LifestyleInsightResponse.builder().factors(List.of()).build();
    }

    List<AiAnalysis> pastAnalyses = analyses.stream()
        .filter(a -> !a.getAnalysisId().equals(latest.getAnalysisId()))
        .collect(Collectors.toList());

    List<Habit> habits = List.of(
        new Habit("수면 6시간 미만", "수면 6시간 이상",
            a -> a.getTodaySkin().getSleepTime() != null && a.getTodaySkin().getSleepTime() < 6,
            a -> a.getTodaySkin().getSleepTime() != null && a.getTodaySkin().getSleepTime() >= 6),
        new Habit("수분 섭취 부족", "수분 충분 섭취",
            a -> a.getTodaySkin().getWaterIntake() != null && a.getTodaySkin().getWaterIntake() < 1.5,
            a -> a.getTodaySkin().getWaterIntake() != null && a.getTodaySkin().getWaterIntake() >= 1.5),
        new Habit("운동 부족", "운동 충분",
            a -> a.getTodaySkin().getExerciseTime() != null && a.getTodaySkin().getExerciseTime() < 30,
            a -> a.getTodaySkin().getExerciseTime() != null && a.getTodaySkin().getExerciseTime() >= 30),
        new Habit("자극적 식단", "자극적이지 않은 식단",
            a -> hasBadFood(a.getTodaySkin().getDietType()),
            a -> hasGoodFood(a.getTodaySkin().getDietType())));

    // trouble/moisture/dullness는 전부 AI 원점수가 "높을수록 좋은 상태"라 lowerIsWorse=true
    List<MetricSpec> metrics = List.of(
        new MetricSpec(DetailedSkinAnalysis::getTrouble, true, "트러블 ↑", "트러블 ↓"),
        new MetricSpec(DetailedSkinAnalysis::getMoisture, true, "건조도 ↑", "건조도 ↓"),
        new MetricSpec(DetailedSkinAnalysis::getDullness, true, "칙칙함 ↑", "칙칙함 ↓"));

    List<LifestyleInsightResponse.LifestyleFactor> factors = new ArrayList<>();
    for (MetricSpec metric : metrics) {
      Integer todayValue = metric.extractor().apply(latestDetail);
      if (todayValue == null) continue;
      Optional<Double> baseline = avgMetric(pastAnalyses, detailMap, metric.extractor());
      if (baseline.isEmpty()) continue;

      double diff = metric.lowerIsWorse() ? baseline.get() - todayValue : todayValue - baseline.get(); // 양수 = 나빠짐
      if (diff > SIMILAR_THRESHOLD) {
        matchHabit(habits, h -> h.isBad().test(latest), pastAnalyses, detailMap, metric)
            .ifPresent(h -> factors.add(factor(h.badCondition(), "negative", metric.worsenedLabel())));
      } else if (diff < -SIMILAR_THRESHOLD) {
        matchHabit(habits, h -> h.isGood().test(latest), pastAnalyses, detailMap, metric)
            .ifPresent(h -> factors.add(factor(h.goodCondition(), "positive", metric.improvedLabel())));
      }
    }

    return LifestyleInsightResponse.builder().factors(factors).build();
  }

  private record Habit(String badCondition, String goodCondition,
      Predicate<AiAnalysis> isBad, Predicate<AiAnalysis> isGood) {}

  private record MetricSpec(Function<DetailedSkinAnalysis, Integer> extractor, boolean lowerIsWorse,
      String worsenedLabel, String improvedLabel) {}

  /**
   * 오늘 그 방향(나쁨/좋음)에 해당하는 습관들 중, 과거 기록에서 이 지표와 가장 상관관계가 컸던
   * (양호군·불량군 평균 차이가 가장 큰) 습관 하나를 고른다. 오늘 해당하는 습관이 없거나, 과거
   * 데이터가 부족해 상관관계를 계산할 수 없으면 빈 값을 반환한다.
   */
  private Optional<Habit> matchHabit(List<Habit> habits, Predicate<Habit> todayCondition,
      List<AiAnalysis> pastAnalyses, Map<Long, DetailedSkinAnalysis> detailMap, MetricSpec metric) {
    Habit best = null;
    double bestScore = Double.NEGATIVE_INFINITY;
    for (Habit habit : habits) {
      if (!todayCondition.test(habit)) continue;
      Optional<Double> badAvg = avgMetric(pastAnalyses.stream().filter(habit.isBad()).collect(Collectors.toList()), detailMap, metric.extractor());
      Optional<Double> goodAvg = avgMetric(pastAnalyses.stream().filter(habit.isGood()).collect(Collectors.toList()), detailMap, metric.extractor());
      if (badAvg.isEmpty() || goodAvg.isEmpty()) continue;
      double score = metric.lowerIsWorse() ? goodAvg.get() - badAvg.get() : badAvg.get() - goodAvg.get();
      if (score > bestScore) {
        bestScore = score;
        best = habit;
      }
    }
    return Optional.ofNullable(best);
  }

  private Optional<Double> avgMetric(List<AiAnalysis> list, Map<Long, DetailedSkinAnalysis> detailMap,
      Function<DetailedSkinAnalysis, Integer> getter) {
    return list.stream()
        .map(a -> detailMap.get(a.getAnalysisId()))
        .filter(Objects::nonNull)
        .map(getter)
        .filter(Objects::nonNull)
        .mapToInt(Integer::intValue)
        .average()
        .stream().boxed().findFirst();
  }

  private LifestyleInsightResponse.LifestyleFactor factor(String condition, String impactType, String impactLabel) {
    return LifestyleInsightResponse.LifestyleFactor.builder()
        .condition(condition)
        .impactType(impactType)
        .impactLabel(impactLabel)
        .build();
  }

  // ======================== Calculations ========================

  /** 날짜 범위의 DetailedSkinAnalysis를 날짜 기준 맵으로 반환 */
  private Map<LocalDate, DetailedSkinAnalysis> fetchDetailByDate(Long userId, LocalDate from, LocalDate to) {
    List<AiAnalysis> analyses = aiAnalysisRepository.findAllByUserIdAndLogDateBetween(userId, from, to);
    return detailedSkinAnalysisRepository.findAllByAiAnalysisIn(analyses).stream()
        .collect(Collectors.toMap(d -> d.getAiAnalysis().getTodaySkin().getLogDate(), d -> d));
  }

  // 트러블 타임라인 이동평균 반경 (매일 기록을 안 하는 유저가 많아 특정 하루 값만 보면 들쭉날쭉해짐)
  private static final int TIMELINE_WINDOW = 3;

  private List<TroubleTimelineResponse.TroublePoint> buildTimeline(Map<Integer, List<Integer>> troubleByDay) {
    List<TroubleTimelineResponse.TroublePoint> timeline = new ArrayList<>();
    for (int d = -14; d <= 14; d++) {
      List<Integer> windowScores = new ArrayList<>();
      for (int w = d - TIMELINE_WINDOW; w <= d + TIMELINE_WINDOW; w++) {
        List<Integer> scores = troubleByDay.get(w);
        if (scores != null) windowScores.addAll(scores);
      }
      Double avg = windowScores.isEmpty() ? null : windowScores.stream().mapToInt(Integer::intValue).average().orElse(0);
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
    // trouble/sebum/dullness는 AI 원점수(높을수록 좋음)를 그대로 노출하지 않고, "심한 정도"로 뒤집어서 보여준다.
    // moisture/elasticity는 원래부터 높을수록 좋은 의미라 그대로 노출한다.
    return CycleDetailResponse.Metrics.builder()
        .trouble(invertScore(roundToInt(avgMetricValue(details, d -> d.getTrouble() != null ? d.getTrouble().doubleValue() : null))))
        .sebum(invertScore(roundToInt(avgMetricValue(details, d -> d.getSebum() != null ? d.getSebum().doubleValue() : null))))
        .dullness(invertScore(roundToInt(avgMetricValue(details, d -> d.getDullness() != null ? d.getDullness().doubleValue() : null))))
        .moisture(roundToInt(avgMetricValue(details, d -> d.getMoisture() != null ? d.getMoisture().doubleValue() : null)))
        .elasticity(roundToInt(avgMetricValue(details, d -> d.getElasticity() != null ? d.getElasticity().doubleValue() : null)))
        .build();
  }

  private Integer invertScore(Integer score) {
    return score != null ? 100 - score : null;
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

  /** 수면/수분/운동/식단 중 하나라도 기록돼 있는지 */
  private boolean hasAnyHabitData(TodaySkin todaySkin) {
    return todaySkin.getSleepTime() != null
        || todaySkin.getWaterIntake() != null
        || todaySkin.getExerciseTime() != null
        || todaySkin.getDietType() != null;
  }

  // baseline 대비 오늘 값이 이 이상 나빠지거나 좋아지면 "뚜렷한 변화"로 판단 (AnalysisService.calcChange와 동일 기준)
  private static final int SIMILAR_THRESHOLD = 5;

  private Double avgMetricValue(List<DetailedSkinAnalysis> details, Function<DetailedSkinAnalysis, Double> extractor) {
    return details.stream()
        .map(extractor)
        .filter(Objects::nonNull)
        .mapToDouble(Double::doubleValue)
        .average()
        .stream().boxed().findFirst().orElse(null);
  }
}