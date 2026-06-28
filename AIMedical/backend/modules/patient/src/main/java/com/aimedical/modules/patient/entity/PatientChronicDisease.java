package com.aimedical.modules.patient.entity;

import com.aimedical.common.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "patient_chronic_disease")
@Getter
@Setter
public class PatientChronicDisease extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private PatientEntity patient;

    @Column(nullable = false, length = 100)
    private String diseaseName;

    @Column(name = "diagnosed_at")
    private LocalDate diagnosedAt;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private DiseaseStatus currentStatus;
}
