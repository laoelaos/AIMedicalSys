package com.aimedical.modules.doctor.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PrescriptionStatusTest {

    @Test
    void shouldHaveCorrectCodes() {
        assertEquals("DRAFT", PrescriptionStatus.DRAFT.getCode());
        assertEquals("PENDING_REVIEW", PrescriptionStatus.PENDING_REVIEW.getCode());
        assertEquals("APPROVED", PrescriptionStatus.APPROVED.getCode());
        assertEquals("REJECTED", PrescriptionStatus.REJECTED.getCode());
    }

    @Test
    void shouldHaveCorrectDescs() {
        assertEquals("草稿", PrescriptionStatus.DRAFT.getDesc());
        assertEquals("待审", PrescriptionStatus.PENDING_REVIEW.getDesc());
        assertEquals("已审", PrescriptionStatus.APPROVED.getDesc());
        assertEquals("已驳回", PrescriptionStatus.REJECTED.getDesc());
    }

    @Test
    void shouldHaveFourValues() {
        assertEquals(4, PrescriptionStatus.values().length);
    }
}
