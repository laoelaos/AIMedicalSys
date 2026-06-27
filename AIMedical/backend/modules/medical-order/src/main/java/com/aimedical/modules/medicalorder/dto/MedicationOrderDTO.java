package com.aimedical.modules.medicalorder.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class MedicationOrderDTO {

    private String orderNo;
    private Long patientId;
    private String patientName;
    private Long doctorId;
    private String doctorName;
    private List<MedicationOrderItemDTO> items;
    private String diagnosis;
    private Boolean isUrgent;

    @Data
    public static class MedicationOrderItemDTO {

        private String itemCode;
        private String itemName;
        private String specification;
        private BigDecimal quantity;
        private String unit;
        private String dosage;
        private String usageMethod;
        private String frequency;
        private Integer days;

    }

}