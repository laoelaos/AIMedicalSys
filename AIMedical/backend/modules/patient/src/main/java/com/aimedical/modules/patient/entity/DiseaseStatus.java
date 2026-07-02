package com.aimedical.modules.patient.entity;

import com.aimedical.common.base.BaseEnum;

public enum DiseaseStatus implements BaseEnum {

    STABLE("STABLE", "稳定"),
    UNSTABLE("UNSTABLE", "不稳定"),
    RECOVERED("RECOVERED", "已康复");

    private final String code;
    private final String desc;

    DiseaseStatus(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    @Override
    public String getCode() { return code; }

    @Override
    public String getDesc() { return desc; }
}
