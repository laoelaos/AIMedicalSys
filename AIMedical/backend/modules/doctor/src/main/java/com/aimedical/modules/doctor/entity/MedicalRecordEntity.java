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

    /** 版本号（草稿=0，正式版本从1递增） */
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

    /** 关联处方ID */
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
