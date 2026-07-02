package com.aimedical.modules.patient.entity;

import com.aimedical.common.base.BaseEnum;

public enum BloodType implements BaseEnum {

    A("A", "A型"),
    B("B", "B型"),
    AB("AB", "AB型"),
    O("O", "O型"),
    UNKNOWN("UNKNOWN", "未知");

    private final String code;
    private final String desc;

    BloodType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    @Override
    public String getCode() { return code; }

    @Override
    public String getDesc() { return desc; }
}
