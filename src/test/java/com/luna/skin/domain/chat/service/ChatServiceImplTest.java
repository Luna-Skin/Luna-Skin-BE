package com.luna.skin.domain.chat.service;

import com.luna.skin.domain.analysis.repository.AiAnalysisRepository;
import com.luna.skin.domain.chat.dto.response.ChatErrorResponse;
import com.luna.skin.domain.chat.dto.response.ChatMessageResponse;
import com.luna.skin.domain.chat.entity.AiChatMessage;
import com.luna.skin.domain.chat.entity.AiChatRoom;
import com.luna.skin.domain.chat.exception.ChatErrorCode;
import com.luna.skin.domain.chat.repository.AiChatMessageRepository;
import com.luna.skin.domain.chat.repository.AiChatRoomRepository;
import com.luna.skin.domain.user.entity.User;
import com.luna.skin.domain.user.repository.UserRepository;
import com.luna.skin.global.exception.CustomException;
import com.luna.skin.infra.openai.OpenAiChatClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatServiceImplTest {

    @Mock
    private AiChatRoomRepository aiChatRoomRepository;
    @Mock
    private AiChatMessageRepository aiChatMessageRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private AiAnalysisRepository aiAnalysisRepository;
    @Mock
    private OpenAiChatClient openAiChatClient;
    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private ChatServiceImpl chatService;

    private User userWithId(Long id) {
        User user = new User();
        ReflectionTestUtils.setField(user, "userId", id);
        return user;
    }

    @Test
    void 메시지_전송_시_유저_메시지와_AI_응답을_저장하고_각각_브로드캐스트한다() {
        AiChatRoom chatRoom = AiChatRoom.builder().user(userWithId(1L)).build();
        when(aiChatRoomRepository.findById(10L)).thenReturn(Optional.of(chatRoom));
        when(aiChatMessageRepository.findAllByChatRoom_ChatRoomIdOrderByCreatedAtAsc(10L))
                .thenReturn(List.of());
        when(openAiChatClient.getReply(anyList())).thenReturn("보습을 강화해보세요");

        chatService.sendMessage(1L, 10L, "요즘 피부가 건조해요");

        ArgumentCaptor<AiChatMessage> savedMessages = ArgumentCaptor.forClass(AiChatMessage.class);
        verify(aiChatMessageRepository, times(2)).save(savedMessages.capture());
        assertThat(savedMessages.getAllValues().get(0).getContent()).isEqualTo("요즘 피부가 건조해요");
        assertThat(savedMessages.getAllValues().get(1).getContent()).isEqualTo("보습을 강화해보세요");

        verify(messagingTemplate, times(2))
                .convertAndSend(eq("/topic/chat/10"), any(ChatMessageResponse.class));
    }

    @Test
    void 채팅방이_존재하지_않으면_메시지_전송을_거부한다() {
        when(aiChatRoomRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> chatService.sendMessage(1L, 999L, "hi"))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ChatErrorCode.CHAT_ROOM_NOT_FOUND);

        verifyNoInteractions(aiChatMessageRepository, openAiChatClient, messagingTemplate);
    }

    @Test
    void 채팅방_소유자가_아니면_메시지_전송을_거부한다() {
        AiChatRoom chatRoom = AiChatRoom.builder().user(userWithId(1L)).build();
        when(aiChatRoomRepository.findById(10L)).thenReturn(Optional.of(chatRoom));

        assertThatThrownBy(() -> chatService.sendMessage(2L, 10L, "hi"))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ChatErrorCode.CHAT_ROOM_ACCESS_DENIED);

        verify(aiChatMessageRepository, never()).save(any());
        verifyNoInteractions(openAiChatClient, messagingTemplate);
    }

    @Test
    void 대화_내역_조회_시_시간순으로_정렬된_메시지_목록을_반환한다() {
        AiChatRoom chatRoom = AiChatRoom.builder().user(userWithId(1L)).build();
        AiChatMessage message1 = AiChatMessage.builder().chatRoom(chatRoom).content("첫 메시지").build();
        AiChatMessage message2 = AiChatMessage.builder().chatRoom(chatRoom).content("두번째 메시지").build();
        when(aiChatRoomRepository.findById(10L)).thenReturn(Optional.of(chatRoom));
        when(aiChatMessageRepository.findAllByChatRoom_ChatRoomIdOrderByCreatedAtAsc(10L))
                .thenReturn(List.of(message1, message2));

        List<ChatMessageResponse> result = chatService.getChatMessages(1L, 10L);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getContent()).isEqualTo("첫 메시지");
        assertThat(result.get(1).getContent()).isEqualTo("두번째 메시지");
    }

    @Test
    void 채팅방이_존재하지_않으면_대화_내역_조회를_거부한다() {
        when(aiChatRoomRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> chatService.getChatMessages(1L, 999L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ChatErrorCode.CHAT_ROOM_NOT_FOUND);

        verifyNoInteractions(aiChatMessageRepository);
    }

    @Test
    void 채팅방_소유자가_아니면_대화_내역_조회를_거부한다() {
        AiChatRoom chatRoom = AiChatRoom.builder().user(userWithId(1L)).build();
        when(aiChatRoomRepository.findById(10L)).thenReturn(Optional.of(chatRoom));

        assertThatThrownBy(() -> chatService.getChatMessages(2L, 10L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ChatErrorCode.CHAT_ROOM_ACCESS_DENIED);

        verifyNoInteractions(aiChatMessageRepository);
    }

    @Test
    void AI_응답에_실패해도_유저_메시지는_저장되고_에러가_브로드캐스트된다() {
        AiChatRoom chatRoom = AiChatRoom.builder().user(userWithId(1L)).build();
        when(aiChatRoomRepository.findById(10L)).thenReturn(Optional.of(chatRoom));
        when(aiChatMessageRepository.findAllByChatRoom_ChatRoomIdOrderByCreatedAtAsc(10L))
                .thenReturn(List.of());
        when(openAiChatClient.getReply(anyList()))
                .thenThrow(new CustomException(ChatErrorCode.AI_RESPONSE_FAILED));

        chatService.sendMessage(1L, 10L, "요즘 피부가 건조해요");

        verify(aiChatMessageRepository, times(1)).save(any(AiChatMessage.class));

        ArgumentCaptor<Object> broadcastCaptor = ArgumentCaptor.forClass(Object.class);
        verify(messagingTemplate, times(2)).convertAndSend(eq("/topic/chat/10"), broadcastCaptor.capture());
        assertThat(broadcastCaptor.getAllValues().get(0)).isInstanceOf(ChatMessageResponse.class);
        assertThat(broadcastCaptor.getAllValues().get(1)).isInstanceOf(ChatErrorResponse.class);
    }
}
