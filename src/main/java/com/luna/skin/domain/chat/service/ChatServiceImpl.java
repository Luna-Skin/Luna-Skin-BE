package com.luna.skin.domain.chat.service;

import com.luna.skin.domain.analysis.entity.AiAnalysis;
import com.luna.skin.domain.analysis.repository.AiAnalysisRepository;
import com.luna.skin.domain.chat.dto.request.CreateChatRoomRequest;
import com.luna.skin.domain.chat.dto.response.ChatRoomListResponse;
import com.luna.skin.domain.chat.dto.response.CreateChatRoomResponse;
import com.luna.skin.domain.chat.entity.AiChatRoom;
import com.luna.skin.domain.chat.exception.ChatErrorCode;
import com.luna.skin.domain.chat.repository.AiChatRoomRepository;
import com.luna.skin.domain.user.entity.User;
import com.luna.skin.domain.user.repository.UserRepository;
import com.luna.skin.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ChatServiceImpl implements ChatService {

    private final AiChatRoomRepository aiChatRoomRepository;
    private final UserRepository userRepository;
    private final AiAnalysisRepository aiAnalysisRepository;

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
    public boolean deleteChatRoom(Long userId, Long chatRoomId) {

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

        return true;
    }

}