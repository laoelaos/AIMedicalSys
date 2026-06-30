package com.aimedical.modules.ai.api.dto.triage;

import java.util.List;

public class TriageRequest {

    private String chiefComplaint;
    private String sessionId;
    private List<String> additionalResponses;

    public TriageRequest() {
    }

    public String getChiefComplaint() {
        return chiefComplaint;
    }

    public void setChiefComplaint(String chiefComplaint) {
        this.chiefComplaint = chiefComplaint;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public List<String> getAdditionalResponses() {
        return additionalResponses;
    }

    public void setAdditionalResponses(List<String> additionalResponses) {
        this.additionalResponses = additionalResponses;
    }
}
