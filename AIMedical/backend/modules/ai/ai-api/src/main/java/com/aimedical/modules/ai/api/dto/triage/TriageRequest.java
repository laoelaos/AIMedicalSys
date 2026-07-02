package com.aimedical.modules.ai.api.dto.triage;

import lombok.Data;

import java.util.List;

@Data
public class TriageRequest {

    private String chiefComplaint;
    private String sessionId;
    private List<AdditionalResponseItem> additionalResponses;

    // Upstream consultation module compatibility
    private String patientId;
    private String ruleVersion;
    private String ruleSetId;
    private String additionalResponsesText;
    private String correctedChiefComplaint;

    public TriageRequest() {
    }
}
