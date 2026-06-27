package com.aimedical.modules.registration.entity;

import com.aimedical.common.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "registration")
@Data
public class Registration extends BaseEntity {

    private Long patientId;

    private Long doctorId;

    @Column(length = 20)
    private String registrationType;

    @Column(length = 64)
    private String department;

    private LocalDate scheduledDate;

    @Column(length = 20)
    private String scheduledTimeSlot;

    @Column(length = 20)
    private String status = "PENDING";

    @Column(length = 500)
    private String cancelReason;

    private LocalDateTime cancelTime;

    @Column(length = 20)
    private String cancelType;

    @Column(length = 20)
    private String triageLevel;

    @Column(length = 500)
    private String chiefComplaint;

    @Column(precision = 10, scale = 2)
    private BigDecimal registrationFee;

    private Integer queueNumber;

    @Column(length = 500)
    private String remark;

}