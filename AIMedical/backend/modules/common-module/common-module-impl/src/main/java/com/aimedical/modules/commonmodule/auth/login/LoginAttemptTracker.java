package com.aimedical.modules.commonmodule.auth.login;

import java.util.concurrent.ConcurrentHashMap;

public class LoginAttemptTracker {

    private static final int USERNAME_THRESHOLD = 5;
    private static final long USERNAME_LOCK_DURATION_MS = 15 * 60 * 1000L;
    private static final int IP_THRESHOLD = 20;
    private static final long IP_LOCK_DURATION_MS = 30 * 60 * 1000L;

    private final ConcurrentHashMap<String, AttemptRecord> usernameAttempts;
    private final ConcurrentHashMap<String, AttemptRecord> ipAttempts;
    private final int usernameThreshold;
    private final long usernameLockDurationMs;
    private final int ipThreshold;
    private final long ipLockDurationMs;

    public LoginAttemptTracker() {
        this(USERNAME_THRESHOLD, USERNAME_LOCK_DURATION_MS, IP_THRESHOLD, IP_LOCK_DURATION_MS);
    }

    LoginAttemptTracker(int usernameThreshold, long usernameLockDurationMs, int ipThreshold, long ipLockDurationMs) {
        this.usernameAttempts = new ConcurrentHashMap<>();
        this.ipAttempts = new ConcurrentHashMap<>();
        this.usernameThreshold = usernameThreshold;
        this.usernameLockDurationMs = usernameLockDurationMs;
        this.ipThreshold = ipThreshold;
        this.ipLockDurationMs = ipLockDurationMs;
    }

    public void recordUsernameFailure(String username) {
        long now = System.currentTimeMillis();
        usernameAttempts.compute(username, (k, prev) -> {
            if (prev == null) {
                return new AttemptRecord(1, now);
            }
            if (now - prev.firstFailureTime() >= usernameLockDurationMs) {
                return new AttemptRecord(1, now);
            }
            return new AttemptRecord(prev.failures() + 1, prev.firstFailureTime());
        });
    }

    public void recordIpFailure(String ip) {
        long now = System.currentTimeMillis();
        ipAttempts.compute(ip, (k, prev) -> {
            if (prev == null) {
                return new AttemptRecord(1, now);
            }
            if (now - prev.firstFailureTime() >= ipLockDurationMs) {
                return new AttemptRecord(1, now);
            }
            return new AttemptRecord(prev.failures() + 1, prev.firstFailureTime());
        });
    }

    public boolean isUsernameLocked(String username) {
        AttemptRecord prev = usernameAttempts.get(username);
        if (prev == null) {
            return false;
        }
        boolean[] locked = new boolean[1];
        usernameAttempts.compute(username, (k, current) -> {
            if (current == null) {
                locked[0] = false;
                return null;
            }
            long now = System.currentTimeMillis();
            if (now - current.firstFailureTime() >= usernameLockDurationMs) {
                locked[0] = false;
                return null;
            }
            locked[0] = current.failures() >= usernameThreshold;
            return current;
        });
        return locked[0];
    }

    public boolean isIpLocked(String ip) {
        AttemptRecord prev = ipAttempts.get(ip);
        if (prev == null) {
            return false;
        }
        boolean[] locked = new boolean[1];
        ipAttempts.compute(ip, (k, current) -> {
            if (current == null) {
                locked[0] = false;
                return null;
            }
            long now = System.currentTimeMillis();
            if (now - current.firstFailureTime() >= ipLockDurationMs) {
                locked[0] = false;
                return null;
            }
            locked[0] = current.failures() >= ipThreshold;
            return current;
        });
        return locked[0];
    }

    public void clearUsername(String username) {
        usernameAttempts.remove(username);
    }

    public void clearIp(String ip) {
        ipAttempts.remove(ip);
    }

    private static record AttemptRecord(int failures, long firstFailureTime) { }
}
