package com.aimedical.modules.prescription.dto.audit;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WarnAlertTest {

    @Test
    void shouldSetAndGetFields() {
        WarnAlert alert = new WarnAlert();
        alert.setAlertCode("W001");
        alert.setAlertMessage("Drug interaction warning");
        alert.setSeverity(AlertSeverity.WARNING);

        assertEquals("W001", alert.getAlertCode());
        assertEquals("Drug interaction warning", alert.getAlertMessage());
        assertEquals(AlertSeverity.WARNING, alert.getSeverity());
    }

    @Test
    void shouldConstructWithAllArgs() {
        WarnAlert alert = new WarnAlert("W001", "Drug interaction warning", AlertSeverity.CRITICAL);

        assertEquals("W001", alert.getAlertCode());
        assertEquals("Drug interaction warning", alert.getAlertMessage());
        assertEquals(AlertSeverity.CRITICAL, alert.getSeverity());
    }

    @Test
    void shouldHandleNullFields() {
        WarnAlert alert = new WarnAlert();

        assertNull(alert.getAlertCode());
        assertNull(alert.getAlertMessage());
        assertNull(alert.getSeverity());
    }
}
