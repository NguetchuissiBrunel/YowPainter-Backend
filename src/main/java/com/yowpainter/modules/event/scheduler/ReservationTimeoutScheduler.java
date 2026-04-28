package com.yowpainter.modules.event.scheduler;

import com.yowpainter.modules.artist.entity.Artist;
import com.yowpainter.modules.artist.repository.ArtistRepository;
import com.yowpainter.modules.event.service.EventService;
import com.yowpainter.shared.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReservationTimeoutScheduler {

    private final EventService eventService;
    private final ArtistRepository artistRepository;

    // Run every 15 minutes, start after 1 minute to allow server to bind port
    @Scheduled(fixedRate = 900000, initialDelay = 60000)
    public void cleanupAbandonedReservations() {
        log.info("Starting Multi-Tenant Event Reservations Cleanup...");
        
        List<Artist> artists = artistRepository.findAll();
        
        for (Artist artist : artists) {
            String tenantId = artist.getSlug();
            try {
                TenantContext.setTenantId(tenantId);
                eventService.cancelAbandonedReservationsForTenant(tenantId);
            } catch (Exception e) {
                log.error("Failed to cleanup reservations for tenant: {}", tenantId, e);
            } finally {
                TenantContext.clear();
            }
        }
        
        log.info("Multi-Tenant Event Reservations Cleanup Finished.");
    }
}
