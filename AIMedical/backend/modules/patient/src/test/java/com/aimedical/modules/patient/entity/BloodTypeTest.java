package com.aimedical.modules.patient.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BloodTypeTest {

    @Test
    void shouldDefineFiveBloodTypes() {
        assertEquals(5, BloodType.values().length);
        assertNotNull(BloodType.valueOf("A"));
        assertNotNull(BloodType.valueOf("B"));
        assertNotNull(BloodType.valueOf("AB"));
        assertNotNull(BloodType.valueOf("O"));
        assertNotNull(BloodType.valueOf("UNKNOWN"));
    }

    @Test
    void shouldExposeCodeAndDescForA() {
        assertEquals("A", BloodType.A.getCode());
        assertEquals("A型", BloodType.A.getDesc());
    }

    @Test
    void shouldExposeCodeAndDescForB() {
        assertEquals("B", BloodType.B.getCode());
        assertEquals("B型", BloodType.B.getDesc());
    }

    @Test
    void shouldExposeCodeAndDescForAB() {
        assertEquals("AB", BloodType.AB.getCode());
        assertEquals("AB型", BloodType.AB.getDesc());
    }

    @Test
    void shouldExposeCodeAndDescForO() {
        assertEquals("O", BloodType.O.getCode());
        assertEquals("O型", BloodType.O.getDesc());
    }

    @Test
    void shouldExposeCodeAndDescForUnknown() {
        assertEquals("UNKNOWN", BloodType.UNKNOWN.getCode());
        assertEquals("未知", BloodType.UNKNOWN.getDesc());
    }
}