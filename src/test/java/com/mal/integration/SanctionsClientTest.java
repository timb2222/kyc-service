package com.mal.integration;

import com.mal.config.RateLimitProperties;
import com.mal.integration.dto.SanctionsRequest;
import com.mal.integration.dto.SanctionsStatus;
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

class SanctionsClientTest {

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
        var p = new RateLimitProperties();
        p.setLimit(limit);
        p.setWindowMillis(windowMs);
        return new SimpleRateLimiter(p);
    }

    private SanctionsRequest req(String id) {
        return new SanctionsRequest(id, "John Doe", "10-10-2000", "USA");
    }

    @Test
    void shouldReturnClear_whenServiceRepliesClear() {
        // given
        server.enqueue(new MockResponse()
                .setBody("""
                            {"status":"CLEAR","confidence":99,"reasons":[]}
                        """)
                .addHeader("Content-Type", "application/json"));

        var client = new SanctionsClient(
                WebClient.builder(),
                limiter(10, 60_000),
                server.url("/").toString(),
                Duration.ofSeconds(1)
        );

        // when
        var resp = client.check(req("c1"));

        // then
        assertEquals(SanctionsStatus.CLEAR, resp.status());
    }

    @Test
    void shouldReturnHit_whenTimeoutOccurs() {
        // given
        server.enqueue(new MockResponse()
                .setBody("""
                            {"status":"CLEAR","confidence":80,"reasons":[]}
                        """)
                .addHeader("Content-Type", "application/json")
                .setBodyDelay(2, TimeUnit.SECONDS));

        var client = new SanctionsClient(
                WebClient.builder(),
                limiter(10, 60_000),
                server.url("/").toString(),
                Duration.ofMillis(300)
        );

        // when
        var resp = client.check(req("c1"));

        // then
        assertEquals(SanctionsStatus.HIT, resp.status());
    }

    @Test
    void shouldReturnHit_whenServerReturns5xx() {
        // given
        server.enqueue(new MockResponse().setResponseCode(500));

        var client = new SanctionsClient(
                WebClient.builder(),
                limiter(10, 60_000),
                server.url("/").toString(),
                Duration.ofSeconds(1)
        );

        // when
        var resp = client.check(req("c1"));

        // then
        assertEquals(SanctionsStatus.HIT, resp.status());
    }

    @Test
    void shouldReturnHit_whenRateLimitExceeded() {
        // given
        var client = new SanctionsClient(
                WebClient.builder(),
                limiter(1, 60_000),
                server.url("/").toString(),
                Duration.ofSeconds(1)
        );

        server.enqueue(new MockResponse().setBody("""
                    {"status":"CLEAR","confidence":95,"reasons":[]}
                """).addHeader("Content-Type", "application/json"));

        client.check(req("c1"));

        // when
        var resp = client.check(req("c2"));

        // then
        assertEquals(SanctionsStatus.HIT, resp.status());
    }

    @Test
    void shouldResetWindow_whenWindowExpires() throws Exception {
        // given
        var client = new SanctionsClient(
                WebClient.builder(),
                limiter(1, 200),
                server.url("/").toString(),
                Duration.ofSeconds(1)
        );

        server.enqueue(new MockResponse().setBody("""
                    {"status":"CLEAR","confidence":95,"reasons":[]}
                """).addHeader("Content-Type", "application/json"));

        client.check(req("c1"));

        var resp1 = client.check(req("c2"));
        assertEquals(SanctionsStatus.HIT, resp1.status());

        Thread.sleep(250);

        server.enqueue(new MockResponse().setBody("""
                    {"status":"CLEAR","confidence":95,"reasons":[]}
                """).addHeader("Content-Type", "application/json"));

        var resp2 = client.check(req("c3"));

        // then
        assertEquals(SanctionsStatus.CLEAR, resp2.status());
    }
}
