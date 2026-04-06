package com.yowpainter.modules.chat.controller;

import com.yowpainter.modules.chat.dto.ChatMessageDto;
import com.yowpainter.modules.chat.service.ChatMessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class ChatController {

    private final SimpMessagingTemplate messagingTemplate;
    private final ChatMessageService chatMessageService;

    @MessageMapping("/chat")
    public void processMessage(@Payload ChatMessageDto chatMessageDto) {
        ChatMessageDto savedMessage = chatMessageService.save(chatMessageDto);
        
        // Destinataire via /user/{recipientId}/queue/messages
        messagingTemplate.convertAndSendToUser(
                chatMessageDto.getRecipientId().toString(),
                "/queue/messages",
                savedMessage
        );
    }

    @GetMapping("/api/messages/{senderId}/{recipientId}")
    public ResponseEntity<List<ChatMessageDto>> findChatMessages(
            @PathVariable UUID senderId,
            @PathVariable UUID recipientId) {
        return ResponseEntity.ok(chatMessageService.findChatMessages(senderId, recipientId));
    }
}
