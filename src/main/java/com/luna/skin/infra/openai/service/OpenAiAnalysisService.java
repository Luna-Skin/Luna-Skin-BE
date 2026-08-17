package com.luna.skin.infra.openai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.luna.skin.domain.analysis.exception.AnalysisErrorCode;
import com.luna.skin.domain.product.exception.ProductErrorCode;
import com.luna.skin.global.exception.CustomException;
import com.luna.skin.infra.openai.OpenAiSkinAnalysisResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class OpenAiAnalysisService {

  private static final List<String> COMMENT_FIELDS = List.of("ai_comment", "phase_comment");
  private static final String S3_BASE_URL = "https://luna-skin-images.s3.ap-northeast-2.amazonaws.com/";

  private final RestTemplate openAiRestTemplate;
  private final ObjectMapper objectMapper;
  private final S3Client s3Client;

  @Value("${luna-skin.ai.openai.endpoint}")
  private String endpoint;

  @Value("${luna-skin.ai.openai.model}")
  private String model;

  @Value("${spring.cloud.aws.s3.bucket}")
  private String bucket;

  public OpenAiSkinAnalysisResult analyzeSkin(String imageUrl, String leftImageUrl, String rightImageUrl, String phaseType) {
    String prompt = buildPrompt(phaseType);

    EncodedImage frontImage = convertToBase64(imageUrl);

    List<Map<String, Object>> content = new ArrayList<>();
    content.add(Map.of("type", "text", "text", prompt));
    content.add(Map.of("type", "image_url", "image_url",
        Map.of("url", "data:" + frontImage.mimeType() + ";base64," + frontImage.base64Data())));

    if (leftImageUrl != null && !leftImageUrl.isBlank()) {
      EncodedImage leftImage = convertToBase64(leftImageUrl);
      content.add(Map.of("type", "image_url", "image_url",
          Map.of("url", "data:" + leftImage.mimeType() + ";base64," + leftImage.base64Data())));
    }
    if (rightImageUrl != null && !rightImageUrl.isBlank()) {
      EncodedImage rightImage = convertToBase64(rightImageUrl);
      content.add(Map.of("type", "image_url", "image_url",
          Map.of("url", "data:" + rightImage.mimeType() + ";base64," + rightImage.base64Data())));
    }

    Map<String, Object> requestBody = Map.of(
        "model", model,
        "response_format", Map.of("type", "json_object"),
        "messages", List.of(Map.of("role", "user", "content", content))
    );

    try {
      Map response = openAiRestTemplate.postForObject(
          endpoint + "/chat/completions", requestBody, Map.class);
      String responseContent = (String) ((Map) ((Map) ((List) ((Map) response).get("choices")).get(0))
          .get("message")).get("content");

      JsonNode root = objectMapper.readTree(responseContent);
      validateGptResponse(root);
      OpenAiSkinAnalysisResult result = objectMapper.treeToValue(root, OpenAiSkinAnalysisResult.class);
      int overallScore = (int) Math.round(
          (result.getTrouble() + result.getSebum() + result.getDullness()
              + result.getMoisture() + result.getElasticity()) / 5.0);
      return result.withOverallScore(overallScore);
    } catch (Exception e) {
      log.error("GPT 분석 실패: {}", e.getMessage(), e);
      throw new CustomException(AnalysisErrorCode.GPT_ANALYSIS_FAILED);
    }
  }

  private EncodedImage convertToBase64(String imageUrl) {
    if (imageUrl == null || !imageUrl.startsWith(S3_BASE_URL)) {
      log.error("허용되지 않은 imageUrl 형식: {}", imageUrl);
      throw new CustomException(AnalysisErrorCode.GPT_ANALYSIS_FAILED);
    }

    try {
      String key = imageUrl.substring(S3_BASE_URL.length());
      byte[] bytes = s3Client.getObjectAsBytes(
          GetObjectRequest.builder().bucket(bucket).key(key).build()
      ).asByteArray();
      return new EncodedImage(Base64.getEncoder().encodeToString(bytes), resolveMimeType(imageUrl));
    } catch (Exception e) {
      log.error("이미지 다운로드 실패: {}", imageUrl);
      throw new CustomException(AnalysisErrorCode.GPT_ANALYSIS_FAILED);
    }
  }

  private String resolveMimeType(String imageUrl) {
    String lower = imageUrl.toLowerCase();
    if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
    if (lower.endsWith(".png")) return "image/png";
    if (lower.endsWith(".webp")) return "image/webp";
    return "image/png";
  }

  private record EncodedImage(String base64Data, String mimeType) {}

  private void validateGptResponse(JsonNode root) {
    List<String> subScoreFields = List.of("trouble", "sebum", "dullness", "moisture", "elasticity");

    for (String field : subScoreFields) {
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

  private String buildPrompt(String phaseType) {
    return """
                당신은 피부과 전문의입니다. 사용자의 얼굴 피부 사진을 분석해주세요.
                현재 생리 주기 단계는 %s입니다.
                
                반드시 아래 JSON 형식으로만 응답하세요. 다른 텍스트는 포함하지 마세요.
                점수는 0~100 사이 정수입니다.
                
                각 항목 채점 기준:
                - trouble(트러블): 트러블/여드름 개수 기준. 0개=100점, 1개=80점, 2~3개=60점, 4~6개=40점, 7~10개=20점, 11개 이상=0점
                - sebum(유분): 피지량 기준. 0~15=100점, 16~30=80점, 31~45=60점, 46~65=40점, 66~80=20점, 81~100=0점 (낮을수록 좋음)
                - dullness(칙칙함): 피부 톤 균일도 기준. 0~15=100점, 16~30=80점, 31~45=60점, 46~65=40점, 66~80=20점, 81~100=0점 (낮을수록 좋음)
                - moisture(수분): 피부 수분량 기준. 78~100=100점, 68~77=80점, 58~67=60점, 43~57=40점, 28~42=20점, 0~27=0점 (높을수록 좋음)
                - elasticity(탄력): 피부 탄력 기준. 85~100=100점, 75~84=80점, 65~74=60점, 50~64=40점, 35~49=20점, 0~34=0점 (높을수록 좋음)
                
                ai_comment, phase_comment 작성 규칙:
                - 문체는 "~예요", "~해요" 체의 친근하지만 전문적인 존댓말로 작성.
                - 뻔한 일반론 대신, 생리 주기 단계와 호르몬 변화를 근거로 한 구체적인 진단처럼 작성.
                - ai_comment: 호르몬 변화와 피부 상태의 인과관계 설명 + 클렌저/토너/마스크 성분 등 구체적인 관리 루틴 추천 +
                  피해야 할 습관(과도한 세안, 자극적인 성분 등) 주의사항까지 2~3문장으로 작성.
                - phase_comment: 현재 주기 단계가 얼굴의 구체적인 부위(T존, 턱, 볼 등)에 미치는 증상을 짧고
                  진단적인 한 문장으로 작성.
                
                {
                  "trouble": 트러블 점수,
                  "sebum": 유분 점수,
                  "dullness": 칙칙함 점수,
                  "moisture": 수분 점수,
                  "elasticity": 탄력 점수,
                  "ai_comment": "종합 분석 코멘트",
                  "phase_comment": "주기 단계별 부위 진단 코멘트"
                }
                """.formatted(phaseType);
  }

  public String generateCompareComment(
      int overallScoreA, int troubleA, int sebumA, int dullnessA, int moistureA, int elasticityA,
      int overallScoreB, int troubleB, int sebumB, int dullnessB, int moistureB, int elasticityB) {

    String prompt = String.format("""
                다음은 두 날짜의 피부 분석 결과입니다.

                [날짜 A]
                종합 점수: %d, 트러블: %d, 유분: %d, 칙칙함: %d, 수분: %d, 탄력: %d

                [날짜 B]
                종합 점수: %d, 트러블: %d, 유분: %d, 칙칙함: %d, 수분: %d, 탄력: %d

                두 날짜의 피부 변화를 바탕으로 현재 피부 상태와 케어 방법을 2~3문장으로 조언해주세요.
                친근하고 실용적인 말투로 작성하고, 수치는 언급하지 마세요.
                """,
        overallScoreA, troubleA, sebumA, dullnessA, moistureA, elasticityA,
        overallScoreB, troubleB, sebumB, dullnessB, moistureB, elasticityB);

    Map<String, Object> requestBody = Map.of(
        "model", model,
        "messages", List.of(Map.of("role", "user", "content", prompt)),
        "max_tokens", 300
    );

    try {
      Map response = openAiRestTemplate.postForObject(
          endpoint + "/chat/completions", requestBody, Map.class);
      return (String) ((Map) ((Map) ((List) ((Map) response).get("choices")).get(0))
          .get("message")).get("content");
    } catch (Exception e) {
      log.error("비교 코멘트 생성 실패: {}", e.getMessage(), e);
      return null;
    }
  }

  public List<String> recommendIngredients(
      int trouble, int sebum, int dullness, int moisture, int elasticity) {

    String prompt = String.format("""
                다음은 사용자의 피부 분석 점수입니다. (0~100, 높을수록 좋음)
                트러블: %d, 유분: %d, 칙칙함: %d, 수분: %d, 탄력: %d

                아래 카테고리 중 이 사용자에게 가장 필요한 2가지를 우선순위 순서로 골라주세요.
                반드시 아래 목록에서만 선택하고, JSON 배열 형식으로만 응답하세요. 다른 텍스트는 포함하지 마세요.

                선택 가능한 카테고리: ["트러블 완화", "피지 조절", "톤 개선", "보습", "탄력 강화"]

                응답 예시: ["트러블 완화", "보습"]
                """, trouble, sebum, dullness, moisture, elasticity);

    Map<String, Object> requestBody = Map.of(
        "model", model,
        "messages", List.of(Map.of("role", "user", "content", prompt)),
        "max_tokens", 50
    );

    try {
      Map response = openAiRestTemplate.postForObject(
          endpoint + "/chat/completions", requestBody, Map.class);
      String content = (String) ((Map) ((Map) ((List) response.get("choices")).get(0))
          .get("message")).get("content");
      return objectMapper.readValue(content, List.class);
    } catch (Exception e) {
      log.error("제품 추천 ingredient 선택 실패: {}", e.getMessage(), e);
      throw new CustomException(ProductErrorCode.RECOMMEND_FAILED);
    }
  }
}