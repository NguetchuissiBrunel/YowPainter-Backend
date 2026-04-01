package com.yowpainter.modules.event.repository;

import com.yowpainter.modules.event.entity.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, UUID> {
    List<Reservation> findByEventId(UUID eventId);
    List<Reservation> findByUserId(UUID userId);
    
    List<Reservation> findByStatusAndReservedAtBefore(com.yowpainter.modules.event.entity.ReservationStatus status, java.time.LocalDateTime threshold);
}
