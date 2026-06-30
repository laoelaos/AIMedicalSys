package com.aimedical.modules.patient.api;

import com.aimedical.common.result.Result;
import com.aimedical.modules.commonmodule.api.AuthService;
import com.aimedical.modules.commonmodule.api.dto.CurrentUserResponse;
import com.aimedical.modules.patient.dto.CancelResponse;
import com.aimedical.modules.patient.dto.RegistrationRequest;
import com.aimedical.modules.patient.dto.RegistrationResponse;
import com.aimedical.modules.patient.service.RegistrationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/patient/registration")
public class RegistrationController {

    private final RegistrationService registrationService;
    private final AuthService authService;

    public RegistrationController(RegistrationService registrationService, AuthService authService) {
        this.registrationService = registrationService;
        this.authService = authService;
    }

    @PostMapping
    public Result<RegistrationResponse> create(@Valid @RequestBody RegistrationRequest request) {
        CurrentUserResponse user = authService.getCurrentUser();
        return Result.success(registrationService.create(request, user.getUserId()));
    }

    @GetMapping
    public Result<List<RegistrationResponse>> list() {
        CurrentUserResponse user = authService.getCurrentUser();
        return Result.success(registrationService.listByUser(user.getUserId()));
    }

    @PostMapping("/{id}/cancel")
    public Result<CancelResponse> cancel(@PathVariable Long id) {
        CurrentUserResponse user = authService.getCurrentUser();
        return Result.success(registrationService.cancel(id, user.getUserId()));
    }
}
