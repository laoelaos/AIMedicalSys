package com.aimedical.modules.commonmodule.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
    @NotBlank String username,
    @NotBlank @Size(min = 1, max = 64) String password
) {}
