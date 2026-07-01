package com.aimedical.modules.ai.api.dto.triage;

public class RecommendedDoctor {

    private String doctorId;
    private String doctorName;
    private String departmentId;
    private int availableSlotCount;
    private float score;

    public RecommendedDoctor() {
    }

    public String getDoctorId() {
        return doctorId;
    }

    public void setDoctorId(String doctorId) {
        this.doctorId = doctorId;
    }

    public String getDoctorName() {
        return doctorName;
    }

    public void setDoctorName(String doctorName) {
        this.doctorName = doctorName;
    }

    public String getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(String departmentId) {
        this.departmentId = departmentId;
    }

    public int getAvailableSlotCount() {
        return availableSlotCount;
    }

    public void setAvailableSlotCount(int availableSlotCount) {
        this.availableSlotCount = availableSlotCount;
    }

    public float getScore() {
        return score;
    }

    public void setScore(float score) {
        this.score = score;
    }
}
