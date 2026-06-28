package com.aimedical.modules.doctor.entity;

import com.aimedical.common.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;

import java.math.BigDecimal;

@Entity
@Table(name = "doctor_profile")
@Data
public class DoctorEntity extends BaseEntity {

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String realName;

    @Column(length = 50)
    private String title;

    @Column(length = 100)
    private String department;

    @Column(precision = 10, scale = 2)
    private BigDecimal consultationFee;
}
