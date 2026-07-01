package com.aimedical.modules.consultation.service;

import com.aimedical.modules.consultation.dto.DialogueCreateRequest;
import com.aimedical.modules.consultation.dto.TriageResponse;

public interface TriageService {

    TriageResponse triage(DialogueCreateRequest request);

    TriageResponse selectDepartment(String sessionId, String departmentId, String departmentName);
}
