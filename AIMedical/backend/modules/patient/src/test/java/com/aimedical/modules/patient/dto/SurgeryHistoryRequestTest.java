package com.aimedical.modules.patient.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SurgeryHistoryRequestTest {

    @Test
    void shouldSetAndGetSurgeryName() {
        SurgeryHistoryRequest r = new SurgeryHistoryRequest();
        r.setSurgeryName("阑尾切除术");
        assertEquals("阑尾切除术", r.getSurgeryName());
    }

    @Test
    void shouldSetAndGetSurgeryAt() {
        SurgeryHistoryRequest r = new SurgeryHistoryRequest();
        r.setSurgeryAt("2023-05-10");
        assertEquals("2023-05-10", r.getSurgeryAt());
    }

    @Test
    void shouldSetAndGetHospital() {
        SurgeryHistoryRequest r = new SurgeryHistoryRequest();
        r.setHospital("市人民医院");
        assertEquals("市人民医院", r.getHospital());
    }
}
