package com.luna.skin.infra.openai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.luna.skin.infra.openai.OpenAiSkinAnalysisResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class OpenAiService {

  private final RestTemplate openAiRestTemplate;
  private final ObjectMapper objectMapper;

  @Value("${luna-skin.ai.openai.endpoint}")
  private String endpoint;

  @Value("${luna-skin.ai.openai.model}")
  private String model;

  public OpenAiSkinAnalysisResult analyzeSkin(String imageUrl, String phaseType) {
    String prompt = buildPrompt(phaseType);

    Map<String, Object> requestBody = Map.of(
        "model", model,
        "response_format", Map.of("type", "json_object"),
        "messages", List.of(
            Map.of("role", "user", "content", List.of(
                Map.of("type", "text", "text", prompt),
                Map.of("type", "image_url", "image_url", Map.of("url", imageUrl))
            ))
        )
    );

    try {
      Map response = openAiRestTemplate.postForObject(
          endpoint + "/chat/completions", requestBody, Map.class);
      String content = (String) ((Map) ((Map) ((List) ((Map) response).get("choices")).get(0))
          .get("message")).get("content");
      return objectMapper.readValue(content, OpenAiSkinAnalysisResult.class);
    } catch (Exception e) {
      log.error("GPT 분석 실패: {}", e.getMessage(), e);
      throw new RuntimeException("GPT 분석 중 오류가 발생했습니다.");
    }
  }

  private String buildPrompt(String phaseType) {
    return """
            이 사진은 사용자의 얼굴 피부 사진입니다. 현재 생리 주기 단계는 %s입니다.
            아래 JSON 형식으로 피부를 분석해주세요. 점수는 0~100 사이 정수입니다.
            {
              "overall_score": 피부 종합 점수,
              "trouble": 트러블 점수 (높을수록 트러블 심함),
              "sebum": 유분 점수 (높을수록 유분 많음),
              "dullness": 칙칙함 점수 (높을수록 칙칙함),
              "moisture": 수분 점수 (높을수록 수분 충분),
              "elasticity": 탄력 점수 (높을수록 탄력 좋음),
              "ai_comment": "피부 상태 AI 분석 코멘트 (2~3문장)",
              "phase_comment": "현재 주기 단계 기반 피부 조언 (1~2문장)"
            }
            """.formatted(phaseType);
  }
}