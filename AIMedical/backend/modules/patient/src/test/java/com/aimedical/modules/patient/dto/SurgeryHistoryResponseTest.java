package com.aimedical.modules.patient.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SurgeryHistoryResponseTest {

    @Test
    void shouldSetAndGetId() {
        SurgeryHistoryResponse r = new SurgeryHistoryResponse();
        r.setId(1L);
        assertEquals(1L, r.getId());
    }

    @Test
    void shouldSetAndGetSurgeryName() {
        SurgeryHistoryResponse r = new SurgeryHistoryResponse();
        r.setSurgeryName("胆囊切除术");
        assertEquals("胆囊切除术", r.getSurgeryName());
    }

    @Test
    void shouldSetAndGetSurgeryAt() {
        SurgeryHistoryResponse r = new SurgeryHistoryResponse();
        r.setSurgeryAt("2022-11-20");
        assertEquals("2022-11-20", r.getSurgeryAt());
    }

    @Test
    void shouldSetAndGetHospital() {
        SurgeryHistoryResponse r = new SurgeryHistoryResponse();
        r.setHospital("省立医院");
        assertEquals("省立医院", r.getHospital());
    }
}
