package com.aimedical.modules.medicalorder.entity;

import com.aimedical.common.base.BaseEnum;

public enum OrderStatus implements BaseEnum {
    DRAFT("DRAFT", "草稿"),
    SUBMITTED("SUBMITTED", "已提交"),
    CHARGED("CHARGED", "已收费"),
    DISPENSED("DISPENSED", "已发药"),
    COMPLETED("COMPLETED", "已完成"),
    CANCELLED("CANCELLED", "已取消");

    private final String code;
    private final String desc;

    OrderStatus(String code, String desc) {
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