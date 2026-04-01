package com.yowpainter.modules.shop.controller;

import com.yowpainter.modules.shop.dto.OrderCreateRequest;
import com.yowpainter.modules.shop.dto.OrderResponse;
import com.yowpainter.modules.shop.dto.ProductCreateRequest;
import com.yowpainter.modules.shop.dto.ProductResponse;
import com.yowpainter.modules.shop.entity.Order;
import com.yowpainter.modules.shop.entity.OrderStatus;
import com.yowpainter.modules.shop.service.ShopService;
import com.yowpainter.modules.payment.service.PaymentService;
import com.yowpainter.shared.tenant.TenantIdentifierResolver;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/shop")
@RequiredArgsConstructor
@Tag(name = "Shop & Orders", description = "Gestion de la boutique, commandes et inventaire")
public class ShopController {

    private final ShopService shopService;
    private final PaymentService paymentService;
    private final TenantIdentifierResolver tenantResolver;

    @PostMapping("/products")
    @PreAuthorize("hasRole('ARTIST')")
    @Operation(summary = "Mettre un produit / oeuvre en vente (Artiste)")
    public ResponseEntity<ProductResponse> createProduct(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody ProductCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(shopService.createProduct(userDetails.getUsername(), request));
    }

    @GetMapping("/v1/public/{artistSlug}/products")
    @Operation(summary = "Lister le catalogue de ventes d'une boutique (tenant spécifique)")
    public ResponseEntity<List<ProductResponse>> getProductsByArtist(@PathVariable String artistSlug) {
        return ResponseEntity.ok(shopService.getProductsByArtist(null));
    }

    @PostMapping("/v1/public/{artistSlug}/orders")
    @PreAuthorize("hasRole('BUYER')")
    @Operation(summary = "Passer une commande dans une boutique spécifique")
    public ResponseEntity<OrderResponse> placeOrder(
            @PathVariable String artistSlug,
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody OrderCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(shopService.placeOrder(userDetails.getUsername(), request));
    }

    @PostMapping("/orders/{id}/checkout")
    @PreAuthorize("hasRole('BUYER')")
    @Operation(summary = "Initier le paiement Mobile Money (MOMO/Orange) pour une commande")
    public ResponseEntity<Map<String, String>> checkoutOrder(
            @PathVariable UUID id,
            @RequestParam String phoneNumber,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        OrderResponse order = shopService.getOrderById(id);
        String tenantId = tenantResolver.resolveCurrentTenantIdentifier();
        
        String paymentReference = paymentService.initiateMobileMoneyPayment(
                id, 
                "ORDER", 
                order.getTotalAmount(), 
                tenantId, 
                userDetails.getUsername(),
                phoneNumber
        );
        
        return ResponseEntity.ok(Map.of("paymentReference", paymentReference));
    }

    @GetMapping("/orders/{id}")
    @Operation(summary = "Details d'une commande")
    public ResponseEntity<OrderResponse> getOrderById(@PathVariable UUID id) {
        return ResponseEntity.ok(shopService.getOrderById(id));
    }

    @GetMapping("/orders/my-sales")
    @PreAuthorize("hasRole('ARTIST')")
    @Operation(summary = "Lister les commandes RECUES (Artiste)")
    public ResponseEntity<List<OrderResponse>> getMySales(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(shopService.getMySales(userDetails.getUsername()));
    }

    @GetMapping("/orders/my-purchases")
    @PreAuthorize("hasRole('BUYER')")
    @Operation(summary = "Lister mes commandes PASSEES (Acheteur)")
    public ResponseEntity<List<OrderResponse>> getMyPurchases(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(shopService.getMyPurchases(userDetails.getUsername()));
    }

    @PatchMapping("/orders/{id}/status")
    @PreAuthorize("hasRole('ARTIST')")
    @Operation(summary = "Mettre a jour le statut d'une commande (SHIPPED, DELIVERED...)")
    public ResponseEntity<Void> updateOrderStatus(@PathVariable UUID id, @RequestParam OrderStatus status) {
        shopService.updateOrderStatus(id, status);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/inventory")
    @PreAuthorize("hasRole('ARTIST')")
    @Operation(summary = "Consulter son inventaire de produits (Artiste)")
    public ResponseEntity<List<ProductResponse>> getInventory(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(shopService.getInventory(userDetails.getUsername()));
    }
}
