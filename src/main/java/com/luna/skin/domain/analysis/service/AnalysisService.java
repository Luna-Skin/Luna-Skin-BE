package com.luna.skin.domain.analysis.service;

import com.luna.skin.domain.analysis.dto.request.SkinAnalysisRequest;
import com.luna.skin.domain.analysis.dto.response.HomeSkinStatusResponse;
import com.luna.skin.domain.analysis.dto.response.ImageUploadResponse;
import com.luna.skin.domain.analysis.dto.response.SkinAnalysisResponse;
import com.luna.skin.domain.analysis.entity.AiAnalysis;
import com.luna.skin.domain.analysis.entity.DetailedSkinAnalysis;
import com.luna.skin.domain.analysis.enums.SkinStatusLabel;
import com.luna.skin.domain.analysis.exception.AnalysisErrorCode;
import com.luna.skin.domain.analysis.repository.AiAnalysisRepository;
import com.luna.skin.domain.analysis.repository.DetailedSkinAnalysisRepository;
import com.luna.skin.domain.cycle.entity.CyclePhase;
import com.luna.skin.domain.cycle.repository.CyclePhaseRepository;
import com.luna.skin.domain.skin.entity.TodaySkin;
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

  public ImageUploadResponse uploadImage(Long userId, MultipartFile image) {
    validateImageFile(image);
    log.info("userId: {} 사진 업로드", userId);
    String imageUrl = imageStorageService.store(image, "analysis");
    return ImageUploadResponse.builder()
        .imageUrl(imageUrl)
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
          request.getImageUrl(), reservation.phaseType(), storageProperties.baseDir());
    } catch (RuntimeException e) {
      self.cancelReservation(reservation.todaySkinId());
      throw e;
    }

    return self.saveAnalysisResult(reservation.todaySkinId(), reservation.phaseType(), date, gptResult, userId);
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public ReservationResult reserveTodaySkin(Long userId, LocalDate date, SkinAnalysisRequest request) {
    if (todaySkinRepository.findByUserUserIdAndLogDate(userId, date).isPresent()) {
      throw new CustomException(AnalysisErrorCode.ALREADY_ANALYZED);
    }

    User user = userRepository.findById(userId)
        .orElseThrow(() -> new CustomException(CommonErrorCode.NOT_FOUND));

    String dietTypeStr = request.getDietType() == null ? null :
        request.getDietType().stream()
            .map(Enum::name)
            .collect(Collectors.joining(","));

    TodaySkin todaySkin = TodaySkin.builder()
        .user(user)
        .logDate(date)
        .imageUrl(request.getImageUrl())
        .sleepTime(request.getSleepTime())
        .waterIntake(request.getWaterIntake())
        .dietType(dietTypeStr)
        .exerciseTime(request.getExerciseTime())
        .skinStatus(request.getSkinStatus())
        .build();
    try {
      todaySkinRepository.save(todaySkin);
    } catch (DataIntegrityViolationException e) {
      throw new CustomException(AnalysisErrorCode.ALREADY_ANALYZED);
    }

    CyclePhase cyclePhase = cyclePhaseRepository.findByCyclePhaseAtNow(userId, date)
        .orElse(null);
    String phaseType = cyclePhase != null ? cyclePhase.getPhaseType().name() : "UNKNOWN";

    return new ReservationResult(todaySkin.getTodaySkinId(), phaseType);
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void cancelReservation(Long todaySkinId) {
    todaySkinRepository.deleteById(todaySkinId);
  }

  @CacheEvict(value = {"lifestyleInsight", "troubleTimeline"}, key = "#userId")
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public SkinAnalysisResponse saveAnalysisResult(
      Long todaySkinId, String phaseType, LocalDate date, OpenAiSkinAnalysisResult gptResult, Long userId) {

    TodaySkin todaySkin = todaySkinRepository.getReferenceById(todaySkinId);
    SkinStatusLabel skinStatusLabel = SkinStatusLabel.from(gptResult.getOverallScore());

    AiAnalysis aiAnalysis = AiAnalysis.builder()
        .todaySkin(todaySkin)
        .overallScore(gptResult.getOverallScore())
        .skinStatusLabel(skinStatusLabel)
        .aiComment(gptResult.getAiComment())
        .phaseComment(gptResult.getPhaseComment())
        .build();
    aiAnalysisRepository.save(aiAnalysis);

    DetailedSkinAnalysis detail = DetailedSkinAnalysis.builder()
        .aiAnalysis(aiAnalysis)
        .trouble(gptResult.getTrouble())
        .sebum(gptResult.getSebum())
        .dullness(gptResult.getDullness())
        .moisture(gptResult.getMoisture())
        .elasticity(gptResult.getElasticity())
        .build();
    detailedSkinAnalysisRepository.save(detail);

    return toSkinAnalysisResponse(aiAnalysis, detail, phaseType);
  }

  private record ReservationResult(Long todaySkinId, String phaseType) {}

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