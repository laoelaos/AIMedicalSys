package com.aimedical.modules.doctor.entity;

import com.aimedical.common.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 病历模板实体（按科室配置）
 *
 * @author AIMedical Team
 * @version 1.0.0
 */
@Entity
@Table(name = "medical_record_template")
@Data
@EqualsAndHashCode(callSuper = true)
public class MedicalRecordTemplateEntity extends BaseEntity {

    /** 所属科室 */
    @Column(name = "department", nullable = false, length = 64)
    private String department;

    /** 模板名称 */
    @Column(name = "name", nullable = false, length = 128)
    private String name;

    /** 主诉模板 */
    @Column(name = "chief_complaint_tpl", columnDefinition = "TEXT")
    private String chiefComplaintTpl;

    /** 现病史模板 */
    @Column(name = "present_illness_tpl", columnDefinition = "TEXT")
    private String presentIllnessTpl;

    /** 既往史模板 */
    @Column(name = "past_history_tpl", columnDefinition = "TEXT")
    private String pastHistoryTpl;

    /** 诊断模板 */
    @Column(name = "diagnosis_tpl", columnDefinition = "TEXT")
    private String diagnosisTpl;

    /** 治疗方案模板 */
    @Column(name = "treatment_plan_tpl", columnDefinition = "TEXT")
    private String treatmentPlanTpl;

    /** 是否启用 */
    @Column(name = "enabled", nullable = false)
    private Boolean enabled = true;

    /** 备注 */
    @Column(name = "remark", length = 500)
    private String remark;
}
