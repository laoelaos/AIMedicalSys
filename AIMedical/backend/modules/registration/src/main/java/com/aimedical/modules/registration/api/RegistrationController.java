package com.aimedical.modules.registration.api;

import com.aimedical.common.result.Result;
import com.aimedical.modules.registration.dto.CancelRegistrationRequest;
import com.aimedical.modules.registration.dto.RegistrationDTO;
import com.aimedical.modules.registration.dto.TriageRecordDTO;
import com.aimedical.modules.registration.service.RegistrationService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/registration")
public class RegistrationController {

    private final RegistrationService registrationService;

    public RegistrationController(RegistrationService registrationService) {
        this.registrationService = registrationService;
    }

    @PostMapping
    public Result<RegistrationDTO> createRegistration(@Valid @RequestBody RegistrationDTO dto) {
        return Result.success(registrationService.createRegistration(dto));
    }

    @GetMapping("/{id}")
    public Result<RegistrationDTO> getRegistration(@PathVariable Long id) {
        return Result.success(registrationService.getRegistration(id));
    }

    @GetMapping("/patient/{patientId}")
    public Result<Page<RegistrationDTO>> getByPatient(@PathVariable Long patientId, Pageable pageable) {
        return Result.success(registrationService.getRegistrationsByPatient(patientId, pageable));
    }

    @GetMapping("/doctor/{doctorId}")
    public Result<Page<RegistrationDTO>> getByDoctor(@PathVariable Long doctorId, Pageable pageable) {
        return Result.success(registrationService.getRegistrationsByDoctor(doctorId, pageable));
    }

    @PostMapping("/{id}/confirm")
    public Result<RegistrationDTO> confirmRegistration(@PathVariable Long id) {
        return Result.success(registrationService.confirmRegistration(id));
    }

    @PostMapping("/{id}/complete")
    public Result<RegistrationDTO> completeRegistration(@PathVariable Long id) {
        return Result.success(registrationService.completeRegistration(id));
    }

    @PostMapping("/{id}/cancel")
    public Result<RegistrationDTO> cancelRegistration(@PathVariable Long id,
                                                      @Valid @RequestBody CancelRegistrationRequest request) {
        return Result.success(registrationService.cancelRegistration(id, request));
    }

    @PostMapping("/{id}/noshow")
    public Result<RegistrationDTO> markNoShow(@PathVariable Long id) {
        return Result.success(registrationService.markNoShow(id));
    }

    @PostMapping("/{id}/triage")
    public Result<TriageRecordDTO> createTriageRecord(@PathVariable Long id,
                                                      @Valid @RequestBody TriageRecordDTO dto) {
        dto.setRegistrationId(id);
        return Result.success(registrationService.createTriageRecord(dto));
    }

    @GetMapping("/{id}/triage")
    public Result<TriageRecordDTO> getTriageRecord(@PathVariable Long id) {
        return Result.success(registrationService.getTriageRecord(id));
    }
}