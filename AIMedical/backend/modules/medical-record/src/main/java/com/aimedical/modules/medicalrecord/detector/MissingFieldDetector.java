package com.aimedical.modules.medicalrecord.detector;

import java.util.List;

import com.aimedical.modules.ai.api.dto.medicalrecord.MedicalRecordGenResponse;
import com.aimedical.modules.medicalrecord.dto.FieldMissingHint;
import com.aimedical.modules.medicalrecord.template.DepartmentTemplateConfig;

public interface MissingFieldDetector {
    List<FieldMissingHint> detect(MedicalRecordGenResponse aiResponse, DepartmentTemplateConfig template);
}
