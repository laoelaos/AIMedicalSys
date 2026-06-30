package com.aimedical.modules.registration.entity;

import com.aimedical.common.base.BaseEnum;

public enum RegistrationType implements BaseEnum {
    OUTPATIENT("OUTPATIENT", "门诊"),
    EXAMINATION("EXAMINATION", "检查"),
    EMERGENCY("EMERGENCY", "急诊");

    private final String code;
    private final String desc;

    RegistrationType(String code, String desc) {
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