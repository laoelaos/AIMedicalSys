package com.aimedical.modules.medicalrecord.template;

import com.aimedical.modules.medicalrecord.enums.MedicalRecordField;
import org.junit.jupiter.api.Test;
import java.util.Map;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

class DepartmentTemplateConfigTest {

    @Test
    void shouldCreateWithAllArgsConstructor() {
        Set<MedicalRecordField> requiredFields = Set.of(MedicalRecordField.CHIEF_COMPLAINT);
        Map<MedicalRecordField, String> promptMessages = Map.of(MedicalRecordField.CHIEF_COMPLAINT, "请描述主诉");
        Map<MedicalRecordField, String> suggestedActions = Map.of(MedicalRecordField.CHIEF_COMPLAINT, "询问患者");

        DepartmentTemplateConfig config = new DepartmentTemplateConfig("dept-01", requiredFields, promptMessages, suggestedActions);

        assertEquals("dept-01", config.getDepartmentId());
        assertEquals(requiredFields, config.getRequiredFields());
        assertEquals(promptMessages, config.getPromptMessages());
        assertEquals(suggestedActions, config.getSuggestedActions());
    }
}
