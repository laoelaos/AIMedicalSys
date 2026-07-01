package com.aimedical.modules.consultation.api;

import com.aimedical.common.result.Result;
import com.aimedical.modules.consultation.dto.DialogueCreateRequest;
import com.aimedical.modules.consultation.dto.TriageResponse;
import com.aimedical.modules.consultation.service.TriageService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/triage")
public class TriageController {

    private final TriageService triageService;

    public TriageController(TriageService triageService) {
        this.triageService = triageService;
    }

    @PostMapping("/consult")
    public Result<TriageResponse> consult(@Valid @RequestBody DialogueCreateRequest request) {
        TriageResponse response = triageService.triage(request);
        return Result.success(response);
    }

    @PostMapping("/select-department")
    public Result<TriageResponse> selectDepartment(@RequestParam String sessionId,
                                                    @RequestParam String departmentId,
                                                    @RequestParam String departmentName) {
        TriageResponse response = triageService.selectDepartment(sessionId, departmentId, departmentName);
        return Result.success(response);
    }
}
