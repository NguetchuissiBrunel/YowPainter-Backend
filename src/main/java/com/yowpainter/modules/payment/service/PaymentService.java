package com.yowpainter.modules.payment.service;

import com.yowpainter.modules.auth.entity.AppUser;
import com.yowpainter.modules.auth.repository.AppUserRepository;
import com.yowpainter.modules.event.service.EventService;
import com.yowpainter.modules.shop.entity.OrderStatus;
import com.yowpainter.modules.shop.entity.Payment;
import com.yowpainter.modules.shop.entity.PaymentStatus;
import com.yowpainter.modules.shop.repository.PaymentRepository;
import com.yowpainter.modules.shop.service.ShopService;
import com.yowpainter.modules.payment.dto.PaymentResponse;
import com.yowpainter.modules.payment.client.CampayClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final AppUserRepository userRepository;
    private final ShopService shopService;
    private final EventService eventService;
    private final CampayClient campayClient;
    private final com.yowpainter.modules.notification.service.NotificationService notificationService;
    private final com.yowpainter.modules.artist.repository.ArtistRepository artistRepository;
    private final com.yowpainter.modules.auth.service.EmailService emailService;

    @Value("${app.frontend-url:http://localhost:3000}")
    private String frontendUrl;

    public List<PaymentResponse> getPaymentHistory(String email) {
        AppUser user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur non trouve"));
        return paymentRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(this::mapToPaymentResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public String initiateMobileMoneyPayment(UUID referenceId, String type, BigDecimal amount, String tenantId, String userEmail, String phoneNumber) {
        try {
            // S'assurer que l'utilisateur existe
            AppUser user = userRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new IllegalArgumentException("Utilisateur non trouve"));

            // Obtenir le token Campay
            String token = campayClient.getToken();

            // Créer la requête de collecte
            CampayClient.CollectRequest collectRequest = CampayClient.CollectRequest.builder()
                    .amount(amount.toString())
                    .from(phoneNumber) // Format attendu : 237xxxxxxxxx
                    .description("Paiement YowPainter - " + (type.equals("ORDER") ? "Commande" : "Billet"))
                    .external_reference(referenceId.toString())
                    .currency("XAF")
                    .build();

            // Lancer la collecte
            CampayClient.CollectResponse collectResponse = campayClient.collect(token, collectRequest);

            // Créer l'enregistrement de paiement local
            Payment payment = Payment.builder()
                    .userId(user.getId())
                    .referenceId(referenceId)
                    .referenceType(type)
                    .amount(amount)
                    .currency("XAF")
                    .status(PaymentStatus.PENDING)
                    .providerReference(collectResponse.getReference())
                    .phoneNumber(phoneNumber)
                    .tenantId(tenantId)
                    .build();
            paymentRepository.save(payment);

            return collectResponse.getReference();
        } catch (Exception e) {
            log.error("Erreur lors de l'initiation du paiement CamPay", e);
            throw new RuntimeException("Erreur de paiement mobile money", e);
        }
    }

    @Transactional
    public void processSuccessfulPayment(String providerReference, String externalReference) {
        UUID referenceId = UUID.fromString(externalReference);
        
        log.info("Processing successful payment for reference: {}", referenceId);

        // Mettre à jour l'enregistrement de paiement local
        Payment payment = paymentRepository.findByReferenceId(referenceId)
                .orElseThrow(() -> new IllegalArgumentException("Paiement non trouve pour la reference: " + referenceId));

        if (payment.getStatus() == PaymentStatus.SUCCEEDED) {
            log.warn("Payment already processed for reference: {}", referenceId);
            return;
        }

        payment.setStatus(PaymentStatus.SUCCEEDED);
        payment.setProviderReference(providerReference);
        paymentRepository.save(payment);

        AppUser user = userRepository.findById(payment.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur non trouve"));

        // Déclencher la logique métier associée dans le bon schéma
        try {
            com.yowpainter.shared.tenant.TenantContext.setTenantId(payment.getTenantId());
            
            String referenceType = payment.getReferenceType();
            UUID userId = payment.getUserId();

            if ("ORDER".equals(referenceType)) {
                shopService.updateOrderStatus(referenceId, OrderStatus.PAID);
                notificationService.createNotification(userId, "Votre commande #" + referenceId.toString().substring(0, 8) + " a été payée avec succès !");
                emailService.sendPaymentConfirmation(user.getEmail(), referenceId.toString().substring(0, 8), payment.getAmount());
                
                // Notifier l'artiste
                var artist = artistRepository.findBySlug(payment.getTenantId());
                artist.ifPresent(a -> emailService.sendNewSaleNotification(a.getEmail(), referenceId.toString().substring(0, 8), payment.getAmount()));
                
            } else if ("RESERVATION".equals(referenceType)) {
                eventService.confirmPaidReservation(referenceId);
                notificationService.createNotification(userId, "Votre réservation a été confirmée !");
                emailService.sendPaymentConfirmation(user.getEmail(), "Réservation #" + referenceId.toString().substring(0, 8), payment.getAmount());
                
                // Notifier l'artiste
                var artist = artistRepository.findBySlug(payment.getTenantId());
                artist.ifPresent(a -> emailService.sendNewSaleNotification(a.getEmail(), "Nouvelle réservation", payment.getAmount()));
            }
        } finally {
            com.yowpainter.shared.tenant.TenantContext.clear();
        }
    }

    @Transactional
    public void processFailedPayment(String providerReference, String externalReference, String status) {
        UUID referenceId = UUID.fromString(externalReference);
        log.warn("Processing failed payment for reference: {} with status: {}", referenceId, status);

        Payment payment = paymentRepository.findByReferenceId(referenceId)
                .orElseThrow(() -> new IllegalArgumentException("Paiement non trouve pour la reference: " + referenceId));

        if (payment.getStatus() == PaymentStatus.SUCCEEDED) {
            log.warn("Attempt to fail an already SUCCEEDED payment: {}", referenceId);
            return;
        }

        payment.setStatus(PaymentStatus.FAILED);
        payment.setProviderReference(providerReference);
        paymentRepository.save(payment);

        // Notifier l'utilisateur
        notificationService.createNotification(payment.getUserId(), "Le paiement pour votre " + 
                (payment.getReferenceType().equals("ORDER") ? "commande" : "réservation") + 
                " a échoué. (" + status + ")");
    }

    private PaymentResponse mapToPaymentResponse(Payment payment) {
        return PaymentResponse.builder()
                .id(payment.getId())
                .referenceId(payment.getReferenceId())
                .referenceType(payment.getReferenceType())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .status(payment.getStatus())
                .createdAt(payment.getCreatedAt())
                .build();
    }
}
