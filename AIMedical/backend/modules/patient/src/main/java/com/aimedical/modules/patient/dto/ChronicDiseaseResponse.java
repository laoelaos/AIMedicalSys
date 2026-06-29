package com.aimedical.modules.patient.dto;

public class ChronicDiseaseResponse {
    private Long id;
    private String diseaseName;
    private String diagnosedAt;
    private String currentStatus;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getDiseaseName() { return diseaseName; }
    public void setDiseaseName(String diseaseName) { this.diseaseName = diseaseName; }
    public String getDiagnosedAt() { return diagnosedAt; }
    public void setDiagnosedAt(String diagnosedAt) { this.diagnosedAt = diagnosedAt; }
    public String getCurrentStatus() { return currentStatus; }
    public void setCurrentStatus(String currentStatus) { this.currentStatus = currentStatus; }
}
