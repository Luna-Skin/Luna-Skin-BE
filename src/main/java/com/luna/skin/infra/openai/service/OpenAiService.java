package com.luna.skin.infra.openai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.luna.skin.domain.analysis.exception.AnalysisErrorCode;
import com.luna.skin.global.exception.CustomException;
import com.luna.skin.infra.openai.OpenAiSkinAnalysisResult;
import java.io.IOException;
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
public class OpenAiService {

  private final RestTemplate openAiRestTemplate;
  private final ObjectMapper objectMapper;

  @Value("${luna-skin.ai.openai.endpoint}")
  private String endpoint;

  @Value("${luna-skin.ai.openai.model}")
  private String model;

  public OpenAiSkinAnalysisResult analyzeSkin(String imageUrl, String phaseType, String baseDir) {
    String prompt = buildPrompt(phaseType);

    // URL → 실제 파일 경로 변환 후 base64 인코딩
    String base64Image = convertToBase64(imageUrl, baseDir);
    String dataUrl = "data:image/png;base64," + base64Image;

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
      return objectMapper.readValue(content, OpenAiSkinAnalysisResult.class);
    } catch (Exception e) {
      log.error("GPT 분석 실패: {}", e.getMessage(), e);
      throw new CustomException(AnalysisErrorCode.GPT_ANALYSIS_FAILED);
    }
  }

  private String convertToBase64(String imageUrl, String baseDir) {
    if (imageUrl == null || !imageUrl.startsWith("/files/")) {
      log.error("허용되지 않은 imageUrl 형식: {}", imageUrl);
      throw new CustomException(AnalysisErrorCode.GPT_ANALYSIS_FAILED);
    }

    try {
      // 저장 서비스가 관리하는 업로드 루트만 허용 (심볼릭 링크 등까지 실제 경로로 해석)
      Path uploadRoot = Path.of(baseDir).toRealPath();
      Path imagePath = uploadRoot
          .resolve(imageUrl.substring("/files/".length()))
          .normalize()
          .toRealPath();

      // ../ 등으로 업로드 루트를 벗어나는 경로("/files/../../.env" 등)는 차단
      if (!imagePath.startsWith(uploadRoot) || !Files.isRegularFile(imagePath)) {
        log.error("업로드 루트를 벗어난 imageUrl: {}", imageUrl);
        throw new CustomException(AnalysisErrorCode.GPT_ANALYSIS_FAILED);
      }

      byte[] bytes = Files.readAllBytes(imagePath);
      return Base64.getEncoder().encodeToString(bytes);
    } catch (IOException e) {
      log.error("이미지 파일 읽기 실패: {}", imageUrl);
      throw new CustomException(AnalysisErrorCode.GPT_ANALYSIS_FAILED);
    }
  }

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