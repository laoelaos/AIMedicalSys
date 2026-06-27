package com.aimedical.modules.registration.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class TriageRecordDTO {

    private Long id;
    private Long registrationId;
    private Long patientId;
    private Long nurseId;
    private String symptoms;
    private BigDecimal temperature;
    private String bloodPressure;
    private Integer heartRate;
    private String triageDepartment;
    private String triageLevel;
    private String triageNote;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

}