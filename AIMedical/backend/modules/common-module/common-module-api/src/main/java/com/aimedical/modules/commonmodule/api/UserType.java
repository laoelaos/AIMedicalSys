package com.aimedical.modules.commonmodule.api;

import com.aimedical.common.base.BaseEnum;

public enum UserType implements BaseEnum {
    DOCTOR("DOCTOR", "医生"),
    PATIENT("PATIENT", "患者"),
    ADMIN("ADMIN", "管理员");

    private final String code;
    private final String desc;

    UserType(String code, String desc) {
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
