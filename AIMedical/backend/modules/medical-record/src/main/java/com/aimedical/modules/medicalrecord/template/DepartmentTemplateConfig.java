package com.aimedical.modules.medicalrecord.template;

import java.util.Map;
import java.util.Set;

import com.aimedical.modules.medicalrecord.enums.MedicalRecordField;

public class DepartmentTemplateConfig {
    private String departmentId;
    private Set<MedicalRecordField> requiredFields;
    private Map<MedicalRecordField, String> promptMessages;
    private Map<MedicalRecordField, String> suggestedActions;

    public DepartmentTemplateConfig(String departmentId, Set<MedicalRecordField> requiredFields,
                                     Map<MedicalRecordField, String> promptMessages,
                                     Map<MedicalRecordField, String> suggestedActions) {
        this.departmentId = departmentId;
        this.requiredFields = requiredFields;
        this.promptMessages = promptMessages;
        this.suggestedActions = suggestedActions;
    }

    public String getDepartmentId() {
        return departmentId;
    }

    public Set<MedicalRecordField> getRequiredFields() {
        return requiredFields;
    }

    public Map<MedicalRecordField, String> getPromptMessages() {
        return promptMessages;
    }

    public Map<MedicalRecordField, String> getSuggestedActions() {
        return suggestedActions;
    }
}
