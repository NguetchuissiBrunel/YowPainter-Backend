package com.yowpainter.modules.event.dto;

import com.yowpainter.modules.event.entity.EventStatus;
import com.yowpainter.modules.event.entity.EventType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class EventResponse {
    private UUID id;
    private UUID artistId;
    private String name;
    private String description;
    private String posterUrl;
    private LocalDateTime startDateTime;
    private LocalDateTime endDateTime;
    private String location;
    private EventType type;
    private int maxCapacity;
    private int reservedCount;
    private BigDecimal ticketPrice;
    private EventStatus status;
}
