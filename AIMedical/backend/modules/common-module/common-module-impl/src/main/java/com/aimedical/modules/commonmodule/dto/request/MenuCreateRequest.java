package com.aimedical.modules.commonmodule.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record MenuCreateRequest(
    @NotBlank String name,
    @NotBlank String permission,
    Long parentId,
    String path,
    String component,
    String icon,
    Integer sort,
    @NotNull Boolean visible
) {}
