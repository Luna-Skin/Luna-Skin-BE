package com.luna.skin.infra.openai;

import com.luna.skin.domain.chat.entity.AiChatMessage;
import com.luna.skin.domain.chat.enums.MessageRole;
import com.luna.skin.domain.chat.exception.ChatErrorCode;
import com.luna.skin.global.exception.CustomException;
import com.luna.skin.infra.openai.dto.OpenAiChatRequest;
import com.luna.skin.infra.openai.dto.OpenAiChatResponse;
import com.luna.skin.infra.openai.dto.OpenAiMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.stream.Stream;

@Slf4j
@Component
@RequiredArgsConstructor
public class OpenAiChatClient {

    private static final String SYSTEM_PROMPT =
            "당신은 Luna Skin 앱의 피부 관리 AI 어시스턴트입니다. 사용자의 피부 고민에 대해 친절하고 전문적으로 답변하세요.";
    private static final int MAX_HISTORY_SIZE = 20;

    private final RestTemplate openAiRestTemplate;

    @Value("${luna-skin.ai.openai.endpoint}")
    private String endpoint;

    @Value("${luna-skin.ai.openai.model}")
    private String model;

    public String getReply(List<AiChatMessage> history) {
        List<OpenAiMessage> messages = Stream.concat(
                Stream.of(new OpenAiMessage("system", SYSTEM_PROMPT)),
                recentHistory(history).stream().map(this::toOpenAiMessage)
        ).toList();

        OpenAiChatRequest request = new OpenAiChatRequest(model, messages);

        try {
            OpenAiChatResponse response = openAiRestTemplate.postForObject(
                    endpoint + "/chat/completions", request, OpenAiChatResponse.class);

            return response.choices().get(0).message().content();
        } catch (RestClientException | NullPointerException | IndexOutOfBoundsException e) {
            log.error("[OpenAiChatClient] AI 응답 실패", e);
            throw new CustomException(ChatErrorCode.AI_RESPONSE_FAILED);
        }
    }

    private List<AiChatMessage> recentHistory(List<AiChatMessage> history) {
        if (history.size() <= MAX_HISTORY_SIZE) {
            return history;
        }
        return history.subList(history.size() - MAX_HISTORY_SIZE, history.size());
    }

    private OpenAiMessage toOpenAiMessage(AiChatMessage message) {
        String role = message.getRole() == MessageRole.AI ? "assistant" : "user";
        return new OpenAiMessage(role, message.getContent());
    }
}
