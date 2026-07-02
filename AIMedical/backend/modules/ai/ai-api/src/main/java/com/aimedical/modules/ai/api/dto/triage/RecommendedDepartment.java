package com.aimedical.modules.ai.api.dto.triage;

public class RecommendedDepartment {

    private String departmentId;
    private String departmentName;
    private float score;

    public RecommendedDepartment() {
    }

    public String getDepartmentId() { return departmentId; }
    public void setDepartmentId(String v) { this.departmentId = v; }

    public String getDepartmentName() { return departmentName; }
    public void setDepartmentName(String v) { this.departmentName = v; }

    public float getScore() { return score; }
    public void setScore(float v) { this.score = v; }
}