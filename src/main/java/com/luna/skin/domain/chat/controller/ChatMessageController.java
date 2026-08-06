package com.luna.skin.domain.chat.controller;

import com.luna.skin.domain.chat.dto.request.SendChatMessageRequest;
import com.luna.skin.domain.chat.service.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

@Tag(name = "ChatMessage", description = "STOMP WebSocket 메시지 전송 핸들러 (참고: springdoc-openapi는 STOMP 엔드포인트를 문서화하지 않으며, 아래 어노테이션은 코드 문서화 목적입니다)")
@Controller
@RequiredArgsConstructor
public class ChatMessageController {

    private final ChatService chatService;

    @Operation(
            summary = "메시지 전송",
            description = "클라이언트가 /pub/chat/send로 보낸 메시지를 저장하고 /topic/chat/{chatRoomId}로 브로드캐스트한 뒤, "
                    + "OpenAI 응답을 받아 동일하게 저장 및 브로드캐스트합니다. 유저/채팅방 식별은 /ws/chat 연결 시점의 세션 정보를 사용합니다."
    )
    @MessageMapping("/chat/send")
    public void sendMessage(
            @Parameter(description = "전송할 메시지") @Payload SendChatMessageRequest request,
            @Parameter(description = "WebSocket 세션 속성에 접근하기 위한 헤더 접근자") SimpMessageHeaderAccessor headerAccessor
    ) {
        Long userId = (Long) headerAccessor.getSessionAttributes().get("userId");
        Long chatRoomId = (Long) headerAccessor.getSessionAttributes().get("chatRoomId");

        chatService.sendMessage(userId, chatRoomId, request.getContent());
    }
}
