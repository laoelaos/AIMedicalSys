package com.aimedical.modules.patient.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ChronicDiseaseRequest {
    @NotBlank(message = "慢病名称不能为空")
    @Size(max = 100, message = "慢病名称不能超过100字符")
    private String diseaseName;
    private String diagnosedAt;
    private String currentStatus;

    public String getDiseaseName() { return diseaseName; }
    public void setDiseaseName(String diseaseName) { this.diseaseName = diseaseName; }
    public String getDiagnosedAt() { return diagnosedAt; }
    public void setDiagnosedAt(String diagnosedAt) { this.diagnosedAt = diagnosedAt; }
    public String getCurrentStatus() { return currentStatus; }
    public void setCurrentStatus(String currentStatus) { this.currentStatus = currentStatus; }
}
