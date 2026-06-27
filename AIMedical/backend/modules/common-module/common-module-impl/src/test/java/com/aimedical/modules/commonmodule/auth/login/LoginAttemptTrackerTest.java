package com.aimedical.modules.commonmodule.auth.login;

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

class LoginAttemptTrackerTest {

    @Test
    void shouldReturnFalseForUnknownUsername() throws Exception {
        LoginAttemptTracker tracker = new LoginAttemptTracker();
        assertFalse(tracker.isUsernameLocked("nonexistent-user"));
        assertEquals(0, readMapSize(tracker, "usernameAttempts"));
    }

    @Test
    void shouldReturnFalseForUnknownIp() throws Exception {
        LoginAttemptTracker tracker = new LoginAttemptTracker();
        assertFalse(tracker.isIpLocked("nonexistent-ip"));
        assertEquals(0, readMapSize(tracker, "ipAttempts"));
    }

    @Test
    void shouldNotLockUsernameBelowThreshold() {
        LoginAttemptTracker tracker = new LoginAttemptTracker();
        for (int i = 0; i < 4; i++) {
            tracker.recordUsernameFailure("user");
        }
        assertFalse(tracker.isUsernameLocked("user"));
    }

    @Test
    void shouldLockUsernameWhenThresholdReached() {
        LoginAttemptTracker tracker = new LoginAttemptTracker();
        for (int i = 0; i < 5; i++) {
            tracker.recordUsernameFailure("user");
        }
        assertTrue(tracker.isUsernameLocked("user"));
    }

    @Test
    void shouldRemainLockedAfterThreshold() {
        LoginAttemptTracker tracker = new LoginAttemptTracker();
        for (int i = 0; i < 5; i++) {
            tracker.recordUsernameFailure("user");
        }
        assertTrue(tracker.isUsernameLocked("user"));
        assertTrue(tracker.isUsernameLocked("user"));
    }

    @Test
    void shouldClearUsernameAfterSuccess() {
        LoginAttemptTracker tracker = new LoginAttemptTracker();
        for (int i = 0; i < 5; i++) {
            tracker.recordUsernameFailure("user");
        }
        assertTrue(tracker.isUsernameLocked("user"));
        tracker.clearUsername("user");
        assertFalse(tracker.isUsernameLocked("user"));
        tracker.recordUsernameFailure("user");
        assertFalse(tracker.isUsernameLocked("user"));
    }

    @Test
    void shouldNotLockIpBelowThreshold() {
        LoginAttemptTracker tracker = new LoginAttemptTracker();
        for (int i = 0; i < 19; i++) {
            tracker.recordIpFailure("ip");
        }
        assertFalse(tracker.isIpLocked("ip"));
    }

    @Test
    void shouldLockIpWhenThresholdReached() {
        LoginAttemptTracker tracker = new LoginAttemptTracker();
        for (int i = 0; i < 20; i++) {
            tracker.recordIpFailure("ip");
        }
        assertTrue(tracker.isIpLocked("ip"));
    }

    @Test
    void shouldClearIpAfterSuccess() {
        LoginAttemptTracker tracker = new LoginAttemptTracker();
        for (int i = 0; i < 20; i++) {
            tracker.recordIpFailure("ip");
        }
        assertTrue(tracker.isIpLocked("ip"));
        tracker.clearIp("ip");
        assertFalse(tracker.isIpLocked("ip"));
        tracker.recordIpFailure("ip");
        assertFalse(tracker.isIpLocked("ip"));
    }

    @Test
    void shouldMaintainIndependentUsernames() {
        LoginAttemptTracker tracker = new LoginAttemptTracker();
        for (int i = 0; i < 5; i++) {
            tracker.recordUsernameFailure("user-a");
        }
        assertTrue(tracker.isUsernameLocked("user-a"));
        assertFalse(tracker.isUsernameLocked("user-b"));
    }

    @Test
    void shouldMaintainIndependentIps() {
        LoginAttemptTracker tracker = new LoginAttemptTracker();
        for (int i = 0; i < 20; i++) {
            tracker.recordIpFailure("ip-a");
        }
        assertTrue(tracker.isIpLocked("ip-a"));
        assertFalse(tracker.isIpLocked("ip-b"));
    }

