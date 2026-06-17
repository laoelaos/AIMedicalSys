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
}
