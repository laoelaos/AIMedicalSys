package com.aimedical.modules.commonmodule.auth.rateLimit;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SlidingWindowCounterTest {

    @Test
    void shouldAcquireWithinLimit() {
        SlidingWindowCounter counter = new SlidingWindowCounter();
        for (int i = 0; i < 5; i++) {
            assertTrue(counter.tryAcquire("ip-1", 5, 60_000));
        }
    }

    @Test
    void shouldRejectWhenExceedLimit() {
        SlidingWindowCounter counter = new SlidingWindowCounter();
        for (int i = 0; i < 5; i++) {
            counter.tryAcquire("ip-1", 5, 60_000);
        }
        assertFalse(counter.tryAcquire("ip-1", 5, 60_000));
    }

    @Test
    void shouldReturnFalseWhenLimitIsZero() {
        SlidingWindowCounter counter = new SlidingWindowCounter();
        assertFalse(counter.tryAcquire("ip-1", 0, 60_000));
    }

    @Test
    void shouldReturnFalseWhenLimitIsNegative() {
        SlidingWindowCounter counter = new SlidingWindowCounter();
        assertFalse(counter.tryAcquire("ip-1", -1, 60_000));
    }

    @Test
    void shouldReturnFalseWhenWindowMsIsZero() {
        SlidingWindowCounter counter = new SlidingWindowCounter();
        assertFalse(counter.tryAcquire("ip-1", 5, 0));
    }

    @Test
    void shouldReturnFalseWhenWindowMsIsNegative() {
        SlidingWindowCounter counter = new SlidingWindowCounter();
        assertFalse(counter.tryAcquire("ip-1", 5, -1));
    }

    @Test
    void shouldThrowNpeWhenKeyIsNull() {
        SlidingWindowCounter counter = new SlidingWindowCounter();
        assertThrows(NullPointerException.class,
                () -> counter.tryAcquire(null, 5, 60_000));
    }

    @Test
    void shouldMaintainIndependentCountersForDifferentKeys() {
        SlidingWindowCounter counter = new SlidingWindowCounter();
        for (int i = 0; i < 5; i++) {
            counter.tryAcquire("ip-1", 3, 60_000);
        }
        assertFalse(counter.tryAcquire("ip-1", 3, 60_000));
        assertTrue(counter.tryAcquire("ip-2", 3, 60_000));
    }

    @Test
    void shouldAllowNewRequestAfterWindowExpiry() throws InterruptedException {
        SlidingWindowCounter counter = new SlidingWindowCounter();
        long shortWindow = 500;
        assertTrue(counter.tryAcquire("ip-1", 1, shortWindow));
        assertFalse(counter.tryAcquire("ip-1", 1, shortWindow));
        Thread.sleep(shortWindow + 50);
        assertTrue(counter.tryAcquire("ip-1", 1, shortWindow));
    }

    @Test
    void shouldHandleConcurrentRequestsForSameKey() throws InterruptedException {
        int threadCount = 10;
        int limit = 5;
        SlidingWindowCounter counter = new SlidingWindowCounter();
        AtomicInteger allowed = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(threadCount);
        CyclicBarrier barrier = new CyclicBarrier(threadCount);

        for (int i = 0; i < threadCount; i++) {
            new Thread(() -> {
                try { barrier.await(); } catch (Exception e) { Thread.currentThread().interrupt(); }
                if (counter.tryAcquire("concurrent-ip", limit, 10_000)) {
                    allowed.incrementAndGet();
                }
                latch.countDown();
            }).start();
        }
        latch.await();
        assertEquals(limit, allowed.get());
    }

    @Test
    void shouldHandleConcurrentRequestsForDifferentKeys() throws InterruptedException {
        int threadCount = 10;
        SlidingWindowCounter counter = new SlidingWindowCounter();
        AtomicInteger allowed = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(threadCount);
        CyclicBarrier barrier = new CyclicBarrier(threadCount);

        for (int i = 0; i < threadCount; i++) {
            String key = "key-" + i;
            new Thread(() -> {
                try { barrier.await(); } catch (Exception e) { Thread.currentThread().interrupt(); }
                if (counter.tryAcquire(key, 1, 10_000)) {
                    allowed.incrementAndGet();
                }
                latch.countDown();
            }).start();
        }
        latch.await();
        assertEquals(threadCount, allowed.get());
    }

    @Test
    void shouldNotDeadlockUnderConcurrentAcquire() throws InterruptedException {
        int threadCount = 20;
        SlidingWindowCounter counter = new SlidingWindowCounter();
        CountDownLatch latch = new CountDownLatch(threadCount);
        CyclicBarrier barrier = new CyclicBarrier(threadCount);

        for (int i = 0; i < threadCount; i++) {
            new Thread(() -> {
                try { barrier.await(); } catch (Exception e) { Thread.currentThread().interrupt(); }
                for (int j = 0; j < 100; j++) {
                    counter.tryAcquire("deadlock-key", 10, 10_000);
                }
                latch.countDown();
            }).start();
        }
        latch.await();
    }
}
