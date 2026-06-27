package com.aimedical.modules.commonmodule.auth;

public interface UserFacade {
    UserInfoResponse findById(Long userId);
    UserInfoResponse findByUsername(String username);
    boolean existsById(Long userId);
}
