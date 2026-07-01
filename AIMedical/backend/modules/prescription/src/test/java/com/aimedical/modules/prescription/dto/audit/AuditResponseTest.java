package com.aimedical.modules.prescription.dto.audit;

import com.aimedical.modules.prescription.service.audit.AuditRiskLevel;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AuditResponseTest {

    @Test
    void shouldSetAndGetFields() {
        AuditResponse resp = new AuditResponse();
        resp.setRiskLevel(AuditRiskLevel.WARN);
        resp.setAlerts(List.of(new AuditAlert()));
        resp.setInteractions(List.of(new DrugInteraction()));
        resp.setSuggestions(List.of(new Suggestion()));
        resp.setFromFallback(true);

        assertEquals(AuditRiskLevel.WARN, resp.getRiskLevel());
        assertEquals(1, resp.getAlerts().size());
        assertEquals(1, resp.getInteractions().size());
        assertEquals(1, resp.getSuggestions().size());
        assertTrue(resp.isFromFallback());
    }
}
