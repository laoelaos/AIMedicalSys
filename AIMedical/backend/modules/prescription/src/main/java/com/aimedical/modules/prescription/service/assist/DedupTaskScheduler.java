package com.aimedical.modules.prescription.service.assist;

import com.aimedical.modules.commonmodule.store.SuggestionStore;
import com.aimedical.modules.prescription.dto.assist.AiSuggestionResult;
import com.aimedical.modules.prescription.dto.assist.AiSuggestionStatus;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class DedupTaskScheduler {

    private static final String DEDUP_KEY_PREFIX = "suggestion-dedup:";

    private final SuggestionStore suggestionStore;

    public DedupTaskScheduler(SuggestionStore suggestionStore) {
        this.suggestionStore = suggestionStore;
    }

    public String schedule(String prescriptionId) {
        String dedupKey = DEDUP_KEY_PREFIX + prescriptionId;
        String candidateTaskId = UUID.randomUUID().toString();

        Object existing = suggestionStore.get(dedupKey);
        if (existing instanceof AiSuggestionResult r) {
            if (r.getStatus() == AiSuggestionStatus.PENDING
                    || r.getStatus() == AiSuggestionStatus.PROCESSING
                    || (r.getStatus() == AiSuggestionStatus.COMPLETED && !r.isConsumed())) {
                return r.getTaskId();
            }
        }

        AiSuggestionResult newResult = new AiSuggestionResult();
        newResult.setTaskId(candidateTaskId);
        newResult.setStatus(AiSuggestionStatus.PENDING);
        newResult.setCreateTime(LocalDateTime.now());

        suggestionStore.put(candidateTaskId, newResult);

        Object oldValue = suggestionStore.createIfNotExists(dedupKey, newResult);
        if (oldValue == null) {
            return candidateTaskId;
        }

        if (oldValue instanceof AiSuggestionResult r) {
            if (r.getStatus() == AiSuggestionStatus.PENDING
                    || r.getStatus() == AiSuggestionStatus.PROCESSING
                    || (r.getStatus() == AiSuggestionStatus.COMPLETED && !r.isConsumed())) {
                return r.getTaskId();
            }
        }

        Object result = suggestionStore.compute(dedupKey, (key, currentValue) -> {
            if (currentValue instanceof AiSuggestionResult current) {
                if (current.getStatus() == AiSuggestionStatus.PENDING
                        || current.getStatus() == AiSuggestionStatus.PROCESSING
                        || (current.getStatus() == AiSuggestionStatus.COMPLETED && !current.isConsumed())) {
                    return current;
                }
            }
            return newResult;
        });

        if (result == newResult) {
            return candidateTaskId;
        } else if (result instanceof AiSuggestionResult winner) {
            return winner.getTaskId();
        } else {
            throw new IllegalStateException("Unexpected value type for dedupKey: " + result);
        }
    }
}
