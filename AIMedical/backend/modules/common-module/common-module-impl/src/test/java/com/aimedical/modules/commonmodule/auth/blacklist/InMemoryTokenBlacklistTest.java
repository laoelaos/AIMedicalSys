package com.aimedical.modules.commonmodule.auth.blacklist;

import java.lang.reflect.Field;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemoryTokenBlacklistTest {

    @Test
    void shouldReturnTrueForBlacklistedJti() {
        InMemoryTokenBlacklist blacklist = new InMemoryTokenBlacklist();
        blacklist.add("test-jti", System.currentTimeMillis() + 60_000);
        assertTrue(blacklist.isBlacklisted("test-jti"));
    }

    @Test
    void shouldReturnFalseForUnknownJti() {
        InMemoryTokenBlacklist blacklist = new InMemoryTokenBlacklist();
        assertFalse(blacklist.isBlacklisted("unknown-jti"));
    }

    @Test
    void shouldHandleMultipleJtiIndependently() {
        InMemoryTokenBlacklist blacklist = new InMemoryTokenBlacklist();
        blacklist.add("jti-1", System.currentTimeMillis() + 60_000);
        blacklist.add("jti-2", System.currentTimeMillis() + 60_000);
        assertTrue(blacklist.isBlacklisted("jti-1"));
        assertTrue(blacklist.isBlacklisted("jti-2"));
        assertFalse(blacklist.isBlacklisted("jti-3"));
    }

    @Test
    void shouldReturnFalseAfterRemoval() throws Exception {
        ConcurrentHashMap<String, Long> preFilled = new ConcurrentHashMap<>();
        preFilled.put("expired-jti", System.currentTimeMillis() - 100_000);
        InMemoryTokenBlacklist blacklist = new InMemoryTokenBlacklist(preFilled);
        blacklist.cleanup();
        assertFalse(blacklist.isBlacklisted("expired-jti"));
    }

    @Test
    void shouldReturnFalseForExpiredEntry() {
        ConcurrentHashMap<String, Long> preFilled = new ConcurrentHashMap<>();
        preFilled.put("expired-jti", System.currentTimeMillis() - 100_000);
        preFilled.put("valid-jti", System.currentTimeMillis() + 100_000);
        InMemoryTokenBlacklist blacklist = new InMemoryTokenBlacklist(preFilled);
        assertTrue(blacklist.isBlacklisted("expired-jti"));
        blacklist.cleanup();
        assertFalse(blacklist.isBlacklisted("expired-jti"));
        assertTrue(blacklist.isBlacklisted("valid-jti"));
    }

    @Test
    void shouldThrowNpeWhenAddWithNullJti() {
        InMemoryTokenBlacklist blacklist = new InMemoryTokenBlacklist();
        assertThrows(NullPointerException.class,
                () -> blacklist.add(null, System.currentTimeMillis() + 60_000));
    }

    @Test
    void shouldThrowNpeWhenIsBlacklistedWithNull() {
        InMemoryTokenBlacklist blacklist = new InMemoryTokenBlacklist();
        assertThrows(NullPointerException.class,
                () -> blacklist.isBlacklisted(null));
    }

    @Test
    void shouldHandleConcurrentAddSameJti() throws Exception {
        int threadCount = 10;
        InMemoryTokenBlacklist blacklist = new InMemoryTokenBlacklist();
        CountDownLatch latch = new CountDownLatch(threadCount);
        CyclicBarrier barrier = new CyclicBarrier(threadCount);

        for (int i = 0; i < threadCount; i++) {
            new Thread(() -> {
                try { barrier.await(); } catch (Exception e) { Thread.currentThread().interrupt(); }
                blacklist.add("same-jti", System.currentTimeMillis() + 60_000);
                latch.countDown();
            }).start();
        }
        assertTrue(latch.await(10, TimeUnit.SECONDS));
        assertTrue(blacklist.isBlacklisted("same-jti"));
        assertEquals(1, readMapSize(blacklist));
    }

    @Test
    void shouldHandleConcurrentAddDifferentJti() throws Exception {
        int threadCount = 10;
        InMemoryTokenBlacklist blacklist = new InMemoryTokenBlacklist();
        CountDownLatch latch = new CountDownLatch(threadCount);
        CyclicBarrier barrier = new CyclicBarrier(threadCount);

        for (int i = 0; i < threadCount; i++) {
            String jti = "jti-" + i;
            new Thread(() -> {
                try { barrier.await(); } catch (Exception e) { Thread.currentThread().interrupt(); }
                blacklist.add(jti, System.currentTimeMillis() + 60_000);
                latch.countDown();
            }).start();
        }
        assertTrue(latch.await(10, TimeUnit.SECONDS));
        assertEquals(threadCount, readMapSize(blacklist));
        for (int i = 0; i < threadCount; i++) {
            assertTrue(blacklist.isBlacklisted("jti-" + i));
        }
    }

    @Test
    void shouldNotThrowWhenAddingExistingJti() {
        InMemoryTokenBlacklist blacklist = new InMemoryTokenBlacklist();
        blacklist.add("same-jti", System.currentTimeMillis() + 60_000);
        blacklist.add("same-jti", System.currentTimeMillis() + 120_000);
        assertTrue(blacklist.isBlacklisted("same-jti"));
    }

    @Test
    void shouldCleanupExpiredEntries() throws Exception {
        ConcurrentHashMap<String, Long> preFilled = new ConcurrentHashMap<>();
        preFilled.put("expired-1", System.currentTimeMillis() - 100_000);
        preFilled.put("expired-2", System.currentTimeMillis() - 50_000);
        preFilled.put("valid-1", System.currentTimeMillis() + 100_000);
        InMemoryTokenBlacklist blacklist = new InMemoryTokenBlacklist(preFilled);
        assertEquals(3, readMapSize(blacklist));
        blacklist.cleanup();
        assertEquals(1, readMapSize(blacklist));
    }

    @Test
    void shouldRetainNonExpiredEntriesAfterCleanup() throws Exception {
        ConcurrentHashMap<String, Long> preFilled = new ConcurrentHashMap<>();
        preFilled.put("expired-jti", System.currentTimeMillis() - 100_000);
        preFilled.put("valid-jti", System.currentTimeMillis() + 100_000);
        InMemoryTokenBlacklist blacklist = new InMemoryTokenBlacklist(preFilled);
        blacklist.cleanup();
        assertFalse(blacklist.isBlacklisted("expired-jti"));
        assertTrue(blacklist.isBlacklisted("valid-jti"));
        assertEquals(1, readMapSize(blacklist));
    }

    private static int readMapSize(InMemoryTokenBlacklist blacklist) throws Exception {
        Field f = InMemoryTokenBlacklist.class.getDeclaredField("blacklist");
        f.setAccessible(true);
        return ((ConcurrentHashMap<?, ?>) f.get(blacklist)).size();
    }
}
