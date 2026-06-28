package com.aimedical.modules.registration.dto;

import com.aimedical.modules.registration.entity.RegistrationStatus;
import com.aimedical.modules.registration.entity.RegistrationType;
import com.aimedical.modules.registration.entity.TriageLevel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class RegistrationDTO {

    private Long id;

    @NotNull(message = "患者ID不能为空")
    private Long patientId;

    @NotNull(message = "医生ID不能为空")
    private Long doctorId;

    @NotNull(message = "挂号类型不能为空")
    private RegistrationType registrationType;

    private String department;

    @NotNull(message = "预约日期不能为空")
    private LocalDate scheduledDate;

    @NotBlank(message = "时间段不能为空")
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
