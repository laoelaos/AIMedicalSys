package com.aimedical.modules.patient.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FamilyHistoryRequestTest {

    @Test
    void shouldSetAndGetRelationship() {
        FamilyHistoryRequest r = new FamilyHistoryRequest();
        r.setRelationship("父亲");
        assertEquals("父亲", r.getRelationship());
    }

    @Test
    void shouldSetAndGetDiseaseName() {
        FamilyHistoryRequest r = new FamilyHistoryRequest();
        r.setDiseaseName("高血压");
        assertEquals("高血压", r.getDiseaseName());
    }

    @Test
    void shouldSetAndGetNote() {
        FamilyHistoryRequest r = new FamilyHistoryRequest();
        r.setNote("确诊于2020年");
        assertEquals("确诊于2020年", r.getNote());
    }
}
