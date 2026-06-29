package com.aimedical.modules.commonmodule.api;

public interface UserQueryService {

    UserDto findByUsername(String username);

    UserDto findById(Long userId);

    void updateUserFields(Long userId, String email, Integer age);
}
