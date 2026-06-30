package com.aimedical.modules.ai.api.dto.triage;

import lombok.Data;

import java.util.List;

@Data
public class TriageResponse {

    private String sessionId;
    private boolean isComplete;
    private boolean isDegraded;
    private String question;
    private List<RecommendedDepartment> departments;
    private List<RecommendedDoctor> doctors;
    private String reason;

    public TriageResponse() {
    }
}
