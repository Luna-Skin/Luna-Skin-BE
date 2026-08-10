package com.luna.skin.domain.chat.repository;

import com.luna.skin.domain.chat.entity.AiChatRoom;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AiChatRoomRepository extends JpaRepository<AiChatRoom,Long> {

    @Modifying
    @Query("update AiChatRoom r set r.title = :title where r.chatRoomId = :chatRoomId")
    int updateTitle(@Param("chatRoomId") Long chatRoomId, @Param("title") String title);

    // 채팅방 목록 최신순 페이지네이션
    Slice<AiChatRoom> findByUser_UserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    // 분석 기록 기반 채팅방 find-or-create 조회
    Optional<AiChatRoom> findByUser_UserIdAndAiAnalysis_AnalysisId(Long userId, Long analysisId);

}
