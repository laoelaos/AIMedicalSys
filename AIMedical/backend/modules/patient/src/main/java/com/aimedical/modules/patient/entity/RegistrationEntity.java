package com.aimedical.modules.patient.entity;

import com.aimedical.common.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "registration")
@Getter
@Setter
public class RegistrationEntity extends BaseEntity {

    @Column(name = "patient_id", nullable = false)
    private Long userId;

    @Column(name = "registration_type", nullable = false, length = 20)
    private String registrationType;

    @Column(name = "doctor_id")
    private Long doctorId;

    @Transient
    private String doctorName;

    @Column(name = "department", length = 64)
    private String departmentName;

    @Transient
    private Long departmentId;

    @Column(name = "exam_item_name", length = 200)
    private String examItemName;

    @Column(name = "exam_item_id")
    private Long examItemId;

    @Column(name = "triage_record_id")
    private Long triageRecordId;

    @Column(name = "scheduled_time_slot", length = 20)
    private String timeSlot;

    @Column(nullable = false, length = 20)
    private String status;
}
