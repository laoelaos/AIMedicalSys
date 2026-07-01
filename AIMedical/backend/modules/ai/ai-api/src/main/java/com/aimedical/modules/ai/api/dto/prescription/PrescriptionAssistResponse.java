package com.aimedical.modules.ai.api.dto.prescription;

import java.util.List;

public class PrescriptionAssistResponse {

    private String prescriptionDraft;
    private List<DoseWarningItem> doseWarnings;
    private List<AllergyWarningItem> allergyWarnings;
    private String errorCode;
    private boolean disclaimerRequired;

    public PrescriptionAssistResponse() {
    }

    public String getPrescriptionDraft() {
        return prescriptionDraft;
    }

    public void setPrescriptionDraft(String prescriptionDraft) {
        this.prescriptionDraft = prescriptionDraft;
    }

    public List<DoseWarningItem> getDoseWarnings() {
        return doseWarnings;
    }

    public void setDoseWarnings(List<DoseWarningItem> doseWarnings) {
        this.doseWarnings = doseWarnings;
    }

    public List<AllergyWarningItem> getAllergyWarnings() {
        return allergyWarnings;
    }

    public void setAllergyWarnings(List<AllergyWarningItem> allergyWarnings) {
        this.allergyWarnings = allergyWarnings;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public boolean isDisclaimerRequired() {
        return disclaimerRequired;
    }

    public void setDisclaimerRequired(boolean disclaimerRequired) {
        this.disclaimerRequired = disclaimerRequired;
    }
}
