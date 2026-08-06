package com.luna.skin.domain.chat.dto.response;

import com.luna.skin.domain.chat.entity.AiChatMessage;
import com.luna.skin.domain.chat.enums.MessageRole;
import com.luna.skin.domain.chat.enums.MessageType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ChatMessageResponse {

    private Long chatMessageId;
    private MessageRole role;
    private MessageType messageType;
    private String content;
    private LocalDateTime createdAt;

    public static ChatMessageResponse from(AiChatMessage aiChatMessage) {
        return ChatMessageResponse.builder()
                .chatMessageId(aiChatMessage.getChatMessageId())
                .role(aiChatMessage.getRole())
                .messageType(aiChatMessage.getMessageType())
                .content(aiChatMessage.getContent())
                .createdAt(aiChatMessage.getCreatedAt())
                .build();
    }
}
