package com.aimedical.modules.prescription.dto.audit;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AuditIssueTest {

    @Test
    void shouldSetAndGetFields() {
        AuditIssue issue = new AuditIssue();
        issue.setFieldName("dose");
        issue.setIssueDescription("Exceeds limit");
        issue.setRuleId("DOSAGE_LIMIT");
        issue.setSeverity(AlertSeverity.CRITICAL);

        assertEquals("dose", issue.getFieldName());
        assertEquals("Exceeds limit", issue.getIssueDescription());
        assertEquals("DOSAGE_LIMIT", issue.getRuleId());
        assertEquals(AlertSeverity.CRITICAL, issue.getSeverity());
    }
}
