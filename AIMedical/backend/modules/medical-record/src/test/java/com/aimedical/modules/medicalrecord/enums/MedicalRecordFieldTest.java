package com.aimedical.modules.medicalrecord.enums;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MedicalRecordFieldTest {

    @Test
    void shouldHaveNineConstants() {
        assertEquals(9, MedicalRecordField.values().length);
    }

    @Test
    void shouldContainAllExpectedFields() {
        assertNotNull(MedicalRecordField.valueOf("CHIEF_COMPLAINT"));
        assertNotNull(MedicalRecordField.valueOf("SYMPTOM_DESCRIPTION"));
        assertNotNull(MedicalRecordField.valueOf("PRESENT_ILLNESS"));
        assertNotNull(MedicalRecordField.valueOf("PAST_HISTORY"));
        assertNotNull(MedicalRecordField.valueOf("PHYSICAL_EXAM"));
        assertNotNull(MedicalRecordField.valueOf("PRELIMINARY_DIAGNOSIS"));
        assertNotNull(MedicalRecordField.valueOf("TREATMENT_PLAN"));
        assertNotNull(MedicalRecordField.valueOf("MISSING_FIELDS"));
        assertNotNull(MedicalRecordField.valueOf("PARTIAL_CONTENT"));
    }

    @Test
    void shouldReturnCorrectEnumNames() {
        assertEquals("CHIEF_COMPLAINT", MedicalRecordField.CHIEF_COMPLAINT.name());
        assertEquals("TREATMENT_PLAN", MedicalRecordField.TREATMENT_PLAN.name());
    }

    @Test
    void shouldThrowForInvalidName() {
        assertThrows(IllegalArgumentException.class, () -> MedicalRecordField.valueOf("INVALID_FIELD"));
    }
}
