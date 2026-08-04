package com.luna.skin.domain.chat.repository;

import com.luna.skin.domain.chat.entity.AiChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AiChatMessageRepository extends JpaRepository<AiChatMessage, Long> {
}
