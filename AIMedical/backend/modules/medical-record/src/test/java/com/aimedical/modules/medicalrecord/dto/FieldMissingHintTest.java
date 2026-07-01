package com.aimedical.modules.medicalrecord.dto;

import com.aimedical.modules.medicalrecord.enums.MedicalRecordField;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FieldMissingHintTest {

    @Test
    void shouldCreateWithDefaultValues() {
        FieldMissingHint hint = new FieldMissingHint();
        assertNull(hint.getMissingField());
        assertNull(hint.getPromptMessage());
        assertNull(hint.getSuggestedAction());
    }

    @Test
    void shouldSetAndGetAllFields() {
        FieldMissingHint hint = new FieldMissingHint();
        hint.setMissingField(MedicalRecordField.CHIEF_COMPLAINT);
        hint.setPromptMessage("主诉字段缺失");
        hint.setSuggestedAction("请补充主诉信息");

        assertEquals(MedicalRecordField.CHIEF_COMPLAINT, hint.getMissingField());
        assertEquals("主诉字段缺失", hint.getPromptMessage());
        assertEquals("请补充主诉信息", hint.getSuggestedAction());
    }
}
