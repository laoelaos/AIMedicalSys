package com.aimedical.modules.doctor.entity;

import com.aimedical.common.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 接诊/叫号队列实体
 *
 * @author AIMedical Team
 * @version 1.0.0
 */
@Entity
@Table(name = "consultation_queue")
@Data
@EqualsAndHashCode(callSuper = true)
public class ConsultationQueueEntity extends BaseEntity {

    /** 患者档案ID */
    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    /** 患者姓名（冗余展示） */
    @Column(name = "patient_name", nullable = false, length = 64)
    private String patientName;

    /** 接诊医生用户ID */
    @Column(name = "doctor_id", nullable = false)
    private Long doctorId;

    /** 科室 */
    @Column(name = "department", length = 64)
    private String department;

    /** 排队号 */
    @Column(name = "queue_no", nullable = false, length = 32)
    private String queueNo;

    /** 状态 WAITING/CALLED/IN_CONSULTATION/FINISHED/SKIPPED */
    @Column(name = "status", nullable = false, length = 20)
    private String status = ConsultationStatus.WAITING.getCode();

    /** 挂号时间 */
    @Column(name = "registered_at")
    private LocalDateTime registeredAt;

    /** 叫号时间 */
    @Column(name = "called_at")
    private LocalDateTime calledAt;

    /** 完成时间 */
    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    /** 备注 */
    @Column(name = "remark", length = 500)
    private String remark;
}
