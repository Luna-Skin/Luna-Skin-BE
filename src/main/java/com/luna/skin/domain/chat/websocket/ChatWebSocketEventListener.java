package com.luna.skin.domain.chat.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Slf4j
@Component
public class ChatWebSocketEventListener {

    @EventListener
    public void handleSessionDisconnect(SessionDisconnectEvent event) {
        SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.wrap(event.getMessage());
        Object userId = accessor.getSessionAttributes().get("userId");
        Object chatRoomId = accessor.getSessionAttributes().get("chatRoomId");

        log.info("[ChatWebSocketEventListener] 연결 종료: userId={}, chatRoomId={}", userId, chatRoomId);
    }
}
