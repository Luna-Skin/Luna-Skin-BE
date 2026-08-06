package com.luna.skin.domain.chat.repository;

import com.luna.skin.domain.chat.entity.AiChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AiChatRoomRepository extends JpaRepository<AiChatRoom,Long> {

    List<AiChatRoom> findAllByUser_UserId(Long userId);

}
