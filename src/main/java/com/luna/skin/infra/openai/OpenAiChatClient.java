package com.luna.skin.infra.openai;

import com.luna.skin.domain.chat.entity.AiChatMessage;
import com.luna.skin.domain.chat.enums.MessageRole;
import com.luna.skin.domain.chat.enums.MessageType;
import com.luna.skin.domain.chat.exception.ChatErrorCode;
import com.luna.skin.global.exception.CustomException;
import com.luna.skin.infra.openai.dto.OpenAiChatRequest;
import com.luna.skin.infra.openai.dto.OpenAiChatResponse;
import com.luna.skin.infra.openai.dto.OpenAiContentPart;
import com.luna.skin.infra.openai.dto.OpenAiRequestMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

import java.util.Base64;
import java.util.List;
import java.util.stream.Stream;

@Slf4j
@Component
@RequiredArgsConstructor
public class OpenAiChatClient {

    private static final String SYSTEM_PROMPT = """
            당신은 LunaSkin 앱의 AI 피부 상담사 "끼끼"입니다.
            LunaSkin은 피부 상태가 생리 주기(생리기·배란기·황체기)에 따라 달라진다는 전제로,
            사용자의 피부 분석 기록과 생활 습관을 바탕으로 맞춤 스킨케어를 상담해주는 서비스입니다.

            답변 원칙:
            - 문체는 "~예요", "~해요" 체의 친근하지만 전문적인 존댓말로 답변하세요.
            - 사용자 메시지와 함께 피부 분석 기록(종합점수, 트러블·유분·칙칙함·수분·탄력 지표, 생리 주기 단계, AI 코멘트)이
              전달되면 반드시 그 기록을 근거로 답변하세요. 지금 상태가 어떤 호르몬 변화·주기 단계와 연결되는지 짚어준 뒤,
              실천 가능한 스킨케어 루틴(성분, 사용 빈도 등)을 구체적으로 제안하세요.
            - 분석 기록이 없는 일반적인 질문에는 통상적인 스킨케어 지식으로 답변하세요.
            - 답변은 2~4문장 내외로 간결하게 작성하세요.
            - 의학적 진단이나 처방을 내리지 마세요. 트러블이 심하거나 증상이 오래 지속되면 피부과 방문을 권유하세요.
            - 당신은 피부·스킨케어·생리주기 상담만 담당합니다. 코딩, 이미지 속 무관한 내용 설명, 일반 상식 등
              그 외 주제의 질문에는 실제로 답변하지 말고, "저는 피부 상담을 도와드리는 끼끼예요! 피부에 대해
              궁금한 점을 말씀해 주시면 도와드릴게요." 같이 정중히 화제를 피부 상담으로 돌리세요.
              단, 인사말(안녕 등)에는 평소처럼 친근하게 인사하고 자연스럽게 피부 질문을 유도하세요.
            - 업로드된 이미지가 피부/얼굴 사진이 아니라면 그 내용을 분석하거나 설명하지 말고, 피부 사진을
              보내달라고 요청하거나 피부 관련 질문으로 유도하세요.
            """;
    public static final int MAX_HISTORY_SIZE = 20;

    private final RestTemplate openAiRestTemplate;
    private final S3Client s3Client;

    @Value("${luna-skin.ai.openai.endpoint}")
    private String endpoint;

    @Value("${luna-skin.ai.openai.model}")
    private String model;

    @Value("${spring.cloud.aws.s3.bucket}")
    private String bucket;

    /**
     * @param history 시간순(오래된순)으로 정렬되고 최근 {@value #MAX_HISTORY_SIZE}개 이하로
     *                이미 제한된 대화 이력이어야 한다.
     */
    public String getReply(List<AiChatMessage> history) {
        return getReply(history, null);
    }

