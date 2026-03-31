package com.yowpainter.modules.payment.controller;

import com.stripe.model.checkout.Session;
import com.yowpainter.modules.payment.dto.PaymentResponse;
import com.yowpainter.modules.payment.service.PaymentService;
import com.yowpainter.shared.tenant.TenantContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
@Tag(name = "Payments", description = "Integration Stripe et historique des transactions")
public class PaymentController {

    private final PaymentService paymentService;

    @Value("${spring.stripe.webhook-secret}")
    private String endpointSecret;

    @PostMapping("/webhook")
    @Operation(summary = "Point d'entree pour les webhooks Stripe")
    public ResponseEntity<String> handleStripeWebhook(@RequestBody String payload, @RequestHeader("Stripe-Signature") String sigHeader) {
        com.stripe.model.Event event;

        try {
            event = com.stripe.net.Webhook.constructEvent(payload, sigHeader, endpointSecret);
        } catch (com.stripe.exception.SignatureVerificationException e) {
            log.warn("Webhook signature verification failed.");
            return ResponseEntity.status(org.springframework.http.HttpStatus.BAD_REQUEST).body("Signature invalide");
        }

        log.info("Received Stripe Webhook: {}", event.getType());

        if ("checkout.session.completed".equals(event.getType())) {
            Session session = (Session) event.getDataObjectDeserializer().getObject().orElse(null);
            if (session != null) {
                String tenantId = session.getMetadata().get("tenantId");
                log.info("Payment success for tenant: {}", tenantId);
                
                try {
                    // Switch context to the correct tenant schema
                    TenantContext.setTenantId(tenantId);
                    paymentService.processSuccessfulPayment(session.getMetadata(), session.getPaymentIntent());
                } finally {
                    TenantContext.clear();
                }
            }
        }

        return ResponseEntity.ok("Received");
    }

    @GetMapping("/history")
    @Operation(summary = "Consulter son historique de paiements")
    public ResponseEntity<List<PaymentResponse>> getPaymentHistory(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(paymentService.getPaymentHistory(userDetails.getUsername()));
    }
}
