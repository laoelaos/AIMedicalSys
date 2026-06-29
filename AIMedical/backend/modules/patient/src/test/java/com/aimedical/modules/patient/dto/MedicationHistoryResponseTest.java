package com.aimedical.modules.patient.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MedicationHistoryResponseTest {

    @Test
    void shouldSetAndGetId() {
        MedicationHistoryResponse r = new MedicationHistoryResponse();
        r.setId(1L);
        assertEquals(1L, r.getId());
    }

    @Test
    void shouldSetAndGetDrugName() {
        MedicationHistoryResponse r = new MedicationHistoryResponse();
        r.setDrugName("布洛芬");
        assertEquals("布洛芬", r.getDrugName());
    }

    @Test
    void shouldSetAndGetReason() {
        MedicationHistoryResponse r = new MedicationHistoryResponse();
        r.setReason("退烧");
        assertEquals("退烧", r.getReason());
    }

    @Test
    void shouldSetAndGetStartedAt() {
        MedicationHistoryResponse r = new MedicationHistoryResponse();
        r.setStartedAt("2024-03-01");
        assertEquals("2024-03-01", r.getStartedAt());
    }

    @Test
    void shouldSetAndGetEndedAt() {
        MedicationHistoryResponse r = new MedicationHistoryResponse();
        r.setEndedAt("2024-03-07");
        assertEquals("2024-03-07", r.getEndedAt());
    }
}
