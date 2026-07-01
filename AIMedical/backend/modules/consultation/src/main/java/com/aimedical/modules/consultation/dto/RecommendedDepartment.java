package com.aimedical.modules.consultation.dto;

public class RecommendedDepartment {

    private String departmentId;
    private String departmentName;
    private float score;

    public RecommendedDepartment() {
    }

    public RecommendedDepartment(Integer departmentId, String departmentName, Integer score) {
        this.departmentId = String.valueOf(departmentId);
        this.departmentName = departmentName;
        this.score = score != null ? score.floatValue() : 0f;
    }

    public RecommendedDepartment(String departmentId, String departmentName, Integer score) {
        this.departmentId = departmentId;
        this.departmentName = departmentName;
        this.score = score != null ? score.floatValue() : 0f;
    }

    public RecommendedDepartment(String departmentId, String departmentName, float score) {
        this.departmentId = departmentId;
        this.departmentName = departmentName;
        this.score = score;
    }

    public String getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(String departmentId) {
        this.departmentId = departmentId;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
    }

    public float getScore() {
        return score;
    }

    public void setScore(float score) {
        this.score = score;
    }
}
