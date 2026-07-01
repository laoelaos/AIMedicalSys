package com.aimedical.modules.medicalrecord.dto;

import com.aimedical.modules.medicalrecord.enums.MedicalRecordField;
import com.aimedical.modules.medicalrecord.exception.MedicalRecordErrorCode;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class RecordGenerateResponseTest {

    @Test
    void shouldCreateWithDefaultValues() {
        RecordGenerateResponse resp = new RecordGenerateResponse();
        assertNull(resp.getFields());
        assertNull(resp.getMissingFieldHints());
        assertFalse(resp.isFromFallback());
        assertFalse(resp.isDegraded());
        assertNull(resp.getErrorCode());
        assertFalse(resp.isSuccess());
    }

    @Test
    void shouldSetAndGetAllFields() {
        RecordGenerateResponse resp = new RecordGenerateResponse();
        Map<MedicalRecordField, String> fields = Map.of(MedicalRecordField.CHIEF_COMPLAINT, "头痛三天");
        List<FieldMissingHint> hints = List.of();
        resp.setFields(fields);
        resp.setMissingFieldHints(hints);
        resp.setFromFallback(true);
        resp.setDegraded(true);
        resp.setErrorCode(MedicalRecordErrorCode.MR_GEN_AI_TIMEOUT);
        resp.setSuccess(true);

        assertSame(fields, resp.getFields());
        assertSame(hints, resp.getMissingFieldHints());
        assertTrue(resp.isFromFallback());
        assertTrue(resp.isDegraded());
        assertEquals(MedicalRecordErrorCode.MR_GEN_AI_TIMEOUT, resp.getErrorCode());
        assertTrue(resp.isSuccess());
    }
}
