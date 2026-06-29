package com.aimedical.modules.patient.entity;

import com.aimedical.common.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "triage_record")
@Getter
@Setter
public class TriageRecordEntity extends BaseEntity {

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @Column(name = "chief_complaint", nullable = false, length = 2000)
    private String chiefComplaint;

    @Column(name = "session_id", length = 100)
    private String sessionId;

    @Column(name = "recommended_departments", length = 4000)
    private String recommendedDepartments;

    @Column(name = "recommended_doctors", length = 4000)
    private String recommendedDoctors;

    @Column(name = "is_degraded", nullable = false)
    private Boolean isDegraded;

    @Column(name = "rule_version", length = 50)
    private String ruleVersion;

    @Column(name = "rule_set_id", length = 50)
    private String ruleSetId;

    @Column(name = "matched_rules", length = 2000)
    private String matchedRules;
}
