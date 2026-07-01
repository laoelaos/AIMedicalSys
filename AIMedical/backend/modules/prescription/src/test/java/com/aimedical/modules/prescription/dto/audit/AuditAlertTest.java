package com.aimedical.modules.prescription.dto.audit;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AuditAlertTest {

    @Test
    void shouldSetAndGetFields() {
        AuditAlert alert = new AuditAlert();
        alert.setAlertCode("A001");
        alert.setAlertMessage("Risk detected");
        alert.setSeverity(AlertSeverity.WARNING);

        assertEquals("A001", alert.getAlertCode());
        assertEquals("Risk detected", alert.getAlertMessage());
        assertEquals(AlertSeverity.WARNING, alert.getSeverity());
    }
}
