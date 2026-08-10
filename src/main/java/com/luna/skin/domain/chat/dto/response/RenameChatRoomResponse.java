package com.luna.skin.domain.chat.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RenameChatRoomResponse {

    private Long chatRoomId;
    private String title;
}
