package com.aimedical.modules.patient.entity;

import com.aimedical.common.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.LocalDate;

@Entity
@Table(name = "patient_profile")
@Data
public class PatientEntity extends BaseEntity {

    @Column(unique = true)
    private Long userId;

    @Column(length = 64)
    private String realName;

    @Column(length = 20)
    private String gender;

    private LocalDate birthDate;

    private Integer age;

    @Column(length = 32)
    private String idCard;

    @Column(length = 20)
    private String phone;

    @Column(length = 64)
    private String emergencyContact;

    @Column(length = 20)
    private String emergencyPhone;

    @Column(length = 255)
    private String address;

    @Column(length = 500)
    private String avatarUrl;

    @Column(length = 500)
    private String remark;
}
