package com.aimedical.modules.patient.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AllergyResponseTest {

    @Test
    void shouldSetAndGetId() {
        AllergyResponse r = new AllergyResponse();
        r.setId(1L);
        assertEquals(1L, r.getId());
    }

    @Test
    void shouldSetAndGetAllergen() {
        AllergyResponse r = new AllergyResponse();
        r.setAllergen("青霉素");
        assertEquals("青霉素", r.getAllergen());
    }

    @Test
    void shouldSetAndGetReactionType() {
        AllergyResponse r = new AllergyResponse();
        r.setReactionType("皮疹");
        assertEquals("皮疹", r.getReactionType());
    }

    @Test
    void shouldSetAndGetSeverity() {
        AllergyResponse r = new AllergyResponse();
        r.setSeverity("MODERATE");
        assertEquals("MODERATE", r.getSeverity());
    }

    @Test
    void shouldSetAndGetOccurredAt() {
        AllergyResponse r = new AllergyResponse();
        r.setOccurredAt("2024-06-15");
        assertEquals("2024-06-15", r.getOccurredAt());
    }
}
