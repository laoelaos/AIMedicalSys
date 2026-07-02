package com.aimedical.modules.doctor.entity;

import com.aimedical.common.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.math.BigDecimal;

/**
 * 处方明细实体
 *
 * @author AIMedical Team
 * @version 1.0.0
 */
@Entity
@Table(name = "prescription_item")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class PrescriptionItemEntity extends BaseEntity {

    /** 关联处方（JPA 关系管理字段，mappedBy 指向此属性） */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "prescription_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private PrescriptionEntity prescription;

    /** 处方ID（只读镜像，由 prescription 关联写入，便于按 prescriptionId 查询） */
    @Column(name = "prescription_id", insertable = false, updatable = false)
    private Long prescriptionId;

    /** 药品名称 */
    @Column(name = "drug_name", nullable = false, length = 128)
    private String drugName;

    /** 规格 */
    @Column(name = "specification", length = 128)
    private String specification;

    /** 剂量 */
    @Column(name = "dosage", length = 64)
    private String dosage;

    /** 用法 */
    @Column(name = "usage_method", length = 128)
    private String usageMethod;

    /** 频次 */
    @Column(name = "frequency", length = 64)
    private String frequency;

    /** 数量 */
    @Column(name = "quantity", precision = 10, scale = 2)
    private BigDecimal quantity;

    /** 单位 */
    @Column(name = "unit", length = 32)
    private String unit;

    /** 备注 */
    @Column(name = "remark", length = 500)
    private String remark;
}
