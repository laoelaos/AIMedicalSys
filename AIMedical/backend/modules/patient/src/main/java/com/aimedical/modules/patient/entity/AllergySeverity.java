package com.aimedical.modules.patient.entity;

import com.aimedical.common.base.BaseEnum;

public enum AllergySeverity implements BaseEnum {

    MILD("MILD", "轻度"),
    MODERATE("MODERATE", "中度"),
    SEVERE("SEVERE", "重度");

    private final String code;
    private final String desc;

    AllergySeverity(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    @Override
    public String getCode() { return code; }

    @Override
    public String getDesc() { return desc; }
}
