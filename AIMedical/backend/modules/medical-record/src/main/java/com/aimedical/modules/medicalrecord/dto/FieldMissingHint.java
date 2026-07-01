package com.aimedical.modules.medicalrecord.dto;

import com.aimedical.modules.medicalrecord.enums.MedicalRecordField;

public class FieldMissingHint {
    private MedicalRecordField missingField;
    private String promptMessage;
    private String suggestedAction;

    public MedicalRecordField getMissingField() {
        return missingField;
    }

    public void setMissingField(MedicalRecordField missingField) {
        this.missingField = missingField;
    }

    public String getPromptMessage() {
        return promptMessage;
    }

    public void setPromptMessage(String promptMessage) {
        this.promptMessage = promptMessage;
    }

    public String getSuggestedAction() {
        return suggestedAction;
    }

    public void setSuggestedAction(String suggestedAction) {
        this.suggestedAction = suggestedAction;
    }
}
