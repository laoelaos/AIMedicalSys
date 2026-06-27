package com.aimedical.modules.patient.entity;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class HealthProfileTest {

    @Test
    void shouldHandleAllFieldsViaGettersAndSetters() {
        HealthProfile entity = new HealthProfile();
        BigDecimal height = new BigDecimal("175.5");
        BigDecimal weight = new BigDecimal("70.2");
        BigDecimal bmi = new BigDecimal("22.8");

        entity.setPatientId(999L);
        entity.setBloodType("A");
        entity.setHeightCm(height);
        entity.setWeightKg(weight);
        entity.setBmi(bmi);
        entity.setMaritalStatus("single");
        entity.setLifestyleNote("non-smoker, occasional alcohol");

        assertEquals(999L, entity.getPatientId());
        assertEquals("A", entity.getBloodType());
        assertEquals(height, entity.getHeightCm());
        assertEquals(weight, entity.getWeightKg());
        assertEquals(bmi, entity.getBmi());
        assertEquals("single", entity.getMaritalStatus());
        assertEquals("non-smoker, occasional alcohol", entity.getLifestyleNote());
    }

    @Test
    void shouldSupportEqualsAcrossAllBranches() {
        HealthProfile a = new HealthProfile();
        a.setPatientId(1L);

        HealthProfile same = new HealthProfile();
        same.setPatientId(1L);

        HealthProfile different = new HealthProfile();
        different.setPatientId(2L);

        assertTrue(a.equals(a));
        assertTrue(a.equals(same));
        assertEquals(a.hashCode(), same.hashCode());
        assertFalse(a.equals(different));
        assertFalse(a.equals(null));
        assertFalse(a.equals("not a HealthProfile"));
    }

    @Test
    void shouldGenerateNonEmptyToString() {
        HealthProfile entity = new HealthProfile();
        entity.setBloodType("O");
        assertTrue(entity.toString().contains("O"));
    }
}