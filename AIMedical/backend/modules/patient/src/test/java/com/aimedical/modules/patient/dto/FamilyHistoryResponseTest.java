package com.aimedical.modules.patient.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FamilyHistoryResponseTest {

    @Test
    void shouldSetAndGetId() {
        FamilyHistoryResponse r = new FamilyHistoryResponse();
        r.setId(1L);
        assertEquals(1L, r.getId());
    }

    @Test
    void shouldSetAndGetRelationship() {
        FamilyHistoryResponse r = new FamilyHistoryResponse();
        r.setRelationship("母亲");
        assertEquals("母亲", r.getRelationship());
    }

    @Test
    void shouldSetAndGetDiseaseName() {
        FamilyHistoryResponse r = new FamilyHistoryResponse();
        r.setDiseaseName("糖尿病");
        assertEquals("糖尿病", r.getDiseaseName());
    }

    @Test
    void shouldSetAndGetNote() {
        FamilyHistoryResponse r = new FamilyHistoryResponse();
        r.setNote("需定期复查");
        assertEquals("需定期复查", r.getNote());
    }
}
