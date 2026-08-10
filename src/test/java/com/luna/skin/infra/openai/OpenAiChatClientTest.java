package com.luna.skin.infra.openai;

import com.luna.skin.domain.chat.entity.AiChatMessage;
import com.luna.skin.domain.chat.enums.MessageRole;
import com.luna.skin.domain.chat.exception.ChatErrorCode;
import com.luna.skin.global.exception.CustomException;
import com.luna.skin.infra.openai.dto.OpenAiChatRequest;
import com.luna.skin.infra.openai.dto.OpenAiChatResponse;
import com.luna.skin.infra.openai.dto.OpenAiMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OpenAiChatClientTest {

    @Mock
    private RestTemplate openAiRestTemplate;

    private OpenAiChatClient openAiChatClient;

    @BeforeEach
    void setUp() {
        openAiChatClient = new OpenAiChatClient(openAiRestTemplate);
        ReflectionTestUtils.setField(openAiChatClient, "endpoint", "https://api.openai.com/v1");
        ReflectionTestUtils.setField(openAiChatClient, "model", "gpt-4o-mini");
    }

    private AiChatMessage message(MessageRole role, String content) {
        return AiChatMessage.builder().role(role).content(content).build();
    }

    @Test
    void 대화_기록으로_OpenAI를_호출하고_응답_내용을_반환한다() {
        List<AiChatMessage> history = List.of(
                message(MessageRole.USER, "피부가 건조해요"),
                message(MessageRole.AI, "보습을 강화해보세요")
        );
        OpenAiChatResponse response = new OpenAiChatResponse(
                List.of(new OpenAiChatResponse.Choice(new OpenAiMessage("assistant", "수분크림을 추천드려요")))
        );
        ArgumentCaptor<Object> requestCaptor = ArgumentCaptor.forClass(Object.class);
        when(openAiRestTemplate.postForObject(anyString(), requestCaptor.capture(), eq(OpenAiChatResponse.class)))
                .thenReturn(response);

        String reply = openAiChatClient.getReply(history);

        assertThat(reply).isEqualTo("수분크림을 추천드려요");

        OpenAiChatRequest sentRequest = (OpenAiChatRequest) requestCaptor.getValue();
        assertThat(sentRequest.model()).isEqualTo("gpt-4o-mini");
        assertThat(sentRequest.messages()).hasSize(3);
        assertThat(sentRequest.messages().get(0).role()).isEqualTo("system");
        assertThat(sentRequest.messages().get(1)).isEqualTo(new OpenAiMessage("user", "피부가 건조해요"));
        assertThat(sentRequest.messages().get(2)).isEqualTo(new OpenAiMessage("assistant", "보습을 강화해보세요"));
    }

    @Test
    void 분석_컨텍스트가_있으면_시스템_프롬프트에_덧붙여_전달한다() {
        OpenAiChatResponse response = new OpenAiChatResponse(
                List.of(new OpenAiChatResponse.Choice(new OpenAiMessage("assistant", "답변")))
        );
        ArgumentCaptor<Object> requestCaptor = ArgumentCaptor.forClass(Object.class);
        when(openAiRestTemplate.postForObject(anyString(), requestCaptor.capture(), eq(OpenAiChatResponse.class)))
                .thenReturn(response);

        openAiChatClient.getReply(List.of(), "오늘 유분 90, 트러블 45.");

        OpenAiChatRequest sentRequest = (OpenAiChatRequest) requestCaptor.getValue();
        assertThat(sentRequest.messages().get(0).role()).isEqualTo("system");
        assertThat(sentRequest.messages().get(0).content()).contains("오늘 유분 90, 트러블 45.");
    }

    @Test
    void OpenAI_호출이_실패하면_AI_RESPONSE_FAILED_예외를_던진다() {
        when(openAiRestTemplate.postForObject(anyString(), org.mockito.ArgumentMatchers.any(), eq(OpenAiChatResponse.class)))
                .thenThrow(new RestClientException("boom"));

        assertThatThrownBy(() -> openAiChatClient.getReply(List.of()))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ChatErrorCode.AI_RESPONSE_FAILED);
    }

    @Test
    void 응답에_choice가_없으면_AI_RESPONSE_FAILED_예외를_던진다() {
        OpenAiChatResponse emptyResponse = new OpenAiChatResponse(List.of());
        when(openAiRestTemplate.postForObject(anyString(), org.mockito.ArgumentMatchers.any(), eq(OpenAiChatResponse.class)))
                .thenReturn(emptyResponse);

        assertThatThrownBy(() -> openAiChatClient.getReply(List.of()))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ChatErrorCode.AI_RESPONSE_FAILED);
    }
}
