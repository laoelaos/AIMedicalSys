package com.aimedical.modules.medicalrecord.dto;

import java.util.List;
import java.util.Map;

import com.aimedical.modules.medicalrecord.enums.MedicalRecordField;
import com.aimedical.modules.medicalrecord.exception.MedicalRecordErrorCode;

public class RecordGenerateResponse {
    private Map<MedicalRecordField, String> fields;
    private List<FieldMissingHint> missingFieldHints;
    private boolean fromFallback;
    private boolean degraded;
    private MedicalRecordErrorCode errorCode;
    private boolean success;

    public Map<MedicalRecordField, String> getFields() {
        return fields;
    }

    public void setFields(Map<MedicalRecordField, String> fields) {
        this.fields = fields;
    }

    public List<FieldMissingHint> getMissingFieldHints() {
        return missingFieldHints;
    }

    public void setMissingFieldHints(List<FieldMissingHint> missingFieldHints) {
        this.missingFieldHints = missingFieldHints;
    }

    public boolean isFromFallback() {
        return fromFallback;
    }

    public void setFromFallback(boolean fromFallback) {
        this.fromFallback = fromFallback;
    }

    public boolean isDegraded() {
        return degraded;
    }

    public void setDegraded(boolean degraded) {
        this.degraded = degraded;
    }

    public MedicalRecordErrorCode getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(MedicalRecordErrorCode errorCode) {
        this.errorCode = errorCode;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }
}
