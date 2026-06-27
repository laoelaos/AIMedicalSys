package com.aimedical.modules.commonmodule.auth.rateLimit;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemoryRateLimitGuardTest {

    @Test
    void shouldThrowNpeWhenKeyIsNull() {
        InMemoryRateLimitGuard guard = new InMemoryRateLimitGuard();
        assertThrows(NullPointerException.class,
                () -> guard.tryAcquire(null));
    }

    @Test
    void shouldMaintainIndependentKeys() {
        InMemoryRateLimitGuard guard = new InMemoryRateLimitGuard(
                new SlidingWindowCounter(), 2, 60_000);
        assertTrue(guard.tryAcquire("ip-a"));
        assertTrue(guard.tryAcquire("ip-a"));
        assertFalse(guard.tryAcquire("ip-a"));
        assertTrue(guard.tryAcquire("ip-b"));
    }

    @Test
    void shouldAllowUpToLimit() {
        InMemoryRateLimitGuard guard = new InMemoryRateLimitGuard();
        for (int i = 0; i < 5; i++) {
            assertTrue(guard.tryAcquire("test-ip"));
        }
    }

    @Test
    void shouldRejectWhenExceedLimit() {
        InMemoryRateLimitGuard guard = new InMemoryRateLimitGuard();
        for (int i = 0; i < 5; i++) {
            guard.tryAcquire("test-ip");
        }
        assertFalse(guard.tryAcquire("test-ip"));
    }

    @Test
    void shouldAllowAfterWindowExpiry() throws InterruptedException {
        InMemoryRateLimitGuard guard = new InMemoryRateLimitGuard();
        for (int i = 0; i < 5; i++) {
            guard.tryAcquire("test-ip");
        }
        assertFalse(guard.tryAcquire("test-ip"));
        Thread.sleep(10_100);
        assertTrue(guard.tryAcquire("test-ip"));
    }

    @Test
    void shouldHandleConcurrentRequests() throws InterruptedException {
        int threadCount = 10;
        int limit = 5;
        InMemoryRateLimitGuard guard = new InMemoryRateLimitGuard(
                new SlidingWindowCounter(), limit, 10_000);
        AtomicInteger allowed = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(threadCount);
        CyclicBarrier barrier = new CyclicBarrier(threadCount);

        for (int i = 0; i < threadCount; i++) {
            new Thread(() -> {
                try {
                    barrier.await();
                } catch (InterruptedException | BrokenBarrierException e) {
                    Thread.currentThread().interrupt();
                }
                if (guard.tryAcquire("test-ip")) {
                    allowed.incrementAndGet();
                }
                latch.countDown();
            }).start();
        }
        latch.await();
        assertTrue(allowed.get() <= limit);
    }

    @Test
    void shouldUseDefaultLimitAndWindow() {
        InMemoryRateLimitGuard guard = new InMemoryRateLimitGuard();
        for (int i = 0; i < 5; i++) {
            assertTrue(guard.tryAcquire("test-ip"));
        }
        assertFalse(guard.tryAcquire("test-ip"));
    }

    @Test
    void shouldRespectCustomLimitAndWindow() {
        InMemoryRateLimitGuard guard = new InMemoryRateLimitGuard(
                new SlidingWindowCounter(), 2, 60_000);
        assertTrue(guard.tryAcquire("test-ip"));
        assertTrue(guard.tryAcquire("test-ip"));
        assertFalse(guard.tryAcquire("test-ip"));
    }
}
