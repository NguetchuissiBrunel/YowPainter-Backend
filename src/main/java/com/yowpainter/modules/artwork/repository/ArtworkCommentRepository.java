package com.yowpainter.modules.artwork.repository;

import com.yowpainter.modules.artwork.entity.ArtworkComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ArtworkCommentRepository extends JpaRepository<ArtworkComment, UUID> {
    List<ArtworkComment> findByArtworkIdOrderByCreatedAtDesc(UUID artworkId);
}
