package com.aimedical.modules.commonmodule.auth.password;

public interface PasswordChangeService {
    boolean isChangeRequired(Long userId);
    void markChangeRequired(Long userId);
    void clearChangeRequired(Long userId);
}
