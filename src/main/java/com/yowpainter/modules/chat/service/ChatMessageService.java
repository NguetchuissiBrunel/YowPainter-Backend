package com.yowpainter.modules.chat.service;

import com.yowpainter.modules.auth.entity.AppUser;
import com.yowpainter.modules.auth.repository.AppUserRepository;
import com.yowpainter.modules.chat.dto.ChatMessageDto;
import com.yowpainter.modules.chat.entity.ChatMessage;
import com.yowpainter.modules.chat.entity.ChatMessageStatus;
import com.yowpainter.modules.chat.repository.ChatMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatMessageService {

    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomService chatRoomService;
    private final AppUserRepository appUserRepository;

    public ChatMessageDto save(ChatMessageDto chatMessageDto) {
        String chatId = chatRoomService
                .getChatRoomId(chatMessageDto.getSenderId(), chatMessageDto.getRecipientId(), true)
                .orElseThrow(() -> new IllegalStateException("Impossible de créer une room"));

        chatMessageDto.setChatId(chatId);

        AppUser sender = appUserRepository.findById(chatMessageDto.getSenderId())
                .orElseThrow(() -> new IllegalArgumentException("Sender non trouvé"));
        AppUser recipient = appUserRepository.findById(chatMessageDto.getRecipientId())
                .orElseThrow(() -> new IllegalArgumentException("Recipient non trouvé"));

        ChatMessage message = ChatMessage.builder()
                .chatId(chatId)
                .sender(sender)
                .recipient(recipient)
                .content(chatMessageDto.getContent())
                .status(ChatMessageStatus.SENT)
                .build();

        var savedMessage = chatMessageRepository.save(message);

        return ChatMessageDto.builder()
                .id(savedMessage.getId())
                .chatId(savedMessage.getChatId())
                .senderId(savedMessage.getSender().getId())
                .recipientId(savedMessage.getRecipient().getId())
                .content(savedMessage.getContent())
                .timestamp(savedMessage.getTimestamp())
                .status(savedMessage.getStatus())
                .build();
    }

    public List<ChatMessageDto> findChatMessages(UUID senderId, UUID recipientId) {
        var chatId = chatRoomService.getChatRoomId(senderId, recipientId, false);
        return chatId.map(cId -> chatMessageRepository.findByChatIdOrderByTimestampAsc(cId).stream()
                .map(msg -> ChatMessageDto.builder()
                        .id(msg.getId())
                        .chatId(msg.getChatId())
                        .senderId(msg.getSender().getId())
                        .recipientId(msg.getRecipient().getId())
                        .content(msg.getContent())
                        .timestamp(msg.getTimestamp())
                        .status(msg.getStatus())
                        .build())
                .collect(Collectors.toList())).orElse(List.of());
    }
}
