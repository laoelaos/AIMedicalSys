package com.aimedical.modules.prescription.dto.audit;

import com.aimedical.modules.patient.entity.AllergySeverity;
import jakarta.validation.constraints.NotBlank;

public class AllergyDetail {

    @NotBlank
    private String allergen;

    private String reactionType;

    private AllergySeverity severity;

    private String occurredAt;

    public AllergyDetail() {
    }

    public String getAllergen() {
        return allergen;
    }

    public void setAllergen(String allergen) {
        this.allergen = allergen;
    }

    public String getReactionType() {
        return reactionType;
    }

    public void setReactionType(String reactionType) {
        this.reactionType = reactionType;
    }

    public AllergySeverity getSeverity() {
        return severity;
    }

    public void setSeverity(AllergySeverity severity) {
        this.severity = severity;
    }

    public String getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(String occurredAt) {
        this.occurredAt = occurredAt;
    }
}
