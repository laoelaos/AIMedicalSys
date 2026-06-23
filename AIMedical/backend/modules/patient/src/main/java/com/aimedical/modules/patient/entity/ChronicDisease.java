package com.aimedical.modules.patient.entity;

import com.aimedical.common.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.LocalDate;

@Entity
@Table(name = "chronic_disease")
@Data
public class ChronicDisease extends BaseEntity {

    private Long healthProfileId;

    @Column(length = 255, nullable = false)
    private String diseaseName;

    private LocalDate diagnosedAt;

    @Column(length = 20)
    private String currentStatus;

    @Column(length = 500)
    private String remark;
}
