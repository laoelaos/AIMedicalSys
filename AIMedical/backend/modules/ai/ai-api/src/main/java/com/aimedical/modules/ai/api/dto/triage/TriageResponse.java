package com.aimedical.modules.ai.api.dto.triage;

import java.util.List;

public class TriageResponse {

    private List<RecommendedDepartment> recommendedDepartments;
    private String reason;

    public TriageResponse() {
    }

    public List<RecommendedDepartment> getRecommendedDepartments() {
        return recommendedDepartments;
    }

    public void setRecommendedDepartments(List<RecommendedDepartment> recommendedDepartments) {
        this.recommendedDepartments = recommendedDepartments;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
