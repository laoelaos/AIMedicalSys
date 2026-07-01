package com.aimedical.modules.prescription.dto.audit;

import com.aimedical.modules.prescription.service.audit.AuditRiskLevel;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class WarnResultTest {

    @Test
    void shouldSetAndGetFields() {
        WarnResult result = new WarnResult();
        result.setRiskLevel(AuditRiskLevel.WARN);
        result.setAlerts(List.of(new WarnAlert("W001", "test", AlertSeverity.WARNING)));
        result.setAuditRecordId(100L);
        result.setPrescriptionHash("abcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890");

        assertEquals(AuditRiskLevel.WARN, result.getRiskLevel());
        assertEquals(1, result.getAlerts().size());
        assertEquals(100L, result.getAuditRecordId());
        assertEquals("abcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890", result.getPrescriptionHash());
    }

    @Test
    void shouldConstructWithAllArgs() {
        List<WarnAlert> alerts = List.of(
                new WarnAlert("W001", "alert 1", AlertSeverity.WARNING),
                new WarnAlert("W002", "alert 2", AlertSeverity.CRITICAL)
        );
        WarnResult result = new WarnResult(AuditRiskLevel.WARN, alerts, 200L, "hash-value");

        assertEquals(AuditRiskLevel.WARN, result.getRiskLevel());
        assertEquals(2, result.getAlerts().size());
        assertEquals(200L, result.getAuditRecordId());
        assertEquals("hash-value", result.getPrescriptionHash());
    }

    @Test
    void shouldHandleNullFields() {
        WarnResult result = new WarnResult();

        assertNull(result.getRiskLevel());
        assertNull(result.getAlerts());
        assertNull(result.getAuditRecordId());
        assertNull(result.getPrescriptionHash());
    }

    @Test
    void shouldHandleEmptyAlerts() {
        WarnResult result = new WarnResult(AuditRiskLevel.WARN, List.of(), 1L, "hash");

        assertNotNull(result.getAlerts());
        assertTrue(result.getAlerts().isEmpty());
    }
}
