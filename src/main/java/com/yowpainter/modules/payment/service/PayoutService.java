package com.yowpainter.modules.payment.service;

import com.yowpainter.modules.artist.entity.Artist;
import com.yowpainter.modules.artist.repository.ArtistRepository;
import com.yowpainter.modules.payment.client.CampayClient;
import com.yowpainter.modules.payment.entity.WalletTransactionType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PayoutService {

    private final CampayClient campayClient;
    private final WalletService walletService;
    private final ArtistRepository artistRepository;

    @Transactional
    public String requestPayout(String artistEmail, BigDecimal amount) {
        Artist artist = artistRepository.findByEmail(artistEmail)
                .orElseThrow(() -> new IllegalArgumentException("Artiste non trouvé"));

        if (artist.getPayoutPhone() == null || artist.getPayoutPhone().isEmpty()) {
            throw new IllegalStateException("Numéro de retrait non configuré pour cet artiste");
        }

        log.info("Initiating payout of {} to {} for artist {}", amount, artist.getPayoutPhone(), artist.getArtistName());

        // 1. Débiter le portefeuille localement d'abord (Sécurité)
        UUID payoutId = UUID.randomUUID();
        walletService.debitWallet(
            artist, 
            amount, 
            WalletTransactionType.WITHDRAWAL, 
            payoutId, 
            "Retrait vers " + artist.getPayoutPhone()
        );

        try {
            // 2. Appeler CamPay Payout
            String token = campayClient.getToken();
            CampayClient.WithdrawalRequest request = CampayClient.WithdrawalRequest.builder()
                    .amount(amount.toString())
                    .to(artist.getPayoutPhone())
                    .description("Retrait YowPainter - " + artist.getArtistName())
                    .external_reference(payoutId.toString())
                    .build();

            CampayClient.WithdrawalResponse response = campayClient.withdraw(token, request);
            log.info("CamPay withdrawal initiated. Reference: {}", response.getReference());
            
            return response.getReference();
        } catch (Exception e) {
            log.error("Erreur lors du retrait CamPay pour l'artiste {}", artist.getArtistName(), e);
            // En cas d'erreur de communication, on pourrait vouloir "roll back" le débit ou marquer comme FAILED
            throw new RuntimeException("Échec du virement Mobile Money. Veuillez contacter le support.", e);
        }
    }
}
