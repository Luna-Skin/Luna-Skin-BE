package com.luna.skin.domain.chat.service;

import com.luna.skin.domain.chat.dto.response.ChatRoomListResponse;

import java.util.List;

public interface ChatService {

    /**
     * [채팅방 목록 조회 매서드]
     *
     * @param userId 채팅방 목록을 조회할 사용자 식별자
     * @return 채팅 목록 조회 응답 DTO(chatRoomId, title, createdAt)
     */
    List<ChatRoomListResponse> getAllChatRoomList(Long userId);


}
