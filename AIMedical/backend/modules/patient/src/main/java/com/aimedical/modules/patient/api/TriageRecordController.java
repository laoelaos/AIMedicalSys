package com.aimedical.modules.patient.api;

import com.aimedical.common.result.Result;
import com.aimedical.modules.commonmodule.api.AuthService;
import com.aimedical.modules.commonmodule.api.dto.CurrentUserResponse;
import com.aimedical.modules.patient.dto.TriageRecordResponse;
import com.aimedical.modules.patient.service.TriageRecordService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/patient/triage-records")
public class TriageRecordController {

    private final TriageRecordService triageRecordService;
    private final AuthService authService;

    public TriageRecordController(TriageRecordService triageRecordService, AuthService authService) {
        this.triageRecordService = triageRecordService;
        this.authService = authService;
    }

    @GetMapping
    public Result<List<TriageRecordResponse>> list(
            @RequestParam(required = false) Long patientId,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime,
            @RequestParam(required = false) Boolean degraded) {

        CurrentUserResponse user = authService.getCurrentUser();
        Long targetPatientId = patientId != null ? patientId : user.getUserId();

        if (Boolean.TRUE.equals(degraded)) {
            return Result.success(triageRecordService.listDegraded(targetPatientId));
        }
        if (startTime != null && endTime != null) {
            return Result.success(triageRecordService.listByTimeRange(targetPatientId, startTime, endTime));
        }
        return Result.success(triageRecordService.listByPatient(targetPatientId));
    }
}
