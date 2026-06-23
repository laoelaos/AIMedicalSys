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

    @Column(unique = true)
    private Long userId;

    @Column(length = 64)
    private String realName;

    @Column(length = 20)
    private String gender;

    @Column(length = 64)
    private String title;

    @Column(length = 64)
    private String department;

    @Column(length = 255)
    private String specialty;

    @Column(columnDefinition = "TEXT")
    private String introduction;

    @Column(length = 64)
    private String licenseNo;

    private Integer practiceYears;

    @Column(precision = 10, scale = 2)
    private BigDecimal consultationFee;

    @Column(length = 500)
    private String remark;

}
