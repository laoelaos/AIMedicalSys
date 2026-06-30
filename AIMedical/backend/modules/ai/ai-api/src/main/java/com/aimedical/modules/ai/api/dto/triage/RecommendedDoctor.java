package com.aimedical.modules.ai.api.dto.triage;

public class RecommendedDoctor {

    private Integer doctorId;
    private String doctorName;
    private Integer availableSlotCount;
    private Integer score;

    public RecommendedDoctor() {
    }

    public Integer getDoctorId() {
        return doctorId;
    }

    public void setDoctorId(Integer doctorId) {
        this.doctorId = doctorId;
    }

    public String getDoctorName() {
        return doctorName;
    }

    public void setDoctorName(String doctorName) {
        this.doctorName = doctorName;
    }

    public Integer getAvailableSlotCount() {
        return availableSlotCount;
    }

    public void setAvailableSlotCount(Integer availableSlotCount) {
        this.availableSlotCount = availableSlotCount;
    }

    public Integer getScore() {
        return score;
    }

    public void setScore(Integer score) {
        this.score = score;
    }
}
