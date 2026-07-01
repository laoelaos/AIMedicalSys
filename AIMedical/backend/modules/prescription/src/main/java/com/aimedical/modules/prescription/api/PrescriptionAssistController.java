package com.aimedical.modules.prescription.api;

import com.aimedical.common.result.Result;
import com.aimedical.modules.prescription.dto.assist.AiSuggestionResult;
import com.aimedical.modules.prescription.dto.assist.DosageCheckRequest;
import com.aimedical.modules.prescription.dto.assist.DosageCheckResponse;
import com.aimedical.modules.prescription.dto.assist.PrescriptionAssistRequest;
import com.aimedical.modules.prescription.dto.assist.PrescriptionAssistResponse;
import com.aimedical.modules.prescription.service.assist.PrescriptionAssistService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/prescription")
public class PrescriptionAssistController {

    private final PrescriptionAssistService prescriptionAssistService;

    public PrescriptionAssistController(PrescriptionAssistService prescriptionAssistService) {
        this.prescriptionAssistService = prescriptionAssistService;
    }

    @PostMapping("/assist")
    public Result<PrescriptionAssistResponse> assist(@Valid @RequestBody PrescriptionAssistRequest request) {
        PrescriptionAssistResponse response = prescriptionAssistService.assist(request);
        return Result.success(response);
    }

    @PostMapping("/assist/check-dose")
    public Result<DosageCheckResponse> checkDose(@Valid @RequestBody DosageCheckRequest request) {
        DosageCheckResponse response = prescriptionAssistService.checkDose(request);
        return Result.success(response);
    }

    @GetMapping("/assist/suggestion/{taskId}")
    public ResponseEntity<Result<AiSuggestionResult>> getSuggestion(@PathVariable String taskId) {
        AiSuggestionResult result = prescriptionAssistService.getSuggestion(taskId);
        return ResponseEntity.ok(Result.success(result));
    }
}
