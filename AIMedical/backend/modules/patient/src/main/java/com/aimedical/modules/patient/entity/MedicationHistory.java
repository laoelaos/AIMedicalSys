package com.aimedical.modules.patient.entity;

import com.aimedical.common.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.LocalDate;

@Entity
@Table(name = "medication_history")
@Data
public class MedicationHistory extends BaseEntity {

    private Long healthProfileId;

    @Column(length = 255, nullable = false)
    private String drugName;

    @Column(length = 500)
    private String reason;

    private LocalDate startedAt;

    private LocalDate endedAt;

    @Column(length = 500)
    private String remark;
}
