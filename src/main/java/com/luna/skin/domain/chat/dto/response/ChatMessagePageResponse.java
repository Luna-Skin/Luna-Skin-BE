package com.luna.skin.domain.chat.dto.response;

import com.luna.skin.domain.chat.entity.AiChatMessage;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.data.domain.Slice;

import java.util.List;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ChatMessagePageResponse {

    private List<ChatMessageResponse> messages;
    private boolean hasNext;

    public static ChatMessagePageResponse from(Slice<AiChatMessage> slice) {
        List<ChatMessageResponse> messages = slice.getContent().stream()
                .map(ChatMessageResponse::from)
                .toList();

        return new ChatMessagePageResponse(messages, slice.hasNext());
    }
}
