package com.aimedical.modules.registration.dto;

import com.aimedical.modules.registration.entity.RegistrationStatus;
import com.aimedical.modules.registration.entity.RegistrationType;
import com.aimedical.modules.registration.entity.TriageLevel;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class RegistrationDTO {

    private Long id;
    private Long patientId;
    private Long doctorId;
    private RegistrationType registrationType;
    private String department;
    private LocalDate scheduledDate;
    private String scheduledTimeSlot;
    private RegistrationStatus status;
    private String cancelReason;
    private LocalDateTime cancelTime;
    private String cancelType;
    private TriageLevel triageLevel;
    private String chiefComplaint;
    private BigDecimal registrationFee;
    private Integer queueNumber;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

}