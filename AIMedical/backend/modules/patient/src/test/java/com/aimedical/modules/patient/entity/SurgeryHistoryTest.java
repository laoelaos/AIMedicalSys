package com.aimedical.modules.patient.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class SurgeryHistoryTest {

    @Test
    void shouldHandleAllFieldsViaGettersAndSetters() {
        SurgeryHistory entity = new SurgeryHistory();
        LocalDate surgeryAt = LocalDate.of(2018, 7, 20);

        entity.setHealthProfileId(500L);
        entity.setSurgeryName("appendectomy");
        entity.setSurgeryAt(surgeryAt);
        entity.setHospital("Central Hospital");
        entity.setRemark("uncomplicated recovery");

        assertEquals(500L, entity.getHealthProfileId());
        assertEquals("appendectomy", entity.getSurgeryName());
        assertEquals(surgeryAt, entity.getSurgeryAt());
        assertEquals("Central Hospital", entity.getHospital());
        assertEquals("uncomplicated recovery", entity.getRemark());
    }

    @Test
    void shouldSupportEqualsAcrossAllBranches() {
        SurgeryHistory a = new SurgeryHistory();
        a.setSurgeryName("knee arthroscopy");
        a.setHealthProfileId(11L);

        SurgeryHistory same = new SurgeryHistory();
        same.setSurgeryName("knee arthroscopy");
        same.setHealthProfileId(11L);

        SurgeryHistory different = new SurgeryHistory();
        different.setSurgeryName("appendectomy");
        different.setHealthProfileId(12L);

        assertTrue(a.equals(a));
        assertTrue(a.equals(same));
        assertEquals(a.hashCode(), same.hashCode());
        assertFalse(a.equals(different));
        assertFalse(a.equals(null));
        assertFalse(a.equals("not a SurgeryHistory"));
    }

    @Test
    void shouldGenerateNonEmptyToString() {
        SurgeryHistory entity = new SurgeryHistory();
        entity.setSurgeryName("cholecystectomy");
        assertTrue(entity.toString().contains("cholecystectomy"));
    }
}