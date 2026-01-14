package com.mal.integration;

import com.mal.config.RateLimitProperties;
import com.mal.integration.dto.AddressVerificationRequest;
import com.mal.integration.dto.VerificationStatus;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AddressVerificationClientTest {

    private MockWebServer server;

    @BeforeEach
    void setup() throws IOException {
        server = new MockWebServer();
        server.start();
    }

    @AfterEach
    void teardown() throws IOException {
        server.shutdown();
    }

    private SimpleRateLimiter limiter(int limit, long windowMs) {
        var props = new RateLimitProperties();
        props.setLimit(limit);
        props.setWindowMillis(windowMs);
        return new SimpleRateLimiter(props);
    }

    @Test
    void shouldReturnPass_whenResponseIsPass() {
        // Given
        server.enqueue(new MockResponse()
                .setBody("""
                            {"status":"PASS","confidence":95,"reasons":[]}
                        """)
                .addHeader("Content-Type", "application/json"));

        var client = new AddressVerificationClient(
                WebClient.builder(),
                limiter(10, 60_000),
                server.url("/").toString(),
                Duration.ofSeconds(1)
        );

        // When
        var resp = client.verify(new AddressVerificationRequest("c1", "addr", "bill", "2024", "x"));

        // Then
        assertEquals(VerificationStatus.PASS, resp.status());
    }

    @Test
    void shouldReturnFail_whenTimeoutOccurs() {
        // Given
        server.enqueue(new MockResponse()
                .setBody("""
                            {"status":"PASS","confidence":95,"reasons":[]}
                        """)
                .addHeader("Content-Type", "application/json")
                .setBodyDelay(2, TimeUnit.SECONDS));

        var client = new AddressVerificationClient(
                WebClient.builder(),
                limiter(10, 60_000),
                server.url("/").toString(),
                Duration.ofMillis(300)
        );

        // When
        var resp = client.verify(new AddressVerificationRequest("c1", "addr", "bill", "2024", "x"));

        // Then
        assertEquals(VerificationStatus.FAIL, resp.status());
    }

    @Test
    void shouldReturnFail_whenServerReturns5xx() {
        // Given
        server.enqueue(new MockResponse().setResponseCode(500));

        var client = new AddressVerificationClient(
                WebClient.builder(),
                limiter(10, 60_000),
                server.url("/").toString(),
                Duration.ofSeconds(1)
        );

        // When
        var resp = client.verify(new AddressVerificationRequest("c1", "addr", "bill", "2024", "x"));

        // Then
        assertEquals(VerificationStatus.FAIL, resp.status());
    }

    @Test
    void shouldReturnManual_whenRateLimitExceeded() {
        // Given
        var client = new AddressVerificationClient(
                WebClient.builder(),
                limiter(1, 60_000),
                server.url("/").toString(),
                Duration.ofSeconds(1)
        );

        server.enqueue(new MockResponse().setBody("""
                    {"status":"PASS","confidence":90,"reasons":[]}
                """).addHeader("Content-Type", "application/json"));

        client.verify(new AddressVerificationRequest("c1", "addr", "bill", "2024", "x"));

        // When
        var resp = client.verify(new AddressVerificationRequest("c2", "addr", "bill", "2024", "x"));

        // Then
        assertEquals(VerificationStatus.MANUAL_REVIEW, resp.status());
    }

    @Test
    void shouldResetWindow_whenWindowExpires() throws Exception {
        // Given
        var client = new AddressVerificationClient(
                WebClient.builder(),
                limiter(1, 200),
                server.url("/").toString(),
                Duration.ofSeconds(1)
        );

        server.enqueue(new MockResponse().setBody("""
                    {"status":"PASS","confidence":90,"reasons":[]}
                """).addHeader("Content-Type", "application/json"));

        client.verify(new AddressVerificationRequest("c1", "addr", "bill", "2024", "x"));

        // When
        var resp1 = client.verify(new AddressVerificationRequest("c2", "addr", "bill", "2024", "x"));

        Thread.sleep(250);

        server.enqueue(new MockResponse().setBody("""
                    {"status":"PASS","confidence":90,"reasons":[]}
                """).addHeader("Content-Type", "application/json"));

        var resp2 = client.verify(new AddressVerificationRequest("c3", "addr", "bill", "2024", "x"));

        // Then
        assertEquals(VerificationStatus.MANUAL_REVIEW, resp1.status());
        assertEquals(VerificationStatus.PASS, resp2.status());
    }
}
