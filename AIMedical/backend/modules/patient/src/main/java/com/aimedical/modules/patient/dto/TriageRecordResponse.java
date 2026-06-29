package com.aimedical.modules.patient.dto;

public class TriageRecordResponse {

    private Long id;
    private Long patientId;
    private String chiefComplaint;
    private String sessionId;
    private String recommendedDepartments;
    private String recommendedDoctors;
    private boolean isDegraded;
    private String ruleVersion;
    private String ruleSetId;
    private String matchedRules;
    private String createdAt;

    public Long getId() { return id; }
    public void setId(Long v) { this.id = v; }
    public Long getPatientId() { return patientId; }
    public void setPatientId(Long v) { this.patientId = v; }
    public String getChiefComplaint() { return chiefComplaint; }
    public void setChiefComplaint(String v) { this.chiefComplaint = v; }
    public String getSessionId() { return sessionId; }
    public void setSessionId(String v) { this.sessionId = v; }
    public String getRecommendedDepartments() { return recommendedDepartments; }
    public void setRecommendedDepartments(String v) { this.recommendedDepartments = v; }
    public String getRecommendedDoctors() { return recommendedDoctors; }
    public void setRecommendedDoctors(String v) { this.recommendedDoctors = v; }
    public boolean isDegraded() { return isDegraded; }
    public void setDegraded(boolean v) { this.isDegraded = v; }
    public String getRuleVersion() { return ruleVersion; }
    public void setRuleVersion(String v) { this.ruleVersion = v; }
    public String getRuleSetId() { return ruleSetId; }
    public void setRuleSetId(String v) { this.ruleSetId = v; }
    public String getMatchedRules() { return matchedRules; }
    public void setMatchedRules(String v) { this.matchedRules = v; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String v) { this.createdAt = v; }
}
