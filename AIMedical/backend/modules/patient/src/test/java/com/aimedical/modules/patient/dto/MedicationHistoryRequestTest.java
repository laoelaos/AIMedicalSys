package com.aimedical.modules.patient.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MedicationHistoryRequestTest {

    @Test
    void shouldSetAndGetDrugName() {
        MedicationHistoryRequest r = new MedicationHistoryRequest();
        r.setDrugName("阿莫西林");
        assertEquals("阿莫西林", r.getDrugName());
    }

    @Test
    void shouldSetAndGetReason() {
        MedicationHistoryRequest r = new MedicationHistoryRequest();
        r.setReason("呼吸道感染");
        assertEquals("呼吸道感染", r.getReason());
    }

    @Test
    void shouldSetAndGetStartedAt() {
        MedicationHistoryRequest r = new MedicationHistoryRequest();
        r.setStartedAt("2024-01-01");
        assertEquals("2024-01-01", r.getStartedAt());
    }

    @Test
    void shouldSetAndGetEndedAt() {
        MedicationHistoryRequest r = new MedicationHistoryRequest();
        r.setEndedAt("2024-01-14");
        assertEquals("2024-01-14", r.getEndedAt());
    }
}
