package com.aimedical.modules.doctor.entity;

import com.aimedical.common.base.BaseEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 处方状态枚举
 *
 * <p>处方状态机：DRAFT(草稿) -> PENDING_REVIEW(待审) -> APPROVED(已审) / REJECTED(已驳回)
 *
 * <p>状态流转规则：
 * <ul>
 *   <li>DRAFT -> PENDING_REVIEW：医生提交审核</li>
 *   <li>PENDING_REVIEW -> APPROVED：审核通过</li>
 *   <li>PENDING_REVIEW -> REJECTED：审核驳回</li>
 *   <li>REJECTED -> PENDING_REVIEW：修改后重新提交</li>
 * </ul>
 *
 * @author AIMedical Team
 * @version 1.0.0
 */
@Getter
@AllArgsConstructor
public enum PrescriptionStatus implements BaseEnum {

    DRAFT("DRAFT", "草稿"),
    PENDING_REVIEW("PENDING_REVIEW", "待审"),
    APPROVED("APPROVED", "已审"),
    REJECTED("REJECTED", "已驳回");

    private final String code;
    private final String desc;
}
