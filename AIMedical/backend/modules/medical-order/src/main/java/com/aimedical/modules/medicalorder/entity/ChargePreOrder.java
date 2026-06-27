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

    private Long orderId;

    private Long patientId;

    @Column(length = 32, unique = true)
    private String chargeNo;

    @Column(precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Column(length = 20)
    private String chargeStatus = "PENDING";

    @Column(length = 500)
    private String remark;

}