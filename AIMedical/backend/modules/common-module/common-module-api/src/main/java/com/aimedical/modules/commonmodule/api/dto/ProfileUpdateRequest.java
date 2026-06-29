package com.aimedical.modules.commonmodule.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ProfileUpdateRequest(
    @NotBlank(message = "昵称不能为空") @Size(max = 50) String nickname,
    @Pattern(regexp = "^1[3-9]\\d{9}$") String phone,
    @Email @Size(max = 100) String email
) {}
