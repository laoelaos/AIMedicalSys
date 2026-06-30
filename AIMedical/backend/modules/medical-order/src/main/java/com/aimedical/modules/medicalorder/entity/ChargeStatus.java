package com.aimedical.modules.medicalorder.entity;

import com.aimedical.common.base.BaseEnum;

public enum ChargeStatus implements BaseEnum {
    PENDING("PENDING", "待收费"),
    CHARGED("CHARGED", "已收费"),
    REFUNDED("REFUNDED", "已退费"),
    CANCELLED("CANCELLED", "已取消");

    private final String code;
    private final String desc;

    ChargeStatus(String code, String desc) {
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