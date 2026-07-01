package com.aimedical.modules.prescription.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class AuditRecordTest {

    @Test
    void shouldSetAndGetFields() {
        AuditRecord record = new AuditRecord();
        record.setPrescriptionId("rx-001");
        record.setPrescriptionOrderId("order-001");
        record.setDoctorId("doc-001");
        record.setPatientId("pat-001");
        record.setAuditTime(LocalDateTime.of(2024, 1, 1, 10, 0));
        record.setFromFallback(false);
        record.setForceSubmitted(true);
        record.setForceSubmitTime(LocalDateTime.now());
        record.setAuditSequence(1);
        record.setLatest(true);
        record.setOriginalPrescription("[]");
        record.setRiskLevel("WARN");
        record.setAiResult("{}");
        record.setAuditIssues("[]");
        record.setVersion(0L);

        assertEquals("rx-001", record.getPrescriptionId());
        assertEquals("order-001", record.getPrescriptionOrderId());
        assertEquals("doc-001", record.getDoctorId());
        assertEquals("pat-001", record.getPatientId());
        assertFalse(record.isFromFallback());
        assertTrue(record.getForceSubmitted());
        assertEquals(1, record.getAuditSequence());
        assertTrue(record.isLatest());
        assertEquals("WARN", record.getRiskLevel());
        assertEquals(0L, record.getVersion().longValue());
    }

    @Test
    void shouldDefaultBooleanToFalse() {
        AuditRecord record = new AuditRecord();
        assertFalse(record.isFromFallback());
    }
}
