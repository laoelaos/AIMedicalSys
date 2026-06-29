package com.aimedical.modules.patient.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GenderTest {

    @Test
    void shouldDefineThreeGenderValues() {
        assertEquals(3, Gender.values().length);
        assertNotNull(Gender.valueOf("MALE"));
        assertNotNull(Gender.valueOf("FEMALE"));
        assertNotNull(Gender.valueOf("UNKNOWN"));
    }

    @Test
    void shouldExposeCodeAndDescForMale() {
        assertEquals("MALE", Gender.MALE.getCode());
        assertEquals("男", Gender.MALE.getDesc());
    }

    @Test
    void shouldExposeCodeAndDescForFemale() {
        assertEquals("FEMALE", Gender.FEMALE.getCode());
        assertEquals("女", Gender.FEMALE.getDesc());
    }

    @Test
    void shouldExposeCodeAndDescForUnknown() {
        assertEquals("UNKNOWN", Gender.UNKNOWN.getCode());
        assertEquals("未知", Gender.UNKNOWN.getDesc());
    }
}