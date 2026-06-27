package com.aimedical.modules.commonmodule.auth.password;

import com.aimedical.common.exception.GlobalErrorCode;

public interface PasswordPolicy {
    GlobalErrorCode validate(String password, String username);
}
