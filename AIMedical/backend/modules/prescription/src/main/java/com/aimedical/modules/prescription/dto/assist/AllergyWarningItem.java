package com.aimedical.modules.prescription.dto.assist;

public class AllergyWarningItem {

    private String drugId;
    private String allergen;
    private AllergyWarningSeverity severity;

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

    public AllergyWarningSeverity getSeverity() {
        return severity;
    }

    public void setSeverity(AllergyWarningSeverity severity) {
        this.severity = severity;
    }
}
