package com.aimedical.modules.admin.entity;

import com.aimedical.common.base.BaseEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum LoginType implements BaseEnum {

    LOGIN("LOGIN", "登录"),
    LOGOUT("LOGOUT", "登出"),
    REFRESH("REFRESH", "刷新令牌");

    private final String code;
    private final String desc;
}
