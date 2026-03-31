package com.yowpainter.modules.shop.repository;

import com.yowpainter.modules.shop.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    List<Payment> findByUserIdOrderByCreatedAtDesc(UUID userId);
    
    java.util.Optional<Payment> findByReferenceId(UUID referenceId);
}
