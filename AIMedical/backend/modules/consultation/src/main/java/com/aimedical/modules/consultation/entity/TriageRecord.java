package com.aimedical.modules.consultation.entity;

import com.aimedical.common.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "triage_record", indexes = {
    @Index(name = "idx_triage_patient_id", columnList = "patientId")
})
public class TriageRecord extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String sessionId;

    private String patientId;

    @Column(columnDefinition = "TEXT")
    private String chiefComplaint;

    @Column(columnDefinition = "TEXT")
    private String aiRecommendedDepartments;

    @Column(columnDefinition = "TEXT")
    private String recommendedDoctors;

    @Column(columnDefinition = "TEXT")
    private String ruleMatchedDepartments;

    private String finalDepartmentId;
    private String finalDepartmentName;
    private Float confidence;
    private Boolean degraded;
    private String ruleVersion;
    private String ruleSetId;
    private LocalDateTime triageTime;

    @Column(columnDefinition = "TEXT")
    private String correctedChiefComplaint;

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getPatientId() {
        return patientId;
    }

    public void setPatientId(String patientId) {
        this.patientId = patientId;
    }

    public String getChiefComplaint() {
        return chiefComplaint;
    }

    public void setChiefComplaint(String chiefComplaint) {
        this.chiefComplaint = chiefComplaint;
    }

    public String getAiRecommendedDepartments() {
        return aiRecommendedDepartments;
    }

    public void setAiRecommendedDepartments(String aiRecommendedDepartments) {
        this.aiRecommendedDepartments = aiRecommendedDepartments;
    }

    public String getRecommendedDoctors() {
        return recommendedDoctors;
    }

    public void setRecommendedDoctors(String recommendedDoctors) {
        this.recommendedDoctors = recommendedDoctors;
    }

    public String getRuleMatchedDepartments() {
        return ruleMatchedDepartments;
    }

    public void setRuleMatchedDepartments(String ruleMatchedDepartments) {
        this.ruleMatchedDepartments = ruleMatchedDepartments;
    }

    public String getFinalDepartmentId() {
        return finalDepartmentId;
    }

    public void setFinalDepartmentId(String finalDepartmentId) {
        this.finalDepartmentId = finalDepartmentId;
    }

    public String getFinalDepartmentName() {
        return finalDepartmentName;
    }

    public void setFinalDepartmentName(String finalDepartmentName) {
        this.finalDepartmentName = finalDepartmentName;
    }

    public Float getConfidence() {
        return confidence;
    }

    public void setConfidence(Float confidence) {
        this.confidence = confidence;
    }

    public Boolean getDegraded() {
        return degraded;
    }

    public void setDegraded(Boolean degraded) {
        this.degraded = degraded;
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

    public LocalDateTime getTriageTime() {
        return triageTime;
    }

    public void setTriageTime(LocalDateTime triageTime) {
        this.triageTime = triageTime;
    }

    public String getCorrectedChiefComplaint() {
        return correctedChiefComplaint;
    }

    public void setCorrectedChiefComplaint(String correctedChiefComplaint) {
        this.correctedChiefComplaint = correctedChiefComplaint;
    }
}
