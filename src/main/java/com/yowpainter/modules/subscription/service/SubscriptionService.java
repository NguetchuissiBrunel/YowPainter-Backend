package com.yowpainter.modules.subscription.service;

import com.yowpainter.modules.artist.entity.Artist;
import com.yowpainter.modules.artist.repository.ArtistRepository;
import com.yowpainter.modules.subscription.entity.Subscription;
import com.yowpainter.modules.subscription.entity.SubscriptionPlan;
import com.yowpainter.modules.subscription.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final ArtistRepository artistRepository;

    public Subscription getSubscriptionForArtist(String email) {
        Artist artist = artistRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Artiste non trouvé"));
        return subscriptionRepository.findByArtistId(artist.getId())
                .orElseGet(() -> createDefaultSubscription(artist.getId()));
    }

    @Transactional
    public Subscription createDefaultSubscription(UUID artistId) {
        return subscriptionRepository.save(Subscription.builder()
                .artistId(artistId)
                .plan(SubscriptionPlan.FREE)
                .startDate(LocalDateTime.now())
                .isActive(true)
                .build());
    }

    @Transactional
    public void upgradePlan(String email, SubscriptionPlan plan) {
        Artist artist = artistRepository.findByEmail(email).orElseThrow();
        Subscription sub = subscriptionRepository.findByArtistId(artist.getId())
                .orElseGet(() -> createDefaultSubscription(artist.getId()));
        
        sub.setPlan(plan);
        sub.setStartDate(LocalDateTime.now());
        // Simuler 30 jours
        sub.setEndDate(LocalDateTime.now().plusDays(30));
        subscriptionRepository.save(sub);
    }

    @Transactional
    public void cancelSubscription(String email) {
        Artist artist = artistRepository.findByEmail(email).orElseThrow();
        subscriptionRepository.findByArtistId(artist.getId()).ifPresent(sub -> {
            sub.setActive(false);
            subscriptionRepository.save(sub);
        });
    }
}
