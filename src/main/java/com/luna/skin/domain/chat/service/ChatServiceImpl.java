package com.luna.skin.domain.chat.service;

import com.luna.skin.domain.chat.dto.response.ChatRoomListResponse;
import com.luna.skin.domain.chat.entity.AiChatRoom;
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





}
