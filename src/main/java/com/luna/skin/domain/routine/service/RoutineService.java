package com.luna.skin.domain.routine.service;

import com.luna.skin.domain.analysis.entity.AiAnalysis;
import com.luna.skin.domain.analysis.entity.DetailedSkinAnalysis;
import com.luna.skin.domain.analysis.repository.AiAnalysisRepository;
import com.luna.skin.domain.analysis.repository.DetailedSkinAnalysisRepository;
import com.luna.skin.domain.cycle.entity.CyclePhase;
import com.luna.skin.domain.cycle.exception.CycleErrorCode;
import com.luna.skin.domain.cycle.repository.CyclePhaseRepository;
import com.luna.skin.domain.routine.dto.response.AiDailyRoutineResponse;
import com.luna.skin.domain.routine.entity.AiDailyRoutine;
import com.luna.skin.domain.routine.entity.RoutineContent;
import com.luna.skin.domain.routine.exception.RoutineErrorCode;
import com.luna.skin.domain.routine.generator.RoutinePromptGenerator;
import com.luna.skin.domain.routine.repository.AiDailyRoutineRepository;
import com.luna.skin.domain.routine.repository.RoutineContentRepository;
import com.luna.skin.domain.user.entity.User;
import com.luna.skin.domain.user.exception.UserErrorCode;
import com.luna.skin.domain.user.repository.UserRepository;
import com.luna.skin.global.exception.CustomException;
import com.luna.skin.infra.openai.OpenAiRoutineClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoutineService {

    private final UserRepository userRepository;
    private final AiDailyRoutineRepository aiDailyRoutineRepository;
    private final CyclePhaseRepository cyclePhaseRepository;
    private final AiAnalysisRepository aiAnalysisRepository;
    private final DetailedSkinAnalysisRepository detailedSkinAnalysisRepository;
    private final RoutineContentRepository routineContentRepository;
    private final RoutinePromptGenerator routinePromptGenerator;
    private final OpenAiRoutineClient openAiRoutineClient;

    @Transactional
    public AiDailyRoutineResponse generateDailyRoutine(Long currentUserId, LocalDate today) {

        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> {
                    log.warn("[]");
                    return new CustomException(UserErrorCode.USER_NOT_FOUND);
                });

        AiDailyRoutineResponse aiDailyRoutineResponse = aiDailyRoutineRepository.findByUserUserIdAndTargetDate(currentUserId, today)
                .map(AiDailyRoutineResponse::from)
                .orElse(null);

        // 데일리 루틴이 없다면
        if(aiDailyRoutineResponse == null) {

            // 오늘 주기 단계 조회
            CyclePhase cyclePhase = cyclePhaseRepository.findByCyclePhaseAtNow(currentUserId, today)
                    .orElseThrow(() -> {
                        log.warn("[]");
                        return new CustomException(CycleErrorCode.CYCLE_NOT_FOUND);
                    });

            // 사용자의 최근 3일 중 가장 최근 투데이 스킨, 상세 지표를 조회후
            // 투데이 스킨이 없으면 주기 단계만으로 분석
            AiAnalysis aiAnalysis = aiAnalysisRepository.findByUserIdAndBetweenDate(currentUserId, today.minusDays(3), today)
                    .orElse(null);// 주기 단계만으로 ai 분석 후 리턴

            // 분석이있으면 해당 분석의 세부 메트릭스 조회
            DetailedSkinAnalysis detailedSkinAnalysis = null;
            if (aiAnalysis != null) {
                detailedSkinAnalysis = detailedSkinAnalysisRepository
                        .findByAiAnalysis(aiAnalysis)
                        .orElse(null);
            }

            // 해당 분석 정보와 오늘 주기 단계를 바탕으로 프롬포트 작성
            List<RoutineContent> allContents = routineContentRepository.findAll();
            String prompt = routinePromptGenerator.generate(cyclePhase, aiAnalysis, detailedSkinAnalysis, allContents);

            // ai 가 데일리 루틴을 추천
            Map<String, Long> idMap = openAiRoutineClient.requestRoutine(prompt);

            // db에 데일리 루틴 저장
            List<RoutineContent> routineContents = separateRoutineContent(idMap);
            AiDailyRoutine dailyRoutine = AiDailyRoutine.of(user, aiAnalysis, today, cyclePhase.getPhaseType(), routineContents);
            aiDailyRoutineRepository.save(dailyRoutine);

            // 반환
            aiDailyRoutineResponse = AiDailyRoutineResponse.from(dailyRoutine);
        }

        return aiDailyRoutineResponse;
    }
    private List<RoutineContent> separateRoutineContent(Map<String, Long> idMap) {

        return idMap.entrySet().stream()
                .map(entry -> {
                    log.debug("[AI 하루 루틴 추천] 카테고리 = {}, content_id = {}", entry.getKey(), entry.getValue());
                    return routineContentRepository.findById(entry.getValue())
                            .orElseThrow(() -> {
                                log.warn("[AI 하루 루틴 추천] 루틴 항목을 찾지 못했습니다. content_id = {}", entry.getValue());
                                return new CustomException(RoutineErrorCode.ROUTINE_CONTENT_NOT_FOUND);
                            });
                })
                .toList(); // RoutineContent들의 리스트를 반환
    }
}
