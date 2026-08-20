package com.luna.skin.domain.analysis.service;

import com.luna.skin.domain.analysis.dto.request.SkinAnalysisRequest;
import com.luna.skin.domain.analysis.dto.response.HomeSkinStatusResponse;
import com.luna.skin.domain.analysis.dto.response.ImageUploadResponse;
import com.luna.skin.domain.analysis.dto.response.SkinAnalysisResponse;
import com.luna.skin.domain.analysis.dto.response.SkinCompareResponse;
import com.luna.skin.domain.analysis.entity.AiAnalysis;
import com.luna.skin.domain.analysis.entity.DetailedSkinAnalysis;
import com.luna.skin.domain.analysis.enums.SkinStatusLabel;
import com.luna.skin.domain.analysis.exception.AnalysisErrorCode;
import com.luna.skin.domain.analysis.repository.AiAnalysisRepository;
import com.luna.skin.domain.analysis.repository.DetailedSkinAnalysisRepository;
import com.luna.skin.domain.cycle.entity.CyclePhase;
import com.luna.skin.domain.cycle.repository.CyclePhaseRepository;
import com.luna.skin.domain.skin.entity.TodaySkin;
import com.luna.skin.domain.skin.enums.SkinStatus;
import com.luna.skin.domain.skin.repository.TodaySkinRepository;
import com.luna.skin.domain.user.entity.User;
import com.luna.skin.domain.user.repository.UserRepository;
import com.luna.skin.global.config.StorageProperties;
import com.luna.skin.global.exception.CommonErrorCode;
import com.luna.skin.global.exception.CustomException;
import com.luna.skin.global.storage.ImageStorageService;
import com.luna.skin.infra.openai.OpenAiSkinAnalysisResult;
import com.luna.skin.infra.openai.service.OpenAiAnalysisService;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalysisService {

  private final ImageStorageService imageStorageService;
  private final TodaySkinRepository todaySkinRepository;
  private final AiAnalysisRepository aiAnalysisRepository;
  private final DetailedSkinAnalysisRepository detailedSkinAnalysisRepository;
  private final CyclePhaseRepository cyclePhaseRepository;
  private final UserRepository userRepository;
  private final OpenAiAnalysisService openAiAnalysisService;
  private final StorageProperties storageProperties;

  @Autowired
  @Lazy
  private AnalysisService self;

  public ImageUploadResponse uploadImages(Long userId, MultipartFile image,
      MultipartFile leftImage, MultipartFile rightImage) {
    validateImageFile(image);
    if (leftImage != null && !leftImage.isEmpty()) validateImageFile(leftImage);
    if (rightImage != null && !rightImage.isEmpty()) validateImageFile(rightImage);
    log.info("userId: {} 사진 업로드", userId);

    String imageUrl = imageStorageService.store(image, "analysis");
    String leftImageUrl = leftImage != null && !leftImage.isEmpty()
        ? imageStorageService.store(leftImage, "analysis") : null;
    String rightImageUrl = rightImage != null && !rightImage.isEmpty()
        ? imageStorageService.store(rightImage, "analysis") : null;

    return ImageUploadResponse.builder()
        .imageUrl(imageUrl)
        .leftImageUrl(leftImageUrl)
        .rightImageUrl(rightImageUrl)
        .build();
  }

  /**
   * OpenAI 호출은 DB 트랜잭션 밖에서 수행한다.
   * 1) 짧은 트랜잭션으로 오늘의 분석 요청을 원자적으로 예약(중복 체크 + TodaySkin 저장)하고 커밋
   * 2) 트랜잭션 없이 OpenAI 네트워크 호출 (지연/장애가 나도 DB 커넥션·락을 물고 있지 않음)
   * 3) 결과를 짧은 트랜잭션으로 저장
   * OpenAI 호출이 실패하면 예약을 취소해, 같은 날짜로 재시도할 수 있게 한다.
   */
  public SkinAnalysisResponse analyze(Long userId, LocalDate date, SkinAnalysisRequest request) {
    log.info("imageUrl: {}", request.getImageUrl());
    log.info("request: {}", request);

    ReservationResult reservation = self.reserveTodaySkin(userId, date, request);

    OpenAiSkinAnalysisResult gptResult;
    try {
      gptResult = openAiAnalysisService.analyzeSkin(
          request.getImageUrl(),
          request.getLeftImageUrl(),
          request.getRightImageUrl(),
          reservation.phaseType());
    } catch (RuntimeException e) {
      self.cancelReservation(reservation);
      throw e;
    }

    return self.saveAnalysisResult(reservation.todaySkinId(), reservation.phaseType(), date, gptResult, userId);
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public ReservationResult reserveTodaySkin(Long userId, LocalDate date, SkinAnalysisRequest request) {
    // dietType이 빈 리스트로 오면 ""가 되어 chk_diet_type 제약을 위반하므로 null로 처리
    String dietTypeStr = request.getDietType() == null || request.getDietType().isEmpty() ? null :
        request.getDietType().stream()
            .map(Enum::name)
            .collect(Collectors.joining(","));

    // 같은 날짜에 이미 기록이 있으면 새로 만들지 않고 덮어쓴다
    TodaySkin todaySkin = todaySkinRepository.findByUserUserIdAndLogDate(userId, date)
        .orElse(null);
    boolean isNewRecord = todaySkin == null;
    // OpenAI 호출이 실패했을 때 되돌릴 수 있도록 덮어쓰기 전 값을 보존
    TodaySkinSnapshot previousSnapshot = todaySkin != null ? TodaySkinSnapshot.of(todaySkin) : null;

    if (todaySkin != null) {
      todaySkin.update(request.getImageUrl(), request.getLeftImageUrl(), request.getRightImageUrl(),
          request.getSleepTime(), request.getWaterIntake(), dietTypeStr,
          request.getExerciseTime(), request.getSkinStatus());
    } else {
      User user = userRepository.findById(userId)
          .orElseThrow(() -> new CustomException(CommonErrorCode.NOT_FOUND));

      todaySkin = TodaySkin.builder()
          .user(user)
          .logDate(date)
          .imageUrl(request.getImageUrl())
          .leftImageUrl(request.getLeftImageUrl())
          .rightImageUrl(request.getRightImageUrl())
          .sleepTime(request.getSleepTime())
          .waterIntake(request.getWaterIntake())
          .dietType(dietTypeStr)
          .exerciseTime(request.getExerciseTime())
          .skinStatus(request.getSkinStatus())
          .build();
      try {
        todaySkinRepository.save(todaySkin);
      } catch (DataIntegrityViolationException e) {
        // 동시 요청으로 유니크 제약을 동시에 통과한 경우: 방금 저장된 기록을 다시 조회해 덮어쓴다
        if (e.getMessage() != null && e.getMessage().contains("uq_today_skin_user_date")) {
          todaySkin = todaySkinRepository.findByUserUserIdAndLogDate(userId, date)
              .orElseThrow(() -> e);
          isNewRecord = false;
          previousSnapshot = TodaySkinSnapshot.of(todaySkin);
          todaySkin.update(request.getImageUrl(), request.getLeftImageUrl(), request.getRightImageUrl(),
              request.getSleepTime(), request.getWaterIntake(), dietTypeStr,
              request.getExerciseTime(), request.getSkinStatus());
        } else {
          throw e;
        }
      }
    }

    CyclePhase cyclePhase = cyclePhaseRepository.findByCyclePhaseAtNow(userId, date)
        .orElse(null);
    String phaseType = cyclePhase != null ? cyclePhase.getPhaseType().name() : "UNKNOWN";

    return new ReservationResult(todaySkin.getTodaySkinId(), phaseType, isNewRecord, previousSnapshot);
  }

  /**
   * OpenAI 호출 실패 시 예약을 되돌린다.
   * 새로 만든 기록이면 삭제하고, 기존 기록을 덮어쓴 경우라면 덮어쓰기 전 값으로 복원한다.
   */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void cancelReservation(ReservationResult reservation) {
    if (reservation.isNewRecord()) {
      todaySkinRepository.deleteById(reservation.todaySkinId());
      return;
    }
    TodaySkin todaySkin = todaySkinRepository.getReferenceById(reservation.todaySkinId());
    TodaySkinSnapshot snapshot = reservation.previousSnapshot();
    todaySkin.update(snapshot.imageUrl(), snapshot.leftImageUrl(), snapshot.rightImageUrl(),
        snapshot.sleepTime(), snapshot.waterIntake(), snapshot.dietType(),
        snapshot.exerciseTime(), snapshot.skinStatus());
  }

  @CacheEvict(value = {"troubleTimeline", "cycleDetail", "lifestyleInsight"}, key = "#userId")
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public SkinAnalysisResponse saveAnalysisResult(
      Long todaySkinId, String phaseType, LocalDate date, OpenAiSkinAnalysisResult gptResult, Long userId) {

    TodaySkin todaySkin = todaySkinRepository.getReferenceById(todaySkinId);
    SkinStatusLabel skinStatusLabel = SkinStatusLabel.from(gptResult.getOverallScore());

    // 같은 today_skin에 이미 분석 결과가 있으면(재분석) 새로 만들지 않고 덮어쓴다
    AiAnalysis aiAnalysis = aiAnalysisRepository.findByTodaySkin_TodaySkinId(todaySkinId)
        .orElse(null);
    if (aiAnalysis != null) {
      aiAnalysis.update(gptResult.getOverallScore(), skinStatusLabel,
          gptResult.getAiComment(), gptResult.getPhaseComment());
    } else {
      aiAnalysis = AiAnalysis.builder()
          .todaySkin(todaySkin)
          .overallScore(gptResult.getOverallScore())
          .skinStatusLabel(skinStatusLabel)
          .aiComment(gptResult.getAiComment())
          .phaseComment(gptResult.getPhaseComment())
          .build();
      aiAnalysisRepository.save(aiAnalysis);
    }

    DetailedSkinAnalysis detail = detailedSkinAnalysisRepository.findByAiAnalysis(aiAnalysis)
        .orElse(null);
    if (detail != null) {
      detail.update(gptResult.getTrouble(), gptResult.getSebum(), gptResult.getDullness(),
          gptResult.getMoisture(), gptResult.getElasticity());
    } else {
      detail = DetailedSkinAnalysis.builder()
          .aiAnalysis(aiAnalysis)
          .trouble(gptResult.getTrouble())
          .sebum(gptResult.getSebum())
          .dullness(gptResult.getDullness())
          .moisture(gptResult.getMoisture())
          .elasticity(gptResult.getElasticity())
          .build();
      detailedSkinAnalysisRepository.save(detail);
    }

    return toSkinAnalysisResponse(aiAnalysis, detail, phaseType);
  }

  private record TodaySkinSnapshot(String imageUrl, String leftImageUrl, String rightImageUrl,
      Double sleepTime, Double waterIntake, String dietType, Integer exerciseTime,
      SkinStatus skinStatus) {
    static TodaySkinSnapshot of(TodaySkin todaySkin) {
      return new TodaySkinSnapshot(todaySkin.getImageUrl(), todaySkin.getLeftImageUrl(),
          todaySkin.getRightImageUrl(), todaySkin.getSleepTime(), todaySkin.getWaterIntake(),
          todaySkin.getDietType(), todaySkin.getExerciseTime(), todaySkin.getSkinStatus());
    }
  }

  private record ReservationResult(Long todaySkinId, String phaseType, boolean isNewRecord,
      TodaySkinSnapshot previousSnapshot) {}

  public SkinAnalysisResponse getAnalysisByDate(Long userId, LocalDate date) {
    AiAnalysis aiAnalysis = aiAnalysisRepository.findByTodaySkinUserUserIdAndTodaySkinLogDate(userId, date)
        .orElseThrow(() -> new CustomException(AnalysisErrorCode.ANALYSIS_NOT_FOUND));

    DetailedSkinAnalysis detail = detailedSkinAnalysisRepository.findByAiAnalysis(aiAnalysis)
        .orElseThrow(() -> new CustomException(AnalysisErrorCode.ANALYSIS_NOT_FOUND));

    String phaseType = cyclePhaseRepository.findByCyclePhaseAtNow(userId, date)
        .map(cp -> cp.getPhaseType().name())
        .orElse("UNKNOWN");

    return toSkinAnalysisResponse(aiAnalysis, detail, phaseType);
  }

  public HomeSkinStatusResponse getTodaySkinStatus(Long userId) {
    return aiAnalysisRepository.findByTodaySkinUserUserIdAndTodaySkinLogDate(userId, LocalDate.now())
        .map(ai -> HomeSkinStatusResponse.builder()
            .skinStatus(ai.getSkinStatusLabel() != null
                ? ai.getSkinStatusLabel().toLabel()
                : "모름")
            .aiComment(ai.getAiComment())
            .build())
        .orElse(HomeSkinStatusResponse.builder()
            .skinStatus("모름")
            .build());
  }

  public SkinCompareResponse compare(Long userId, LocalDate dateA, LocalDate dateB) {
    AiAnalysis analysisA = aiAnalysisRepository
        .findByTodaySkinUserUserIdAndTodaySkinLogDate(userId, dateA)
        .orElseThrow(() -> new CustomException(AnalysisErrorCode.ANALYSIS_NOT_FOUND));
    AiAnalysis analysisB = aiAnalysisRepository
        .findByTodaySkinUserUserIdAndTodaySkinLogDate(userId, dateB)
        .orElseThrow(() -> new CustomException(AnalysisErrorCode.ANALYSIS_NOT_FOUND));

    DetailedSkinAnalysis detailA = detailedSkinAnalysisRepository.findByAiAnalysis(analysisA)
        .orElseThrow(() -> new CustomException(AnalysisErrorCode.ANALYSIS_NOT_FOUND));
    DetailedSkinAnalysis detailB = detailedSkinAnalysisRepository.findByAiAnalysis(analysisB)
        .orElseThrow(() -> new CustomException(AnalysisErrorCode.ANALYSIS_NOT_FOUND));

    String aiComment = openAiAnalysisService.generateCompareComment(
        analysisA.getOverallScore(), detailA.getTrouble(), detailA.getSebum(),
        detailA.getDullness(), detailA.getMoisture(), detailA.getElasticity(),
        analysisB.getOverallScore(), detailB.getTrouble(), detailB.getSebum(),
        detailB.getDullness(), detailB.getMoisture(), detailB.getElasticity());

    return SkinCompareResponse.builder()
        .dateA(toSnapshot(analysisA, detailA))
        .dateB(toSnapshot(analysisB, detailB))
        .changes(toChanges(detailA, detailB))
        .aiComment(aiComment)
        .build();
  }

  private SkinCompareResponse.DaySnapshot toSnapshot(AiAnalysis analysis, DetailedSkinAnalysis detail) {
    SkinStatusLabel label = analysis.getSkinStatusLabel();
    return SkinCompareResponse.DaySnapshot.builder()
        .date(analysis.getTodaySkin().getLogDate().toString())
        .imageUrl(analysis.getTodaySkin().getImageUrl())
        .overallScore(analysis.getOverallScore())
        .skinStatus(label != null ? label.toLabel() : "모름")
        .metrics(SkinCompareResponse.Metrics.builder()
            .trouble(detail.getTrouble())
            .sebum(detail.getSebum())
            .dullness(detail.getDullness())
            .moisture(detail.getMoisture())
            .elasticity(detail.getElasticity())
            .build())
        .build();
  }

  private SkinCompareResponse.MetricChanges toChanges(DetailedSkinAnalysis a, DetailedSkinAnalysis b) {
    return SkinCompareResponse.MetricChanges.builder()
        .trouble(calcChange(a.getTrouble(), b.getTrouble()))
        .sebum(calcChange(a.getSebum(), b.getSebum()))
        .dullness(calcChange(a.getDullness(), b.getDullness()))
        .moisture(calcChange(a.getMoisture(), b.getMoisture()))
        .elasticity(calcChange(a.getElasticity(), b.getElasticity()))
        .build();
  }

  private String calcChange(int before, int after) {
    int diff = after - before;
    if (Math.abs(diff) <= 5) return "SIMILAR";
    return diff > 0 ? "IMPROVED" : "WORSENED";
  }

  private void validateImageFile(MultipartFile image) {
    if (image == null || image.isEmpty()) {
      throw new CustomException(AnalysisErrorCode.EMPTY_FILE);
    }
    String originalFilename = image.getOriginalFilename();
    if (originalFilename == null || originalFilename.isEmpty()) {
      throw new CustomException(AnalysisErrorCode.INVALID_FILE_TYPE);
    }
    String ext = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();
    if (!List.of("jpg", "jpeg", "png").contains(ext)) {
      throw new CustomException(AnalysisErrorCode.INVALID_FILE_TYPE);
    }
  }

  private SkinAnalysisResponse toSkinAnalysisResponse(AiAnalysis aiAnalysis, DetailedSkinAnalysis detail, String phaseType) {
    TodaySkin todaySkin = aiAnalysis.getTodaySkin();
    SkinStatusLabel label = aiAnalysis.getSkinStatusLabel();

    return SkinAnalysisResponse.builder()
        .analysisId(aiAnalysis.getAnalysisId())
        .date(todaySkin.getLogDate().toString())
        .imageUrl(todaySkin.getImageUrl())
        .leftImageUrl(todaySkin.getLeftImageUrl())
        .rightImageUrl(todaySkin.getRightImageUrl())
        .overallScore(aiAnalysis.getOverallScore())
        .skinStatus(label != null ? label.toLabel() : "모름")
        .cyclePhase(phaseType)
        .phaseComment(aiAnalysis.getPhaseComment())
        .aiComment(aiAnalysis.getAiComment())
        .detailedMetrics(SkinAnalysisResponse.DetailedMetrics.builder()
            .trouble(detail.getTrouble())
            .sebum(detail.getSebum())
            .dullness(detail.getDullness())
            .moisture(detail.getMoisture())
            .elasticity(detail.getElasticity())
            .build())
        .build();
  }
}