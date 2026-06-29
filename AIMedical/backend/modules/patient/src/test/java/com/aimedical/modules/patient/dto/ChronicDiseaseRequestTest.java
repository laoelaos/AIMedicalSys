package com.aimedical.modules.patient.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ChronicDiseaseRequestTest {

    @Test
    void shouldSetAndGetDiseaseName() {
        ChronicDiseaseRequest r = new ChronicDiseaseRequest();
        r.setDiseaseName("高血压");
        assertEquals("高血压", r.getDiseaseName());
    }

    @Test
    void shouldSetAndGetDiagnosedAt() {
        ChronicDiseaseRequest r = new ChronicDiseaseRequest();
        r.setDiagnosedAt("2023-01-01");
        assertEquals("2023-01-01", r.getDiagnosedAt());
    }

    @Test
    void shouldSetAndGetCurrentStatus() {
        ChronicDiseaseRequest r = new ChronicDiseaseRequest();
        r.setCurrentStatus("STABLE");
        assertEquals("STABLE", r.getCurrentStatus());
    }
}
