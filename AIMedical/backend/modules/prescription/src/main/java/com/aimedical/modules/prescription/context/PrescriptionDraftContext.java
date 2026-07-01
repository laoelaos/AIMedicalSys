package com.aimedical.modules.prescription.context;

import com.aimedical.modules.commonmodule.store.DraftContextStore;
import com.aimedical.modules.prescription.task.DraftContextCleanupTask;
import org.springframework.stereotype.Component;
import java.time.Instant;
import java.util.Collections;
import java.util.List;

@Component
public class PrescriptionDraftContext {

    private static final String CRITICAL_ALERTS_SUFFIX = ":criticalAlerts";

    private final DraftContextStore draftContextStore;
    private final DraftContextCleanupTask cleanupTask;

    public PrescriptionDraftContext(DraftContextStore draftContextStore,
                                     DraftContextCleanupTask cleanupTask) {
        this.draftContextStore = draftContextStore;
        this.cleanupTask = cleanupTask;
    }

    public List<DosageAlert> getCriticalAlerts(String prescriptionId) {
        String key = prescriptionId + CRITICAL_ALERTS_SUFFIX;
        Object value = draftContextStore.get(key);
        if (value instanceof List<?>) {
            return (List<DosageAlert>) value;
        }
        return Collections.emptyList();
    }

    public void updateCriticalAlerts(String prescriptionId, List<DosageAlert> alerts) {
        String key = prescriptionId + CRITICAL_ALERTS_SUFFIX;
        if (alerts == null || alerts.isEmpty()) {
            draftContextStore.remove(key);
            cleanupTask.removeTimestamp(key);
        } else {
            draftContextStore.put(key, alerts);
            cleanupTask.recordWrite(key, Instant.now());
        }
    }

    public SnapshotResult snapshotCriticalAlerts(String prescriptionId) {
        String key = prescriptionId + CRITICAL_ALERTS_SUFFIX;
        Object value = draftContextStore.get(key);
        if (value instanceof List<?>) {
            List<DosageAlert> alerts = (List<DosageAlert>) value;
            return new SnapshotResult(!alerts.isEmpty(), alerts);
        }
        return new SnapshotResult(false, Collections.emptyList());
    }

    public static class SnapshotResult {
        public final boolean hasAlerts;
        public final List<DosageAlert> alerts;

        public SnapshotResult(boolean hasAlerts, List<DosageAlert> alerts) {
            this.hasAlerts = hasAlerts;
            this.alerts = alerts;
        }
    }

    public int getContextCriticalCount(String prescriptionId) {
        return getCriticalAlerts(prescriptionId).size();
    }
}
