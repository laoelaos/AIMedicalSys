package com.aimedical.modules.prescription.service.audit;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AuditRiskLevelTest {

    @Test
    void shouldDefineThreeLevels() {
        assertEquals(3, AuditRiskLevel.values().length);
        assertNotNull(AuditRiskLevel.valueOf("PASS"));
        assertNotNull(AuditRiskLevel.valueOf("WARN"));
        assertNotNull(AuditRiskLevel.valueOf("BLOCK"));
    }
}
