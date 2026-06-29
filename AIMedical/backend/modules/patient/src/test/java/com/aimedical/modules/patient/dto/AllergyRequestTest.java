package com.aimedical.modules.patient.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AllergyRequestTest {

    @Test
    void shouldSetAndGetAllergen() {
        AllergyRequest r = new AllergyRequest();
        r.setAllergen("花粉");
        assertEquals("花粉", r.getAllergen());
    }

    @Test
    void shouldSetAndGetReactionType() {
        AllergyRequest r = new AllergyRequest();
        r.setReactionType("打喷嚏");
        assertEquals("打喷嚏", r.getReactionType());
    }

    @Test
    void shouldSetAndGetSeverity() {
        AllergyRequest r = new AllergyRequest();
        r.setSeverity("MILD");
        assertEquals("MILD", r.getSeverity());
    }

    @Test
    void shouldSetAndGetOccurredAt() {
        AllergyRequest r = new AllergyRequest();
        r.setOccurredAt("2024-01-01");
        assertEquals("2024-01-01", r.getOccurredAt());
    }
}
