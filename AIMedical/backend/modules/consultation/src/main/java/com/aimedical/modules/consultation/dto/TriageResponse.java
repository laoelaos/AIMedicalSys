package com.aimedical.modules.consultation.dto;

import java.util.List;

public class TriageResponse {

    private List<RecommendedDepartment> departments;
    private List<RecommendedDoctor> doctors;
    private String reason;
    private List<MatchedRule> matchedRules;
    private String sessionId;
    private boolean needFollowUp;
    private String followUpQuestion;
    private Float confidence;
    private boolean degraded;
    private String fallbackHint;
    private Boolean ruleVersionMismatch;
    private String correctedChiefComplaint;

    public TriageResponse() {
    }

    public List<RecommendedDepartment> getDepartments() {
        return departments;
    }

    public void setDepartments(List<RecommendedDepartment> departments) {
        this.departments = departments;
    }

    public List<RecommendedDoctor> getDoctors() {
        return doctors;
    }

    public void setDoctors(List<RecommendedDoctor> doctors) {
        this.doctors = doctors;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public List<MatchedRule> getMatchedRules() {
        return matchedRules;
    }

    public void setMatchedRules(List<MatchedRule> matchedRules) {
        this.matchedRules = matchedRules;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public boolean isNeedFollowUp() {
        return needFollowUp;
    }

    public void setNeedFollowUp(boolean needFollowUp) {
        this.needFollowUp = needFollowUp;
    }

    public String getFollowUpQuestion() {
        return followUpQuestion;
    }

    public void setFollowUpQuestion(String followUpQuestion) {
        this.followUpQuestion = followUpQuestion;
    }

    public Float getConfidence() {
        return confidence;
    }

    public void setConfidence(Float confidence) {
        this.confidence = confidence;
    }

    public boolean isDegraded() {
        return degraded;
    }

    public void setDegraded(boolean degraded) {
        this.degraded = degraded;
    }

    public String getFallbackHint() {
        return fallbackHint;
    }

    public void setFallbackHint(String fallbackHint) {
        this.fallbackHint = fallbackHint;
    }

    public Boolean getRuleVersionMismatch() {
        return ruleVersionMismatch;
    }

    public void setRuleVersionMismatch(Boolean ruleVersionMismatch) {
        this.ruleVersionMismatch = ruleVersionMismatch;
    }

    public String getCorrectedChiefComplaint() {
        return correctedChiefComplaint;
    }

    public void setCorrectedChiefComplaint(String correctedChiefComplaint) {
        this.correctedChiefComplaint = correctedChiefComplaint;
    }
}
