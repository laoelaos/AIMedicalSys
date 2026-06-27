package com.aimedical.modules.medicalorder.entity;

import com.aimedical.common.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;

import java.math.BigDecimal;

@Entity
@Table(name = "medical_order_item")
@Data
public class MedicalOrderItem extends BaseEntity {

    private Long orderId;

    @Column(length = 20)
    private String itemType;

    @Column(length = 64)
    private String itemCode;

    @Column(length = 255)
    private String itemName;

    @Column(length = 255)
    private String specification;

    @Column(precision = 10, scale = 2)
    private BigDecimal quantity;

    @Column(length = 20)
    private String unit;

    @Column(precision = 10, scale = 2)
    private BigDecimal unitPrice;

    @Column(precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(length = 100)
    private String dosage;

    @Column(length = 100)
    private String usageMethod;

    @Column(length = 50)
    private String frequency;

    private Integer days;

    @Column(length = 500)
    private String remark;

}