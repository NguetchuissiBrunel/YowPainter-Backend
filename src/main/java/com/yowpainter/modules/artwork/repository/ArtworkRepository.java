package com.yowpainter.modules.artwork.repository;

import com.yowpainter.modules.artwork.entity.Artwork;
import com.yowpainter.modules.artwork.entity.ArtworkStatus;
import com.yowpainter.modules.artwork.entity.ArtworkStyle;
import com.yowpainter.modules.artwork.entity.ArtworkTechnique;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ArtworkRepository extends JpaRepository<Artwork, UUID> {
    List<Artwork> findByArtistId(UUID artistId);
    List<Artwork> findByStatus(ArtworkStatus status);
    
    @Query("SELECT a FROM Artwork a WHERE a.status = 'PUBLISHED' OR a.status = 'ON_SALE' ORDER BY a.publishedAt DESC")
    List<Artwork> findPublicArtworks();

    @Query("SELECT a FROM Artwork a WHERE (LOWER(a.title) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(a.description) LIKE LOWER(CONCAT('%', :q, '%'))) AND (a.status = 'PUBLISHED' OR a.status = 'ON_SALE')")
    List<Artwork> searchPublicArtworks(@Param("q") String q);

    @Query("SELECT a FROM Artwork a WHERE a.status = 'ON_SALE' ORDER BY a.likeCount DESC")
    List<Artwork> findFeaturedArtworks();

    @Query("SELECT DISTINCT a.style FROM Artwork a WHERE a.style IS NOT NULL")
    List<ArtworkStyle> findDistinctStyles();

    @Query("SELECT DISTINCT a.technique FROM Artwork a WHERE a.technique IS NOT NULL")
    List<ArtworkTechnique> findDistinctTechniques();

    @Query(value = "SELECT DISTINCT jsonb_array_elements_text(tags) FROM artwork WHERE tags IS NOT NULL", nativeQuery = true)
    List<String> findDistinctTags();
}
