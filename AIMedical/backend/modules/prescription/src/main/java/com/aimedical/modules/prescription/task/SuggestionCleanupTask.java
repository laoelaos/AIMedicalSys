package com.aimedical.modules.prescription.task;

import com.aimedical.modules.commonmodule.store.SuggestionStore;
import com.aimedical.modules.commonmodule.store.SuggestionStoreEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;

@Component
public class SuggestionCleanupTask {

    private static final Logger log = LoggerFactory.getLogger(SuggestionCleanupTask.class);
    private static final long TTL_MINUTES = 60;

    private final SuggestionStore suggestionStore;

    public SuggestionCleanupTask(SuggestionStore suggestionStore) {
        this.suggestionStore = suggestionStore;
    }

    @Scheduled(cron = "0 0/5 * * * ?")
    public void cleanupExpiredSuggestions() {
        Instant now = Instant.now();
        for (String key : new ArrayList<>(suggestionStore.keySet())) {
            try {
                Object value = suggestionStore.get(key);
                if (value instanceof SuggestionStoreEntry entry
                        && isExpiredAndConsumed(entry, now)) {
                    suggestionStore.remove(key);
                    log.info("Removed expired suggestion: {}", key);
                }
            } catch (ClassCastException e) {
                log.warn("Suggestion entry {} has unexpected type, skipping", key);
            }
        }
    }

    private boolean isExpiredAndConsumed(SuggestionStoreEntry entry, Instant now) {
        String status = entry.getStatusName();
        boolean isCompleted = "COMPLETED".equals(status);
        boolean isFailed = "FAILED".equals(status);
        boolean isExpired = entry.getTimestamp() != null
                && entry.getTimestamp().plusSeconds(TTL_MINUTES * 60).isBefore(now);
        if (isCompleted) {
            return entry.isConsumed() && isExpired;
        }
        if (isFailed) {
            return isExpired;
        }
        return false;
    }

}
