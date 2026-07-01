package com.aimedical.modules.prescription.context;

import com.aimedical.modules.commonmodule.store.DraftContextStore;
import com.aimedical.modules.prescription.task.DraftContextCleanupTask;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PrescriptionDraftContextTest {

    @Mock
    private DraftContextStore draftContextStore;

    @Mock
    private DraftContextCleanupTask cleanupTask;

    private PrescriptionDraftContext context;

    @BeforeEach
    void setUp() {
        context = new PrescriptionDraftContext(draftContextStore, cleanupTask);
    }

    @Test
    void shouldReturnEmptyListWhenNoAlerts() {
        when(draftContextStore.get("rx-001:criticalAlerts")).thenReturn(null);

        List<DosageAlert> alerts = context.getCriticalAlerts("rx-001");
        assertTrue(alerts.isEmpty());
    }

    @Test
    void shouldReturnAlertsWhenPresent() {
        DosageAlert alert = new DosageAlert();
        alert.setDrugCode("drug-001");
        alert.setSeverity("CRITICAL");
        alert.setMessage("Overdose");
        when(draftContextStore.get("rx-001:criticalAlerts")).thenReturn(List.of(alert));

        List<DosageAlert> alerts = context.getCriticalAlerts("rx-001");
        assertEquals(1, alerts.size());
        assertEquals("drug-001", alerts.get(0).getDrugCode());
    }

    @Test
    void shouldReturnEmptyListWhenStoredValueIsNotList() {
        when(draftContextStore.get("rx-001:criticalAlerts")).thenReturn("not-a-list");

        List<DosageAlert> alerts = context.getCriticalAlerts("rx-001");
        assertTrue(alerts.isEmpty());
    }

    @Test
    void snapshotShouldReturnNoAlertsWhenStoreHasNull() {
        when(draftContextStore.get("rx-001:criticalAlerts")).thenReturn(null);

        PrescriptionDraftContext.SnapshotResult result = context.snapshotCriticalAlerts("rx-001");

        assertFalse(result.hasAlerts);
        assertTrue(result.alerts.isEmpty());
    }

    @Test
    void snapshotShouldReturnHasAlertsTrueWhenAlertsExist() {
        DosageAlert alert = new DosageAlert();
        alert.setDrugCode("drug-001");
        alert.setSeverity("CRITICAL");
        alert.setMessage("Overdose");
        when(draftContextStore.get("rx-001:criticalAlerts")).thenReturn(List.of(alert));

        PrescriptionDraftContext.SnapshotResult result = context.snapshotCriticalAlerts("rx-001");

        assertTrue(result.hasAlerts);
        assertEquals(1, result.alerts.size());
        assertEquals("drug-001", result.alerts.get(0).getDrugCode());
    }

    @Test
    void snapshotShouldReturnHasAlertsFalseForNonListValue() {
        when(draftContextStore.get("rx-001:criticalAlerts")).thenReturn("invalid");

        PrescriptionDraftContext.SnapshotResult result = context.snapshotCriticalAlerts("rx-001");

        assertFalse(result.hasAlerts);
        assertTrue(result.alerts.isEmpty());
    }

    @Test
    void snapshotShouldReturnHasAlertsFalseForEmptyList() {
        when(draftContextStore.get("rx-001:criticalAlerts")).thenReturn(List.of());

        PrescriptionDraftContext.SnapshotResult result = context.snapshotCriticalAlerts("rx-001");

        assertFalse(result.hasAlerts);
        assertTrue(result.alerts.isEmpty());
    }

    @Test
    void updateCriticalAlertsShouldPutWhenNonEmpty() {
        DosageAlert alert = new DosageAlert();
        alert.setDrugCode("drug-001");
        alert.setSeverity("CRITICAL");

        context.updateCriticalAlerts("rx-001", List.of(alert));

        verify(draftContextStore).put("rx-001:criticalAlerts", List.of(alert));
        verify(cleanupTask).recordWrite(eq("rx-001:criticalAlerts"), any(Instant.class));
    }

    @Test
    void updateCriticalAlertsShouldRemoveWhenEmpty() {
        context.updateCriticalAlerts("rx-001", List.of());

        verify(draftContextStore).remove("rx-001:criticalAlerts");
        verify(cleanupTask).removeTimestamp("rx-001:criticalAlerts");
    }

    @Test
    void updateCriticalAlertsShouldRemoveWhenNull() {
        context.updateCriticalAlerts("rx-001", null);

        verify(draftContextStore).remove("rx-001:criticalAlerts");
        verify(cleanupTask).removeTimestamp("rx-001:criticalAlerts");
    }

    @Test
    void getContextCriticalCountShouldReturnZeroWhenNoAlerts() {
        when(draftContextStore.get("rx-001:criticalAlerts")).thenReturn(null);

        assertEquals(0, context.getContextCriticalCount("rx-001"));
    }

    @Test
    void getContextCriticalCountShouldReturnCorrectCount() {
        DosageAlert alert1 = new DosageAlert();
        alert1.setDrugCode("drug-001");
        DosageAlert alert2 = new DosageAlert();
        alert2.setDrugCode("drug-002");
        when(draftContextStore.get("rx-001:criticalAlerts")).thenReturn(List.of(alert1, alert2));

        assertEquals(2, context.getContextCriticalCount("rx-001"));
    }
}
