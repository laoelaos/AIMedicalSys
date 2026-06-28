package com.aimedical.modules.medicalorder.dto;

import com.aimedical.modules.medicalorder.entity.ChargeStatus;
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
    private ChargeStatus chargeStatus;
    private String remark;
    private List<ChargePreOrderItemDTO> items;

}