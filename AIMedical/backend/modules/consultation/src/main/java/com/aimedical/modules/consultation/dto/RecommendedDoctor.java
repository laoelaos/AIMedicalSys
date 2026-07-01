package com.aimedical.modules.consultation.dto;

public class RecommendedDoctor {

    private String doctorId;
    private String doctorName;
    private String departmentId;
    private int availableSlotCount;
    private float score;

    public RecommendedDoctor() {
    }

    public RecommendedDoctor(String doctorId, String doctorName, String departmentId,
                              int availableSlotCount, float score) {
        this.doctorId = doctorId;
        this.doctorName = doctorName;
        this.departmentId = departmentId;
        this.availableSlotCount = availableSlotCount;
        this.score = score;
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
