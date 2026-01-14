package com.mal.integration;

import com.mal.config.RateLimitProperties;
import com.mal.integration.dto.DocumentVerificationRequest;
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

class DocumentVerificationClientTest {

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

    private DocumentVerificationRequest req(String id) {
        return new DocumentVerificationRequest(
                id,
                "PASSPORT",
                "A1234567",
                "2030-01-01",
                "https://cdn/doc.jpg"
        );
    }

    @Test
    void shouldReturnPass_whenResponseIsPass() {
        // given
        server.enqueue(new MockResponse()
                .setBody("""
                            {"status":"PASS","confidence":92,"reasons":[]}
                        """)
                .addHeader("Content-Type", "application/json"));

        var client = new DocumentVerificationClient(
                WebClient.builder(),
                limiter(10, 60_000),
                server.url("/").toString(),
                Duration.ofSeconds(1)
        );

        // when
        var resp = client.verify(req("c1"));

        // then
        assertEquals(VerificationStatus.PASS, resp.status());
    }

    @Test
    void shouldReturnFail_whenTimeoutOccurs() {
        // given
        server.enqueue(new MockResponse()
                .setBody("""
                            {"status":"PASS","confidence":90,"reasons":[]}
                        """)
                .addHeader("Content-Type", "application/json")
                .setBodyDelay(2, TimeUnit.SECONDS));

        var client = new DocumentVerificationClient(
                WebClient.builder(),
                limiter(10, 60_000),
                server.url("/").toString(),
                Duration.ofMillis(300)
        );

        // when
        var resp = client.verify(req("c1"));

        // then
        assertEquals(VerificationStatus.FAIL, resp.status());
    }

    @Test
    void shouldReturnFail_whenServerReturns5xx() {
        // given
        server.enqueue(new MockResponse().setResponseCode(500));

        var client = new DocumentVerificationClient(
                WebClient.builder(),
                limiter(10, 60_000),
                server.url("/").toString(),
                Duration.ofSeconds(1)
        );

        // when
        var resp = client.verify(req("c1"));

        // then
        assertEquals(VerificationStatus.FAIL, resp.status());
    }

    @Test
    void shouldReturnManual_whenRateLimitExceeded() {
        // given
        var client = new DocumentVerificationClient(
                WebClient.builder(),
                limiter(1, 60_000),
                server.url("/").toString(),
                Duration.ofSeconds(1)
        );

        server.enqueue(new MockResponse()
                .setBody("""
                            {"status":"PASS","confidence":90,"reasons":[]}
                        """)
                .addHeader("Content-Type", "application/json"));

        client.verify(req("c1"));

        // when
        var resp = client.verify(req("c2"));

        // then
        assertEquals(VerificationStatus.MANUAL_REVIEW, resp.status());
    }

    @Test
    void shouldResetWindow_whenWindowExpires() throws Exception {
        // given
        var client = new DocumentVerificationClient(
                WebClient.builder(),
                limiter(1, 200),
                server.url("/").toString(),
                Duration.ofSeconds(1)
        );

        server.enqueue(new MockResponse()
                .setBody("""
                            {"status":"PASS","confidence":90,"reasons":[]}
                        """)
                .addHeader("Content-Type", "application/json"));

        client.verify(req("c1"));

        // first blocked
        var resp1 = client.verify(req("c2"));
        assertEquals(VerificationStatus.MANUAL_REVIEW, resp1.status());

        Thread.sleep(250);

        server.enqueue(new MockResponse()
                .setBody("""
                            {"status":"PASS","confidence":90,"reasons":[]}
                        """)
                .addHeader("Content-Type", "application/json"));

        // when
        var resp2 = client.verify(req("c3"));

        // then
        assertEquals(VerificationStatus.PASS, resp2.status());
    }
}
