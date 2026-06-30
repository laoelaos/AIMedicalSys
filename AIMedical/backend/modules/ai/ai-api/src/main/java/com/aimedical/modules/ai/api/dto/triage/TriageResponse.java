package com.aimedical.modules.ai.api.dto.triage;

import java.util.List;

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

    public String getSessionId() { return sessionId; }
    public void setSessionId(String v) { this.sessionId = v; }

    public boolean isComplete() { return isComplete; }
    public void setComplete(boolean v) { isComplete = v; }

    public boolean isDegraded() { return isDegraded; }
    public void setDegraded(boolean v) { isDegraded = v; }

    public String getQuestion() { return question; }
    public void setQuestion(String v) { this.question = v; }

    public List<RecommendedDepartment> getDepartments() { return departments; }
    public void setDepartments(List<RecommendedDepartment> v) { this.departments = v; }

    public List<RecommendedDoctor> getDoctors() { return doctors; }
    public void setDoctors(List<RecommendedDoctor> v) { this.doctors = v; }

    public String getReason() { return reason; }
    public void setReason(String v) { this.reason = v; }
}
