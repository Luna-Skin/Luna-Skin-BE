package com.luna.skin.infra.openai;

import com.luna.skin.domain.routine.exception.RoutineErrorCode;
import com.luna.skin.global.exception.CustomException;
import com.luna.skin.infra.openai.dto.OpenAiChatResponse;
import com.luna.skin.infra.openai.dto.OpenAiMessage;
import com.luna.skin.infra.openai.dto.OpenAiRoutineRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class OpenAiRoutineClient {

    @Value("${luna-skin.ai.openai.api-key}")
    private String apiKey;

    public static final String SYSTEM_PROMPT =
            "당신은 생리 주기 기반 피부 관리 AI입니다.\n" +
                    "아래 후보 목록에서 각 카테고리(SKINCARE, ACTION, EXERCISE)별로 사용자에게 가장 적합한 content_id를 정확히 1개씩 선택하세요.\n" +
                    "반드시 아래 JSON 형식으로만 응답하세요. 마크다운 코드블록 없이 순수 JSON만 출력하세요:\n" +
                    "{\"SKINCARE\": <content_id>, \"ACTION\": <content_id>, \"EXERCISE\": <content_id>}";

    private final RestTemplate openAiRestTemplate;
    private final ObjectMapper objectMapper;

    @Value("${luna-skin.ai.openai.endpoint}")
    private String endpoint;

    @Value("${luna-skin.ai.openai.model}")
    private String model;

    public Map<String, Long> requestRoutine(String prompt) {
        List<OpenAiMessage> openAiMessages = List.of(
                new OpenAiMessage("system", SYSTEM_PROMPT),
                new OpenAiMessage("user", prompt)
        );

        OpenAiRoutineRequest request = OpenAiRoutineRequest.from(model, openAiMessages);

        try {
            OpenAiChatResponse response = openAiRestTemplate.postForObject(
                    endpoint + "/chat/completions", request, OpenAiChatResponse.class);

            String jsonString = response.choices().get(0).message().content();

            return objectMapper.readValue(jsonString, new TypeReference<Map<String, Long>>() {});

        } catch (RestClientException | NullPointerException | IndexOutOfBoundsException | JsonProcessingException e){
            log.error("[OpenAiRoutineClient] AI 루틴 생성 및 파싱 실패", e);
            throw new CustomException(RoutineErrorCode.AI_ROUTINE_GENERATION_FAILED);
        }
    }


}
