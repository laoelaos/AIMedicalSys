package com.aimedical.modules.commonmodule.api;

import com.aimedical.common.base.BaseEnum;

/**
 * 岗位类型枚举
 *
 * <p>定义医生端不同岗位的类型，用于权限控制和菜单动态化。
 *
 * @author AIMedical Team
 * @version 1.0.0
 */
public enum PositionEnum implements BaseEnum {

    OUTPATIENT("OUTPATIENT", "门诊医生"),
    EXAMINATION("EXAMINATION", "检查医生"),
    LABTEST("LABTEST", "检验医生"),
    PHARMACY("PHARMACY", "药房医生"),
    RECEPTION("RECEPTION", "线下接诊医生");

    private final String code;
    private final String desc;

    PositionEnum(String code, String desc) {
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