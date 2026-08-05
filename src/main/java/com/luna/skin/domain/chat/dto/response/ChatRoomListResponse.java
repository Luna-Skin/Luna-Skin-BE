package com.luna.skin.domain.chat.dto.response;

import com.luna.skin.domain.chat.entity.AiChatRoom;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
public class ChatRoomListResponse {

    private Long chatRoomId;

    private String title;

    private LocalDateTime createdAt;

    public static ChatRoomListResponse from(AiChatRoom aiChatRoom) {
        return ChatRoomListResponse.builder()
                .chatRoomId(aiChatRoom.getChatRoomId())
                .title(aiChatRoom.getTitle())
                .createdAt(aiChatRoom.getCreatedAt())
                .build();
    }
}
