package com.aimedical.modules.patient.entity;

import com.aimedical.common.base.BaseEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum AllergySeverity implements BaseEnum {

    MILD("MILD", "轻度"),
    MODERATE("MODERATE", "中度"),
    SEVERE("SEVERE", "重度");

    private final String code;
    private final String desc;
}
