package com.aimedical.modules.prescription.dto.audit;

import java.util.List;

import com.aimedical.modules.prescription.service.audit.AuditRiskLevel;

public class WarnResult {
    private AuditRiskLevel riskLevel;
    private List<WarnAlert> alerts;
    private Long auditRecordId;
    private String prescriptionHash;

    public WarnResult() {
    }

    public WarnResult(AuditRiskLevel riskLevel, List<WarnAlert> alerts, Long auditRecordId, String prescriptionHash) {
        this.riskLevel = riskLevel;
        this.alerts = alerts;
        this.auditRecordId = auditRecordId;
        this.prescriptionHash = prescriptionHash;
    }

    public AuditRiskLevel getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(AuditRiskLevel riskLevel) {
        this.riskLevel = riskLevel;
    }

    public List<WarnAlert> getAlerts() {
        return alerts;
    }

    public void setAlerts(List<WarnAlert> alerts) {
        this.alerts = alerts;
    }

    public Long getAuditRecordId() {
        return auditRecordId;
    }

    public void setAuditRecordId(Long auditRecordId) {
        this.auditRecordId = auditRecordId;
    }

    public String getPrescriptionHash() {
        return prescriptionHash;
    }

    public void setPrescriptionHash(String prescriptionHash) {
        this.prescriptionHash = prescriptionHash;
    }
}
