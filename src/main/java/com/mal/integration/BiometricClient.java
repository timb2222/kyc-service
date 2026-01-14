package com.mal.integration;

import com.mal.integration.dto.BiometricRequest;
import com.mal.integration.dto.BiometricResponse;
import com.mal.integration.dto.VerificationStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;

@Component
public class BiometricClient {

    private final WebClient client;
    private final Duration timeout;
    private final SimpleRateLimiter limiter;

    public BiometricClient(
            WebClient.Builder builder,
            SimpleRateLimiter limiter,
            @Value("${kyc.endpoints.biometric}") String url,
            @Value("${kyc.timeout.biometric}") Duration timeout
    ) {
        this.limiter = limiter;
        this.client = builder.baseUrl(url).build();
        this.timeout = timeout;
    }

    @Retryable(
            maxAttemptsExpression = "#{${kyc.retry.attempts}}",
            backoff = @Backoff(delayExpression = "#{${kyc.retry.backoff}.toMillis()}")
    )
    public BiometricResponse match(BiometricRequest request) {

        if (!limiter.tryAcquire()) {
            return new BiometricResponse(VerificationStatus.MANUAL_REVIEW, 0, 0.0);
        }

        return client.post()
                .uri("/api/v1/face-match")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(BiometricResponse.class)
                .timeout(timeout)
                .onErrorReturn(new BiometricResponse(VerificationStatus.FAIL, 0, 0.0))
                .block();
    }

    @Recover
    public BiometricResponse recover(Throwable t, BiometricRequest req) {
        return new BiometricResponse(VerificationStatus.FAIL, 0, 0.0);
    }
}
