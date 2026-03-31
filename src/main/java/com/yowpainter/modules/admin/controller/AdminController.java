package com.yowpainter.modules.admin.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Administration", description = "Controles globaux de la plateforme (Restreint aux Admins)")
public class AdminController {

    @GetMapping("/tenants")
    @Operation(summary = "Lister tous les artistes / tenants enregistres")
    public ResponseEntity<List<Map<String, Object>>> getAllTenants() {
        return ResponseEntity.ok(List.of(
            Map.of("id", UUID.randomUUID(), "name", "Artiste 1", "slug", "artiste1", "status", "ACTIVE"),
            Map.of("id", UUID.randomUUID(), "name", "Artiste 2", "slug", "artiste2", "status", "SUSPENDED")
        ));
    }

    @PatchMapping("/tenants/{id}/status")
    @Operation(summary = "Activer ou suspendre un tenant")
    public ResponseEntity<Void> updateTenantStatus(@PathVariable UUID id, @RequestParam String status) {
        return ResponseEntity.ok().build();
    }

    @GetMapping("/users")
    @Operation(summary = "Lister tous les utilisateurs de la plateforme")
    public ResponseEntity<List<Map<String, Object>>> getAllUsers() {
        return ResponseEntity.ok(List.of(
            Map.of("email", "admin@yowpainter.com", "role", "ROLE_ADMIN"),
            Map.of("email", "buyer@gmail.com", "role", "ROLE_BUYER")
        ));
    }

    @DeleteMapping("/users/{id}")
    @Operation(summary = "Supprimer definitivement un utilisateur")
    public ResponseEntity<Void> deleteUser(@PathVariable UUID id) {
        return ResponseEntity.ok().build();
    }

    @GetMapping("/stats")
    @Operation(summary = "Statistiques globales de la plateforme")
    public ResponseEntity<Map<String, Object>> getGlobalStats() {
        return ResponseEntity.ok(Map.of(
            "total_tenants", 150,
            "total_users", 3500,
            "total_sales_volume", 45000.0,
            "active_subscriptions", 120
        ));
    }

    @GetMapping("/logs")
    @Operation(summary = "Consulter les logs d'audit (Mock)")
    public ResponseEntity<List<String>> getAuditLogs() {
        return ResponseEntity.ok(List.of("User X logged in", "Tenant Y created artwork Z"));
    }

    @GetMapping("/me")
    @Operation(summary = "Récupérer le profil de l'administrateur connecté")
    public ResponseEntity<Map<String, String>> getMe(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(Map.of(
            "email", userDetails.getUsername(),
            "role", "ROLE_ADMIN"
        ));
    }
}
