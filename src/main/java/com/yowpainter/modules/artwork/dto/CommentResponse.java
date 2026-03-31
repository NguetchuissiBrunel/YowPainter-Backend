package com.yowpainter.modules.artwork.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class CommentResponse {
    private UUID id;
    private String userName;
    private String userAvatar;
    private String content;
    private LocalDateTime createdAt;
}
