package com.aimedical.modules.patient.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class AllergyHistoryTest {

    @Test
    void shouldHandleAllFieldsViaGettersAndSetters() {
        AllergyHistory entity = new AllergyHistory();
        LocalDate occurred = LocalDate.of(2024, 1, 15);

        entity.setHealthProfileId(100L);
        entity.setAllergen("penicillin");
        entity.setReactionType("rash");
        entity.setSeverity("SEVERE");
        entity.setOccurredAt(occurred);
        entity.setNote("patient carries epinephrine");
        entity.setRemark("first occurrence at age 12");

        assertEquals(100L, entity.getHealthProfileId());
        assertEquals("penicillin", entity.getAllergen());
        assertEquals("rash", entity.getReactionType());
        assertEquals("SEVERE", entity.getSeverity());
        assertEquals(occurred, entity.getOccurredAt());
        assertEquals("patient carries epinephrine", entity.getNote());
        assertEquals("first occurrence at age 12", entity.getRemark());
    }

    @Test
    void shouldSupportEqualsAcrossAllBranches() {
        AllergyHistory a = new AllergyHistory();
        a.setAllergen("pollen");
        a.setHealthProfileId(5L);

        AllergyHistory same = new AllergyHistory();
        same.setAllergen("pollen");
        same.setHealthProfileId(5L);

        AllergyHistory different = new AllergyHistory();
        different.setAllergen("nuts");
        different.setHealthProfileId(9L);

        assertTrue(a.equals(a));
        assertTrue(a.equals(same));
        assertEquals(a.hashCode(), same.hashCode());
        assertFalse(a.equals(different));
        assertFalse(a.equals(null));
        assertFalse(a.equals("not an AllergyHistory"));
    }

    @Test
    void shouldGenerateNonEmptyToString() {
        AllergyHistory entity = new AllergyHistory();
        entity.setAllergen("nuts");
        assertTrue(entity.toString().contains("nuts"));
    }
}