package com.aimedical.modules.patient.entity;

import com.aimedical.common.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.LocalDate;

@Entity
@Table(name = "surgery_history")
@Data
public class SurgeryHistory extends BaseEntity {

    private Long healthProfileId;

    @Column(length = 255, nullable = false)
    private String surgeryName;

    private LocalDate surgeryAt;

    @Column(length = 255)
    private String hospital;

    @Column(length = 500)
    private String remark;
}
