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
    long countByRecipientIdAndStatus(UUID recipientId, com.yowpainter.modules.chat.entity.ChatMessageStatus status);
    long countByRecipientIdAndSenderIdAndStatus(UUID recipientId, UUID senderId, com.yowpainter.modules.chat.entity.ChatMessageStatus status);
    
    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("UPDATE ChatMessage m SET m.status = 'READ' WHERE m.recipient.id = :recipientId AND m.sender.id = :senderId AND m.status = 'SENT'")
    void markAsRead(UUID recipientId, UUID senderId);
}
