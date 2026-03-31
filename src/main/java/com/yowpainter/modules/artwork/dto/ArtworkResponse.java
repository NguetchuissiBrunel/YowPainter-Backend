package com.yowpainter.modules.artwork.dto;

import com.yowpainter.modules.artwork.entity.ArtworkStatus;
import com.yowpainter.modules.artwork.entity.ArtworkStyle;
import com.yowpainter.modules.artwork.entity.ArtworkTechnique;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class ArtworkResponse {
    private UUID id;
    private UUID artistId;
    private String artistName;
    private String title;
    private String description;
    private ArtworkTechnique technique;
    private ArtworkStyle style;
    private String dimensions;
    private List<String> tags;
    private ArtworkStatus status;
    private int viewCount;
    private int likeCount;
    private List<String> imageUrls;
    private LocalDateTime publishedAt;
    private LocalDateTime createdAt;
}
