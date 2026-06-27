package com.aimedical.modules.registration.api;

import com.aimedical.common.result.Result;
import com.aimedical.modules.registration.dto.CancelRegistrationRequest;
import com.aimedical.modules.registration.dto.RegistrationDTO;
import com.aimedical.modules.registration.dto.TriageRecordDTO;
import com.aimedical.modules.registration.service.RegistrationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/registration")
public class RegistrationController {

    private final RegistrationService registrationService;

    public RegistrationController(RegistrationService registrationService) {
        this.registrationService = registrationService;
    }

    @PostMapping
    public Result<RegistrationDTO> createRegistration(@RequestBody RegistrationDTO dto) {
        return Result.success(registrationService.createRegistration(dto));
    }

    @GetMapping("/{id}")
    public Result<RegistrationDTO> getRegistration(@PathVariable Long id) {
        return Result.success(registrationService.getRegistration(id));
    }

    @GetMapping("/patient/{patientId}")
    public Result<List<RegistrationDTO>> getByPatient(@PathVariable Long patientId) {
        return Result.success(registrationService.getRegistrationsByPatient(patientId));
    }

    @GetMapping("/doctor/{doctorId}")
    public Result<List<RegistrationDTO>> getByDoctor(@PathVariable Long doctorId) {
        return Result.success(registrationService.getRegistrationsByDoctor(doctorId));
    }

    @PutMapping("/{id}/confirm")
    public Result<RegistrationDTO> confirmRegistration(@PathVariable Long id) {
        return Result.success(registrationService.confirmRegistration(id));
    }

    @PutMapping("/{id}/complete")
    public Result<RegistrationDTO> completeRegistration(@PathVariable Long id) {
        return Result.success(registrationService.completeRegistration(id));
    }

    @PutMapping("/{id}/cancel")
    public Result<RegistrationDTO> cancelRegistration(@PathVariable Long id,
                                                      @RequestBody CancelRegistrationRequest request) {
        return Result.success(registrationService.cancelRegistration(id, request));
    }

    @PutMapping("/{id}/noshow")
    public Result<RegistrationDTO> markNoShow(@PathVariable Long id) {
        return Result.success(registrationService.markNoShow(id));
    }

    @PostMapping("/{registrationId}/triage")
    public Result<TriageRecordDTO> createTriageRecord(@PathVariable Long registrationId,
                                                      @RequestBody TriageRecordDTO dto) {
        dto.setRegistrationId(registrationId);
        return Result.success(registrationService.createTriageRecord(dto));
    }

    @GetMapping("/{registrationId}/triage")
    public Result<TriageRecordDTO> getTriageRecord(@PathVariable Long registrationId) {
        return Result.success(registrationService.getTriageRecord(registrationId));
    }
}