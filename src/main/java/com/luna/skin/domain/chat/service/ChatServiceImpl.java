package com.luna.skin.domain.chat.service;

import com.luna.skin.domain.analysis.entity.AiAnalysis;
import com.luna.skin.domain.analysis.repository.AiAnalysisRepository;
import com.luna.skin.domain.chat.dto.request.CreateChatRoomRequest;
import com.luna.skin.domain.chat.dto.response.ChatRoomListResponse;
import com.luna.skin.domain.chat.dto.response.CreateChatRoomResponse;
import com.luna.skin.domain.chat.entity.AiChatRoom;
import com.luna.skin.domain.chat.repository.AiChatRoomRepository;
import com.luna.skin.domain.user.entity.User;
import com.luna.skin.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.springframework.data.jpa.domain.AbstractPersistable_.id;

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
    public List<ChatRoomListResponse> getAllChatRoomList(Long userId){

        log.info("[ChatService] 채팅방 목록 조회 - 시작");

        List<ChatRoomListResponse> chatRoomList =

                aiChatRoomRepository.findAllByUser_UserId(userId)

                        .stream()

                        .map(ChatRoomListResponse::from)

                        .toList();

        log.info("[ChatService] 채팅방 목록 조회 - 완료: 채팅방 개수:{}", chatRoomList.size());

        return chatRoomList;
    }

    @Override
    @Transactional
    public CreateChatRoomResponse createChatRoom(Long userId, CreateChatRoomRequest createChatRoomRequest) {

        log.info("[ChatService] 채팅방 생성 - 시작: title:{}", createChatRoomRequest.getTitle());

        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error("[ChatService] 채팅방 생성 - 에러: 존재하지 않는 유저입니다. id={}", userId);

                    return new IllegalArgumentException("존재하지 않는 유저입니다. id=" + userId);
                });


        Long aiAnalysisId = createChatRoomRequest.getAiAnalysis();

        if (aiAnalysisId != null && aiAnalysisId > 0) {

            AiAnalysis aiAnalysis = aiAnalysisRepository.findById(aiAnalysisId)
                    .orElseThrow(() -> {
                        log.error("[ChatService] 채팅방 생성 - 에러: 존재하지 않는 분석입니다. id={}", aiAnalysisId);

                        return new IllegalArgumentException("존재하지 않는 분석입니다. id=" + aiAnalysisId);
                    });

            AiChatRoom aiChatRoom = AiChatRoom.builder()
                    .user(user)
                    .title(createChatRoomRequest.getTitle())
                    .aiAnalysis(aiAnalysis)
                    .build();

            return CreateChatRoomResponse.from(
                    aiChatRoomRepository.save(aiChatRoom)
            );
        }
        AiChatRoom aiChatRoom = AiChatRoom.builder()
                .user(user)
                .title(createChatRoomRequest.getTitle())
                .build();
        log.info("[ChatService] 채팅방 생성 - 완료: 채팅방 제목={}", createChatRoomRequest.getTitle());
        return CreateChatRoomResponse.from(aiChatRoomRepository.save(aiChatRoom)

        );
    }

    @Override
    public boolean deleteChatRoom(Long chatRoomId) {

        log.info("[ChatService] 채팅방 삭제 - 시작: chatRoomId={}", chatRoomId);

        if(!aiChatRoomRepository.findById(chatRoomId).isPresent()) {
            log.error("[ChatService] 채팅방 삭제 - 에러: 해당 채팅방 식별자를 찾을수 없습니다");
            new IllegalArgumentException("해당 채팅방 식별자를 찾을수 없습니다.");
            return false;

        }else {
            aiChatRoomRepository.deleteById(chatRoomId);
            log.info("[ChatService] 채팅방 삭제 - 완료");
            return true;
        }
    }


}
