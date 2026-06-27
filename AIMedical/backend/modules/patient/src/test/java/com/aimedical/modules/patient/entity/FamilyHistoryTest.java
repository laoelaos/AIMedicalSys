package com.aimedical.modules.patient.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FamilyHistoryTest {

    @Test
    void shouldHandleAllFieldsViaGettersAndSetters() {
        FamilyHistory entity = new FamilyHistory();

        entity.setHealthProfileId(300L);
        entity.setRelationship("father");
        entity.setDiseaseName("diabetes type 2");
        entity.setNote("diagnosed at age 55");
        entity.setRemark("controlled with diet");

        assertEquals(300L, entity.getHealthProfileId());
        assertEquals("father", entity.getRelationship());
        assertEquals("diabetes type 2", entity.getDiseaseName());
        assertEquals("diagnosed at age 55", entity.getNote());
        assertEquals("controlled with diet", entity.getRemark());
    }

    @Test
    void shouldSupportEqualsAcrossAllBranches() {
        FamilyHistory a = new FamilyHistory();
        a.setDiseaseName("cancer");
        a.setRelationship("mother");

        FamilyHistory same = new FamilyHistory();
        same.setDiseaseName("cancer");
        same.setRelationship("mother");

        FamilyHistory different = new FamilyHistory();
        different.setDiseaseName("diabetes");
        different.setRelationship("father");

        assertTrue(a.equals(a));
        assertTrue(a.equals(same));
        assertEquals(a.hashCode(), same.hashCode());
        assertFalse(a.equals(different));
        assertFalse(a.equals(null));
        assertFalse(a.equals("not a FamilyHistory"));
    }

    @Test
    void shouldGenerateNonEmptyToString() {
        FamilyHistory entity = new FamilyHistory();
        entity.setDiseaseName("heart disease");
        assertTrue(entity.toString().contains("heart disease"));
    }
}