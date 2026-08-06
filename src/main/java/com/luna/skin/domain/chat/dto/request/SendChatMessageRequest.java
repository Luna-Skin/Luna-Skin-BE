package com.luna.skin.domain.chat.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class SendChatMessageRequest {

    @Schema(description = "메시지 내용", example = "요즘 피부가 건조하고 트러블이 자주 나요")
    private String content;
}
