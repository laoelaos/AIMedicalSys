package com.aimedical.modules.patient.entity;

import com.aimedical.common.base.BaseEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum BloodType implements BaseEnum {

    A("A", "A型"),
    B("B", "B型"),
    AB("AB", "AB型"),
    O("O", "O型"),
    UNKNOWN("UNKNOWN", "未知");

    private final String code;
    private final String desc;
}
