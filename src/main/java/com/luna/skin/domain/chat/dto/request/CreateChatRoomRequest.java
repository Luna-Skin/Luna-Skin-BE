package com.luna.skin.domain.chat.dto.request;

import com.luna.skin.domain.analysis.entity.AiAnalysis;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class CreateChatRoomRequest {

    @Schema(description = "채팅방 제목", example = "피부에 대한 고민")
    private String title;

    @Schema(description = "분석 식별자", example = "")
    private Long aiAnalysis;
}
