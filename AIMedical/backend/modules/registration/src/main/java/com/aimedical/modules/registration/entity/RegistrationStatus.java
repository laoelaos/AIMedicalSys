package com.aimedical.modules.registration.entity;

import com.aimedical.common.base.BaseEnum;

public enum RegistrationStatus implements BaseEnum {
    PENDING("PENDING", "待确认"),
    CONFIRMED("CONFIRMED", "已确认"),
    COMPLETED("COMPLETED", "已完成"),
    CANCELLED("CANCELLED", "已取消"),
    NO_SHOW("NO_SHOW", "未到诊");

    private final String code;
    private final String desc;

    RegistrationStatus(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public String getDesc() {
        return desc;
    }
}