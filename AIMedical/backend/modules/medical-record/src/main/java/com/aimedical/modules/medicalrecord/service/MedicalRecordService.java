package com.aimedical.modules.medicalrecord.service;

import com.aimedical.modules.medicalrecord.dto.RecordGenerateRequest;
import com.aimedical.modules.medicalrecord.dto.RecordGenerateResponse;

public interface MedicalRecordService {
    RecordGenerateResponse generate(RecordGenerateRequest request);
}
