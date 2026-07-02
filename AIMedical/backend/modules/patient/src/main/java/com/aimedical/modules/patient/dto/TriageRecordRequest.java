package com.aimedical.modules.patient.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public class TriageRecordRequest {

    @NotNull
    private Long patientId;

    @NotBlank
    private String chiefComplaint;
    private String sessionId;
    private List<String> recommendedDepartments;
    private List<String> recommendedDoctors;
    private boolean degraded;
    private String ruleVersion;
    private String ruleSetId;
    private List<String> matchedRules;

    public Long getPatientId() { return patientId; }
    public void setPatientId(Long v) { this.patientId = v; }
    public String getChiefComplaint() { return chiefComplaint; }
    public void setChiefComplaint(String v) { this.chiefComplaint = v; }
    public String getSessionId() { return sessionId; }
    public void setSessionId(String v) { this.sessionId = v; }
    public List<String> getRecommendedDepartments() { return recommendedDepartments; }
    public void setRecommendedDepartments(List<String> v) { this.recommendedDepartments = v; }
    public List<String> getRecommendedDoctors() { return recommendedDoctors; }
    public void setRecommendedDoctors(List<String> v) { this.recommendedDoctors = v; }
    public boolean getIsDegraded() { return degraded; }
    public void setDegraded(boolean v) { this.degraded = v; }
    public String getRuleVersion() { return ruleVersion; }
    public void setRuleVersion(String v) { this.ruleVersion = v; }
    public String getRuleSetId() { return ruleSetId; }
    public void setRuleSetId(String v) { this.ruleSetId = v; }
    public List<String> getMatchedRules() { return matchedRules; }
    public void setMatchedRules(List<String> v) { this.matchedRules = v; }
}
