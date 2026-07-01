package com.aimedical.modules.prescription.dto.assist;

import java.math.BigDecimal;

public class DosageAlert {

    private DosageAlertLevel alertLevel;
    private DoseWarningType warningType;
    private String message;
    private String drugCode;
    private double currentDose;
    private BigDecimal suggestedValue;
    private String errorCode;

    public DosageAlert() {
    }

    public DosageAlertLevel getAlertLevel() {
        return alertLevel;
    }

    public void setAlertLevel(DosageAlertLevel alertLevel) {
        this.alertLevel = alertLevel;
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

    public String getDrugCode() {
        return drugCode;
    }

    public void setDrugCode(String drugCode) {
        this.drugCode = drugCode;
    }

    public double getCurrentDose() {
        return currentDose;
    }

    public void setCurrentDose(double currentDose) {
        this.currentDose = currentDose;
    }

    public BigDecimal getSuggestedValue() {
        return suggestedValue;
    }

    public void setSuggestedValue(BigDecimal suggestedValue) {
        this.suggestedValue = suggestedValue;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }
}
