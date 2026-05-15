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
        
        // On récupère l'email du destinataire pour envoyer au bon "User" STOMP
        // car Spring Security utilise l'email comme Principal name.
        String recipientEmail = chatMessageService.getRecipientEmail(chatMessageDto.getRecipientId());

        messagingTemplate.convertAndSendToUser(
                recipientEmail,
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

    @GetMapping("/api/chat/contacts/{userId}")
    public ResponseEntity<List<com.yowpainter.modules.chat.dto.UserChatDto>> getContacts(@PathVariable UUID userId) {
        return ResponseEntity.ok(chatMessageService.getRecentContacts(userId));
    }

    @GetMapping("/api/chat/search")
    public ResponseEntity<List<com.yowpainter.modules.chat.dto.UserChatDto>> searchUsers(@org.springframework.web.bind.annotation.RequestParam String q) {
        return ResponseEntity.ok(chatMessageService.searchUsers(q));
    }
}
