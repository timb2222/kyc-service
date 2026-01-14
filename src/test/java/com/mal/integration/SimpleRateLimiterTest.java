package com.mal.integration;

import com.mal.config.RateLimitProperties;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SimpleRateLimiterTest {

    private SimpleRateLimiter limiter(int limit, long windowMs) {
        var p = new RateLimitProperties();
        p.setLimit(limit);
        p.setWindowMillis(windowMs);
        return new SimpleRateLimiter(p);
    }

    @Test
    void shouldAllowRequests_untilLimitReached() {
        // given
        var limiter = limiter(3, 60_000);

        // when
        var r1 = limiter.tryAcquire();
        var r2 = limiter.tryAcquire();
        var r3 = limiter.tryAcquire();
        var r4 = limiter.tryAcquire(); // over limit

        // then
        assertTrue(r1);
        assertTrue(r2);
        assertTrue(r3);
        assertFalse(r4);
    }

    @Test
    void shouldReset_afterWindowExpires() throws Exception {
        // given
        var limiter = limiter(1, 100);

        // consume 1
        assertTrue(limiter.tryAcquire());

        // when — within window
        assertFalse(limiter.tryAcquire());

        // wait window to expire
        Thread.sleep(120);

        // then
        assertTrue(limiter.tryAcquire());
    }

    @Test
    void shouldStartEmpty_afterInit() {
        // given
        var limiter = limiter(2, 1000);

        // when / then
        assertTrue(limiter.tryAcquire());
        assertTrue(limiter.tryAcquire());
    }

    @Test
    void shouldNotOverflowBeyondLimit_inSameWindow() {
        // given
        var limiter = limiter(2, 1000);

        // when
        var r1 = limiter.tryAcquire();
        var r2 = limiter.tryAcquire();
        var r3 = limiter.tryAcquire();

        // then
        assertTrue(r1);
        assertTrue(r2);
        assertFalse(r3);
    }

    @Test
    void shouldHandleMultipleWindows() throws Exception {
        // given
        var limiter = limiter(1, 100);

        // window 1
        assertTrue(limiter.tryAcquire());
        assertFalse(limiter.tryAcquire());

        // window 2
        Thread.sleep(120);
        assertTrue(limiter.tryAcquire());
        assertFalse(limiter.tryAcquire());

        // window 3
        Thread.sleep(120);
        assertTrue(limiter.tryAcquire());
    }
}
