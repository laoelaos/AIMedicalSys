package com.aimedical.modules.patient.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class MedicationHistoryTest {

    @Test
    void shouldHandleAllFieldsViaGettersAndSetters() {
        MedicationHistory entity = new MedicationHistory();
        LocalDate started = LocalDate.of(2023, 3, 1);
        LocalDate ended = LocalDate.of(2023, 9, 1);

        entity.setHealthProfileId(400L);
        entity.setDrugName("metformin");
        entity.setReason("type 2 diabetes");
        entity.setStartedAt(started);
        entity.setEndedAt(ended);
        entity.setRemark("500mg twice daily");

        assertEquals(400L, entity.getHealthProfileId());
        assertEquals("metformin", entity.getDrugName());
        assertEquals("type 2 diabetes", entity.getReason());
        assertEquals(started, entity.getStartedAt());
        assertEquals(ended, entity.getEndedAt());
        assertEquals("500mg twice daily", entity.getRemark());
    }

    @Test
    void shouldSupportEqualsAcrossAllBranches() {
        MedicationHistory a = new MedicationHistory();
        a.setDrugName("aspirin");
        a.setHealthProfileId(7L);

        MedicationHistory same = new MedicationHistory();
        same.setDrugName("aspirin");
        same.setHealthProfileId(7L);

        MedicationHistory different = new MedicationHistory();
        different.setDrugName("ibuprofen");
        different.setHealthProfileId(8L);

        assertTrue(a.equals(a));
        assertTrue(a.equals(same));
        assertEquals(a.hashCode(), same.hashCode());
        assertFalse(a.equals(different));
        assertFalse(a.equals(null));
        assertFalse(a.equals("not a MedicationHistory"));
    }

    @Test
    void shouldGenerateNonEmptyToString() {
        MedicationHistory entity = new MedicationHistory();
        entity.setDrugName("ibuprofen");
        assertTrue(entity.toString().contains("ibuprofen"));
    }
}