package com.yowpainter.modules.payment.controller;

import com.yowpainter.modules.payment.dto.PaymentResponse;
import com.yowpainter.modules.payment.service.PaymentService;
import com.yowpainter.shared.tenant.TenantContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
@Tag(name = "Payments", description = "Intégration Mobile Money (MTN, Orange) via CamPay")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/callback")
    @Operation(summary = "Point d'entrée pour les callbacks CamPay")
    public ResponseEntity<String> handleCampayCallback(@RequestBody Map<String, String> payload) {
        log.info("Received CamPay Callback: {}", payload);

        String status = payload.get("status");
        String reference = payload.get("reference");
        String externalReference = payload.get("external_reference");

        if ("SUCCESSFUL".equals(status)) {
            // Dans un système multi-tenant, on peut avoir besoin du tenantId. 
            // On peut le passer dans external_reference (ex: "tenantId:referenceId") 
            // ou le stocker préalablement. Pour cet exemple, on suppose que le service 
            // gère la résolution ou que le tenant est global.
            
            // Note: Pour YowPainter, on doit s'assurer que le TenantContext est correct si on utilise des schémas séparés.
            // Si le externalReference contient le tenantId, on peut l'extraire.
            
            paymentService.processSuccessfulPayment(reference, externalReference);
        }

        return ResponseEntity.ok("Received");
    }

    @GetMapping("/history")
    @Operation(summary = "Consulter son historique de paiements")
    public ResponseEntity<List<PaymentResponse>> getPaymentHistory(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(paymentService.getPaymentHistory(userDetails.getUsername()));
    }
}

