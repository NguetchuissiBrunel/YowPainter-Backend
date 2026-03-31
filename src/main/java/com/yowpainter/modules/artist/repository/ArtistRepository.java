package com.yowpainter.modules.artist.repository;

import com.yowpainter.modules.artist.entity.Artist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ArtistRepository extends JpaRepository<Artist, UUID> {
    Optional<Artist> findBySlug(String slug);
    Optional<Artist> findByEmail(String email);
}
