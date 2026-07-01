package com.aimedical.modules.prescription.dto.audit;

import com.aimedical.modules.patient.entity.AllergySeverity;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AllergyDetailTest {

    @Test
    void shouldSetAndGetFields() {
        AllergyDetail detail = new AllergyDetail();
        detail.setAllergen("Penicillin");
        detail.setReactionType("Rash");
        detail.setSeverity(AllergySeverity.MODERATE);
        detail.setOccurredAt("2024-01-01");

        assertEquals("Penicillin", detail.getAllergen());
        assertEquals("Rash", detail.getReactionType());
        assertEquals(AllergySeverity.MODERATE, detail.getSeverity());
        assertEquals("2024-01-01", detail.getOccurredAt());
    }
}
