package com.yowpainter.modules.artwork.repository;

import com.yowpainter.modules.artwork.entity.ArtworkLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ArtworkLikeRepository extends JpaRepository<ArtworkLike, UUID> {
    Optional<ArtworkLike> findByArtworkIdAndUserId(UUID artworkId, UUID userId);
    long countByArtworkId(UUID artworkId);
}
