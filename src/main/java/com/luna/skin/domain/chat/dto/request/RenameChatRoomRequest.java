package com.luna.skin.domain.chat.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class RenameChatRoomRequest {

    @NotBlank
    @Schema(description = "변경할 채팅방 제목", example = "피부 트러블 상담")
    private String title;
}
