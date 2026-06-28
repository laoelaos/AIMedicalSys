package com.aimedical.modules.medicalorder.dto;

import com.aimedical.modules.medicalorder.entity.OrderType;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class MedicalOrderItemDTO {

    private Long id;
    private Long orderId;
    private OrderType itemType;
    private String itemCode;
    private String itemName;
    private String specification;
    private BigDecimal quantity;
    private String unit;
    private BigDecimal unitPrice;
    private BigDecimal amount;
    private String dosage;
    private String usageMethod;
    private String frequency;
    private Integer days;
    private String remark;

}