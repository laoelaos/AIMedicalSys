package com.aimedical.modules.ai.api.dto.prescription;

public class AllergyWarningItem {

    private String drugId;
    private String allergen;
    private String severity;

    public AllergyWarningItem() {
    }

    public String getDrugId() {
        return drugId;
    }

    public void setDrugId(String drugId) {
        this.drugId = drugId;
    }

    public String getAllergen() {
        return allergen;
    }

    public void setAllergen(String allergen) {
        this.allergen = allergen;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }
}
