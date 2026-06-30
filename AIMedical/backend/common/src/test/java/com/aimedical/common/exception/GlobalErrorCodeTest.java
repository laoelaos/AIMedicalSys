package com.aimedical.common.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GlobalErrorCodeTest {

    @Test
    void shouldHaveExpectedConstants() {
        assertEquals(35, GlobalErrorCode.values().length);
        assertNotNull(GlobalErrorCode.valueOf("SUCCESS"));
        assertNotNull(GlobalErrorCode.valueOf("SYSTEM_ERROR"));
        assertNotNull(GlobalErrorCode.valueOf("PARAM_INVALID"));
        assertNotNull(GlobalErrorCode.valueOf("NOT_FOUND"));
        assertNotNull(GlobalErrorCode.valueOf("UNAUTHORIZED"));
        assertNotNull(GlobalErrorCode.valueOf("FORBIDDEN"));
        assertNotNull(GlobalErrorCode.valueOf("LOGIN_FAILED"));
        assertNotNull(GlobalErrorCode.valueOf("ACCOUNT_DISABLED"));
        assertNotNull(GlobalErrorCode.valueOf("ACCOUNT_LOCKED"));
        assertNotNull(GlobalErrorCode.valueOf("RATE_LIMITED"));
        assertNotNull(GlobalErrorCode.valueOf("RATE_LIMITED_GLOBAL"));
        assertNotNull(GlobalErrorCode.valueOf("PASSWORD_TOO_SHORT"));
        assertNotNull(GlobalErrorCode.valueOf("PASSWORD_TOO_LONG"));
        assertNotNull(GlobalErrorCode.valueOf("PASSWORD_WEAK"));
        assertNotNull(GlobalErrorCode.valueOf("PASSWORD_CONTAINS_USERNAME"));
        assertNotNull(GlobalErrorCode.valueOf("PASSWORD_COMMON"));
        assertNotNull(GlobalErrorCode.valueOf("TOKEN_REFRESH_FAILED"));
        assertNotNull(GlobalErrorCode.valueOf("PASSWORD_CHANGE_REQUIRED"));
        assertNotNull(GlobalErrorCode.valueOf("CHILDREN_EXIST"));
        assertNotNull(GlobalErrorCode.valueOf("PASSWORD_MISMATCH"));
        assertNotNull(GlobalErrorCode.valueOf("PRESCRIPTION_NOT_FOUND"));
        assertNotNull(GlobalErrorCode.valueOf("PRESCRIPTION_INVALID_STATE"));
        assertNotNull(GlobalErrorCode.valueOf("PRESCRIPTION_NOT_AUDITABLE"));
        assertNotNull(GlobalErrorCode.valueOf("MEDICAL_RECORD_NOT_FOUND"));
        assertNotNull(GlobalErrorCode.valueOf("MEDICAL_RECORD_INVALID_STATE"));
        assertNotNull(GlobalErrorCode.valueOf("CONSULTATION_NOT_FOUND"));
        assertNotNull(GlobalErrorCode.valueOf("CONSULTATION_NOT_CALLABLE"));
        assertNotNull(GlobalErrorCode.valueOf("AI_SERVICE_UNAVAILABLE"));
        assertNotNull(GlobalErrorCode.valueOf("REGISTRATION_STATUS_INVALID"));
        assertNotNull(GlobalErrorCode.valueOf("REGISTRATION_CANCEL_FORBIDDEN"));
        assertNotNull(GlobalErrorCode.valueOf("ORDER_STATUS_INVALID"));
        assertNotNull(GlobalErrorCode.valueOf("ORDER_ITEM_EMPTY"));
        assertNotNull(GlobalErrorCode.valueOf("CHARGE_PRE_ORDER_EXISTS"));
        assertNotNull(GlobalErrorCode.valueOf("TRIAGE_RECORD_EXISTS"));
    }

    @Test
    void successShouldReturnCorrectCodeAndMessage() {
        assertEquals("SUCCESS", GlobalErrorCode.SUCCESS.getCode());
        assertEquals("成功", GlobalErrorCode.SUCCESS.getMessage());
    }

    @Test
    void systemErrorShouldReturnCorrectCodeAndMessage() {
        assertEquals("SYSTEM_ERROR", GlobalErrorCode.SYSTEM_ERROR.getCode());
        assertEquals("系统异常", GlobalErrorCode.SYSTEM_ERROR.getMessage());
    }

    @Test
    void paramInvalidShouldReturnCorrectCodeAndMessage() {
        assertEquals("PARAM_INVALID", GlobalErrorCode.PARAM_INVALID.getCode());
        assertEquals("参数校验失败", GlobalErrorCode.PARAM_INVALID.getMessage());
    }

    @Test
    void notFoundShouldReturnCorrectCodeAndMessage() {
        assertEquals("NOT_FOUND", GlobalErrorCode.NOT_FOUND.getCode());
        assertEquals("资源不存在", GlobalErrorCode.NOT_FOUND.getMessage());
    }

    @Test
    void unauthorizedShouldReturnCorrectCodeAndMessage() {
        assertEquals("UNAUTHORIZED", GlobalErrorCode.UNAUTHORIZED.getCode());
        assertEquals("未认证或令牌已失效", GlobalErrorCode.UNAUTHORIZED.getMessage());
    }

    @Test
    void forbiddenShouldReturnCorrectCodeAndMessage() {
        assertEquals("FORBIDDEN", GlobalErrorCode.FORBIDDEN.getCode());
        assertEquals("无权限访问", GlobalErrorCode.FORBIDDEN.getMessage());
    }

    @Test
    void loginFailedShouldReturnCorrectCodeAndMessage() {
        assertEquals("LOGIN_FAILED", GlobalErrorCode.LOGIN_FAILED.getCode());
        assertEquals("用户名或密码错误", GlobalErrorCode.LOGIN_FAILED.getMessage());
    }

    @Test
    void accountDisabledShouldReturnCorrectCodeAndMessage() {
        assertEquals("ACCOUNT_DISABLED", GlobalErrorCode.ACCOUNT_DISABLED.getCode());
        assertEquals("账户已被管理员停用", GlobalErrorCode.ACCOUNT_DISABLED.getMessage());
    }

    @Test
    void accountLockedShouldReturnCorrectCodeAndMessage() {
        assertEquals("ACCOUNT_LOCKED", GlobalErrorCode.ACCOUNT_LOCKED.getCode());
        assertEquals("账户已锁定，请{锁定时间}后重试", GlobalErrorCode.ACCOUNT_LOCKED.getMessage());
    }

    @Test
    void passwordTooShortShouldReturnCorrectCodeAndMessage() {
        assertEquals("PASSWORD_TOO_SHORT", GlobalErrorCode.PASSWORD_TOO_SHORT.getCode());
        assertEquals("密码长度不能少于8位", GlobalErrorCode.PASSWORD_TOO_SHORT.getMessage());
    }

    @Test
    void passwordTooLongShouldReturnCorrectCodeAndMessage() {
        assertEquals("PASSWORD_TOO_LONG", GlobalErrorCode.PASSWORD_TOO_LONG.getCode());
        assertEquals("密码长度不能超过64位", GlobalErrorCode.PASSWORD_TOO_LONG.getMessage());
    }

    @Test
    void passwordWeakShouldReturnCorrectCodeAndMessage() {
        assertEquals("PASSWORD_WEAK", GlobalErrorCode.PASSWORD_WEAK.getCode());
        assertEquals("密码不符合复杂度要求", GlobalErrorCode.PASSWORD_WEAK.getMessage());
    }

    @Test
    void passwordContainsUsernameShouldReturnCorrectCodeAndMessage() {
        assertEquals("PASSWORD_CONTAINS_USERNAME", GlobalErrorCode.PASSWORD_CONTAINS_USERNAME.getCode());
        assertEquals("密码不能包含用户名", GlobalErrorCode.PASSWORD_CONTAINS_USERNAME.getMessage());
    }

    @Test
    void passwordCommonShouldReturnCorrectCodeAndMessage() {
        assertEquals("PASSWORD_COMMON", GlobalErrorCode.PASSWORD_COMMON.getCode());
        assertEquals("密码过于常见", GlobalErrorCode.PASSWORD_COMMON.getMessage());
    }

    @Test
    void tokenRefreshFailedShouldReturnCorrectCodeAndMessage() {
        assertEquals("TOKEN_REFRESH_FAILED", GlobalErrorCode.TOKEN_REFRESH_FAILED.getCode());
        assertEquals("令牌刷新失败，请重新登录", GlobalErrorCode.TOKEN_REFRESH_FAILED.getMessage());
    }

    @Test
    void passwordChangeRequiredShouldReturnCorrectCodeAndMessage() {
        assertEquals("PASSWORD_CHANGE_REQUIRED", GlobalErrorCode.PASSWORD_CHANGE_REQUIRED.getCode());
        assertEquals("需要修改密码", GlobalErrorCode.PASSWORD_CHANGE_REQUIRED.getMessage());
    }

    @Test
    void childrenExistShouldReturnCorrectCodeAndMessage() {
        assertEquals("CHILDREN_EXIST", GlobalErrorCode.CHILDREN_EXIST.getCode());
        assertEquals("存在子菜单，无法删除", GlobalErrorCode.CHILDREN_EXIST.getMessage());
    }

    @Test
    void passwordMismatchShouldReturnCorrectCodeAndMessage() {
        assertEquals("PASSWORD_MISMATCH", GlobalErrorCode.PASSWORD_MISMATCH.getCode());
        assertEquals("旧密码不正确", GlobalErrorCode.PASSWORD_MISMATCH.getMessage());
    }

    @Test
    void registrationStatusInvalidShouldReturnCorrectCodeAndMessage() {
        assertEquals("REGISTRATION_STATUS_INVALID", GlobalErrorCode.REGISTRATION_STATUS_INVALID.getCode());
        assertEquals("当前挂号状态不允许此操作", GlobalErrorCode.REGISTRATION_STATUS_INVALID.getMessage());
    }

    @Test
    void registrationCancelForbiddenShouldReturnCorrectCodeAndMessage() {
        assertEquals("REGISTRATION_CANCEL_FORBIDDEN", GlobalErrorCode.REGISTRATION_CANCEL_FORBIDDEN.getCode());
        assertEquals("预约时间距现在不足2小时，无法在线取消，请到窗口线下办理", GlobalErrorCode.REGISTRATION_CANCEL_FORBIDDEN.getMessage());
    }

    @Test
    void orderStatusInvalidShouldReturnCorrectCodeAndMessage() {
        assertEquals("ORDER_STATUS_INVALID", GlobalErrorCode.ORDER_STATUS_INVALID.getCode());
        assertEquals("当前订单状态不允许此操作", GlobalErrorCode.ORDER_STATUS_INVALID.getMessage());
    }

    @Test
    void orderItemEmptyShouldReturnCorrectCodeAndMessage() {
        assertEquals("ORDER_ITEM_EMPTY", GlobalErrorCode.ORDER_ITEM_EMPTY.getCode());
        assertEquals("订单至少需要包含一个项目", GlobalErrorCode.ORDER_ITEM_EMPTY.getMessage());
    }

    @Test
    void chargePreOrderExistsShouldReturnCorrectCodeAndMessage() {
        assertEquals("CHARGE_PRE_ORDER_EXISTS", GlobalErrorCode.CHARGE_PRE_ORDER_EXISTS.getCode());
        assertEquals("该订单已生成收费前置单，不可重复生成", GlobalErrorCode.CHARGE_PRE_ORDER_EXISTS.getMessage());
    }

    @Test
    void triageRecordExistsShouldReturnCorrectCodeAndMessage() {
        assertEquals("TRIAGE_RECORD_EXISTS", GlobalErrorCode.TRIAGE_RECORD_EXISTS.getCode());
        assertEquals("该挂号已存在分诊记录，不可重复创建", GlobalErrorCode.TRIAGE_RECORD_EXISTS.getMessage());
    }
}