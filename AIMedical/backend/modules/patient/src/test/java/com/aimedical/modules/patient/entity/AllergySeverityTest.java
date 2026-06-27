package com.aimedical.modules.patient.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AllergySeverityTest {

    @Test
    void shouldDefineThreeSeverityLevels() {
        assertEquals(3, AllergySeverity.values().length);
        assertNotNull(AllergySeverity.valueOf("MILD"));
        assertNotNull(AllergySeverity.valueOf("MODERATE"));
        assertNotNull(AllergySeverity.valueOf("SEVERE"));
    }

    @Test
    void shouldExposeCodeAndDescForMild() {
        assertEquals("MILD", AllergySeverity.MILD.getCode());
        assertEquals("轻度", AllergySeverity.MILD.getDesc());
    }

    @Test
    void shouldExposeCodeAndDescForModerate() {
        assertEquals("MODERATE", AllergySeverity.MODERATE.getCode());
        assertEquals("中度", AllergySeverity.MODERATE.getDesc());
    }

    @Test
    void shouldExposeCodeAndDescForSevere() {
        assertEquals("SEVERE", AllergySeverity.SEVERE.getCode());
        assertEquals("重度", AllergySeverity.SEVERE.getDesc());
    }
}