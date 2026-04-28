package com.yowpainter.modules.shop.scheduler;

import com.yowpainter.modules.artist.entity.Artist;
import com.yowpainter.modules.artist.repository.ArtistRepository;
import com.yowpainter.modules.artwork.entity.Artwork;
import com.yowpainter.modules.artwork.repository.ArtworkRepository;
import com.yowpainter.modules.shop.entity.Order;
import com.yowpainter.modules.shop.entity.OrderItem;
import com.yowpainter.modules.shop.entity.OrderStatus;
import com.yowpainter.modules.shop.entity.Product;
import com.yowpainter.modules.shop.repository.OrderRepository;
import com.yowpainter.modules.shop.repository.ProductRepository;
import com.yowpainter.modules.shop.service.ShopService;
import com.yowpainter.shared.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderTimeoutScheduler {

    private final ShopService shopService;
    private final ArtistRepository artistRepository;
    
    @Qualifier("taskExecutor")
    private final Executor taskExecutor;

    // Run every 15 minutes, start after 1 minute
    @Scheduled(fixedRate = 900000, initialDelay = 60000)
    public void cancelAbandonedOrders() {
        log.info("Starting Parallel Multi-Tenant Abandoned Orders Cleanup...");
        
        List<Artist> artists = artistRepository.findAll();
        
        for (Artist artist : artists) {
            String tenantId = artist.getSlug();
            CompletableFuture.runAsync(() -> {
                try {
                    TenantContext.executeInTenant(tenantId, () -> {
                        shopService.cancelAbandonedOrdersForTenant(tenantId);
                        return null;
                    });
                } catch (Exception e) {
                    log.error("Failed to cleanup orders for tenant: {}", tenantId, e);
                }
            }, taskExecutor);
        }
    }
}
