package com.aimedical.modules.patient.entity;

import com.aimedical.common.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;

import java.math.BigDecimal;

@Entity
@Table(name = "health_profile")
@Data
public class HealthProfile extends BaseEntity {

    @Column(unique = true)
    private Long patientId;

    @Column(length = 20)
    private String bloodType;

    @Column(precision = 5, scale = 2)
    private BigDecimal heightCm;

    @Column(precision = 5, scale = 2)
    private BigDecimal weightKg;

    @Column(precision = 5, scale = 2)
    private BigDecimal bmi;

    @Column(length = 32)
    private String maritalStatus;

    @Column(columnDefinition = "TEXT")
    private String lifestyleNote;
}
