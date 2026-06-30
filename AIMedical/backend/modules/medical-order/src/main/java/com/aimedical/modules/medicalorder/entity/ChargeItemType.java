package com.aimedical.modules.medicalorder.entity;

import com.aimedical.common.base.BaseEnum;

public enum ChargeItemType implements BaseEnum {
    DRUG("DRUG", "药品"),
    EXAMINATION("EXAMINATION", "检查"),
    LAB_TEST("LAB_TEST", "检验"),
    MATERIAL("MATERIAL", "材料"),
    OTHER("OTHER", "其他");

    private final String code;
    private final String desc;

    ChargeItemType(String code, String desc) {
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