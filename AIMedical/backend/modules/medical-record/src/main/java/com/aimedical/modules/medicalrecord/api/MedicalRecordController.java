package com.aimedical.modules.medicalrecord.api;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.aimedical.common.result.Result;
import com.aimedical.modules.medicalrecord.dto.RecordGenerateRequest;
import com.aimedical.modules.medicalrecord.dto.RecordGenerateResponse;
import com.aimedical.modules.medicalrecord.exception.MedicalRecordErrorCode;
import com.aimedical.modules.medicalrecord.service.MedicalRecordService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/medical-record")
public class MedicalRecordController {

    private final MedicalRecordService medicalRecordService;

    public MedicalRecordController(MedicalRecordService medicalRecordService) {
        this.medicalRecordService = medicalRecordService;
    }

    @PostMapping("/generate")
    public Result<RecordGenerateResponse> generate(@Valid @RequestBody RecordGenerateRequest request) {
        if (request.isStream()) {
            return Result.fail(MedicalRecordErrorCode.MR_GEN_STREAM_NOT_SUPPORTED);
        }
        RecordGenerateResponse response = medicalRecordService.generate(request);
        if (response.isSuccess()) {
            return Result.success(response);
        }
        return Result.fail(response.getErrorCode());
    }
}
