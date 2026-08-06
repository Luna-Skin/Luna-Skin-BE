package com.luna.skin.domain.chat.service;

import com.luna.skin.domain.chat.dto.request.CreateChatRoomRequest;
import com.luna.skin.domain.chat.dto.response.ChatRoomListResponse;
import com.luna.skin.domain.chat.dto.response.CreateChatRoomResponse;

import java.util.List;

public interface ChatService {

    /**
     * [채팅방 목록 조회 매서드]
     *
     * @param userId 채팅방 목록을 조회할 사용자 식별자
     * @return 채팅 목록 조회 응답 DTO(chatRoomId, title, createdAt)
     */
    List<ChatRoomListResponse> getAllChatRoomList(Long userId);

    /**
     * [채팅방 생성 매서드]
     *
     * @param userId
     * @param createChatRoomRequest title, analysis_id(선택)
     * @return 채팅 생성 응답 DTO(chatRoomId, title, createdAt)
     */
    CreateChatRoomResponse createChatRoom(Long userId, CreateChatRoomRequest createChatRoomRequest);

    /**
     * [채팅방 삭제 매서드]
     *
     * @param userId 채팅방을 삭제하려는 사용자 식별자 (소유자 검증용)
     * @param chatRoomId 삭제할 채팅방 식별자
     */
    void deleteChatRoom(Long userId, Long chatRoomId);




}
