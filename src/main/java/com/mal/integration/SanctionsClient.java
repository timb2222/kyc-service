package com.mal.integration;

import com.mal.integration.dto.SanctionsRequest;
import com.mal.integration.dto.SanctionsResponse;
import com.mal.integration.dto.SanctionsStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;

@Component
public class SanctionsClient {

    private final WebClient client;
    private final Duration timeout;
    private final SimpleRateLimiter limiter;

    public SanctionsClient(
            WebClient.Builder builder,
            SimpleRateLimiter limiter,
            @Value("${kyc.endpoints.sanctions}") String url,
            @Value("${kyc.timeout.sanctions}") Duration timeout) {

        this.limiter = limiter;
        this.client = builder.baseUrl(url).build();
        this.timeout = timeout;
    }

    @Retryable(
            maxAttemptsExpression = "#{${kyc.retry.attempts}}",
            backoff = @Backoff(delayExpression = "#{${kyc.retry.backoff}.toMillis()}")
    )
    public SanctionsResponse check(SanctionsRequest request) {

        if (!limiter.tryAcquire()) {
            return new SanctionsResponse(SanctionsStatus.HIT, 0, null);
        }

        return client.post()
                .uri("/api/v1/check-sanctions")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(SanctionsResponse.class)
                .timeout(timeout)
                .onErrorReturn(new SanctionsResponse(SanctionsStatus.HIT, 0, null))
                .block();
    }

    @Recover
    public SanctionsResponse recover(Throwable t, SanctionsRequest req) {
        return new SanctionsResponse(SanctionsStatus.HIT, 0, null);
    }
}
