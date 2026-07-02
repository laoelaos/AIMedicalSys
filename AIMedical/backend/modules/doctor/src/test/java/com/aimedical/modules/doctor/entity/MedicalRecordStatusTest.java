package com.aimedical.modules.doctor.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MedicalRecordStatusTest {

    @Test
    void shouldHaveCorrectCodes() {
        assertEquals("DRAFT", MedicalRecordStatus.DRAFT.getCode());
        assertEquals("OFFICIAL", MedicalRecordStatus.OFFICIAL.getCode());
    }

    @Test
    void shouldHaveCorrectDescs() {
        assertEquals("草稿", MedicalRecordStatus.DRAFT.getDesc());
        assertEquals("正式", MedicalRecordStatus.OFFICIAL.getDesc());
    }

    @Test
    void shouldHaveTwoValues() {
        assertEquals(2, MedicalRecordStatus.values().length);
    }
}
