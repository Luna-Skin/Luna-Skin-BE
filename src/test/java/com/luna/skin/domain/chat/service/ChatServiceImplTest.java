package com.luna.skin.domain.chat.service;

import com.luna.skin.domain.analysis.entity.AiAnalysis;
import com.luna.skin.domain.analysis.entity.DetailedSkinAnalysis;
import com.luna.skin.domain.analysis.enums.SkinStatusLabel;
import com.luna.skin.domain.analysis.repository.AiAnalysisRepository;
import com.luna.skin.domain.analysis.repository.DetailedSkinAnalysisRepository;
import com.luna.skin.domain.chat.dto.response.ChatErrorResponse;
import com.luna.skin.domain.chat.dto.response.ChatMessagePageResponse;
import com.luna.skin.domain.chat.dto.response.ChatMessageResponse;
import com.luna.skin.domain.chat.dto.response.ChatRoomListPageResponse;
import com.luna.skin.domain.chat.dto.response.CreateChatRoomResponse;
import com.luna.skin.domain.chat.dto.response.RenameChatRoomResponse;
import com.luna.skin.domain.chat.entity.AiChatMessage;
import com.luna.skin.domain.chat.entity.AiChatRoom;
import com.luna.skin.domain.chat.exception.ChatErrorCode;
import com.luna.skin.domain.chat.repository.AiChatMessageRepository;
import com.luna.skin.domain.chat.repository.AiChatRoomRepository;
import com.luna.skin.domain.skin.entity.TodaySkin;
import com.luna.skin.domain.user.entity.User;
import com.luna.skin.domain.user.repository.UserRepository;
import com.luna.skin.global.exception.CustomException;
import com.luna.skin.infra.openai.OpenAiChatClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.SliceImpl;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
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
    private DetailedSkinAnalysisRepository detailedSkinAnalysisRepository;
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

    // sendMessage는 커밋 이후에만 브로드캐스트하므로, 트랜잭션 동기화를 직접 열고 커밋을 흉내낸 뒤 검증한다.
    private void sendMessageAndSimulateCommit(Long userId, Long chatRoomId, String content) {
        TransactionSynchronizationManager.initSynchronization();
        try {
            chatService.sendMessage(userId, chatRoomId, content);
            TransactionSynchronizationManager.getSynchronizations()
                    .forEach(TransactionSynchronization::afterCommit);
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void 메시지_전송_시_유저_메시지와_AI_응답을_저장하고_각각_브로드캐스트한다() {
        AiChatRoom chatRoom = AiChatRoom.builder().user(userWithId(1L)).build();
        when(aiChatRoomRepository.findById(10L)).thenReturn(Optional.of(chatRoom));
        when(aiChatMessageRepository.findByChatRoom_ChatRoomIdOrderByCreatedAtDesc(eq(10L), any()))
                .thenReturn(new SliceImpl<>(List.of()));
        when(openAiChatClient.getReply(anyList(), any())).thenReturn("보습을 강화해보세요");

        sendMessageAndSimulateCommit(1L, 10L, "요즘 피부가 건조해요");

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
    void AI_응답에_실패해도_유저_메시지는_저장되고_에러가_브로드캐스트된다() {
        AiChatRoom chatRoom = AiChatRoom.builder().user(userWithId(1L)).build();
        when(aiChatRoomRepository.findById(10L)).thenReturn(Optional.of(chatRoom));
        when(aiChatMessageRepository.findByChatRoom_ChatRoomIdOrderByCreatedAtDesc(eq(10L), any()))
                .thenReturn(new SliceImpl<>(List.of()));
        when(openAiChatClient.getReply(anyList(), any()))
                .thenThrow(new CustomException(ChatErrorCode.AI_RESPONSE_FAILED));

        sendMessageAndSimulateCommit(1L, 10L, "요즘 피부가 건조해요");

        verify(aiChatMessageRepository, times(1)).save(any(AiChatMessage.class));

        ArgumentCaptor<Object> broadcastCaptor = ArgumentCaptor.forClass(Object.class);
        verify(messagingTemplate, times(2)).convertAndSend(eq("/topic/chat/10"), broadcastCaptor.capture());
        assertThat(broadcastCaptor.getAllValues().get(0)).isInstanceOf(ChatMessageResponse.class);
        assertThat(broadcastCaptor.getAllValues().get(1)).isInstanceOf(ChatErrorResponse.class);
    }

    @Test
    void 메시지_전송이_커밋되기_전에는_브로드캐스트되지_않는다() {
        AiChatRoom chatRoom = AiChatRoom.builder().user(userWithId(1L)).build();
        when(aiChatRoomRepository.findById(10L)).thenReturn(Optional.of(chatRoom));
        when(aiChatMessageRepository.findByChatRoom_ChatRoomIdOrderByCreatedAtDesc(eq(10L), any()))
                .thenReturn(new SliceImpl<>(List.of()));
        when(openAiChatClient.getReply(anyList(), any())).thenReturn("보습을 강화해보세요");

        TransactionSynchronizationManager.initSynchronization();
        try {
            chatService.sendMessage(1L, 10L, "요즘 피부가 건조해요");
            // afterCommit을 흉내내지 않음: 롤백 시나리오와 동일
            verifyNoInteractions(messagingTemplate);
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void 대화_내역_조회_시_최신순으로_정렬된_메시지_목록을_반환한다() {
        AiChatRoom chatRoom = AiChatRoom.builder().user(userWithId(1L)).build();
        AiChatMessage message1 = AiChatMessage.builder().chatRoom(chatRoom).content("첫 메시지").build();
        AiChatMessage message2 = AiChatMessage.builder().chatRoom(chatRoom).content("두번째 메시지").build();
        when(aiChatRoomRepository.findById(10L)).thenReturn(Optional.of(chatRoom));
        when(aiChatMessageRepository.findByChatRoom_ChatRoomIdOrderByCreatedAtDesc(eq(10L), any()))
                .thenReturn(new SliceImpl<>(List.of(message2, message1), PageRequest.of(0, 20), false));

        ChatMessagePageResponse result = chatService.getChatMessages(1L, 10L, 0, 20);

        assertThat(result.getMessages()).hasSize(2);
        assertThat(result.getMessages().get(0).getContent()).isEqualTo("두번째 메시지");
        assertThat(result.isHasNext()).isFalse();
    }

    @Test
    void 채팅방이_존재하지_않으면_대화_내역_조회를_거부한다() {
        when(aiChatRoomRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> chatService.getChatMessages(1L, 999L, 0, 20))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ChatErrorCode.CHAT_ROOM_NOT_FOUND);

        verifyNoInteractions(aiChatMessageRepository);
    }

    @Test
    void 채팅방_소유자가_아니면_대화_내역_조회를_거부한다() {
        AiChatRoom chatRoom = AiChatRoom.builder().user(userWithId(1L)).build();
        when(aiChatRoomRepository.findById(10L)).thenReturn(Optional.of(chatRoom));

        assertThatThrownBy(() -> chatService.getChatMessages(2L, 10L, 0, 20))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ChatErrorCode.CHAT_ROOM_ACCESS_DENIED);

        verifyNoInteractions(aiChatMessageRepository);
    }

    @Test
    void 채팅방_목록을_페이지네이션으로_조회한다() {
        AiChatRoom room = AiChatRoom.builder().user(userWithId(1L)).title("피부 고민").build();
        when(aiChatRoomRepository.findByUser_UserIdOrderByCreatedAtDesc(eq(1L), any()))
                .thenReturn(new SliceImpl<>(List.of(room), PageRequest.of(0, 20), false));

        ChatRoomListPageResponse result = chatService.getAllChatRoomList(1L, 0, 20);

        assertThat(result.getChatRooms()).hasSize(1);
        assertThat(result.getChatRooms().get(0).getTitle()).isEqualTo("피부 고민");
        assertThat(result.isHasNext()).isFalse();
    }

    @Test
    void 채팅방_삭제_시_메시지를_먼저_삭제한_후_채팅방을_삭제한다() {
        AiChatRoom chatRoom = AiChatRoom.builder().user(userWithId(1L)).build();
        when(aiChatRoomRepository.findById(10L)).thenReturn(Optional.of(chatRoom));

        chatService.deleteChatRoom(1L, 10L);

        InOrder inOrder = inOrder(aiChatMessageRepository, aiChatRoomRepository);
        inOrder.verify(aiChatMessageRepository).deleteAllByChatRoom_ChatRoomId(10L);
        inOrder.verify(aiChatRoomRepository).delete(chatRoom);
    }

    @Test
    void 채팅방이_존재하지_않으면_삭제를_거부한다() {
        when(aiChatRoomRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> chatService.deleteChatRoom(1L, 999L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ChatErrorCode.CHAT_ROOM_NOT_FOUND);

        verifyNoInteractions(aiChatMessageRepository);
    }

    @Test
    void 채팅방_소유자가_아니면_삭제를_거부한다() {
        AiChatRoom chatRoom = AiChatRoom.builder().user(userWithId(1L)).build();
        when(aiChatRoomRepository.findById(10L)).thenReturn(Optional.of(chatRoom));

        assertThatThrownBy(() -> chatService.deleteChatRoom(2L, 10L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ChatErrorCode.CHAT_ROOM_ACCESS_DENIED);

        verifyNoInteractions(aiChatMessageRepository);
        verify(aiChatRoomRepository, never()).delete(any());
    }

    @Test
    void 채팅방_이름을_변경한다() {
        AiChatRoom chatRoom = AiChatRoom.builder().user(userWithId(1L)).build();
        when(aiChatRoomRepository.findById(10L)).thenReturn(Optional.of(chatRoom));

        RenameChatRoomResponse response = chatService.renameChatRoom(1L, 10L, "새 제목");

        assertThat(response.getChatRoomId()).isEqualTo(10L);
        assertThat(response.getTitle()).isEqualTo("새 제목");
        verify(aiChatRoomRepository).updateTitle(10L, "새 제목");
    }

    @Test
    void 채팅방이_존재하지_않으면_이름_변경을_거부한다() {
        when(aiChatRoomRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> chatService.renameChatRoom(1L, 999L, "새 제목"))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ChatErrorCode.CHAT_ROOM_NOT_FOUND);
    }

    @Test
    void 채팅방_소유자가_아니면_이름_변경을_거부한다() {
        AiChatRoom chatRoom = AiChatRoom.builder().user(userWithId(1L)).build();
        when(aiChatRoomRepository.findById(10L)).thenReturn(Optional.of(chatRoom));

        assertThatThrownBy(() -> chatService.renameChatRoom(2L, 10L, "새 제목"))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ChatErrorCode.CHAT_ROOM_ACCESS_DENIED);

        verify(aiChatRoomRepository, never()).updateTitle(any(), any());
    }

    @Test
    void 분석_기록에_대한_채팅방이_없으면_새로_생성한다() {
        User user = userWithId(1L);
        TodaySkin todaySkin = TodaySkin.builder().user(user).logDate(LocalDate.of(2026, 8, 6)).build();
        AiAnalysis aiAnalysis = AiAnalysis.builder()
                .analysisId(5L).todaySkin(todaySkin).overallScore(70).skinStatusLabel(SkinStatusLabel.NORMAL).build();

        when(aiAnalysisRepository.findById(5L)).thenReturn(Optional.of(aiAnalysis));
        when(aiChatRoomRepository.findByUser_UserIdAndAiAnalysis_AnalysisId(1L, 5L)).thenReturn(Optional.empty());
        when(aiChatRoomRepository.save(any(AiChatRoom.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CreateChatRoomResponse response = chatService.findOrCreateChatRoomFromAnalysis(1L, 5L);

        assertThat(response.getTitle()).isEqualTo("2026년 8월 6일 피부 상담");
        verify(aiChatRoomRepository).save(any(AiChatRoom.class));
    }

    @Test
    void 분석_기록에_대한_채팅방이_이미_있으면_기존_방을_재사용한다() {
        User user = userWithId(1L);
        TodaySkin todaySkin = TodaySkin.builder().user(user).logDate(LocalDate.of(2026, 8, 6)).build();
        AiAnalysis aiAnalysis = AiAnalysis.builder()
                .analysisId(5L).todaySkin(todaySkin).skinStatusLabel(SkinStatusLabel.NORMAL).build();
        AiChatRoom existingRoom = AiChatRoom.builder()
                .chatRoomId(99L).user(user).title("기존 방").aiAnalysis(aiAnalysis).build();

        when(aiAnalysisRepository.findById(5L)).thenReturn(Optional.of(aiAnalysis));
        when(aiChatRoomRepository.findByUser_UserIdAndAiAnalysis_AnalysisId(1L, 5L)).thenReturn(Optional.of(existingRoom));

        CreateChatRoomResponse response = chatService.findOrCreateChatRoomFromAnalysis(1L, 5L);

        assertThat(response.getChatRoomId()).isEqualTo(99L);
        assertThat(response.getTitle()).isEqualTo("기존 방");
        verify(aiChatRoomRepository, never()).save(any());
    }

    @Test
    void 분석_기록이_존재하지_않으면_채팅방_생성을_거부한다() {
        when(aiAnalysisRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> chatService.findOrCreateChatRoomFromAnalysis(1L, 999L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ChatErrorCode.CHAT_ANALYSIS_NOT_FOUND);

        verifyNoInteractions(aiChatRoomRepository);
    }

    @Test
    void 본인_소유의_분석이_아니면_채팅방_생성을_거부한다() {
        TodaySkin todaySkin = TodaySkin.builder().user(userWithId(1L)).logDate(LocalDate.of(2026, 8, 6)).build();
        AiAnalysis aiAnalysis = AiAnalysis.builder()
                .analysisId(5L).todaySkin(todaySkin).skinStatusLabel(SkinStatusLabel.NORMAL).build();
        when(aiAnalysisRepository.findById(5L)).thenReturn(Optional.of(aiAnalysis));

        assertThatThrownBy(() -> chatService.findOrCreateChatRoomFromAnalysis(2L, 5L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ChatErrorCode.CHAT_ANALYSIS_ACCESS_DENIED);

        verifyNoInteractions(aiChatRoomRepository);
    }

    @Test
    void 분석_기록에_연결된_채팅방에서_메시지를_보내면_그날_지표가_AI_컨텍스트로_전달된다() {
        User user = userWithId(1L);
        TodaySkin todaySkin = TodaySkin.builder().user(user).logDate(LocalDate.of(2026, 8, 6)).build();
        AiAnalysis aiAnalysis = AiAnalysis.builder()
                .analysisId(5L).todaySkin(todaySkin).overallScore(70)
                .skinStatusLabel(SkinStatusLabel.NORMAL).aiComment("황체기 피지 분비 증가").build();
        AiChatRoom chatRoom = AiChatRoom.builder().user(user).aiAnalysis(aiAnalysis).build();
        DetailedSkinAnalysis detail = DetailedSkinAnalysis.builder()
                .aiAnalysis(aiAnalysis).trouble(45).sebum(90).dullness(65).moisture(20).elasticity(50).build();

        when(aiChatRoomRepository.findById(10L)).thenReturn(Optional.of(chatRoom));
        when(aiChatMessageRepository.findByChatRoom_ChatRoomIdOrderByCreatedAtDesc(eq(10L), any()))
                .thenReturn(new SliceImpl<>(List.of()));
        when(detailedSkinAnalysisRepository.findByAiAnalysis(aiAnalysis)).thenReturn(Optional.of(detail));
        when(openAiChatClient.getReply(anyList(), any())).thenReturn("답변");

        sendMessageAndSimulateCommit(1L, 10L, "왜 유분이 높아졌어?");

        ArgumentCaptor<String> contextCaptor = ArgumentCaptor.forClass(String.class);
        verify(openAiChatClient).getReply(anyList(), contextCaptor.capture());
        assertThat(contextCaptor.getValue())
                .contains("유분 90")
                .contains("트러블 45")
                .contains("황체기 피지 분비 증가");
    }
}
