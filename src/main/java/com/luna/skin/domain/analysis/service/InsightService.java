package com.luna.skin.domain.analysis.service;

import com.luna.skin.domain.analysis.dto.response.LifestyleInsightResponse;
import com.luna.skin.domain.analysis.entity.AiAnalysis;
import com.luna.skin.domain.analysis.entity.DetailedSkinAnalysis;
import com.luna.skin.domain.analysis.repository.AiAnalysisRepository;
import com.luna.skin.domain.analysis.repository.DetailedSkinAnalysisRepository;
import com.luna.skin.domain.skin.enums.ExerciseTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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

    // 자극적 식단
    List<String> badFoods = List.of("SPICY_FOOD", "CAFFEINE", "HIGH_FAT", "SUGAR", "SODA", "ALCOHOL");
    addFactorIfSignificant(factors, analyses, detailMap,
        a -> {
          String diet = a.getTodaySkin().getDietType();
          if (diet == null) return false;
          return Arrays.stream(diet.split(",")).anyMatch(badFoods::contains);
        },
        a -> {
          String diet = a.getTodaySkin().getDietType();
          if (diet == null) return true;
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

    double badAvg = avgMetric(analyses.stream().filter(badCondition).collect(Collectors.toList()), detailMap, metric);
    double goodAvg = avgMetric(analyses.stream().filter(goodCondition).collect(Collectors.toList()), detailMap, metric);

    if (badAvg == 0 && goodAvg == 0) return;

    if (badAvg > goodAvg) {
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

  private double avgMetric(List<AiAnalysis> list, Map<Long, DetailedSkinAnalysis> detailMap,
      Function<DetailedSkinAnalysis, Integer> getter) {
    return list.stream()
        .map(a -> detailMap.get(a.getAnalysisId()))
        .filter(Objects::nonNull)
        .map(getter)
        .filter(Objects::nonNull)
        .mapToInt(Integer::intValue)
        .average()
        .orElse(0);
  }
}