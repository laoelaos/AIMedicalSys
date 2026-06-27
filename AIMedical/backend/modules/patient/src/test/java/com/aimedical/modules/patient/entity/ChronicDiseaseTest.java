package com.aimedical.modules.patient.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class ChronicDiseaseTest {

    @Test
    void shouldHandleAllFieldsViaGettersAndSetters() {
        ChronicDisease entity = new ChronicDisease();
        LocalDate diagnosed = LocalDate.of(2020, 6, 1);

        entity.setHealthProfileId(200L);
        entity.setDiseaseName("hypertension");
        entity.setDiagnosedAt(diagnosed);
        entity.setCurrentStatus("STABLE");
        entity.setRemark("controlled with medication");

        assertEquals(200L, entity.getHealthProfileId());
        assertEquals("hypertension", entity.getDiseaseName());
        assertEquals(diagnosed, entity.getDiagnosedAt());
        assertEquals("STABLE", entity.getCurrentStatus());
        assertEquals("controlled with medication", entity.getRemark());
    }

    @Test
    void shouldSupportEqualsAcrossAllBranches() {
        ChronicDisease a = new ChronicDisease();
        a.setDiseaseName("diabetes");
        a.setHealthProfileId(10L);

        ChronicDisease same = new ChronicDisease();
        same.setDiseaseName("diabetes");
        same.setHealthProfileId(10L);

        ChronicDisease different = new ChronicDisease();
        different.setDiseaseName("hypertension");
        different.setHealthProfileId(11L);

        assertTrue(a.equals(a));
        assertTrue(a.equals(same));
        assertEquals(a.hashCode(), same.hashCode());
        assertFalse(a.equals(different));
        assertFalse(a.equals(null));
        assertFalse(a.equals("not a ChronicDisease"));
    }

    @Test
    void shouldGenerateNonEmptyToString() {
        ChronicDisease entity = new ChronicDisease();
        entity.setDiseaseName("asthma");
        assertTrue(entity.toString().contains("asthma"));
    }
}