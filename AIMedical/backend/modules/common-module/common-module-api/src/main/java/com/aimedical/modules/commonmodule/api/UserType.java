package com.aimedical.modules.commonmodule.api;

import com.aimedical.common.base.BaseEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum UserType implements BaseEnum {

    DOCTOR("DOCTOR", "医生"),
    PATIENT("PATIENT", "患者"),
    ADMIN("ADMIN", "管理员");

    private final String code;
    private final String desc;
}
