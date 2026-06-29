package com.aimedical.modules.patient;

import com.aimedical.modules.patient.entity.Gender;
import com.aimedical.modules.patient.entity.PatientEntity;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Phase 1 patient module smoke test — verifies core entity instantiation.
 */
class PatientPlaceholderTest {

    @Test
    void shouldCreatePatientEntityWithDefaults() {
        PatientEntity entity = new PatientEntity();
        assertNull(entity.getId());
        assertNull(entity.getRealName());
        assertNotNull(entity.getAllergies());
        assertNotNull(entity.getChronicDiseases());
        assertNotNull(entity.getFamilyHistories());
        assertNotNull(entity.getSurgeryHistories());
        assertNotNull(entity.getMedicationHistories());
        assertTrue(entity.getAllergies().isEmpty());
    }

    @Test
    void shouldSetAndGetBasicFields() {
        PatientEntity entity = new PatientEntity();
        entity.setUserId(1001L);
        entity.setRealName("Test Patient");
        entity.setGender(Gender.MALE);
        entity.setPhone("13800138000");

        assertEquals(1001L, entity.getUserId());
        assertEquals("Test Patient", entity.getRealName());
        assertEquals(Gender.MALE, entity.getGender());
        assertEquals("13800138000", entity.getPhone());
    }
}