    @Test
    void shouldUnlockAfterLockDurationExpiry() throws Exception {
        LoginAttemptTracker tracker = new LoginAttemptTracker(5, 100L, 20, 100L);
        String key = "user";
        for (int i = 0; i < 5; i++) {
            tracker.recordUsernameFailure(key);
        }
        assertTrue(tracker.isUsernameLocked(key));
        long originalFirstFailureTime = readFirstFailureTime(tracker, "usernameAttempts", key);
        Thread.sleep(300);
        assertFalse(tracker.isUsernameLocked(key));
        tracker.recordUsernameFailure(key);
        assertFalse(tracker.isUsernameLocked(key));
        long newFirstFailureTime = readFirstFailureTime(tracker, "usernameAttempts", key);
        assertTrue(newFirstFailureTime > originalFirstFailureTime);
    }

    @Test
    void shouldResetFirstFailureTimeAfterExpiry() throws Exception {
        LoginAttemptTracker tracker = new LoginAttemptTracker(5, 100L, 20, 100L);
        String key = "user";
        for (int i = 0; i < 5; i++) {
            tracker.recordUsernameFailure(key);
        }
        long originalFirstFailureTime = readFirstFailureTime(tracker, "usernameAttempts", key);
        Thread.sleep(300);
        assertFalse(tracker.isUsernameLocked(key));
        tracker.recordUsernameFailure(key);
        long newFirstFailureTime = readFirstFailureTime(tracker, "usernameAttempts", key);
        assertTrue(newFirstFailureTime > originalFirstFailureTime);
    }

    @Test
    void shouldThrowNpeWhenRecordUsernameFailureWithNull() {
        LoginAttemptTracker tracker = new LoginAttemptTracker();
        assertThrows(NullPointerException.class, () -> tracker.recordUsernameFailure(null));
    }

    @Test
    void shouldThrowNpeWhenRecordIpFailureWithNull() {
        LoginAttemptTracker tracker = new LoginAttemptTracker();
        assertThrows(NullPointerException.class, () -> tracker.recordIpFailure(null));
    }

    @Test
    void shouldThrowNpeWhenIsUsernameLockedWithNull() {
        LoginAttemptTracker tracker = new LoginAttemptTracker();
        assertThrows(NullPointerException.class, () -> tracker.isUsernameLocked(null));
    }

    @Test
    void shouldThrowNpeWhenIsIpLockedWithNull() {
        LoginAttemptTracker tracker = new LoginAttemptTracker();
        assertThrows(NullPointerException.class, () -> tracker.isIpLocked(null));
    }

    @Test
    void shouldThrowNpeWhenClearUsernameWithNull() {
        LoginAttemptTracker tracker = new LoginAttemptTracker();
        assertThrows(NullPointerException.class, () -> tracker.clearUsername(null));
    }

    @Test
    void shouldThrowNpeWhenClearIpWithNull() {
        LoginAttemptTracker tracker = new LoginAttemptTracker();
        assertThrows(NullPointerException.class, () -> tracker.clearIp(null));
    }

    @Test
    void shouldHandleConcurrentRecordUsernameFailure() throws Exception {
        int threadCount = 10;
        int callsPerThread = 100;
        LoginAttemptTracker tracker = new LoginAttemptTracker();
        CountDownLatch latch = new CountDownLatch(threadCount);
        CyclicBarrier barrier = new CyclicBarrier(threadCount);

        for (int i = 0; i < threadCount; i++) {
            new Thread(() -> {
                try { barrier.await(); } catch (Exception e) { Thread.currentThread().interrupt(); }
                for (int j = 0; j < callsPerThread; j++) {
                    tracker.recordUsernameFailure("concurrent-user");
                }
                latch.countDown();
            }).start();
        }
        assertTrue(latch.await(10, TimeUnit.SECONDS));
        assertEquals(threadCount * callsPerThread, readFailures(tracker, "usernameAttempts", "concurrent-user"));
    }

