package com.aimedical.modules.doctor.entity;

import com.aimedical.common.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 病历实体（含版本管理）
 *
 * <p>状态 DRAFT(草稿) -> OFFICIAL(正式)。同一患者可存在多个版本，通过 version_no 区分。
 *
 * <p>并发控制：使用继承自 {@link BaseEntity} 的 JPA {@code @Version} 乐观锁（version 列），
 * 防止记录级并发覆盖；publish 时版本号自增在同一事务内完成。
 *
 * @author AIMedical Team
 * @version 1.0.0
 */
@Entity
@Table(name = "medical_record")
@Data
@EqualsAndHashCode(callSuper = true)
public class MedicalRecordEntity extends BaseEntity {

    /** 患者档案ID */
    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    /** 医生用户ID */
    @Column(name = "doctor_id", nullable = false)
    private Long doctorId;

    /** 科室 */
    @Column(name = "department", length = 64)
    private String department;

    /** 版本号（业务版本，草稿=0，正式版本从1递增） */
    @Column(name = "version_no", nullable = false)
    private Integer versionNo = 0;

    /** 状态 DRAFT/OFFICIAL */
    @Column(name = "status", nullable = false, length = 20)
    private String status = MedicalRecordStatus.DRAFT.getCode();

    /** 主诉 */
    @Column(name = "chief_complaint", columnDefinition = "TEXT")
    private String chiefComplaint;

    /** 现病史 */
    @Column(name = "present_illness", columnDefinition = "TEXT")
    private String presentIllness;

    /** 既往史 */
    @Column(name = "past_history", columnDefinition = "TEXT")
    private String pastHistory;

    /** 诊断 */
    @Column(name = "diagnosis", columnDefinition = "TEXT")
    private String diagnosis;

    /** 治疗方案 */
    @Column(name = "treatment_plan", columnDefinition = "TEXT")
    private String treatmentPlan;

    /** 关联处方ID（裸 Long 引用，未使用 @ManyToOne JPA 关联；
     *  避免双向关联带来的级联复杂度，按需通过 PrescriptionRepository 单独查询） */
    @Column(name = "prescription_id")
    private Long prescriptionId;

    /** 使用的模板ID */
    @Column(name = "template_id")
    private Long templateId;

    /** 是否AI生成 */
    @Column(name = "ai_generated", nullable = false)
    private Boolean aiGenerated = false;

    /** 备注 */
    @Column(name = "remark", length = 500)
    private String remark;
}
