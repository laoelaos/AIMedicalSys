package com.aimedical.modules.doctor.entity;

import com.aimedical.common.base.BaseEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * AI 处方审核风险等级
 *
 * <p>不同风险档位采取差异化阻断策略。
 *
 * @author AIMedical Team
 * @version 1.0.0
 */
@Getter
@AllArgsConstructor
public enum AiRiskLevel implements BaseEnum {

    LOW("LOW", "低风险"),
    MEDIUM("MEDIUM", "中风险"),
    HIGH("HIGH", "高风险");

    private final String code;
    private final String desc;
}
