package com.aimedical.modules.prescription.task;

import com.aimedical.modules.commonmodule.store.DraftContextStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class DraftContextCleanupTask {

    private static final Logger log = LoggerFactory.getLogger(DraftContextCleanupTask.class);
    private static final long TTL_MINUTES = 60;

    private final DraftContextStore draftContextStore;
    private final ConcurrentHashMap<String, Instant> writeTimestamps;

    public DraftContextCleanupTask(DraftContextStore draftContextStore) {
        this.draftContextStore = draftContextStore;
        this.writeTimestamps = new ConcurrentHashMap<>();
    }

    public void recordWrite(String key, Instant timestamp) {
        writeTimestamps.put(key, timestamp);
    }

    public void removeTimestamp(String key) {
        writeTimestamps.remove(key);
    }

    @Scheduled(cron = "0 0/5 * * * ?")
    public void cleanupExpiredDrafts() {
        Instant now = Instant.now();
        writeTimestamps.forEach((key, ts) -> {
            if (ts != null && ts.plusSeconds(TTL_MINUTES * 60).isBefore(now)) {
                draftContextStore.remove(key);
                writeTimestamps.remove(key);
                log.info("Removed expired draft context: {}", key);
            }
        });
    }
}
