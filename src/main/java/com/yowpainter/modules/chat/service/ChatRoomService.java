package com.yowpainter.modules.chat.service;

import com.yowpainter.modules.auth.entity.AppUser;
import com.yowpainter.modules.auth.repository.AppUserRepository;
import com.yowpainter.modules.chat.entity.ChatRoom;
import com.yowpainter.modules.chat.repository.ChatRoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChatRoomService {

    private final ChatRoomRepository chatRoomRepository;
    private final AppUserRepository appUserRepository;

    @Transactional
    public Optional<String> getChatRoomId(UUID senderId, UUID recipientId, boolean createNewRoomIfNotExists) {
        return chatRoomRepository
                .findBySenderIdAndRecipientId(senderId, recipientId)
                .map(ChatRoom::getChatId)
                .or(() -> {
                    if (!createNewRoomIfNotExists) {
                        return Optional.empty();
                    }
                    var chatId = createChatId(senderId, recipientId);
                    return Optional.of(chatId);
                });
    }

    private String createChatId(UUID senderId, UUID recipientId) {
        AppUser sender = appUserRepository.findById(senderId)
                .orElseThrow(() -> new IllegalArgumentException("Sender non trouvé"));
        AppUser recipient = appUserRepository.findById(recipientId)
                .orElseThrow(() -> new IllegalArgumentException("Recipient non trouvé"));

        var chatId = String.format("%s_%s", senderId, recipientId);
        
        ChatRoom senderRecipient = ChatRoom.builder()
                .chatId(chatId)
                .sender(sender)
                .recipient(recipient)
                .build();

        ChatRoom recipientSender = ChatRoom.builder()
                .chatId(chatId)
                .sender(recipient)
                .recipient(sender)
                .build();

        chatRoomRepository.save(senderRecipient);
        chatRoomRepository.save(recipientSender);

        return chatId;
    }
}
