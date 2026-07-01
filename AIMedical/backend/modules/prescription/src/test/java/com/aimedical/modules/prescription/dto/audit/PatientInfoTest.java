package com.aimedical.modules.prescription.dto.audit;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PatientInfoTest {

    @Test
    void shouldSetAndGetFields() {
        PatientInfo info = new PatientInfo();
        info.setPatientId("pat-001");
        info.setAge(35);
        info.setGender("M");
        info.setAllergyHistory("Penicillin");
        info.setAllergyDetails(List.of(new AllergyDetail()));
        info.setComorbidities(List.of("Diabetes"));
        info.setWeight(70.5);

        assertEquals("pat-001", info.getPatientId());
        assertEquals(35, info.getAge().intValue());
        assertEquals("M", info.getGender());
        assertEquals("Penicillin", info.getAllergyHistory());
        assertEquals(1, info.getAllergyDetails().size());
        assertEquals(1, info.getComorbidities().size());
        assertEquals(70.5, info.getWeight());
    }

    @Test
    void shouldWeightDefaultToNull() {
        PatientInfo info = new PatientInfo();
        assertNull(info.getWeight());
    }

    @Test
    void shouldSetAndGetWeightNull() {
        PatientInfo info = new PatientInfo();
        info.setWeight(65.0);
        assertEquals(65.0, info.getWeight());
        info.setWeight(null);
        assertNull(info.getWeight());
    }
}
