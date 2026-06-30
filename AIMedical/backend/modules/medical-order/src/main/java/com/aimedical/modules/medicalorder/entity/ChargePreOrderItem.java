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
@Table(name = "charge_pre_order_item")
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class ChargePreOrderItem extends BaseEntity {

    @Column(name = "charge_pre_order_id")
    private Long chargePreOrderId;

    @Column(name = "order_item_id")
    private Long orderItemId;

    @Column(length = 255)
    private String itemName;

    @Column(precision = 10, scale = 2)
    private BigDecimal quantity;

    @Column(precision = 10, scale = 2)
    private BigDecimal unitPrice;

    @Column(precision = 10, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private ChargeItemType chargeItemType;

}