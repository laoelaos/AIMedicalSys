package com.aimedical.modules.patient.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class SurgeryHistoryRequest {
    @NotBlank(message = "手术名称不能为空")
    @Size(max = 100, message = "手术名称不能超过100字符")
    private String surgeryName;
    private String surgeryAt;
    @Size(max = 100, message = "医院名称不能超过100字符")
    private String hospital;

    public String getSurgeryName() { return surgeryName; }
    public void setSurgeryName(String surgeryName) { this.surgeryName = surgeryName; }
    public String getSurgeryAt() { return surgeryAt; }
    public void setSurgeryAt(String surgeryAt) { this.surgeryAt = surgeryAt; }
    public String getHospital() { return hospital; }
    public void setHospital(String hospital) { this.hospital = hospital; }
}
