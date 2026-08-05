package com.luna.skin.domain.chat.dto.response;

import com.luna.skin.domain.chat.entity.AiChatRoom;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Builder
public class CreateChatRoomResponse {

    private Long chatRoomId;
    private String title;
    private LocalDateTime createdAt;

    public static CreateChatRoomResponse from(AiChatRoom aiChatRoom) {
        return CreateChatRoomResponse.builder()
                .chatRoomId(aiChatRoom.getChatRoomId())
                .title(aiChatRoom.getTitle())
                .createdAt(aiChatRoom.getCreatedAt())
                .build();

    }
}
