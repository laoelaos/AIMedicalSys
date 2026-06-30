package com.aimedical.modules.ai.api.dto.triage;

import lombok.Data;

@Data
public class RecommendedDepartment {

    private Integer departmentId;
    private String departmentName;
    private Integer score;

    public RecommendedDepartment() {
    }
}