    @Test
    void shouldHandleConcurrentRecordIpFailure() throws Exception {
        int threadCount = 10;
        int callsPerThread = 100;
        LoginAttemptTracker tracker = new LoginAttemptTracker();
        CountDownLatch latch = new CountDownLatch(threadCount);
        CyclicBarrier barrier = new CyclicBarrier(threadCount);

        for (int i = 0; i < threadCount; i++) {
            new Thread(() -> {
                try { barrier.await(); } catch (Exception e) { Thread.currentThread().interrupt(); }
                for (int j = 0; j < callsPerThread; j++) {
                    tracker.recordIpFailure("concurrent-ip");
                }
                latch.countDown();
            }).start();
        }
        assertTrue(latch.await(10, TimeUnit.SECONDS));
        assertEquals(threadCount * callsPerThread, readFailures(tracker, "ipAttempts", "concurrent-ip"));
    }

    @Test
    void shouldResetUsernameFailuresWhenWindowExpires() throws Exception {
        LoginAttemptTracker tracker = new LoginAttemptTracker(5, 100L, 20, 100L);
        for (int i = 0; i < 5; i++) {
            tracker.recordUsernameFailure("user");
        }
        assertEquals(5, readFailures(tracker, "usernameAttempts", "user"));
        Thread.sleep(300);
        tracker.recordUsernameFailure("user");
        assertEquals(1, readFailures(tracker, "usernameAttempts", "user"));
    }

    @Test
    void shouldResetIpFailuresWhenWindowExpires() throws Exception {
        LoginAttemptTracker tracker = new LoginAttemptTracker(5, 100L, 20, 100L);
        for (int i = 0; i < 20; i++) {
            tracker.recordIpFailure("ip");
        }
        assertEquals(20, readFailures(tracker, "ipAttempts", "ip"));
        Thread.sleep(300);
        tracker.recordIpFailure("ip");
        assertEquals(1, readFailures(tracker, "ipAttempts", "ip"));
    }

    @Test
    void shouldKeepFirstFailureTimeWhenWindowNotExpiredForUsername() throws Exception {
        LoginAttemptTracker tracker = new LoginAttemptTracker(5, 60_000L, 20, 60_000L);
        tracker.recordUsernameFailure("user");
        long originalFirstFailureTime = readFirstFailureTime(tracker, "usernameAttempts", "user");
        tracker.recordUsernameFailure("user");
        assertEquals(originalFirstFailureTime, readFirstFailureTime(tracker, "usernameAttempts", "user"));
        assertEquals(2, readFailures(tracker, "usernameAttempts", "user"));
    }

    @Test
    void shouldKeepFirstFailureTimeWhenWindowNotExpiredForIp() throws Exception {
        LoginAttemptTracker tracker = new LoginAttemptTracker(5, 60_000L, 20, 60_000L);
        tracker.recordIpFailure("ip");
        long originalFirstFailureTime = readFirstFailureTime(tracker, "ipAttempts", "ip");
        tracker.recordIpFailure("ip");
        assertEquals(originalFirstFailureTime, readFirstFailureTime(tracker, "ipAttempts", "ip"));
        assertEquals(2, readFailures(tracker, "ipAttempts", "ip"));
    }

    private static int readMapSize(LoginAttemptTracker tracker, String fieldName) throws Exception {
        Field f = LoginAttemptTracker.class.getDeclaredField(fieldName);
        f.setAccessible(true);
        return ((ConcurrentHashMap<?, ?>) f.get(tracker)).size();
    }

    private static int readFailures(LoginAttemptTracker tracker, String fieldName, String key) throws Exception {
        Object record = readRecord(tracker, fieldName, key);
        return (int) record.getClass().getMethod("failures").invoke(record);
    }

    private static long readFirstFailureTime(LoginAttemptTracker tracker, String fieldName, String key) throws Exception {
        Object record = readRecord(tracker, fieldName, key);
        return (long) record.getClass().getMethod("firstFailureTime").invoke(record);
    }

    private static Object readRecord(LoginAttemptTracker tracker, String fieldName, String key) throws Exception {
        Field f = LoginAttemptTracker.class.getDeclaredField(fieldName);
        f.setAccessible(true);
        return ((ConcurrentHashMap<String, ?>) f.get(tracker)).get(key);
    }
}
