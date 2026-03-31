package com.yowpainter.modules.shop.repository;

import com.yowpainter.modules.shop.entity.Product;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductRepository extends JpaRepository<Product, UUID> {
    List<Product> findByArtistIdAndIsActiveTrue(UUID artistId);
    List<Product> findByArtistId(UUID artistId);
    Optional<Product> findByArtworkId(UUID artworkId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE p.id = :id")
    Optional<Product> findByIdWithPessimisticWriteLock(@Param("id") UUID id);
}
