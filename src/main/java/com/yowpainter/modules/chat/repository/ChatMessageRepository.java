package com.yowpainter.modules.chat.repository;

import com.yowpainter.modules.chat.entity.ChatMessage;
import com.yowpainter.modules.chat.entity.ChatMessageStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, UUID> {
    List<ChatMessage> findByChatIdOrderByTimestampAsc(String chatId);
    long countByRecipientIdAndStatus(UUID recipientId, ChatMessageStatus status);
}
