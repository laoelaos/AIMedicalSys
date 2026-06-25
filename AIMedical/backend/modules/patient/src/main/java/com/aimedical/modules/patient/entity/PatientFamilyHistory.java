package com.aimedical.modules.patient.entity;

import com.aimedical.common.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "patient_family_history")
@Getter
@Setter
public class PatientFamilyHistory extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private PatientEntity patient;

    @Column(nullable = false, length = 50)
    private String relationship;

    @Column(nullable = false, length = 100)
    private String diseaseName;

    @Column(length = 200)
    private String note;
}