    /**
     * @param history 시간순(오래된순)으로 정렬되고 최근 {@value #MAX_HISTORY_SIZE}개 이하로
     *                이미 제한된 대화 이력이어야 한다.
     * @param analysisContext 채팅방이 특정 분석 기록과 연결된 경우 시스템 프롬프트에 덧붙일 컨텍스트. 없으면 null.
     */
    public String getReply(List<AiChatMessage> history, String analysisContext) {
        String systemPrompt = (analysisContext == null || analysisContext.isBlank())
                ? SYSTEM_PROMPT
                : SYSTEM_PROMPT + "\n\n" + analysisContext;

        List<OpenAiRequestMessage> messages = Stream.concat(
                Stream.of(new OpenAiRequestMessage("system", systemPrompt)),
                history.stream().map(this::toRequestMessage)
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

    private OpenAiRequestMessage toRequestMessage(AiChatMessage message) {
        String role = message.getRole() == MessageRole.AI ? "assistant" : "user";

        if (message.getMessageType() == MessageType.IMAGE && message.getFileUrl() != null) {
            String caption = (message.getContent() == null || message.getContent().isBlank())
                    ? "이미지를 업로드했어요."
                    : message.getContent();
            List<OpenAiContentPart> content = List.of(
                    OpenAiContentPart.text(caption),
                    OpenAiContentPart.imageUrl(toDataUri(message.getFileUrl()))
            );
            return new OpenAiRequestMessage(role, content);
        }

        if (message.getMessageType() == MessageType.FILE) {
            return new OpenAiRequestMessage(role, "[파일 첨부: " + message.getContent() + "]");
        }

        return new OpenAiRequestMessage(role, message.getContent());
    }

    // S3 버킷이 비공개라 OpenAI가 이미지 URL을 직접 가져올 수 없으므로, 서버가 대신 다운로드해 base64 데이터 URI로 변환한다.
    private String toDataUri(String imageUrl) {
        String s3BaseUrl = "https://" + bucket + ".s3.ap-northeast-2.amazonaws.com/";
        if (!imageUrl.startsWith(s3BaseUrl)) {
            log.warn("[OpenAiChatClient] 예상한 S3 URL 형식이 아니라 base64 변환 없이 원본 URL을 그대로 전달합니다: {} (bucket={})", imageUrl, bucket);
            return imageUrl;
        }

        byte[] bytes;
        try {
            String key = imageUrl.substring(s3BaseUrl.length());
            bytes = s3Client.getObjectAsBytes(
                    GetObjectRequest.builder().bucket(bucket).key(key).build()
            ).asByteArray();
        } catch (Exception e) {
            log.error("[OpenAiChatClient] 채팅 이미지 다운로드 실패: {}", imageUrl, e);
            throw new CustomException(ChatErrorCode.AI_RESPONSE_FAILED);
        }

        String mimeType = detectMimeType(bytes);
        if (mimeType == null) {
            log.error("[OpenAiChatClient] 지원하지 않는 이미지 형식: {}", imageUrl);
            throw new CustomException(ChatErrorCode.CHAT_UNSUPPORTED_IMAGE_FORMAT);
        }

        log.info("[OpenAiChatClient] 채팅 이미지 base64 변환 완료: bytes={}, mimeType={}", bytes.length, mimeType);

        return "data:" + mimeType + ";base64," + Base64.getEncoder().encodeToString(bytes);
    }

    // 파일 확장자 대신 실제 바이트 시그니처로 판별한다. 확장자가 없거나(모바일 업로드 등) 실제 포맷과 달라도
    // 정확히 판별하기 위함이며, OpenAI가 지원하는 png/jpeg/gif/webp가 아니면 null을 반환한다.
    private String detectMimeType(byte[] bytes) {
        if (bytes.length >= 8 && (bytes[0] & 0xFF) == 0x89 && bytes[1] == 0x50 && bytes[2] == 0x4E && bytes[3] == 0x47) {
            return "image/png";
        }
        if (bytes.length >= 3 && (bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8 && (bytes[2] & 0xFF) == 0xFF) {
            return "image/jpeg";
        }
        if (bytes.length >= 3 && bytes[0] == 'G' && bytes[1] == 'I' && bytes[2] == 'F') {
            return "image/gif";
        }
        if (bytes.length >= 12
                && bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F'
                && bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P') {
            return "image/webp";
        }
        return null;
    }
}
