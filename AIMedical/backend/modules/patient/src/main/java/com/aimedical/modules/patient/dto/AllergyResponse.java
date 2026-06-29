package com.aimedical.modules.patient.dto;

public class AllergyResponse {
    private Long id;
    private String allergen;
    private String reactionType;
    private String severity;
    private String occurredAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getAllergen() { return allergen; }
    public void setAllergen(String allergen) { this.allergen = allergen; }
    public String getReactionType() { return reactionType; }
    public void setReactionType(String reactionType) { this.reactionType = reactionType; }
    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }
    public String getOccurredAt() { return occurredAt; }
    public void setOccurredAt(String occurredAt) { this.occurredAt = occurredAt; }
}
