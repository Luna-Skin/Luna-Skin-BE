package com.luna.skin.domain.routine.generator;

import com.luna.skin.domain.analysis.entity.AiAnalysis;
import com.luna.skin.domain.analysis.entity.DetailedSkinAnalysis;
import com.luna.skin.domain.cycle.entity.CyclePhase;
import com.luna.skin.domain.routine.entity.RoutineContent;
import com.luna.skin.domain.routine.enums.RoutineCategory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RoutinePromptGenerator {

    // 투테이 스킨분석이 있을때
    public static final String PROMPT_TEMPLATE =
            "사용자의 현재 생리 주기 단계는 '%s'이고, 최근 피부 분석 결과는 다음과 같습니다.\n" +
                    "- 피부 측정 종합 점수 (100점 만점): %d점\n" +
                    "- AI 코멘트: %s\n" +
                    "- 유분(sebum, 100점 만점): %d점\n" +
                    "- 트러블(trouble, 100점 만점): %d점\n" +
                    "- 수분(moisture, 100점 만점): %d점\n" +
                    "- 칙칙함(dullness, 100점 만점): %d점\n" +
                    "- 탄력(elasticity, 100점 만점): %d점\n\n" +
                    "--- [선택 가능한 루틴 후보 목록] ---\n" +
                    "%s\n\n" +
                    "위 데이터를 바탕으로 각 카테고리에서 가장 알맞은 content_id를 1개씩 선택하세요.";
    // 투테이 스킨 분석이 없을때
    public static final String PROMPT_TEMPLATE_NO_ANALYSIS =
            "사용자의 현재 생리 주기 단계는 '%s'입니다.\n" +
                    "최근 피부 분석 결과가 없습니다. 오직 생리 주기 단계와 아래 후보 목록만을 참고하여 선택해주세요.\n\n" +
                    "--- [선택 가능한 루틴 후보 목록] ---\n" +
                    "%s\n\n" +
                    "위 데이터를 바탕으로 각 카테고리에서 가장 알맞은 content_id를 1개씩 선택하세요.";

    public String generate(CyclePhase cyclePhase, AiAnalysis aiAnalysis,
                           DetailedSkinAnalysis detailedSkinAnalysis, List<RoutineContent> allContents) {
        String contentList = buildContentList(allContents);

        // 분석이 없으면 주기 단계로만 평가
        if (aiAnalysis == null || detailedSkinAnalysis == null) {
            return String.format(PROMPT_TEMPLATE_NO_ANALYSIS,
                    cyclePhase.getPhaseType(),             // 생리 주기 단계
                    contentList
            );
        }

        // 분석이 있으면 분석 상세 결과로 평가
        return String.format(PROMPT_TEMPLATE,
                cyclePhase.getPhaseType(),             // 생리 주기 단계
                aiAnalysis.getOverallScore(),          // 피부 측정 종합 점수
                aiAnalysis.getAiComment(),             // AI 코멘트
                detailedSkinAnalysis.getSebum(),       // 유분
                detailedSkinAnalysis.getTrouble(),     // 트러블
                detailedSkinAnalysis.getMoisture(),    // 수분
                detailedSkinAnalysis.getDullness(),    // 칙칙함
                detailedSkinAnalysis.getElasticity(),  // 탄력
                contentList
        );
    }

    private String buildContentList(List<RoutineContent> contents) {
        StringBuilder sb = new StringBuilder();
        for (RoutineCategory category : RoutineCategory.values()) {
            sb.append("[").append(category.name()).append("]\n");
            contents.stream()
                    .filter(c -> c.getCategory() == category)
                    .forEach(c -> sb.append("(").append(c.getContentId()).append(") ").append(c.getContent()).append("\n"));
            sb.append("\n");
        }
        return sb.toString();
    }
}