package com.aimedical.modules.prescription;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PrescriptionErrorCodeTest {

    @Test
    void shouldDefineAllErrorCodes() {
        assertEquals(10, PrescriptionErrorCode.values().length);
    }

    @Test
    void shouldExposeCodeAndMessage() {
        assertEquals("RX_AUDIT_BLOCKED", PrescriptionErrorCode.RX_AUDIT_BLOCKED.getCode());
        assertEquals("审核阻断", PrescriptionErrorCode.RX_AUDIT_BLOCKED.getMessage());
        assertEquals("RX_AUDIT_PRESCRIPTION_MODIFIED", PrescriptionErrorCode.RX_AUDIT_PRESCRIPTION_MODIFIED.getCode());
        assertEquals("处方已变更", PrescriptionErrorCode.RX_AUDIT_PRESCRIPTION_MODIFIED.getMessage());
        assertEquals("RX_AUDIT_CONCURRENT_SUBMIT", PrescriptionErrorCode.RX_AUDIT_CONCURRENT_SUBMIT.getCode());
        assertEquals("RX_AUDIT_FORCE_SUBMIT_INVALID", PrescriptionErrorCode.RX_AUDIT_FORCE_SUBMIT_INVALID.getCode());
        assertEquals("RX_AUDIT_REVOKE_NOT_WARN", PrescriptionErrorCode.RX_AUDIT_REVOKE_NOT_WARN.getCode());
        assertEquals("RX_AUDIT_REVOKE_ALREADY_REVOKED", PrescriptionErrorCode.RX_AUDIT_REVOKE_ALREADY_REVOKED.getCode());
        assertEquals("RX_AUDIT_REVOKE_NOT_FOUND", PrescriptionErrorCode.RX_AUDIT_REVOKE_NOT_FOUND.getCode());

        assertEquals("RX_ASSIST_AI_NO_RECOMMENDATION", PrescriptionErrorCode.RX_ASSIST_AI_NO_RECOMMENDATION.getCode());
        assertEquals("RX_ASSIST_SUGGESTION_NOT_FOUND", PrescriptionErrorCode.RX_ASSIST_SUGGESTION_NOT_FOUND.getCode());
        assertEquals("RX_ASSIST_DOSE_STANDARD_NOT_FOUND", PrescriptionErrorCode.RX_ASSIST_DOSE_STANDARD_NOT_FOUND.getCode());
    }
}
