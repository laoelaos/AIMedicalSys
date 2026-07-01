package com.aimedical.modules.prescription.dto.audit;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AlertSeverityTest {

    @Test
    void shouldDefineThreeLevels() {
        assertEquals(3, AlertSeverity.values().length);
        assertNotNull(AlertSeverity.valueOf("INFO"));
        assertNotNull(AlertSeverity.valueOf("WARNING"));
        assertNotNull(AlertSeverity.valueOf("CRITICAL"));
    }
}
