package com.aimedical.modules.prescription.entity;

import com.aimedical.common.base.BaseEntity;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalDateTime;

@Entity
@Table(name = "audit_record", indexes = {
    @Index(name = "idx_audit_prescription_id", columnList = "prescriptionId"),
    @Index(name = "idx_audit_order_is_latest", columnList = "prescriptionOrderId,isLatest")
})
@AttributeOverride(name = "id", column = @Column(name = "audit_id"))
public class AuditRecord extends BaseEntity {

    @Column(nullable = false)
    private String prescriptionId;

    private String prescriptionOrderId;

    private String doctorId;

    private String patientId;

    private LocalDateTime auditTime;

    private boolean fromFallback;

    private Boolean forceSubmitted;

    private LocalDateTime forceSubmitTime;

    private int auditSequence;

    private boolean isLatest;

    @Column(columnDefinition = "TEXT")
    private String originalPrescription;

    private String riskLevel;

    @Column(columnDefinition = "TEXT")
    private String aiResult;

    @Column(columnDefinition = "TEXT")
    private String auditIssues;

    @Version
    private Long version;

    public AuditRecord() {
    }

    public String getPrescriptionId() {
        return prescriptionId;
    }

    public void setPrescriptionId(String prescriptionId) {
        this.prescriptionId = prescriptionId;
    }

    public String getPrescriptionOrderId() {
        return prescriptionOrderId;
    }

    public void setPrescriptionOrderId(String prescriptionOrderId) {
        this.prescriptionOrderId = prescriptionOrderId;
    }

    public String getDoctorId() {
        return doctorId;
    }

    public void setDoctorId(String doctorId) {
        this.doctorId = doctorId;
    }

    public String getPatientId() {
        return patientId;
    }

    public void setPatientId(String patientId) {
        this.patientId = patientId;
    }

    public LocalDateTime getAuditTime() {
        return auditTime;
    }

    public void setAuditTime(LocalDateTime auditTime) {
        this.auditTime = auditTime;
    }

    public boolean isFromFallback() {
        return fromFallback;
    }

    public void setFromFallback(boolean fromFallback) {
        this.fromFallback = fromFallback;
    }

    public Boolean getForceSubmitted() {
        return forceSubmitted;
    }

    public void setForceSubmitted(Boolean forceSubmitted) {
        this.forceSubmitted = forceSubmitted;
    }

    public LocalDateTime getForceSubmitTime() {
        return forceSubmitTime;
    }

    public void setForceSubmitTime(LocalDateTime forceSubmitTime) {
        this.forceSubmitTime = forceSubmitTime;
    }

    public int getAuditSequence() {
        return auditSequence;
    }

    public void setAuditSequence(int auditSequence) {
        this.auditSequence = auditSequence;
    }

    public boolean isLatest() {
        return isLatest;
    }

    public void setLatest(boolean latest) {
        isLatest = latest;
    }

    public String getOriginalPrescription() {
        return originalPrescription;
    }

    public void setOriginalPrescription(String originalPrescription) {
        this.originalPrescription = originalPrescription;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }

    public String getAiResult() {
        return aiResult;
    }

    public void setAiResult(String aiResult) {
        this.aiResult = aiResult;
    }

    public String getAuditIssues() {
        return auditIssues;
    }

    public void setAuditIssues(String auditIssues) {
        this.auditIssues = auditIssues;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}
