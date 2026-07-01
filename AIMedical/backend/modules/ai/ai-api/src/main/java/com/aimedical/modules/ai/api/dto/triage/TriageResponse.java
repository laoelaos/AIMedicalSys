package com.aimedical.modules.ai.api.dto.triage;

import java.util.ArrayList;
import java.util.List;

public class TriageResponse {

    private String sessionId;
    private boolean completed;
    private boolean degraded;
    private String question;
    private List<RecommendedDepartment> departments;
    private List<RecommendedDoctor> doctors;
    private String reason;

    // Upstream consultation module compatibility
    private String correctedChiefComplaint;
    private Float confidence;
    private String followUpQuestion;
    private boolean needFollowUp;

    public TriageResponse() {
    }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String v) { this.sessionId = v; }

    public boolean getIsComplete() { return completed; }
    public void setComplete(boolean v) { this.completed = v; }

    public boolean getIsDegraded() { return degraded; }
    public void setDegraded(boolean v) { this.degraded = v; }

    public String getQuestion() { return question; }
    public void setQuestion(String v) { this.question = v; }

    public List<RecommendedDepartment> getDepartments() { return departments; }
    public void setDepartments(List<RecommendedDepartment> v) { this.departments = v; }

    public List<RecommendedDoctor> getDoctors() { return doctors; }
    public void setDoctors(List<RecommendedDoctor> v) { this.doctors = v; }

    public String getReason() { return reason; }
    public void setReason(String v) { this.reason = v; }

    // Upstream compatibility aliases
    public List<RecommendedDepartment> getRecommendedDepartments() { return departments; }
    public void setRecommendedDepartments(List<RecommendedDepartment> v) { this.departments = v; }

    public List<RecommendedDoctor> getRecommendedDoctors() { return doctors; }
    public void setRecommendedDoctors(List<RecommendedDoctor> v) { this.doctors = v; }

    public boolean isNeedFollowUp() { return needFollowUp; }
    public void setNeedFollowUp(boolean v) { this.needFollowUp = v; }

    public boolean isComplete() { return getIsComplete(); }
    public boolean isDegraded() { return getIsDegraded(); }

    public String getCorrectedChiefComplaint() { return correctedChiefComplaint; }
    public void setCorrectedChiefComplaint(String v) { this.correctedChiefComplaint = v; }

    public Float getConfidence() { return confidence; }
    public void setConfidence(Float v) { this.confidence = v; }
    public void setConfidence(float v) { this.confidence = v; }

    public String getFollowUpQuestion() { return followUpQuestion; }
    public void setFollowUpQuestion(String v) { this.followUpQuestion = v; }

    private List<MatchedRuleItem> matchedRules = new ArrayList<>();

    public List<MatchedRuleItem> getMatchedRules() { return matchedRules; }
    public void setMatchedRules(List<MatchedRuleItem> v) { this.matchedRules = v; }
}
