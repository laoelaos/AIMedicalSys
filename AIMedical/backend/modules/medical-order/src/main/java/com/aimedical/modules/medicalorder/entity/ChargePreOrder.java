package com.aimedical.modules.medicalorder.entity;

import com.aimedical.common.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "charge_pre_order")
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class ChargePreOrder extends BaseEntity {

    @Column(name = "order_id")
    private Long orderId;

    @Column(name = "patient_id")
    private Long patientId;

    @Column(name = "charge_no", length = 32, unique = true)
    private String chargeNo;

    @Column(name = "total_amount", precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "charge_status", length = 20)
    private ChargeStatus chargeStatus = ChargeStatus.PENDING;

    @Column(length = 500)
    private String remark;

}