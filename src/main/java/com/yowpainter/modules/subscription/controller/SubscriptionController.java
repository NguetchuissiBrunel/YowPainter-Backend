package com.yowpainter.modules.subscription.controller;

import com.yowpainter.modules.subscription.entity.Subscription;
import com.yowpainter.modules.subscription.entity.SubscriptionPlan;
import com.yowpainter.modules.subscription.service.SubscriptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/subscription")
@RequiredArgsConstructor
@Tag(name = "Subscriptions", description = "Gestion des forfaits SaaS pour les artistes")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    @GetMapping("/plans")
    @Operation(summary = "Lister les forfaits disponibles")
    public ResponseEntity<List<Map<String, Object>>> getPlans() {
        return ResponseEntity.ok(Arrays.stream(SubscriptionPlan.values())
                .map(p -> Map.of(
                        "name", p.name(),
                        "price", p == SubscriptionPlan.FREE ? 0 : (p == SubscriptionPlan.PRO ? 19 : 49),
                        "features", p == SubscriptionPlan.FREE ? List.of("5 artworks") : List.of("Unlimited artworks")
                )).collect(Collectors.toList()));
    }

    @GetMapping("/my-plan")
    @PreAuthorize("hasRole('ARTIST')")
    @Operation(summary = "Consulter mon forfait actuel")
    public ResponseEntity<Subscription> getMyPlan(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(subscriptionService.getSubscriptionForArtist(userDetails.getUsername()));
    }

    @PostMapping("/upgrade")
    @PreAuthorize("hasRole('ARTIST')")
    @Operation(summary = "Passer a un forfait superieur")
    public ResponseEntity<Void> upgradePlan(@AuthenticationPrincipal UserDetails userDetails, @RequestParam SubscriptionPlan planName) {
        subscriptionService.upgradePlan(userDetails.getUsername(), planName);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/cancel")
    @PreAuthorize("hasRole('ARTIST')")
    @Operation(summary = "Resilier son abonnement")
    public ResponseEntity<Void> cancelSubscription(@AuthenticationPrincipal UserDetails userDetails) {
        subscriptionService.cancelSubscription(userDetails.getUsername());
        return ResponseEntity.ok().build();
    }
}
