package com.yowpainter.modules.shop.repository;

import com.yowpainter.modules.shop.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {
    List<Order> findByBuyerIdOrderByCreatedAtDesc(UUID buyerId);
    
    // Dans le schema du tenant, tous les ordres sont "pour" cet artiste
    List<Order> findAllByOrderByCreatedAtDesc();

    List<Order> findByStatusAndCreatedAtBefore(com.yowpainter.modules.shop.entity.OrderStatus status, java.time.LocalDateTime dateTime);
}
