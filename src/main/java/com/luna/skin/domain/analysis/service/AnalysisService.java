package com.luna.skin.domain.analysis.service;

import com.luna.skin.domain.analysis.dto.request.SkinAnalysisRequest;
import com.luna.skin.domain.analysis.dto.response.ImageUploadResponse;
import com.luna.skin.domain.analysis.dto.response.SkinAnalysisResponse;
import com.luna.skin.domain.analysis.dto.response.SkinAnalysisResponse.DetailedMetrics;
import com.luna.skin.domain.analysis.entity.AiAnalysis;
import com.luna.skin.domain.analysis.entity.DetailedSkinAnalysis;
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
import com.luna.skin.infra.openai.service.OpenAiService;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnalysisService {

  private final ImageStorageService imageStorageService;
  private final TodaySkinRepository todaySkinRepository;
  private final AiAnalysisRepository aiAnalysisRepository;
  private final DetailedSkinAnalysisRepository detailedSkinAnalysisRepository;
  private final CyclePhaseRepository cyclePhaseRepository;
  private final UserRepository userRepository;
  private final OpenAiService openAiService;
  private final StorageProperties storageProperties;

  public ImageUploadResponse uploadImage(MultipartFile image) {
    validateImageFile(image);

    String imageUrl = imageStorageService.store(image, "analysis");
    return ImageUploadResponse.builder()
        .imageUrl(imageUrl)
        .build();
  }

  @Transactional
  public SkinAnalysisResponse analyze(Long userId, LocalDate date, SkinAnalysisRequest request) {

    log.info("imageUrl: {}", request.getImageUrl());
    log.info("request: {}", request);

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
    todaySkinRepository.save(todaySkin);

    CyclePhase cyclePhase = cyclePhaseRepository.findByCyclePhaseAtNow(userId, date)
        .orElse(null);
    String phaseType = cyclePhase != null ? cyclePhase.getPhaseType().name() : "UNKNOWN";

    OpenAiSkinAnalysisResult gptResult = openAiService.analyzeSkin(
        request.getImageUrl(), phaseType, storageProperties.baseDir());

    AiAnalysis aiAnalysis = AiAnalysis.builder()
        .todaySkin(todaySkin)
        .overallScore(gptResult.getOverallScore())
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

    String skinStatus = calculateSkinStatus(gptResult.getOverallScore());

    return SkinAnalysisResponse.builder()
        .analysisId(aiAnalysis.getAnalysisId())
        .date(date.toString())
        .overallScore(gptResult.getOverallScore())
        .skinStatus(skinStatus)
        .cyclePhase(phaseType)
        .phaseComment(gptResult.getPhaseComment())
        .aiComment(gptResult.getAiComment())
        .detailedMetrics(SkinAnalysisResponse.DetailedMetrics.builder()
            .trouble(gptResult.getTrouble())
            .sebum(gptResult.getSebum())
            .dullness(gptResult.getDullness())
            .moisture(gptResult.getMoisture())
            .elasticity(gptResult.getElasticity())
            .build())
        .build();
  }

  private String calculateSkinStatus(Integer score) {
    if (score >= 80) return "좋음";
    if (score >= 60) return "보통";
    return "나쁨";
  }

  private void validateImageFile(MultipartFile image) {
    if(image == null || image.isEmpty()) {
      throw new CustomException(AnalysisErrorCode.EMPTY_FILE);
    }

    String originalFilename = image.getOriginalFilename();
    if(originalFilename == null || originalFilename.isEmpty()) {
      throw new CustomException(AnalysisErrorCode.INVALID_FILE_TYPE);
    }

    String ext = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();
    if (!List.of("jpg", "jpeg", "png").contains(ext)) {
      throw new CustomException(AnalysisErrorCode.INVALID_FILE_TYPE);
    }
  }
}
