package com.aimedical.modules.prescription.rule;

import com.aimedical.modules.prescription.service.audit.AuditRiskLevel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LocalRuleResultTest {

    @Test
    void shouldSetAndGetFields() {
        LocalRuleResult result = new LocalRuleResult();
        result.setRuleId("ALLERGY_CHECK");
        result.setPassed(false);
        result.setMessage("Severe allergy found");
        result.setSeverity(AuditRiskLevel.BLOCK);

        assertEquals("ALLERGY_CHECK", result.getRuleId());
        assertFalse(result.isPassed());
        assertEquals("Severe allergy found", result.getMessage());
        assertEquals(AuditRiskLevel.BLOCK, result.getSeverity());
    }

    @Test
    void shouldConstructWithAllArgs() {
        LocalRuleResult result = new LocalRuleResult("DOSAGE_LIMIT", true, null, AuditRiskLevel.PASS);

        assertEquals("DOSAGE_LIMIT", result.getRuleId());
        assertTrue(result.isPassed());
        assertNull(result.getMessage());
        assertEquals(AuditRiskLevel.PASS, result.getSeverity());
    }
}
