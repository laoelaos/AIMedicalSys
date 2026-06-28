package com.aimedical.modules.medicalorder.dto;

import com.aimedical.modules.medicalorder.entity.OrderStatus;
import com.aimedical.modules.medicalorder.entity.OrderType;
import jakarta.validation.Valid;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class MedicalOrderDTO {

    private Long id;
    private Long patientId;
    private Long doctorId;
    private Long registrationId;
    private String orderNo;
    private OrderType orderType;
    private OrderStatus orderStatus;
    private String diagnosis;
    private BigDecimal totalAmount;
    private Boolean isUrgent;
    private String remark;
    @Valid
    private List<MedicalOrderItemDTO> items;

}