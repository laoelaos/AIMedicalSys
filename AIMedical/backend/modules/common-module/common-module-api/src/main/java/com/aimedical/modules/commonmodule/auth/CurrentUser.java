package com.aimedical.modules.commonmodule.auth;

import com.aimedical.modules.commonmodule.api.UserType;

public interface CurrentUser {
    Long getUserId();
    String getUsername();
    UserType getUserType();
}
