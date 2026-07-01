package com.aimedical.modules.prescription.rule;

import com.aimedical.modules.prescription.service.audit.AuditRiskLevel;

public class LocalRuleResult {

    private String ruleId;
    private boolean passed;
    private String message;
    private AuditRiskLevel severity;

    public LocalRuleResult() {
    }

    public LocalRuleResult(String ruleId, boolean passed, String message, AuditRiskLevel severity) {
        this.ruleId = ruleId;
        this.passed = passed;
        this.message = message;
        this.severity = severity;
    }

    public String getRuleId() {
        return ruleId;
    }

    public void setRuleId(String ruleId) {
        this.ruleId = ruleId;
    }

    public boolean isPassed() {
        return passed;
    }

    public void setPassed(boolean passed) {
        this.passed = passed;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public AuditRiskLevel getSeverity() {
        return severity;
    }

    public void setSeverity(AuditRiskLevel severity) {
        this.severity = severity;
    }
}
