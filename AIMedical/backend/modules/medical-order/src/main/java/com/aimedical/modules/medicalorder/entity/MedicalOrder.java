package com.aimedical.modules.medicalorder.entity;

import com.aimedical.common.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * 医嘱聚合根。
 * <p>
 * 采用 DDD 聚合根设计：MedicalOrderItem 作为独立实体通过 orderId 外键关联，
 * 不使用 JPA {@code @OneToMany} 关系映射，以避免 N+1 查询、级联删除等隐式行为，
 * 同时保持聚合根对明细的生命周期管理权。
 */
@Entity
@Table(name = "medical_order")
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class MedicalOrder extends BaseEntity {

    @Column(name = "patient_id")
    private Long patientId;

    @Column(name = "doctor_id")
    private Long doctorId;

    @Column(name = "registration_id")
    private Long registrationId;

    @Column(name = "order_no", length = 32, unique = true)
    private String orderNo;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_type", length = 20)
    private OrderType orderType;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_status", length = 20)
    private OrderStatus orderStatus = OrderStatus.DRAFT;

    @Column(name = "diagnosis", columnDefinition = "TEXT")
    private String diagnosis;

    @Column(name = "total_amount", precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "is_urgent")
    private Boolean isUrgent = false;

    @Column(name = "remark", length = 500)
    private String remark;

}
