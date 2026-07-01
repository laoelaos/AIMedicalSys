package com.aimedical.modules.prescription.dto.assist;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DosageAlertLevelTest {

    @Test
    void shouldHaveAllValues() {
        assertEquals(3, DosageAlertLevel.values().length);
        assertEquals(DosageAlertLevel.INFO, DosageAlertLevel.valueOf("INFO"));
        assertEquals(DosageAlertLevel.WARNING, DosageAlertLevel.valueOf("WARNING"));
        assertEquals(DosageAlertLevel.CRITICAL, DosageAlertLevel.valueOf("CRITICAL"));
    }
}
