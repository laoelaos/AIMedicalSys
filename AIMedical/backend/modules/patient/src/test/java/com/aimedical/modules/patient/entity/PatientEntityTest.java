package com.aimedical.modules.patient.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PatientEntityTest {

    @Test
    void shouldCreateWithDefaultConstructor() {
        PatientEntity entity = new PatientEntity();
        assertNotNull(entity);
    }

    @Test
    void shouldExtendBaseEntity() {
        PatientEntity entity = new PatientEntity();
        assertNull(entity.getId());
        assertFalse(entity.getDeleted());
    }

    @Test
    void shouldHaveEmptyHealthRecordCollections() {
        PatientEntity entity = new PatientEntity();
        assertNotNull(entity.getAllergies());
        assertNotNull(entity.getChronicDiseases());
        assertNotNull(entity.getFamilyHistories());
        assertNotNull(entity.getSurgeryHistories());
        assertNotNull(entity.getMedicationHistories());
    }
}
