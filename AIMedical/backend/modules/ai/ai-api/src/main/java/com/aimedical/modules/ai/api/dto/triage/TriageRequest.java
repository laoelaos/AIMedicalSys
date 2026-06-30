package com.aimedical.modules.ai.api.dto.triage;

import lombok.Data;

import java.util.List;

@Data
public class TriageRequest {

    private String chiefComplaint;
    private String sessionId;
    private List<String> additionalResponses;

    public TriageRequest() {
    }
}
