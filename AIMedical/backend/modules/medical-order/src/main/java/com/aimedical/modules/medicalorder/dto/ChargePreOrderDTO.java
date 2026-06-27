package com.aimedical.modules.medicalorder.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class ChargePreOrderDTO {

    private Long id;
    private Long orderId;
    private Long patientId;
    private String chargeNo;
    private BigDecimal totalAmount;
    private String chargeStatus;
    private String remark;
    private List<ChargePreOrderItemDTO> items;

}