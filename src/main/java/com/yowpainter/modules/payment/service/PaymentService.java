package com.yowpainter.modules.payment.service;

import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import com.yowpainter.modules.auth.entity.AppUser;
import com.yowpainter.modules.auth.repository.AppUserRepository;
import com.yowpainter.modules.event.service.EventService;
import com.yowpainter.modules.shop.entity.OrderStatus;
import com.yowpainter.modules.shop.entity.Payment;
import com.yowpainter.modules.shop.entity.PaymentStatus;
import com.yowpainter.modules.shop.repository.PaymentRepository;
import com.yowpainter.modules.shop.service.ShopService;
import com.yowpainter.modules.payment.dto.PaymentResponse;
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

    @Value("${app.frontend-url:http://localhost:3000}")
    private String frontendUrl;

    public List<PaymentResponse> getPaymentHistory(String email) {
        AppUser user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur non trouve"));
        return paymentRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(this::mapToPaymentResponse)
                .collect(Collectors.toList());
    }

    public String createCheckoutSession(UUID referenceId, String type, BigDecimal amount, String tenantId, String userEmail) {
        try {
            SessionCreateParams params = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.PAYMENT)
                    .setSuccessUrl(frontendUrl + "/payment/success?session_id={CHECKOUT_SESSION_ID}")
                    .setCancelUrl(frontendUrl + "/payment/cancel")
                    .setCustomerEmail(userEmail)
                    .addLineItem(
                            SessionCreateParams.LineItem.builder()
                                    .setQuantity(1L)
                                    .setPriceData(
                                            SessionCreateParams.LineItem.PriceData.builder()
                                                    .setCurrency("eur")
                                                    .setUnitAmount(amount.multiply(new BigDecimal(100)).longValue())
                                                    .setProductData(
                                                            SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                    .setName(type.equals("ORDER") ? "Commande Boutique YowPainter" : "Billet Événement YowPainter")
                                                                    .build()
                                                    )
                                                    .build()
                                    )
                                    .build()
                    )
                    .putMetadata("tenantId", tenantId)
                    .putMetadata("referenceId", referenceId.toString())
                    .putMetadata("type", type)
                    .putMetadata("userEmail", userEmail)
                    .build();

            Session session = Session.create(params);
            return session.getUrl();
        } catch (com.stripe.exception.StripeException e) {
            log.error("Erreur lors de la creation de la session Stripe", e);
            throw new RuntimeException("Erreur de paiement", e);
        }
    }

    private final com.yowpainter.modules.notification.service.NotificationService notificationService;
    private final com.yowpainter.modules.artist.repository.ArtistRepository artistRepository;

    @Transactional
    public void processSuccessfulPayment(Map<String, String> metadata, String stripePaymentIntentId) {
        UUID referenceId = UUID.fromString(metadata.get("referenceId"));
        String type = metadata.get("type");
        String tenantSlug = metadata.get("tenantId");

        log.info("Processing successful payment for {} : {}", type, referenceId);

        // Résoudre l'userId depuis le email stocké dans les metadata (sécurité : évite le null)
        String userEmail = metadata.get("userEmail");
        UUID userId = userRepository.findByEmail(userEmail)
                .map(u -> u.getId())
                .orElse(null);

        // Mettre à jour ou créer l'enregistrement de paiement local
        Payment payment = paymentRepository.findByReferenceId(referenceId)
                .orElse(Payment.builder()
                        .referenceId(referenceId)
                        .referenceType(type)
                        .userId(userId)
                        .status(PaymentStatus.PENDING)
                        .build());

        payment.setStatus(PaymentStatus.SUCCEEDED);
        payment.setStripePaymentIntentId(stripePaymentIntentId);
        if (payment.getUserId() == null && userId != null) {
            payment.setUserId(userId);
        }
        payment = paymentRepository.save(payment);

        // Déclencher la logique métier associée
        if ("ORDER".equals(type)) {
            shopService.updateOrderStatus(referenceId, OrderStatus.PAID);
            notificationService.createNotification(userId, "Votre commande #" + referenceId.toString().substring(0, 8) + " a été payée avec succès !");
        } else if ("RESERVATION".equals(type)) {
            eventService.confirmPaidReservation(referenceId);
            notificationService.createNotification(userId, "Votre réservation pour l'événement a été confirmée !");
        }

        // Notifier l'artiste (propriétaire du tenant)
        artistRepository.findBySlug(tenantSlug).ifPresent(artist -> {
            notificationService.createNotification(artist.getId(), "Nouvelle vente réalisée sur votre boutique (" + type + ") !");
        });
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
