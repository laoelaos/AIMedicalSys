package com.aimedical.modules.doctor.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ConsultationStatusTest {

    @Test
    void shouldHaveCorrectCodes() {
        assertEquals("WAITING", ConsultationStatus.WAITING.getCode());
        assertEquals("CALLED", ConsultationStatus.CALLED.getCode());
        assertEquals("IN_CONSULTATION", ConsultationStatus.IN_CONSULTATION.getCode());
        assertEquals("FINISHED", ConsultationStatus.FINISHED.getCode());
        assertEquals("SKIPPED", ConsultationStatus.SKIPPED.getCode());
    }

    @Test
    void shouldHaveCorrectDescs() {
        assertEquals("候诊", ConsultationStatus.WAITING.getDesc());
        assertEquals("已叫号", ConsultationStatus.CALLED.getDesc());
        assertEquals("接诊中", ConsultationStatus.IN_CONSULTATION.getDesc());
        assertEquals("完成", ConsultationStatus.FINISHED.getDesc());
        assertEquals("过号", ConsultationStatus.SKIPPED.getDesc());
    }

    @Test
    void shouldHaveFiveValues() {
        assertEquals(5, ConsultationStatus.values().length);
    }
}
