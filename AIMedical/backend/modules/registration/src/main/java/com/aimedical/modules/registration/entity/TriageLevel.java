package com.aimedical.modules.registration.entity;

import com.aimedical.common.base.BaseEnum;

public enum TriageLevel implements BaseEnum {
    LEVEL_1("LEVEL_1", "一级(危急)"),
    LEVEL_2("LEVEL_2", "二级(重症)"),
    LEVEL_3("LEVEL_3", "三级(急症)"),
    LEVEL_4("LEVEL_4", "四级(非急症)");

    private final String code;
    private final String desc;

    TriageLevel(String code, String desc) {
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