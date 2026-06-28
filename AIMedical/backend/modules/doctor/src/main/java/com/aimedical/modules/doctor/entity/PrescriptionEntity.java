package com.aimedical.modules.doctor.entity;

import com.aimedical.common.base.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 处方实体
 *
 * <p>处方状态机：DRAFT -> PENDING_REVIEW -> APPROVED / REJECTED。
 *
 * @author AIMedical Team
 * @version 1.0.0
 */
@Entity
@Table(name = "prescription")
@Data
@EqualsAndHashCode(callSuper = true)
public class PrescriptionEntity extends BaseEntity {

    /** 患者档案ID */
    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    /** 患者姓名（冗余展示） */
    @Column(name = "patient_name", nullable = false, length = 64)
    private String patientName;

    /** 开方医生用户ID */
    @Column(name = "doctor_id", nullable = false)
    private Long doctorId;

    /** 科室 */
    @Column(name = "department", length = 64)
    private String department;

    /** 状态 DRAFT/PENDING_REVIEW/APPROVED/REJECTED */
    @Column(name = "status", nullable = false, length = 20)
    private String status = PrescriptionStatus.DRAFT.getCode();

    /** 诊断 */
    @Column(name = "diagnosis", length = 500)
    private String diagnosis;

    /** 是否经AI审核 */
    @Column(name = "ai_checked", nullable = false)
    private Boolean aiChecked = false;

    /** AI风险等级 LOW/MEDIUM/HIGH */
    @Column(name = "ai_risk_level", length = 20)
    private String aiRiskLevel;

    /** 审核备注 */
    @Column(name = "audit_remark", length = 500)
    private String auditRemark;

    /** 审核人用户ID */
    @Column(name = "audited_by")
    private Long auditedBy;

    /** 审核时间 */
    @Column(name = "audited_at")
    private LocalDateTime auditedAt;

    /** 备注 */
    @Column(name = "remark", length = 500)
    private String remark;

    /** 处方明细列表 */
    @OneToMany(mappedBy = "prescription", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<PrescriptionItemEntity> items = new ArrayList<>();
}
