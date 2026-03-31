package com.yowpainter.modules.event.dto;

import com.yowpainter.modules.event.entity.ReservationStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class ReservationResponse {
    private UUID id;
    private UUID eventId;
    private String eventName;
    private UUID userId;
    private String userName;
    private String userEmail;
    private ReservationStatus status;
    private LocalDateTime createdAt;
}
