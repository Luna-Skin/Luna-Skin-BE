package com.luna.skin.domain.chat.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ChatErrorResponse {

    private String type;
    private String message;

    public static ChatErrorResponse of(String message) {
        return ChatErrorResponse.builder()
                .type("ERROR")
                .message(message)
                .build();
    }
}
