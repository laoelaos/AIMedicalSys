package com.aimedical.modules.ai.api.dto.triage;

import lombok.Data;

import java.util.List;

@Data
public class TriageResponse {

    private List<RecommendedDepartment> recommendedDepartments;
    private String reason;

    public TriageResponse() {
    }
}
