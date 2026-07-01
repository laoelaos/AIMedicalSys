package com.aimedical.modules.prescription.service.assist;

import com.aimedical.modules.prescription.dto.assist.AiSuggestionResult;
import com.aimedical.modules.prescription.dto.assist.DosageCheckRequest;
import com.aimedical.modules.prescription.dto.assist.DosageCheckResponse;
import com.aimedical.modules.prescription.dto.assist.PrescriptionAssistRequest;
import com.aimedical.modules.prescription.dto.assist.PrescriptionAssistResponse;

public interface PrescriptionAssistService {

    PrescriptionAssistResponse assist(PrescriptionAssistRequest request);

    DosageCheckResponse checkDose(DosageCheckRequest request);

    AiSuggestionResult getSuggestion(String taskId);
}
