package com.aimedical.modules.commonmodule.auth.rateLimit;

public class InMemoryRateLimitGuard implements RateLimitGuard {

    private static final int DEFAULT_LIMIT = 5;
    private static final long DEFAULT_WINDOW_MS = 10_000L;

    private final SlidingWindowCounter counter;
    private final int defaultLimit;
    private final long defaultWindowMs;

    public InMemoryRateLimitGuard() {
        this(new SlidingWindowCounter(), DEFAULT_LIMIT, DEFAULT_WINDOW_MS);
    }

    public InMemoryRateLimitGuard(SlidingWindowCounter counter) {
        this(counter, DEFAULT_LIMIT, DEFAULT_WINDOW_MS);
    }

    public InMemoryRateLimitGuard(SlidingWindowCounter counter, int defaultLimit, long defaultWindowMs) {
        this.counter = counter;
        this.defaultLimit = defaultLimit;
        this.defaultWindowMs = defaultWindowMs;
    }

    @Override
    public boolean tryAcquire(String key, int limit, long windowMs) {
        return counter.tryAcquire(key, limit, windowMs);
    }

    public boolean tryAcquire(String key) {
        return counter.tryAcquire(key, defaultLimit, defaultWindowMs);
    }
}
