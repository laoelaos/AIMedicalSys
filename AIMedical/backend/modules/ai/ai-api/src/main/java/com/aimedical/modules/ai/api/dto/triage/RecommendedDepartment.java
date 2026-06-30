package com.aimedical.modules.ai.api.dto.triage;

public class RecommendedDepartment {

    private Integer departmentId;
    private String departmentName;
    private Integer score;

    public RecommendedDepartment() {
    }

    public Integer getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(Integer departmentId) {
        this.departmentId = departmentId;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
    }

    public Integer getScore() {
        return score;
    }

    public void setScore(Integer score) {
        this.score = score;
    }
}
