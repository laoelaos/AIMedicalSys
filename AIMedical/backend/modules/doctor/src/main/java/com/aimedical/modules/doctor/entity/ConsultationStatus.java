package com.aimedical.modules.doctor.entity;

import com.aimedical.common.base.BaseEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

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

    /**
     * 活跃状态（已叫号/接诊中），用于并发接诊约束校验。
     * 同一医生同一时间仅允许一名患者处于这些状态。
     */
    public static List<String> getActiveStatuses() {
        return List.of(CALLED.getCode(), IN_CONSULTATION.getCode());
    }

    /**
     * 队列展示状态（候诊+已叫号+接诊中），用于医生查看当前活跃队列。
     */
    public static List<String> getQueueDisplayStatuses() {
        return List.of(WAITING.getCode(), CALLED.getCode(), IN_CONSULTATION.getCode());
    }
}
