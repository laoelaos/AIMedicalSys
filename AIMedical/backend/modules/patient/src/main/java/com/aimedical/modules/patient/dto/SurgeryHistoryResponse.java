package com.aimedical.modules.patient.dto;

public class SurgeryHistoryResponse {
    private Long id;
    private String surgeryName;
    private String surgeryAt;
    private String hospital;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getSurgeryName() { return surgeryName; }
    public void setSurgeryName(String surgeryName) { this.surgeryName = surgeryName; }
    public String getSurgeryAt() { return surgeryAt; }
    public void setSurgeryAt(String surgeryAt) { this.surgeryAt = surgeryAt; }
    public String getHospital() { return hospital; }
    public void setHospital(String hospital) { this.hospital = hospital; }
}
