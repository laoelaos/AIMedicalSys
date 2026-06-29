package com.aimedical.modules.patient.dto;

public class MedicationHistoryResponse {
    private Long id;
    private String drugName;
    private String reason;
    private String startedAt;
    private String endedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getDrugName() { return drugName; }
    public void setDrugName(String drugName) { this.drugName = drugName; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getStartedAt() { return startedAt; }
    public void setStartedAt(String startedAt) { this.startedAt = startedAt; }
    public String getEndedAt() { return endedAt; }
    public void setEndedAt(String endedAt) { this.endedAt = endedAt; }
}
