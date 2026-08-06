package com.luna.skin.domain.chat.websocket;

import com.luna.skin.domain.chat.entity.AiChatRoom;
import com.luna.skin.domain.chat.exception.ChatErrorCode;
import com.luna.skin.domain.chat.repository.AiChatRoomRepository;
import com.luna.skin.global.exception.CommonErrorCode;
import com.luna.skin.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatConnectInterceptor implements ChannelInterceptor {

    private static final String USER_ID_HEADER = "X-USER-ID";
    private static final String CHAT_ROOM_ID_HEADER = "X-CHAT-ROOM-ID";

    private final AiChatRoomRepository aiChatRoomRepository;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            Long userId = parseHeader(accessor.getFirstNativeHeader(USER_ID_HEADER));
            Long chatRoomId = parseHeader(accessor.getFirstNativeHeader(CHAT_ROOM_ID_HEADER));

            AiChatRoom chatRoom = aiChatRoomRepository.findById(chatRoomId)
                    .orElseThrow(() -> {
                        log.error("[ChatConnectInterceptor] 연결 거부 - 채팅방을 찾을 수 없음: chatRoomId={}", chatRoomId);
                        return new CustomException(ChatErrorCode.CHAT_ROOM_NOT_FOUND);
                    });

            if (!chatRoom.getUser().getUserId().equals(userId)) {
                log.error("[ChatConnectInterceptor] 연결 거부 - 본인 소유의 채팅방이 아님: userId={}, chatRoomId={}", userId, chatRoomId);
                throw new CustomException(ChatErrorCode.CHAT_ROOM_ACCESS_DENIED);
            }

            accessor.getSessionAttributes().put("userId", userId);
            accessor.getSessionAttributes().put("chatRoomId", chatRoomId);

            log.info("[ChatConnectInterceptor] 연결 허용: userId={}, chatRoomId={}", userId, chatRoomId);
        }

        return message;
    }

    private Long parseHeader(String headerValue) {
        if (!StringUtils.hasText(headerValue)) {
            throw new CustomException(CommonErrorCode.UNAUTHORIZED);
        }
        try {
            return Long.valueOf(headerValue);
        } catch (NumberFormatException e) {
            throw new CustomException(CommonErrorCode.UNAUTHORIZED);
        }
    }
}
