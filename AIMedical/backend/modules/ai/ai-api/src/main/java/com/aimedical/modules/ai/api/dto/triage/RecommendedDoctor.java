package com.aimedical.modules.ai.api.dto.triage;

public class RecommendedDoctor {

    private String doctorId;
    private String doctorName;
    private Integer availableSlotCount = 0;
    private float score;

    public RecommendedDoctor() {
    }

    public String getDoctorId() { return doctorId; }
    public void setDoctorId(String v) { this.doctorId = v; }

    public String getDoctorName() { return doctorName; }
    public void setDoctorName(String v) { this.doctorName = v; }

    public Integer getAvailableSlotCount() { return availableSlotCount; }
    public void setAvailableSlotCount(Integer v) { this.availableSlotCount = v; }

    public float getScore() { return score; }
    public void setScore(float v) { this.score = v; }

    // Upstream compatibility
    private String departmentId;
    public String getDepartmentId() { return departmentId; }
    public void setDepartmentId(String v) { this.departmentId = v; }
}