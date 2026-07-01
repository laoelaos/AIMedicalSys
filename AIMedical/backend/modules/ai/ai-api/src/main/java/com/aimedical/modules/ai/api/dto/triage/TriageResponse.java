package com.aimedical.modules.ai.api.dto.triage;

import java.util.List;

public class TriageResponse {

    private List<RecommendedDepartment> recommendedDepartments;
    private String reason;
    private List<RecommendedDoctor> recommendedDoctors;
    private List<MatchedRuleItem> matchedRules;
    private boolean needFollowUp;
    private String followUpQuestion;
    private Float confidence;
    private boolean degraded;
    private String sessionId;
    private String correctedChiefComplaint;

    public TriageResponse() {
    }

    public List<RecommendedDepartment> getRecommendedDepartments() {
        return recommendedDepartments;
    }

    public void setRecommendedDepartments(List<RecommendedDepartment> recommendedDepartments) {
        this.recommendedDepartments = recommendedDepartments;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public List<RecommendedDoctor> getRecommendedDoctors() {
        return recommendedDoctors;
    }

    public void setRecommendedDoctors(List<RecommendedDoctor> recommendedDoctors) {
        this.recommendedDoctors = recommendedDoctors;
    }

    public List<MatchedRuleItem> getMatchedRules() {
        return matchedRules;
    }

    public void setMatchedRules(List<MatchedRuleItem> matchedRules) {
        this.matchedRules = matchedRules;
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

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getCorrectedChiefComplaint() {
        return correctedChiefComplaint;
    }

    public void setCorrectedChiefComplaint(String correctedChiefComplaint) {
        this.correctedChiefComplaint = correctedChiefComplaint;
    }
}
