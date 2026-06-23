package com.aimedical.modules.patient.entity;

import com.aimedical.common.base.BaseEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum DiseaseStatus implements BaseEnum {

    STABLE("STABLE", "稳定"),
    UNSTABLE("UNSTABLE", "不稳定"),
    RECOVERED("RECOVERED", "已康复");

    private final String code;
    private final String desc;
}
