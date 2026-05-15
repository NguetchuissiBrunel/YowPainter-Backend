package com.yowpainter.modules.chat.service;

import com.yowpainter.modules.auth.entity.AppUser;
import com.yowpainter.modules.auth.repository.AppUserRepository;
import com.yowpainter.modules.chat.dto.ChatMessageDto;
import com.yowpainter.modules.chat.entity.ChatMessage;
import com.yowpainter.modules.chat.entity.ChatMessageStatus;
import com.yowpainter.modules.chat.repository.ChatMessageRepository;
import com.yowpainter.modules.chat.repository.ChatRoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatMessageService {

    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomService chatRoomService;
    private final ChatRoomRepository chatRoomRepository;
    private final AppUserRepository appUserRepository;

    public List<com.yowpainter.modules.chat.dto.UserChatDto> searchUsers(String query) {
        return appUserRepository.findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCaseOrEmailContainingIgnoreCase(query, query, query)
                .stream()
                .map(this::mapToUserChatDto)
                .collect(Collectors.toList());
    }

    public List<com.yowpainter.modules.chat.dto.UserChatDto> getRecentContacts(UUID userId) {
        return chatRoomRepository.findBySenderIdOrRecipientId(userId, userId).stream()
                .map(room -> {
                    UUID contactId = room.getSender().getId().equals(userId) ? room.getRecipient().getId() : room.getSender().getId();
                    AppUser contact = appUserRepository.findById(contactId).orElse(null);
                    if (contact == null) return null;

                    var dto = mapToUserChatDto(contact);
                    dto.setUnreadCount((int) chatMessageRepository.countByRecipientIdAndSenderIdAndStatus(userId, contactId, com.yowpainter.modules.chat.entity.ChatMessageStatus.SENT));
                    return dto;
                })
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toList());
    }

    public List<com.yowpainter.modules.chat.dto.UserChatDto> getSuggestedContacts(UUID userId) {
        // Retourne quelques artistes comme suggestions
        return appUserRepository.findAll().stream()
                .filter(u -> u instanceof com.yowpainter.modules.artist.entity.Artist && !u.getId().equals(userId))
                .limit(5)
                .map(this::mapToUserChatDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public void markMessagesAsRead(UUID recipientId, UUID senderId) {
        chatMessageRepository.markAsRead(recipientId, senderId);
    }

    private com.yowpainter.modules.chat.dto.UserChatDto mapToUserChatDto(AppUser user) {
        String name = user.getFirstName() + " " + user.getLastName();
        if (user instanceof com.yowpainter.modules.artist.entity.Artist artist) {
            name = artist.getArtistName();
        }
        return com.yowpainter.modules.chat.dto.UserChatDto.builder()
                .id(user.getId())
                .name(name)
                .profilePictureUrl(user.getProfilePictureUrl())
                .role(user.getRole().name())
                .build();
    }

    public String getRecipientEmail(UUID userId) {
        return appUserRepository.findById(userId)
                .map(AppUser::getEmail)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur non trouvé"));
    }

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
