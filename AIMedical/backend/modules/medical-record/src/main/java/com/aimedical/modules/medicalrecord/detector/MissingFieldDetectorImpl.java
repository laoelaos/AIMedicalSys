package com.aimedical.modules.medicalrecord.detector;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.aimedical.modules.ai.api.dto.medicalrecord.MedicalRecordGenResponse;
import com.aimedical.modules.medicalrecord.converter.MedicalRecordConverter;
import com.aimedical.modules.medicalrecord.dto.FieldMissingHint;
import com.aimedical.modules.medicalrecord.enums.MedicalRecordField;
import com.aimedical.modules.medicalrecord.template.DepartmentTemplateConfig;

@Component
public class MissingFieldDetectorImpl implements MissingFieldDetector {

    private final MedicalRecordConverter converter;

    public MissingFieldDetectorImpl(MedicalRecordConverter converter) {
        this.converter = converter;
    }

    @Override
    public List<FieldMissingHint> detect(MedicalRecordGenResponse aiResponse, DepartmentTemplateConfig template) {
        Map<MedicalRecordField, String> fieldsMap = converter.toFieldsMap(aiResponse);
        List<FieldMissingHint> hints = new ArrayList<>();
        for (MedicalRecordField field : template.getRequiredFields()) {
            String value = fieldsMap.get(field);
            if (value == null || value.trim().isEmpty()) {
                hints.add(buildHint(field, template));
            }
        }
        return hints;
    }

    private FieldMissingHint buildHint(MedicalRecordField field, DepartmentTemplateConfig template) {
        FieldMissingHint hint = new FieldMissingHint();
        hint.setMissingField(field);
        String prompt = template.getPromptMessages().getOrDefault(field, "{{fieldName}}字段缺失");
        String action = template.getSuggestedActions().getOrDefault(field, "请补充{{fieldName}}信息");
        hint.setPromptMessage(resolvePlaceholders(prompt, field));
        hint.setSuggestedAction(resolvePlaceholders(action, field));
        return hint;
    }

    private String resolvePlaceholders(String text, MedicalRecordField field) {
        return text.replace("{{fieldName}}", getFieldName(field));
    }

    private String getFieldName(MedicalRecordField field) {
        switch (field) {
            case CHIEF_COMPLAINT: return "主诉";
            case SYMPTOM_DESCRIPTION: return "症状描述";
            case PRESENT_ILLNESS: return "现病史";
            case PAST_HISTORY: return "既往史";
            case PHYSICAL_EXAM: return "体格检查";
            case PRELIMINARY_DIAGNOSIS: return "初步诊断";
            case TREATMENT_PLAN: return "治疗方案";
            default: return field.name();
        }
    }
}
