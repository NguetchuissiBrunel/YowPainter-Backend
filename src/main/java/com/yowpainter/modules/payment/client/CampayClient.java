package com.yowpainter.modules.payment.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yowpainter.modules.payment.config.CampayConfig;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

@Slf4j
@Component
public class CampayClient {

    private final CampayConfig config;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public CampayClient(CampayConfig config, ObjectMapper objectMapper) {
        this.config = config;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public String getToken() throws Exception {
        Map<String, String> body = Map.of(
                "username", config.getAppUsername(),
                "password", config.getAppPassword()
        );
        String jsonBody = objectMapper.writeValueAsString(body);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(config.getBaseUrl() + config.getTokenUrl()))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 200) {
            Map<String, Object> map = objectMapper.readValue(response.body(), Map.class);
            return (String) map.get("token");
        } else {
            log.error("Failed to get token from CamPay: {}", response.body());
            throw new RuntimeException("CamPay Token Error");
        }
    }

    public CollectResponse collect(String token, CollectRequest collectRequest) throws Exception {
        String jsonBody = objectMapper.writeValueAsString(collectRequest);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(config.getBaseUrl() + config.getCollectUrl()))
                .header("Content-Type", "application/json")
                .header("Authorization", "Token " + token)
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 200 || response.statusCode() == 201) {
            return objectMapper.readValue(response.body(), CollectResponse.class);
        } else {
            log.error("Failed to collect via CamPay: {}", response.body());
            throw new RuntimeException("CamPay Collect Error");
        }
    }

    public TransactionStatusResponse checkTransactionStatus(String token, String reference) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(config.getBaseUrl() + config.getTransactionUrl() + reference + "/"))
                .header("Authorization", "Token " + token)
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 200) {
            return objectMapper.readValue(response.body(), TransactionStatusResponse.class);
        } else {
            log.error("Failed to check status from CamPay: {}", response.body());
            throw new RuntimeException("CamPay Status Error");
        }
    }

    public WithdrawalResponse withdraw(String token, WithdrawalRequest withdrawalRequest) throws Exception {
        String jsonBody = objectMapper.writeValueAsString(withdrawalRequest);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(config.getBaseUrl() + config.getWithdrawUrl()))
                .header("Content-Type", "application/json")
                .header("Authorization", "Token " + token)
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 200 || response.statusCode() == 201) {
            return objectMapper.readValue(response.body(), WithdrawalResponse.class);
        } else {
            log.error("Failed to withdraw via CamPay: {}", response.body());
            throw new RuntimeException("CamPay Withdrawal Error");
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CollectRequest {
        private String amount;
        private String from;
        private String description;
        private String external_reference;
        private String currency; // XAF
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CollectResponse {
        private String reference;
        private String status;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TransactionStatusResponse {
        private String reference;
        private String status;
        private String amount;
        private String currency;
        private String operator;
        private String external_reference;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WithdrawalRequest {
        private String amount;
        private String to; // Numéro de téléphone Mobile Money
        private String description;
        private String external_reference;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WithdrawalResponse {
        private String reference;
        private String status;
    }
}
