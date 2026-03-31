package com.yowpainter.modules.search.dto;

import com.yowpainter.modules.artist.dto.ArtistResponse;
import com.yowpainter.modules.artwork.dto.ArtworkResponse;
import com.yowpainter.modules.event.dto.EventResponse;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class GlobalSearchResponse {
    private List<ArtistResponse> artists;
    private List<ArtworkResponse> artworks;
    private List<EventResponse> events;
}
