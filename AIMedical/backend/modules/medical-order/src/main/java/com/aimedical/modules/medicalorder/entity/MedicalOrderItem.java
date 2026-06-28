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
@Table(name = "medical_order_item")
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class MedicalOrderItem extends BaseEntity {

    @Column(name = "order_id")
    private Long orderId;

    @Enumerated(EnumType.STRING)
    @Column(name = "item_type", length = 20)
    private ItemType itemType;

    @Column(name = "item_code", length = 64)
    private String itemCode;

    @Column(name = "item_name", length = 255)
    private String itemName;

    @Column(name = "specification", length = 255)
    private String specification;

    @Column(name = "quantity", precision = 10, scale = 2)
    private BigDecimal quantity;

    @Column(name = "unit", length = 20)
    private String unit;

    @Column(name = "unit_price", precision = 10, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "amount", precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(name = "dosage", length = 100)
    private String dosage;

    @Column(name = "usage_method", length = 100)
    private String usageMethod;

    @Column(name = "frequency", length = 50)
    private String frequency;

    @Column(name = "days")
    private Integer days;

    @Column(name = "remark", length = 500)
    private String remark;

}
