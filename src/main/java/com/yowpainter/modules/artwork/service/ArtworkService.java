package com.yowpainter.modules.artwork.service;

import com.yowpainter.modules.artist.entity.Artist;
import com.yowpainter.modules.artist.repository.ArtistRepository;
import com.yowpainter.modules.artwork.dto.*;
import com.yowpainter.modules.artwork.entity.*;
import com.yowpainter.modules.artwork.repository.ArtworkCommentRepository;
import com.yowpainter.modules.artwork.repository.ArtworkLikeRepository;
import com.yowpainter.modules.artwork.repository.ArtworkRepository;
import com.yowpainter.modules.auth.entity.AppUser;
import com.yowpainter.modules.auth.repository.AppUserRepository;
import com.yowpainter.modules.shop.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ArtworkService {

    private final ArtworkRepository artworkRepository;
    private final ArtistRepository artistRepository;
    private final AppUserRepository userRepository;
    private final ArtworkLikeRepository likeRepository;
    private final ArtworkCommentRepository commentRepository;
    private final ProductRepository productRepository;

    @Transactional
    public ArtworkResponse createArtwork(String artistEmail, ArtworkCreateRequest request) {
        Artist artist = artistRepository.findByEmail(artistEmail)
                .orElseThrow(() -> new IllegalArgumentException("Artiste introuvable"));

        Artwork artwork = Artwork.builder()
                .artistId(artist.getId())
                .title(request.getTitle())
                .description(request.getDescription())
                .technique(request.getTechnique())
                .style(request.getStyle())
                .dimensions(request.getDimensions())
                .tags(request.getTags())
                .status(ArtworkStatus.DRAFT)
                .publishedAt(null)
                .build();

        addImagesToArtwork(artwork, request.getImageUrls());
        return mapToResponse(artworkRepository.save(artwork));
    }

    public List<ArtworkResponse> getPublicArtworks() {
        return artworkRepository.findPublicArtworks().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<ArtworkResponse> getArtworksByArtistId(UUID artistId) {
        return artworkRepository.findByArtistId(artistId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public ArtworkResponse getArtworkById(UUID id) {
        Artwork artwork = artworkRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Oeuvre non trouvee"));
        artwork.setViewCount(artwork.getViewCount() + 1);
        artworkRepository.save(artwork);
        return mapToResponse(artwork);
    }

    public List<ArtworkResponse> searchArtworks(String query) {
        return artworkRepository.searchPublicArtworks(query).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<ArtworkResponse> getFeaturedArtworks() {
        return artworkRepository.findFeaturedArtworks().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void toggleLike(UUID artworkId, String userEmail) {
        Artwork artwork = artworkRepository.findById(artworkId)
                .orElseThrow(() -> new IllegalArgumentException("Oeuvre non trouvee"));
        AppUser user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur non trouve"));

        Optional<ArtworkLike> existingLike = likeRepository.findByArtworkIdAndUserId(artworkId, user.getId());
        if (existingLike.isPresent()) {
            likeRepository.delete(existingLike.get());
            artwork.setLikeCount(Math.max(0, artwork.getLikeCount() - 1));
        } else {
            likeRepository.save(ArtworkLike.builder().artwork(artwork).user(user).build());
            artwork.setLikeCount(artwork.getLikeCount() + 1);
        }
        artworkRepository.save(artwork);
    }

    @Transactional
    public CommentResponse addComment(UUID artworkId, String userEmail, CommentRequest request) {
        Artwork artwork = artworkRepository.findById(artworkId)
                .orElseThrow(() -> new IllegalArgumentException("Oeuvre non trouvee"));
        AppUser user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur non trouve"));

        ArtworkComment comment = ArtworkComment.builder()
                .artwork(artwork)
                .user(user)
                .content(request.getContent())
                .build();

        comment = commentRepository.save(comment);
        return mapToCommentResponse(comment);
    }

    public List<CommentResponse> getComments(UUID artworkId) {
        return commentRepository.findByArtworkIdOrderByCreatedAtDesc(artworkId).stream()
                .map(this::mapToCommentResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public ArtworkResponse updateArtwork(UUID id, String artistEmail, ArtworkCreateRequest request) {
        Artwork artwork = artworkRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Oeuvre non trouvee"));
        
        // Verifier proprietaire
        Artist artist = artistRepository.findByEmail(artistEmail).orElseThrow();
        if (!artwork.getArtistId().equals(artist.getId())) {
            throw new IllegalArgumentException("Acces non autorise");
        }

        artwork.setTitle(request.getTitle());
        artwork.setDescription(request.getDescription());
        artwork.setTechnique(request.getTechnique());
        artwork.setStyle(request.getStyle());
        artwork.setDimensions(request.getDimensions());
        artwork.setTags(request.getTags());

        return mapToResponse(artworkRepository.save(artwork));
    }

    @Transactional
    public void updateStatus(UUID id, String artistEmail, ArtworkStatus status) {
        Artwork artwork = artworkRepository.findById(id).orElseThrow();
        Artist artist = artistRepository.findByEmail(artistEmail).orElseThrow();
        if (!artwork.getArtistId().equals(artist.getId())) throw new IllegalArgumentException("Non autorise");
        
        artwork.setStatus(status);
        if (status == ArtworkStatus.PUBLISHED && artwork.getPublishedAt() == null) {
            artwork.setPublishedAt(LocalDateTime.now());
        }

        // Synchroniser avec le produit boutique si nécessaire
        productRepository.findByArtworkId(id).ifPresent(product -> {
            if (status == ArtworkStatus.SUSPENDED) {
                product.setActive(false);
            } else if (status == ArtworkStatus.ON_SALE) {
                product.setActive(true);
            }
            productRepository.save(product);
        });

        artworkRepository.save(artwork);
    }

    @Transactional
    public void bulkDeleteArtworks(List<UUID> ids, String artistEmail) {
        Artist artist = artistRepository.findByEmail(artistEmail).orElseThrow();
        List<Artwork> artworks = artworkRepository.findAllById(ids);
        for (Artwork artwork : artworks) {
            if (!artwork.getArtistId().equals(artist.getId())) {
                throw new IllegalArgumentException("Acces non autorise pour l'oeuvre: " + artwork.getTitle());
            }
        }
        artworkRepository.deleteAll(artworks);
    }

    @Transactional
    public void bulkUpdateStatus(List<UUID> ids, String artistEmail, ArtworkStatus status) {
        Artist artist = artistRepository.findByEmail(artistEmail).orElseThrow();
        List<Artwork> artworks = artworkRepository.findAllById(ids);
        for (Artwork artwork : artworks) {
            if (!artwork.getArtistId().equals(artist.getId())) {
                throw new IllegalArgumentException("Acces non autorise pour l'oeuvre: " + artwork.getTitle());
            }
            artwork.setStatus(status);
            if (status == ArtworkStatus.PUBLISHED && artwork.getPublishedAt() == null) {
                artwork.setPublishedAt(LocalDateTime.now());
            }

            productRepository.findByArtworkId(artwork.getId()).ifPresent(product -> {
                if (status == ArtworkStatus.SUSPENDED) {
                    product.setActive(false);
                } else if (status == ArtworkStatus.ON_SALE) {
                    product.setActive(true);
                }
                productRepository.save(product);
            });
        }
        artworkRepository.saveAll(artworks);
    }

    public List<ArtworkStyle> getStyles() { return artworkRepository.findDistinctStyles(); }
    public List<ArtworkTechnique> getTechniques() { return artworkRepository.findDistinctTechniques(); }
    public List<String> getSuggestions(String q) { 
        return artworkRepository.findDistinctTags().stream()
                .filter(t -> t.toLowerCase().contains(q.toLowerCase()))
                .collect(Collectors.toList());
    }

    private void addImagesToArtwork(Artwork artwork, List<String> imageUrls) {
        if (imageUrls == null) return;
        artwork.getImages().clear();
        for (int i = 0; i < imageUrls.size(); i++) {
            artwork.addImage(ArtworkImage.builder()
                    .imageUrl(imageUrls.get(i))
                    .isPrimary(i == 0)
                    .sortOrder(i)
                    .build());
        }
    }

    private ArtworkResponse mapToResponse(Artwork artwork) {
        String artistName = artistRepository.findById(artwork.getArtistId())
                .map(Artist::getArtistName)
                .orElse("Artiste Inconnu");
        
        return ArtworkResponse.builder()
                .id(artwork.getId())
                .artistId(artwork.getArtistId())
                .artistName(artistName)
                .title(artwork.getTitle())
                .description(artwork.getDescription())
                .technique(artwork.getTechnique())
                .style(artwork.getStyle())
                .dimensions(artwork.getDimensions())
                .tags(artwork.getTags())
                .status(artwork.getStatus())
                .viewCount(artwork.getViewCount())
                .likeCount(artwork.getLikeCount())
                .imageUrls(artwork.getImages().stream().map(ArtworkImage::getImageUrl).collect(Collectors.toList()))
                .publishedAt(artwork.getPublishedAt())
                .createdAt(artwork.getCreatedAt())
                .build();
    }

    private CommentResponse mapToCommentResponse(ArtworkComment comment) {
        CommentResponse res = new CommentResponse();
        res.setId(comment.getId());
        res.setUserName(comment.getUser().getFirstName() + " " + comment.getUser().getLastName());
        res.setUserAvatar(comment.getUser().getProfilePictureUrl());
        res.setContent(comment.getContent());
        res.setCreatedAt(comment.getCreatedAt());
        return res;
    }
}
