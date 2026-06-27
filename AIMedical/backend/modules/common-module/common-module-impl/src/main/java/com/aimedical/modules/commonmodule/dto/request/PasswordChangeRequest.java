package com.aimedical.modules.commonmodule.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PasswordChangeRequest(
    @NotBlank @Size(max = 128) String oldPassword,
    @NotBlank @Size(min = 8, max = 64) String newPassword
) {}
