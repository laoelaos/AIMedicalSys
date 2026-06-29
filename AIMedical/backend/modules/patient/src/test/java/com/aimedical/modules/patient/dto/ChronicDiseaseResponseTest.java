package com.aimedical.modules.patient.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ChronicDiseaseResponseTest {

    @Test
    void shouldSetAndGetId() {
        ChronicDiseaseResponse r = new ChronicDiseaseResponse();
        r.setId(1L);
        assertEquals(1L, r.getId());
    }

    @Test
    void shouldSetAndGetDiseaseName() {
        ChronicDiseaseResponse r = new ChronicDiseaseResponse();
        r.setDiseaseName("糖尿病");
        assertEquals("糖尿病", r.getDiseaseName());
    }

    @Test
    void shouldSetAndGetDiagnosedAt() {
        ChronicDiseaseResponse r = new ChronicDiseaseResponse();
        r.setDiagnosedAt("2023-06-01");
        assertEquals("2023-06-01", r.getDiagnosedAt());
    }

    @Test
    void shouldSetAndGetCurrentStatus() {
        ChronicDiseaseResponse r = new ChronicDiseaseResponse();
        r.setCurrentStatus("UNSTABLE");
        assertEquals("UNSTABLE", r.getCurrentStatus());
    }
}
