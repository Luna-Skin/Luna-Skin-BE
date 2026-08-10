package com.luna.skin.domain.chat.websocket;

import com.luna.skin.domain.chat.entity.AiChatRoom;
import com.luna.skin.domain.chat.exception.ChatErrorCode;
import com.luna.skin.domain.chat.repository.AiChatRoomRepository;
import com.luna.skin.domain.user.entity.User;
import com.luna.skin.global.exception.CommonErrorCode;
import com.luna.skin.global.exception.CustomException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatConnectInterceptorTest {

    @Mock
    private AiChatRoomRepository aiChatRoomRepository;

    @InjectMocks
    private ChatConnectInterceptor chatConnectInterceptor;

    private Message<byte[]> connectMessage(String userIdHeader, String chatRoomIdHeader) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        if (userIdHeader != null) {
            accessor.setNativeHeader("X-USER-ID", userIdHeader);
        }
        if (chatRoomIdHeader != null) {
            accessor.setNativeHeader("X-CHAT-ROOM-ID", chatRoomIdHeader);
        }
        accessor.setSessionAttributes(new HashMap<>());
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    private AiChatRoom chatRoomOwnedBy(Long ownerId) {
        User user = new User();
        ReflectionTestUtils.setField(user, "userId", ownerId);
        return AiChatRoom.builder().user(user).build();
    }

    @Test
    void 유저와_채팅방_소유자가_일치하면_연결을_허용한다() {
        Message<byte[]> message = connectMessage("1", "10");
        when(aiChatRoomRepository.findById(10L)).thenReturn(Optional.of(chatRoomOwnedBy(1L)));

        Message<?> result = chatConnectInterceptor.preSend(message, null);

        assertThat(result).isSameAs(message);
        StompHeaderAccessor resultAccessor = StompHeaderAccessor.wrap(result);
        assertThat(resultAccessor.getSessionAttributes()).containsEntry("userId", 1L).containsEntry("chatRoomId", 10L);
    }

    @Test
    void 채팅방이_존재하지_않으면_연결을_거부한다() {
        Message<byte[]> message = connectMessage("1", "999");
        when(aiChatRoomRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> chatConnectInterceptor.preSend(message, null))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ChatErrorCode.CHAT_ROOM_NOT_FOUND);
    }

    @Test
    void 채팅방_소유자가_아니면_연결을_거부한다() {
        Message<byte[]> message = connectMessage("2", "10");
        when(aiChatRoomRepository.findById(10L)).thenReturn(Optional.of(chatRoomOwnedBy(1L)));

        assertThatThrownBy(() -> chatConnectInterceptor.preSend(message, null))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ChatErrorCode.CHAT_ROOM_ACCESS_DENIED);
    }

    @Test
    void 유저ID_헤더가_없으면_인증_에러를_던진다() {
        Message<byte[]> message = connectMessage(null, "10");

        assertThatThrownBy(() -> chatConnectInterceptor.preSend(message, null))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(CommonErrorCode.UNAUTHORIZED);

        verifyNoInteractions(aiChatRoomRepository);
    }

    @Test
    void CONNECT가_아닌_프레임은_검증_없이_통과시킨다() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SEND);
        accessor.setSessionAttributes(new HashMap<>());
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        Message<?> result = chatConnectInterceptor.preSend(message, null);

        assertThat(result).isSameAs(message);
        verifyNoInteractions(aiChatRoomRepository);
    }
}
