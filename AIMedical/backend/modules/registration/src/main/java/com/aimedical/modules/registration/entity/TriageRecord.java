package com.aimedical.modules.registration.entity;

import com.aimedical.common.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;

import java.math.BigDecimal;

@Entity
@Table(name = "triage_record")
@Data
public class TriageRecord extends BaseEntity {

    private Long registrationId;

    private Long patientId;

    private Long nurseId;

    @Column(columnDefinition = "TEXT")
    private String symptoms;

    @Column(precision = 4, scale = 1)
    private BigDecimal temperature;

    @Column(length = 20)
    private String bloodPressure;

    private Integer heartRate;

    @Column(length = 64)
    private String triageDepartment;

    @Column(length = 20)
    private String triageLevel;

    @Column(length = 500)
    private String triageNote;

}