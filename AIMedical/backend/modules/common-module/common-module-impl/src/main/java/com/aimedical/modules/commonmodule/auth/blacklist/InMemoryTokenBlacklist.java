package com.aimedical.modules.commonmodule.auth.blacklist;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class InMemoryTokenBlacklist implements TokenBlacklist {

    private final ConcurrentHashMap<String, Long> blacklist;

    public InMemoryTokenBlacklist() {
        this.blacklist = new ConcurrentHashMap<>();
        ScheduledExecutorService cleanupExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "token-blacklist-cleanup");
            t.setDaemon(true);
            return t;
        });
        cleanupExecutor.scheduleWithFixedDelay(this::cleanup, 5, 5, TimeUnit.MINUTES);
    }

    InMemoryTokenBlacklist(ConcurrentHashMap<String, Long> blacklist) {
        this.blacklist = blacklist;
    }

    @Override
    public void add(String jti, long expirationTime) {
        blacklist.put(jti, expirationTime);
    }

    @Override
    public boolean isBlacklisted(String jti) {
        return blacklist.containsKey(jti);
    }

    void cleanup() {
        blacklist.entrySet().removeIf(entry -> entry.getValue() < System.currentTimeMillis());
    }
}
