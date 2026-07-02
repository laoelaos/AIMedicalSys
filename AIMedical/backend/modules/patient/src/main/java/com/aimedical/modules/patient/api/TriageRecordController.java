package com.aimedical.modules.patient.api;

import com.aimedical.common.exception.BusinessException;
import com.aimedical.common.exception.GlobalErrorCode;
import com.aimedical.common.result.Result;
import com.aimedical.modules.commonmodule.api.AuthService;
import com.aimedical.modules.commonmodule.api.dto.CurrentUserResponse;
import com.aimedical.modules.patient.dto.TriageRecordResponse;
import com.aimedical.modules.patient.service.TriageRecordService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/patient/triage-records")
@PreAuthorize("hasRole('PATIENT')")
public class TriageRecordController {

    private final TriageRecordService triageRecordService;
    private final AuthService authService;

    public TriageRecordController(TriageRecordService triageRecordService, AuthService authService) {
        this.triageRecordService = triageRecordService;
        this.authService = authService;
    }

    @GetMapping
    public Result<Page<TriageRecordResponse>> list(
            @RequestParam(required = false) Long patientId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(required = false) Boolean degraded,
            Pageable pageable) {

        CurrentUserResponse user = authService.getCurrentUser();
        boolean isPatient = user.getRoles() != null && user.getRoles().contains("PATIENT");
        if (patientId != null && isPatient && !patientId.equals(user.getUserId())) {
            throw new BusinessException(GlobalErrorCode.FORBIDDEN, "无权查看其他患者的分诊记录");
        }
        Long targetPatientId = patientId != null ? patientId : user.getUserId();

        if (Boolean.TRUE.equals(degraded)) {
            return Result.success(triageRecordService.listDegraded(targetPatientId, pageable));
        }
        if (startTime != null && endTime != null) {
            return Result.success(triageRecordService.listByTimeRange(targetPatientId, startTime, endTime, pageable));
        }
        return Result.success(triageRecordService.listByPatient(targetPatientId, pageable));
    }
}
