package com.mal.integration;

import com.mal.config.RateLimitProperties;
import org.springframework.stereotype.Component;

@Component
public class SimpleRateLimiter {

    // todo migrate to redis
    private final int limit;
    private final long windowMillis;

    private int used = 0;
    private long windowStart = System.currentTimeMillis();

    public SimpleRateLimiter(RateLimitProperties props) {
        this.limit = props.getLimit();
        this.windowMillis = props.getWindowMillis();
    }

    public synchronized boolean tryAcquire() {
        long now = System.currentTimeMillis();

        if (now - windowStart > windowMillis) {
            windowStart = now;
            used = 0;
        }

        if (used < limit) {
            used++;
            return true;
        }
        return false;
    }
}
