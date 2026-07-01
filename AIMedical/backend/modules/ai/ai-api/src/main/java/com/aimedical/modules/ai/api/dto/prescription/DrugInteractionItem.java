package com.aimedical.modules.ai.api.dto.prescription;

public class DrugInteractionItem {

    private String drugPair;
    private String severity;
    private String description;

    public DrugInteractionItem() {
    }

    public String getDrugPair() {
        return drugPair;
    }

    public void setDrugPair(String drugPair) {
        this.drugPair = drugPair;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
