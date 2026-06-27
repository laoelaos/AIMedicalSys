package com.aimedical.modules.medicalorder.dto;

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
    private String orderType;
    private String orderStatus;
    private String diagnosis;
    private BigDecimal totalAmount;
    private Boolean isUrgent;
    private String remark;
    private List<MedicalOrderItemDTO> items;

}