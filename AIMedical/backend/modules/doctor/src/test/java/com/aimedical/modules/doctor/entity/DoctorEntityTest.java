package com.aimedical.modules.doctor.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DoctorEntityTest {

    @Test
    void shouldCreateWithDefaultConstructor() {
        DoctorEntity entity = new DoctorEntity();
        assertNotNull(entity);
    }

    @Test
    void shouldExtendBaseEntity() {
        DoctorEntity entity = new DoctorEntity();
        assertNull(entity.getId());
        assertFalse(entity.getDeleted());
    }
}
