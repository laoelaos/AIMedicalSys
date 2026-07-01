package com.aimedical.modules.prescription.dto.assist;

import java.util.List;

public class PrescriptionAssistResponse {

    private String prescriptionDraft;
    private List<DoseWarning> doseWarnings;
    private List<AllergyWarningItem> allergyWarnings;
    private String errorCode;
    private boolean disclaimerRequired;
    private String prescriptionId;

    public PrescriptionAssistResponse() {
    }

    public String getPrescriptionDraft() {
        return prescriptionDraft;
    }

    public void setPrescriptionDraft(String prescriptionDraft) {
        this.prescriptionDraft = prescriptionDraft;
    }

    public List<DoseWarning> getDoseWarnings() {
        return doseWarnings;
    }

    public void setDoseWarnings(List<DoseWarning> doseWarnings) {
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

    public String getPrescriptionId() {
        return prescriptionId;
    }

    public void setPrescriptionId(String prescriptionId) {
        this.prescriptionId = prescriptionId;
    }
}
