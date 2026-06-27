package com.aimedical.modules.medicalorder.entity;

import com.aimedical.common.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;

import java.math.BigDecimal;

@Entity
@Table(name = "medical_order")
@Data
public class MedicalOrder extends BaseEntity {

    private Long patientId;

    private Long doctorId;

    private Long registrationId;

    @Column(length = 32, unique = true)
    private String orderNo;

    @Column(length = 20)
    private String orderType;

    @Column(length = 20)
    private String orderStatus = "DRAFT";

    @Column(columnDefinition = "TEXT")
    private String diagnosis;

    @Column(precision = 10, scale = 2)
    private BigDecimal totalAmount;

    private Boolean isUrgent = false;

    @Column(length = 500)
    private String remark;

}