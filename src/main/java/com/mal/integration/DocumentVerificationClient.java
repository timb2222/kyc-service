package com.mal.integration;

import com.mal.integration.dto.DocumentVerificationRequest;
import com.mal.integration.dto.DocumentVerificationResponse;
import com.mal.integration.dto.VerificationStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;

@Component
public class DocumentVerificationClient {

    private final WebClient client;
    private final Duration timeout;
    private final SimpleRateLimiter limiter;

    public DocumentVerificationClient(
            WebClient.Builder builder,
            SimpleRateLimiter limiter,
            @Value("${kyc.endpoints.document}") String url,
            @Value("${kyc.timeout.document}") Duration timeout
    ) {
        this.limiter = limiter;
        this.client = builder.baseUrl(url).build();
        this.timeout = timeout;
    }

    @Retryable(
            maxAttemptsExpression = "#{${kyc.retry.attempts}}",
            backoff = @Backoff(delayExpression = "#{${kyc.retry.backoff}.toMillis()}")
    )
    public DocumentVerificationResponse verify(DocumentVerificationRequest request) {

        if (!limiter.tryAcquire()) {
            return new DocumentVerificationResponse(VerificationStatus.MANUAL_REVIEW, 0, null);
        }

        return client.post()
                .uri("/api/v1/verify-document")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(DocumentVerificationResponse.class)
                .timeout(timeout)
                .onErrorReturn(new DocumentVerificationResponse(VerificationStatus.FAIL, 0, null))
                .block();
    }

    @Recover
    public DocumentVerificationResponse recover(Throwable t, DocumentVerificationRequest req) {
        return new DocumentVerificationResponse(VerificationStatus.FAIL, 0, null);
    }
}
