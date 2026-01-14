package com.mal.integration;

import com.mal.integration.dto.AddressVerificationRequest;
import com.mal.integration.dto.AddressVerificationResponse;
import com.mal.integration.dto.VerificationStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;

@Component
public class AddressVerificationClient {

    private final WebClient client;
    private final Duration timeout;
    private final SimpleRateLimiter limiter;

    public AddressVerificationClient(
            WebClient.Builder builder,
            SimpleRateLimiter limiter,
            @Value("${kyc.endpoints.address}") String url,
            @Value("${kyc.timeout.address}") Duration timeout) {

        this.limiter = limiter;
        this.client = builder.baseUrl(url).build();
        this.timeout = timeout;
    }

    @Retryable(
            maxAttemptsExpression = "#{${kyc.retry.attempts}}",
            backoff = @Backoff(delayExpression = "#{${kyc.retry.backoff}.toMillis()}")
    )
    public AddressVerificationResponse verify(AddressVerificationRequest request) {

        if (!limiter.tryAcquire()) {
            return new AddressVerificationResponse(VerificationStatus.MANUAL_REVIEW, 0, null);
        }

        return client.post()
                .uri("/api/v1/verify-address")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(AddressVerificationResponse.class)
                .timeout(timeout)
                .onErrorReturn(new AddressVerificationResponse(VerificationStatus.FAIL, 0, null))
                .block();
    }

    @Recover
    public AddressVerificationResponse recover(Throwable t, AddressVerificationRequest req) {
        return new AddressVerificationResponse(VerificationStatus.FAIL, 0, null);
    }
}
