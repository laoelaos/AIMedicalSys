package com.aimedical.modules.prescription.rule;

import com.aimedical.modules.prescription.dto.audit.AuditRequest;
import com.aimedical.modules.prescription.dto.audit.PrescriptionItem;
import com.aimedical.modules.prescription.service.audit.AuditRiskLevel;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DrugInteractionRuleTest {

    private final DrugInteractionRule rule = new DrugInteractionRule();

    @Test
    void shouldAlwaysReturnPass() {
        AuditRequest request = new AuditRequest();
        request.setPrescriptionItems(List.of(new PrescriptionItem()));

        LocalRuleResult result = rule.check(request);

        assertEquals("DRUG_INTERACTION", result.getRuleId());
        assertTrue(result.isPassed());
        assertEquals(AuditRiskLevel.PASS, result.getSeverity());
    }
}
