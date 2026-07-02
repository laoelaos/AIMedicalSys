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

    @Column(name = "user_id", unique = true, nullable = false)
    private Long userId;

    @Column(name = "real_name", length = 64, nullable = false)
    private String realName;

    @Column(name = "gender", length = 20)
    private String gender;

    @Column(name = "title", length = 64)
    private String title;

    @Column(name = "department", length = 64)
    private String department;

    @Column(name = "specialty", length = 255)
    private String specialty;

    @Column(name = "introduction", columnDefinition = "TEXT")
    private String introduction;

    @Column(name = "license_no", length = 64)
    private String licenseNo;

    @Column(name = "practice_years")
    private Integer practiceYears;

    @Column(name = "consultation_fee", precision = 10, scale = 2)
    private BigDecimal consultationFee;

    @Column(name = "remark", length = 500)
    private String remark;

}
