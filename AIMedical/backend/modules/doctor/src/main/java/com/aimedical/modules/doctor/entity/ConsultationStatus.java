package com.aimedical.modules.doctor.entity;

import com.aimedical.common.base.BaseEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 接诊队列状态枚举
 *
 * <p>状态流转：WAITING(候诊) -> CALLED(已叫号) -> IN_CONSULTATION(接诊中) -> FINISHED(完成)；
 * WAITING/CALLED -> SKIPPED(过号)。
 *
 * @author AIMedical Team
 * @version 1.0.0
 */
@Getter
@AllArgsConstructor
public enum ConsultationStatus implements BaseEnum {

    WAITING("WAITING", "候诊"),
    CALLED("CALLED", "已叫号"),
    IN_CONSULTATION("IN_CONSULTATION", "接诊中"),
    FINISHED("FINISHED", "完成"),
    SKIPPED("SKIPPED", "过号");

    private final String code;
    private final String desc;
}
