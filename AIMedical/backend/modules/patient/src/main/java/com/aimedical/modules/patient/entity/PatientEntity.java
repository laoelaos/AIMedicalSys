package com.aimedical.modules.patient.entity;

import com.aimedical.common.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Table(name = "patient_profile")
@Data
@EqualsAndHashCode(callSuper = true)
public class PatientEntity extends BaseEntity {

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String realName;

    @Column(length = 20)
    private String gender;

    @Column(length = 500)
    private String avatarUrl;

    @Column(length = 20)
    private String phone;
}
