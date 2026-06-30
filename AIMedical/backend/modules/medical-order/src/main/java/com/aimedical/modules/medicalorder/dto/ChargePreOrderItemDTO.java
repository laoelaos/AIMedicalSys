package com.aimedical.modules.medicalorder.dto;

import com.aimedical.modules.medicalorder.entity.ChargeItemType;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ChargePreOrderItemDTO {

    private Long id;
    private Long chargePreOrderId;
    private Long orderItemId;
    private String itemName;
    private BigDecimal quantity;
    private BigDecimal unitPrice;
    private BigDecimal amount;
    private ChargeItemType chargeItemType;

}