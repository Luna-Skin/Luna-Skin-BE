package com.luna.skin.domain.chat.service;

import com.luna.skin.domain.analysis.entity.AiAnalysis;
import com.luna.skin.domain.analysis.repository.AiAnalysisRepository;
import com.luna.skin.domain.chat.dto.request.CreateChatRoomRequest;
import com.luna.skin.domain.chat.dto.response.ChatErrorResponse;
import com.luna.skin.domain.chat.dto.response.ChatMessageResponse;
import com.luna.skin.domain.chat.dto.response.ChatRoomListResponse;
import com.luna.skin.domain.chat.dto.response.CreateChatRoomResponse;
import com.luna.skin.domain.chat.entity.AiChatMessage;
import com.luna.skin.domain.chat.entity.AiChatRoom;
import com.luna.skin.domain.chat.enums.MessageRole;
import com.luna.skin.domain.chat.enums.MessageType;
import com.luna.skin.domain.chat.exception.ChatErrorCode;
import com.luna.skin.domain.chat.repository.AiChatMessageRepository;
import com.luna.skin.domain.chat.repository.AiChatRoomRepository;
import com.luna.skin.domain.user.entity.User;
import com.luna.skin.domain.user.repository.UserRepository;
import com.luna.skin.global.exception.CustomException;
import com.luna.skin.infra.openai.OpenAiChatClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ChatServiceImpl implements ChatService {

    private static final String CHAT_ROOM_TOPIC_PREFIX = "/topic/chat/";

    private final AiChatRoomRepository aiChatRoomRepository;
    private final AiChatMessageRepository aiChatMessageRepository;
    private final UserRepository userRepository;
    private final AiAnalysisRepository aiAnalysisRepository;
    private final OpenAiChatClient openAiChatClient;
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    @Transactional(readOnly = true)
    public List<ChatRoomListResponse> getAllChatRoomList(Long userId) {

        log.info("[ChatService] 채팅방 목록 조회 - 시작");

        List<ChatRoomListResponse> chatRoomList = aiChatRoomRepository.findAllByUser_UserId(userId)
                .stream()
                .map(ChatRoomListResponse::from)
                .toList();

        log.info("[ChatService] 채팅방 목록 조회 - 완료: 채팅방 개수:{}", chatRoomList.size());

        return chatRoomList;
    }

    @Override
    public CreateChatRoomResponse createChatRoom(Long userId, CreateChatRoomRequest createChatRoomRequest) {

        log.info("[ChatService] 채팅방 생성 - 시작: title:{}", createChatRoomRequest.getTitle());

        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error("[ChatService] 채팅방 생성 - 에러: 존재하지 않는 유저입니다. id={}", userId);
                    return new CustomException(ChatErrorCode.CHAT_USER_NOT_FOUND);
                });

        Long aiAnalysisId = createChatRoomRequest.getAiAnalysis();
        AiAnalysis aiAnalysis = null;

        if (aiAnalysisId != null && aiAnalysisId > 0) {
            aiAnalysis = aiAnalysisRepository.findById(aiAnalysisId)
                    .orElseThrow(() -> {
                        log.error("[ChatService] 채팅방 생성 - 에러: 존재하지 않는 분석입니다. id={}", aiAnalysisId);
                        return new CustomException(ChatErrorCode.CHAT_ANALYSIS_NOT_FOUND);
                    });
        }

        AiChatRoom aiChatRoom = AiChatRoom.builder()
                .user(user)
                .title(createChatRoomRequest.getTitle())
                .aiAnalysis(aiAnalysis)
                .build();

        CreateChatRoomResponse response = CreateChatRoomResponse.from(aiChatRoomRepository.save(aiChatRoom));

        log.info("[ChatService] 채팅방 생성 - 완료: 채팅방 제목={}", createChatRoomRequest.getTitle());

        return response;
    }

    @Override
    public void deleteChatRoom(Long userId, Long chatRoomId) {

        log.info("[ChatService] 채팅방 삭제 - 시작: chatRoomId={}", chatRoomId);

        AiChatRoom aiChatRoom = aiChatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> {
                    log.error("[ChatService] 채팅방 삭제 - 에러: 해당 채팅방 식별자를 찾을 수 없습니다. chatRoomId={}", chatRoomId);
                    return new CustomException(ChatErrorCode.CHAT_ROOM_NOT_FOUND);
                });

        if (!aiChatRoom.getUser().getUserId().equals(userId)) {
            log.error("[ChatService] 채팅방 삭제 - 에러: 본인 소유의 채팅방이 아닙니다. userId={}, chatRoomId={}", userId, chatRoomId);
            throw new CustomException(ChatErrorCode.CHAT_ROOM_ACCESS_DENIED);
        }

        aiChatRoomRepository.delete(aiChatRoom);

        log.info("[ChatService] 채팅방 삭제 - 완료");
    }

    @Override
    public void sendMessage(Long userId, Long chatRoomId, String content) {

        log.info("[ChatService] 메시지 전송 - 시작: chatRoomId={}", chatRoomId);

        AiChatRoom aiChatRoom = aiChatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> {
                    log.error("[ChatService] 메시지 전송 - 에러: 해당 채팅방 식별자를 찾을 수 없습니다. chatRoomId={}", chatRoomId);
                    return new CustomException(ChatErrorCode.CHAT_ROOM_NOT_FOUND);
                });

        if (!aiChatRoom.getUser().getUserId().equals(userId)) {
            log.error("[ChatService] 메시지 전송 - 에러: 본인 소유의 채팅방이 아닙니다. userId={}, chatRoomId={}", userId, chatRoomId);
            throw new CustomException(ChatErrorCode.CHAT_ROOM_ACCESS_DENIED);
        }

        AiChatMessage userMessage = AiChatMessage.builder()
                .chatRoom(aiChatRoom)
                .role(MessageRole.USER)
                .messageType(MessageType.TEXT)
                .content(content)
                .build();
        aiChatMessageRepository.save(userMessage);
        messagingTemplate.convertAndSend(CHAT_ROOM_TOPIC_PREFIX + chatRoomId, ChatMessageResponse.from(userMessage));

        List<AiChatMessage> history = aiChatMessageRepository.findAllByChatRoom_ChatRoomIdOrderByCreatedAtAsc(chatRoomId);

        String aiReply;
        try {
            aiReply = openAiChatClient.getReply(history);
        } catch (CustomException e) {
            log.error("[ChatService] 메시지 전송 - AI 응답 실패: chatRoomId={}, message={}", chatRoomId, e.getMessage());
            messagingTemplate.convertAndSend(CHAT_ROOM_TOPIC_PREFIX + chatRoomId, ChatErrorResponse.of(e.getMessage()));
            return;
        }

        AiChatMessage aiMessage = AiChatMessage.builder()
                .chatRoom(aiChatRoom)
                .role(MessageRole.AI)
                .messageType(MessageType.TEXT)
                .content(aiReply)
                .build();
        aiChatMessageRepository.save(aiMessage);
        messagingTemplate.convertAndSend(CHAT_ROOM_TOPIC_PREFIX + chatRoomId, ChatMessageResponse.from(aiMessage));

        log.info("[ChatService] 메시지 전송 - 완료: chatRoomId={}", chatRoomId);
    }

}