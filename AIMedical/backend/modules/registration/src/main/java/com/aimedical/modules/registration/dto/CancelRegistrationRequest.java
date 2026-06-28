package com.aimedical.modules.registration.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CancelRegistrationRequest {

    @NotBlank(message = "取消原因不能为空")
    private String cancelReason;

}