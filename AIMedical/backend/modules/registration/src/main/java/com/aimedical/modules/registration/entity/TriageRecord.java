package com.aimedical.modules.registration.entity;

import com.aimedical.common.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "triage_record")
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class TriageRecord extends BaseEntity {

    @Column(name = "registration_id")
    private Long registrationId;

    @Column(name = "patient_id")
    private Long patientId;

    @Column(name = "nurse_id")
    private Long nurseId;

    @Column(name = "symptoms", columnDefinition = "TEXT")
    private String symptoms;

    @Column(name = "temperature", precision = 4, scale = 1)
    private BigDecimal temperature;

    @Column(name = "blood_pressure", length = 20)
    private String bloodPressure;

    @Column(name = "heart_rate")
    private Integer heartRate;

    @Column(name = "triage_department", length = 64)
    private String triageDepartment;

    @Enumerated(EnumType.STRING)
    @Column(name = "triage_level", length = 20)
    private TriageLevel triageLevel;

    @Column(name = "triage_note", length = 500)
    private String triageNote;

}
