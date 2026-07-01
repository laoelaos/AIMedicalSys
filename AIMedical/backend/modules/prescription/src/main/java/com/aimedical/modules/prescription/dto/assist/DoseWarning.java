package com.aimedical.modules.prescription.dto.assist;

public class DoseWarning {

    private String drugId;
    private DoseWarningType warningType;
    private String message;
    private DosageAlertLevel severity;

    public DoseWarning() {
    }

    public String getDrugId() {
        return drugId;
    }

    public void setDrugId(String drugId) {
        this.drugId = drugId;
    }

    public DoseWarningType getWarningType() {
        return warningType;
    }

    public void setWarningType(DoseWarningType warningType) {
        this.warningType = warningType;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public DosageAlertLevel getSeverity() {
        return severity;
    }

    public void setSeverity(DosageAlertLevel severity) {
        this.severity = severity;
    }
}
