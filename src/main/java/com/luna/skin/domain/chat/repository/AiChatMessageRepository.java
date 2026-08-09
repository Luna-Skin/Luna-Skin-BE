package com.luna.skin.domain.chat.repository;

import com.luna.skin.domain.chat.entity.AiChatMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiChatMessageRepository extends JpaRepository<AiChatMessage, Long> {

    void deleteAllByChatRoom_ChatRoomId(Long chatRoomId);

    // 최신순 메시지 조회 (대화 내역 페이지네이션, AI 응답 생성용 최근 이력 조회에 공용으로 사용)
    Slice<AiChatMessage> findByChatRoom_ChatRoomIdOrderByCreatedAtDesc(Long chatRoomId, Pageable pageable);
}
