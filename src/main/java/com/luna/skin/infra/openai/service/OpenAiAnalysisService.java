package com.luna.skin.infra.openai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.luna.skin.domain.analysis.exception.AnalysisErrorCode;
import com.luna.skin.global.exception.CustomException;
import com.luna.skin.infra.openai.OpenAiSkinAnalysisResult;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
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
public class OpenAiAnalysisService {

  private static final List<String> SCORE_FIELDS =
      List.of("overall_score", "trouble", "sebum", "dullness", "moisture", "elasticity");
  private static final List<String> COMMENT_FIELDS = List.of("ai_comment", "phase_comment");

  private final RestTemplate openAiRestTemplate;
  private final ObjectMapper objectMapper;

  @Value("${luna-skin.ai.openai.endpoint}")
  private String endpoint;

  @Value("${luna-skin.ai.openai.model}")
  private String model;

  public OpenAiSkinAnalysisResult analyzeSkin(String imageUrl, String phaseType, String baseDir) {
    String prompt = buildPrompt(phaseType);

    // URL → 실제 파일 경로 변환 후 base64 인코딩
    EncodedImage image = convertToBase64(imageUrl, baseDir);
    String dataUrl = "data:" + image.mimeType() + ";base64," + image.base64Data();

    Map<String, Object> requestBody = Map.of(
        "model", model,
        "response_format", Map.of("type", "json_object"),
        "messages", List.of(
            Map.of("role", "user", "content", List.of(
                Map.of("type", "text", "text", prompt),
                Map.of("type", "image_url", "image_url", Map.of("url", dataUrl))
            ))
        )
    );

    try {
      Map response = openAiRestTemplate.postForObject(
          endpoint + "/chat/completions", requestBody, Map.class);
      String content = (String) ((Map) ((Map) ((List) ((Map) response).get("choices")).get(0))
          .get("message")).get("content");

      JsonNode root = objectMapper.readTree(content);
      validateGptResponse(root);
      return objectMapper.treeToValue(root, OpenAiSkinAnalysisResult.class);
    } catch (Exception e) {
      log.error("GPT 분석 실패: {}", e.getMessage(), e);
      throw new CustomException(AnalysisErrorCode.GPT_ANALYSIS_FAILED);
    }
  }

  private void validateGptResponse(JsonNode root) {
    for (String field : SCORE_FIELDS) {
      JsonNode node = root.get(field);
      if (node == null || node.isNull() || !node.isIntegralNumber()) {
        throw new IllegalArgumentException(field + " 필드가 없거나 정수가 아닙니다.");
      }
      int value = node.intValue();
      if (value < 0 || value > 100) {
        throw new IllegalArgumentException(field + " 값이 0~100 범위를 벗어났습니다: " + value);
      }
    }

    for (String field : COMMENT_FIELDS) {
      JsonNode node = root.get(field);
      if (node == null || node.isNull() || !node.isTextual() || node.asText().isBlank()) {
        throw new IllegalArgumentException(field + " 필드가 없거나 비어있습니다.");
      }
    }
  }

  private EncodedImage convertToBase64(String imageUrl, String baseDir) {
    if (imageUrl == null || imageUrl.isBlank()) {
      log.error("imageUrl이 null 또는 비어있습니다.");
      throw new CustomException(AnalysisErrorCode.GPT_ANALYSIS_FAILED);
    }

    try {
      URL url = new URL(imageUrl);
      byte[] bytes = url.openStream().readAllBytes();
      String base64Data = Base64.getEncoder().encodeToString(bytes);
      String mimeType = resolveMimeTypeFromUrl(imageUrl);
      return new EncodedImage(base64Data, mimeType);
    } catch (IOException e) {
      log.error("이미지 다운로드 실패: {}", imageUrl);
      throw new CustomException(AnalysisErrorCode.GPT_ANALYSIS_FAILED);
    }
  }

  private String resolveMimeTypeFromUrl(String imageUrl) {
    String lower = imageUrl.toLowerCase();
    if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
    if (lower.endsWith(".png")) return "image/png";
    if (lower.endsWith(".webp")) return "image/webp";
    return "image/png";
  }

  private record EncodedImage(String base64Data, String mimeType) {}

  private String buildPrompt(String phaseType) {
    return """
            당신은 피부과 전문의입니다. 사용자의 얼굴 피부 사진을 분석해주세요.
            현재 생리 주기 단계는 %s입니다.

            반드시 아래 JSON 형식으로만 응답하세요. 다른 텍스트는 포함하지 마세요.
            점수는 0~100 사이 정수입니다.

            ai_comment, phase_comment 작성 규칙:
            - 문체는 "~예요", "~해요" 체의 친근하지만 전문적인 존댓말로 작성.
            - 뻔한 일반론 대신, 생리 주기 단계와 호르몬 변화를 근거로 한 구체적인 진단처럼 작성.
            - ai_comment: 호르몬 변화와 피부 상태의 인과관계 설명 + 클렌저/토너/마스크 성분 등 구체적인 관리 루틴 추천 +
              피해야 할 습관(과도한 세안, 자극적인 성분 등) 주의사항까지 2~3문장으로 작성.
              예시: "현재 피부는 황체기의 프로게스테론 상승으로 피지선이 활성화된 상태예요. 주 1회 클레이 마스크와
              BHA 토너 루틴이 효과적이에요. 과도한 세안은 오히려 피지 분비를 촉진하니 하루 2회를 넘기지 마세요."
            - phase_comment: 현재 주기 단계가 얼굴의 구체적인 부위(T존, 턱, 볼 등)에 미치는 증상을 짧고
              진단적인 한 문장으로 작성.
              예시: "황체기 피지 분비 증가로 T존 유분·턱 민감도가 높아졌어요."

            {
              "overall_score": 전체적인 피부 건강 종합 점수,
              "trouble": 트러블/여드름 점수 (높을수록 트러블 심함),
              "sebum": 유분 점수 (높을수록 유분 많음),
              "dullness": 칙칙함 점수 (높을수록 칙칙하고 생기 없음),
              "moisture": 수분 점수 (높을수록 수분 충분),
              "elasticity": 탄력 점수 (높을수록 탄력 좋음),
              "ai_comment": "위 규칙에 따른 종합 분석 코멘트",
              "phase_comment": "위 규칙에 따른 주기 단계별 부위 진단 코멘트"
            }
            """.formatted(phaseType);
  }
}