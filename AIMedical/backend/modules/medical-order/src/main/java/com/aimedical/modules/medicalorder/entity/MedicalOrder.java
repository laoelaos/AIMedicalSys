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

    @Column(name = "patient_id")
    private Long patientId;

    @Column(name = "doctor_id")
    private Long doctorId;

    @Column(name = "registration_id")
    private Long registrationId;

    @Column(length = 32, unique = true)
    private String orderNo;

    @Column(name = "order_type", length = 20)
    private String orderType;

    @Column(name = "order_status", length = 20)
    private String orderStatus = "DRAFT";

    @Column(columnDefinition = "TEXT")
    private String diagnosis;

    @Column(name = "total_amount", precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "is_urgent")
    private Boolean isUrgent = false;

    @Column(length = 500)
    private String remark;

}