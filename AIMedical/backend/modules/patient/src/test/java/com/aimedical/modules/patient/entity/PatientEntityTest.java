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
    void shouldHandleAllFieldsViaGettersAndSetters() {
        PatientEntity entity = new PatientEntity();

        entity.setUserId(1001L);
        entity.setRealName("Alice Wang");
        entity.setGender("FEMALE");
        entity.setAvatarUrl("https://cdn.example.com/avatar/1001.png");
        entity.setPhone("13800001111");

        assertEquals(1001L, entity.getUserId());
        assertEquals("Alice Wang", entity.getRealName());
        assertEquals("FEMALE", entity.getGender());
        assertEquals("https://cdn.example.com/avatar/1001.png", entity.getAvatarUrl());
        assertEquals("13800001111", entity.getPhone());
    }

    @Test
    void shouldInvokeHashCodeAndEquals() {
        PatientEntity a = new PatientEntity();
        a.setUserId(1L);
        a.setRealName("Bob");

        PatientEntity b = new PatientEntity();
        b.setUserId(1L);
        b.setRealName("Bob");

        a.hashCode();
        b.hashCode();
        a.equals(b);
        b.equals(a);
        a.equals(a);
    }

    @Test
    void shouldGenerateNonEmptyToString() {
        PatientEntity entity = new PatientEntity();
        entity.setRealName("Carol");
        assertTrue(entity.toString().contains("Carol"));
    }
}