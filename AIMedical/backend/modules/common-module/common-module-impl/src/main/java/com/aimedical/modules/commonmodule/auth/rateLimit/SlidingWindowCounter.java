package com.aimedical.modules.commonmodule.auth.rateLimit;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class SlidingWindowCounter {

    private final ConcurrentHashMap<String, Deque<Long>> windows;
    private final ScheduledExecutorService cleanupExecutor;

    public SlidingWindowCounter() {
        this.windows = new ConcurrentHashMap<>();
        this.cleanupExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "sliding-window-cleanup");
            t.setDaemon(true);
            return t;
        });
        this.cleanupExecutor.scheduleWithFixedDelay(this::cleanup, 60, 60, TimeUnit.SECONDS);
    }

    public boolean tryAcquire(String key, int limit, long windowMs) {
        if (limit <= 0 || windowMs <= 0) {
            return false;
        }
        long now = System.currentTimeMillis();
        long threshold = now - windowMs;

        boolean[] result = new boolean[1];
        windows.compute(key, (k, deque) -> {
            if (deque == null) {
                deque = new ArrayDeque<>();
            }
            while (!deque.isEmpty() && deque.peekFirst() < threshold) {
                deque.pollFirst();
            }
            if (deque.size() < limit) {
                deque.addLast(now);
                result[0] = true;
            } else {
                result[0] = false;
            }
            return deque;
        });
        return result[0];
    }

    private void cleanup() {
        windows.forEach((k, v) -> { if (v.isEmpty()) windows.remove(k, v); });
    }
}
