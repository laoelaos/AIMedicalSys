package com.aimedical.modules.patient.api;

import com.aimedical.common.result.Result;
import com.aimedical.modules.patient.service.PatientService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/patient")
public class PatientController {

    private final PatientService patientService;

    public PatientController(PatientService patientService) {
        this.patientService = patientService;
    }

    @GetMapping("/placeholder")
    public Result<String> placeholder() {
        return patientService.getPlaceholder();
    }
}
