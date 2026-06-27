package com.aimedical.modules.commonmodule.auth;

import java.util.Set;

public record UserInfoResponse(
    Long id,
    String username,
    String realName,
    String phone,
    String email,
    String role,
    String position,
    Set<String> permissions
) {}
