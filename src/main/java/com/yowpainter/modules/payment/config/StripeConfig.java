package com.yowpainter.modules.payment.config;

import com.stripe.Stripe;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StripeConfig {

    @Value("${spring.stripe.api-key}")
    private String stripeApiKey;

    @PostConstruct
    public void init() {
        // Initialisation globale du SDK Stripe avec la clé secrète
        Stripe.apiKey = stripeApiKey;
    }
}
