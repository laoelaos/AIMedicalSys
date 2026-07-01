package com.aimedical.modules.medicalrecord.entity;

import java.time.LocalDateTime;
import java.util.Map;

import com.aimedical.modules.medicalrecord.converter.MedicalRecordContentConverter;
import com.aimedical.modules.medicalrecord.enums.MedicalRecordField;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "medical_record", indexes = {
    @Index(name = "idx_visit_id_fallback", columnList = "visitIdFallback")
})
public class MedicalRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long recordId;

    @Column(nullable = false)
    private String patientId;

    @Column(nullable = false, unique = true)
    private String visitId;

    private String departmentId;

    @Column(name = "content_json", columnDefinition = "TEXT")
    @Convert(converter = MedicalRecordContentConverter.class)
    private Map<MedicalRecordField, String> content;

    private String doctorId;

    private Boolean visitIdFallback;

    @Version
    private Integer version;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getRecordId() {
        return recordId;
    }

    public void setRecordId(Long recordId) {
        this.recordId = recordId;
    }

    public String getPatientId() {
        return patientId;
    }

    public void setPatientId(String patientId) {
        this.patientId = patientId;
    }

    public String getVisitId() {
        return visitId;
    }

    public void setVisitId(String visitId) {
        this.visitId = visitId;
    }

    public String getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(String departmentId) {
        this.departmentId = departmentId;
    }

    public Map<MedicalRecordField, String> getContent() {
        return content;
    }

    public void setContent(Map<MedicalRecordField, String> content) {
        this.content = content;
    }

    public String getDoctorId() {
        return doctorId;
    }

    public void setDoctorId(String doctorId) {
        this.doctorId = doctorId;
    }

    public Boolean getVisitIdFallback() {
        return visitIdFallback;
    }

    public void setVisitIdFallback(Boolean visitIdFallback) {
        this.visitIdFallback = visitIdFallback;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
