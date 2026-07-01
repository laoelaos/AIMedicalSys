package com.aimedical.modules.ai.api.dto.triage;

import java.util.List;

public class TriageRequest {

    private String chiefComplaint;
    private List<AdditionalResponseItem> additionalResponses;
    private String patientId;
    private String sessionId;
    private String ruleVersion;
    private String ruleSetId;
    private String correctedChiefComplaint;
    private String additionalResponsesText;

    public TriageRequest() {
    }

    public String getChiefComplaint() {
        return chiefComplaint;
    }

    public void setChiefComplaint(String chiefComplaint) {
        this.chiefComplaint = chiefComplaint;
    }

    public List<AdditionalResponseItem> getAdditionalResponses() {
        return additionalResponses;
    }

    public void setAdditionalResponses(List<AdditionalResponseItem> additionalResponses) {
        this.additionalResponses = additionalResponses;
    }

    public String getPatientId() {
        return patientId;
    }

    public void setPatientId(String patientId) {
        this.patientId = patientId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getRuleVersion() {
        return ruleVersion;
    }

    public void setRuleVersion(String ruleVersion) {
        this.ruleVersion = ruleVersion;
    }

    public String getRuleSetId() {
        return ruleSetId;
    }

    public void setRuleSetId(String ruleSetId) {
        this.ruleSetId = ruleSetId;
    }

    public String getCorrectedChiefComplaint() {
        return correctedChiefComplaint;
    }

    public void setCorrectedChiefComplaint(String correctedChiefComplaint) {
        this.correctedChiefComplaint = correctedChiefComplaint;
    }

    public String getAdditionalResponsesText() { return additionalResponsesText; }

    public void setAdditionalResponsesText(String additionalResponsesText) { this.additionalResponsesText = additionalResponsesText; }
}
