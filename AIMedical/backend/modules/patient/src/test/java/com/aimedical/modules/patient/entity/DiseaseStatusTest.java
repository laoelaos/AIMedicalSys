package com.aimedical.modules.patient.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DiseaseStatusTest {

    @Test
    void shouldDefineThreeStatusValues() {
        assertEquals(3, DiseaseStatus.values().length);
        assertNotNull(DiseaseStatus.valueOf("STABLE"));
        assertNotNull(DiseaseStatus.valueOf("UNSTABLE"));
        assertNotNull(DiseaseStatus.valueOf("RECOVERED"));
    }

    @Test
    void shouldExposeCodeAndDescForStable() {
        assertEquals("STABLE", DiseaseStatus.STABLE.getCode());
        assertEquals("稳定", DiseaseStatus.STABLE.getDesc());
    }

    @Test
    void shouldExposeCodeAndDescForUnstable() {
        assertEquals("UNSTABLE", DiseaseStatus.UNSTABLE.getCode());
        assertEquals("不稳定", DiseaseStatus.UNSTABLE.getDesc());
    }

    @Test
    void shouldExposeCodeAndDescForRecovered() {
        assertEquals("RECOVERED", DiseaseStatus.RECOVERED.getCode());
        assertEquals("已康复", DiseaseStatus.RECOVERED.getDesc());
    }
}