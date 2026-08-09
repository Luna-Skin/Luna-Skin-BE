package com.luna.skin.domain.chat.service;

import com.luna.skin.domain.chat.dto.request.CreateChatRoomRequest;
import com.luna.skin.domain.chat.dto.response.ChatMessagePageResponse;
import com.luna.skin.domain.chat.dto.response.ChatRoomListPageResponse;
import com.luna.skin.domain.chat.dto.response.CreateChatRoomResponse;
import com.luna.skin.domain.chat.dto.response.RenameChatRoomResponse;

public interface ChatService {

    /**
     * [채팅방 목록 조회 매서드]
     *
     * @param userId 채팅방 목록을 조회할 사용자 식별자
     * @param page 조회할 페이지 번호 (0부터 시작)
     * @param size 페이지 크기
     * @return 페이지네이션된 채팅방 목록 응답 DTO
     */
    ChatRoomListPageResponse getAllChatRoomList(Long userId, int page, int size);

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

    /**
     * [메시지 전송 매서드]
     * 유저 메시지를 저장 후 브로드캐스트하고, OpenAI 응답을 받아 저장 후 브로드캐스트한다.
     *
     * @param userId 메시지를 보내는 사용자 식별자 (소유자 검증용)
     * @param chatRoomId 메시지를 보낼 채팅방 식별자
     * @param content 메시지 내용
     */
    void sendMessage(Long userId, Long chatRoomId, String content);

    /**
     * [대화 내역 조회 매서드]
     *
     * @param userId 채팅방에 접근하려는 사용자 식별자 (소유자 검증용)
     * @param chatRoomId 대화 내역을 조회할 채팅방 식별자
     * @param page 조회할 페이지 번호 (0부터 시작)
     * @param size 페이지 크기
     * @return 페이지네이션된 메시지 목록 응답 DTO
     */
    ChatMessagePageResponse getChatMessages(Long userId, Long chatRoomId, int page, int size);

    /**
     * [채팅방 이름 수정 매서드]
     *
     * @param userId 채팅방에 접근하려는 사용자 식별자 (소유자 검증용)
     * @param chatRoomId 이름을 변경할 채팅방 식별자
     * @param newName 변경할 채팅방 제목
     * @return 채팅방 이름 변경 응답 DTO
     */
    RenameChatRoomResponse renameChatRoom(Long userId, Long chatRoomId, String newName);

    /**
     * [분석 기록 기반 채팅방 조회/생성 매서드]
     * 해당 분석 기록에 대한 채팅방이 이미 있으면 그대로 반환하고, 없으면 새로 생성한다(find-or-create).
     *
     * @param userId 요청 사용자 식별자 (분석 기록 소유자 검증용)
     * @param analysisId 질문할 대상 분석 기록 식별자
     * @return 채팅방 생성/조회 응답 DTO
     */
    CreateChatRoomResponse findOrCreateChatRoomFromAnalysis(Long userId, Long analysisId);

}
