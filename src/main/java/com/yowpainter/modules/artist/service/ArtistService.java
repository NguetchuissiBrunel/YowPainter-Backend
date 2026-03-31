package com.yowpainter.modules.artist.service;

import com.yowpainter.modules.artist.dto.ArtistAnalyticsResponse;
import com.yowpainter.modules.artist.dto.ArtistResponse;
import com.yowpainter.modules.artist.dto.ArtistUpdateRequest;
import com.yowpainter.modules.artist.entity.Artist;
import com.yowpainter.modules.artist.repository.ArtistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ArtistService {

    private final ArtistRepository artistRepository;

    public ArtistResponse getArtistBySlug(String slug) {
        Artist artist = artistRepository.findBySlug(slug)
                .orElseThrow(() -> new IllegalArgumentException("Artiste non trouve avec le slug: " + slug));
        return mapToResponse(artist);
    }

    public ArtistResponse getArtistById(UUID id) {
        Artist artist = artistRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Artiste non trouve avec l'ID: " + id));
        return mapToResponse(artist);
    }

    public List<ArtistResponse> searchArtists(String query) {
        // Simple search logic for MVP
        return artistRepository.findAll().stream()
                .filter(a -> a.getArtistName().toLowerCase().contains(query.toLowerCase()) || 
                             a.getSlug().toLowerCase().contains(query.toLowerCase()))
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public ArtistResponse getArtistByEmail(String email) {
        Artist artist = artistRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Artiste non trouve avec l'email: " + email));
        return mapToResponse(artist);
    }

    @Transactional
    public ArtistResponse updateArtist(String email, ArtistUpdateRequest request) {
        Artist artist = artistRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Artiste non trouve"));

        artist.setFirstName(request.getFirstName());
        artist.setLastName(request.getLastName());
        artist.setArtistName(request.getArtistName());
        artist.setBio(request.getBio());
        artist.setProfilePictureUrl(request.getProfilePictureUrl());
        artist.setBannerUrl(request.getBannerUrl());
        artist.setLocation(request.getLocation());

        return mapToResponse(artistRepository.save(artist));
    }

    public ArtistAnalyticsResponse getArtistAnalytics(String email) {
        // Mock analytics for now
        // In a real app, this would query artwork_like, order, and event tables
        return ArtistAnalyticsResponse.builder()
                .totalArtworks(12)
                .publishedArtworks(8)
                .totalLikes(156)
                .totalSales(5)
                .totalRevenue(1250.0)
                .upcomingEvents(2)
                .build();
    }

    private ArtistResponse mapToResponse(Artist artist) {
        return ArtistResponse.builder()
                .id(artist.getId())
                .firstName(artist.getFirstName())
                .lastName(artist.getLastName())
                .email(artist.getEmail())
                .artistName(artist.getArtistName())
                .slug(artist.getSlug())
                .bio(artist.getBio())
                .profilePictureUrl(artist.getProfilePictureUrl())
                .bannerUrl(artist.getBannerUrl())
                .location(artist.getLocation())
                .status(artist.getStatus())
                .build();
    }
}
