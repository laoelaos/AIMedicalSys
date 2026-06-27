package com.aimedical.modules.commonmodule.auth.rateLimit;

public interface RateLimitGuard {

    boolean tryAcquire(String key, int limit, long windowMs);
}
