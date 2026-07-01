package com.aimedical.modules.prescription.context;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DosageAlertTest {

    @Test
    void shouldSetAndGetFields() {
        DosageAlert alert = new DosageAlert();
        alert.setSeverity("CRITICAL");
        alert.setMessage("Dose exceeds limit");
        alert.setDrugCode("drug-001");

        assertEquals("CRITICAL", alert.getSeverity());
        assertEquals("Dose exceeds limit", alert.getMessage());
        assertEquals("drug-001", alert.getDrugCode());
    }
}
