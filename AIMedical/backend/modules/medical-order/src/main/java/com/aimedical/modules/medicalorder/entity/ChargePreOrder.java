package com.aimedical.modules.medicalorder.entity;

import com.aimedical.common.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;

import java.math.BigDecimal;

@Entity
@Table(name = "charge_pre_order")
@Data
public class ChargePreOrder extends BaseEntity {

    @Column(name = "order_id")
    private Long orderId;

    @Column(name = "patient_id")
    private Long patientId;

    @Column(name = "charge_no", length = 32, unique = true)
    private String chargeNo;

    @Column(name = "total_amount", precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "charge_status", length = 20)
    private String chargeStatus = "PENDING";

    @Column(length = 500)
    private String remark;

}