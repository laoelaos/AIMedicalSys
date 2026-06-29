package com.aimedical.modules.patient.exception;

import com.aimedical.common.exception.ErrorCode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PatientErrorCodeTest {

    @Test
    void shouldDefineFiveConstants() {
        assertEquals(5, PatientErrorCode.values().length);
        assertNotNull(PatientErrorCode.valueOf("PATIENT_NOT_FOUND"));
        assertNotNull(PatientErrorCode.valueOf("PATIENT_HEALTH_RECORD_NOT_FOUND"));
        assertNotNull(PatientErrorCode.valueOf("PATIENT_HEALTH_RECORD_FORBIDDEN"));
        assertNotNull(PatientErrorCode.valueOf("PATIENT_INVALID_GENDER"));
        assertNotNull(PatientErrorCode.valueOf("PATIENT_DUPLICATE_PHONE"));
    }

    @Test
    void shouldReturnCodeForNotFound() {
        assertEquals("PATIENT_NOT_FOUND", PatientErrorCode.PATIENT_NOT_FOUND.getCode());
    }

    @Test
    void shouldReturnMessageForNotFound() {
        assertEquals("患者不存在", PatientErrorCode.PATIENT_NOT_FOUND.getMessage());
    }

    @Test
    void shouldReturnCodeForHealthRecordNotFound() {
        assertEquals("PATIENT_HEALTH_RECORD_NOT_FOUND", PatientErrorCode.PATIENT_HEALTH_RECORD_NOT_FOUND.getCode());
    }

    @Test
    void shouldReturnMessageForHealthRecordNotFound() {
        assertEquals("健康档案记录不存在", PatientErrorCode.PATIENT_HEALTH_RECORD_NOT_FOUND.getMessage());
    }

    @Test
    void shouldReturnCodeForForbidden() {
        assertEquals("PATIENT_HEALTH_RECORD_FORBIDDEN", PatientErrorCode.PATIENT_HEALTH_RECORD_FORBIDDEN.getCode());
    }

    @Test
    void shouldReturnMessageForForbidden() {
        assertEquals("无权操作该健康档案记录", PatientErrorCode.PATIENT_HEALTH_RECORD_FORBIDDEN.getMessage());
    }

    @Test
    void shouldReturnCodeForInvalidGender() {
        assertEquals("PATIENT_INVALID_GENDER", PatientErrorCode.PATIENT_INVALID_GENDER.getCode());
    }

    @Test
    void shouldReturnMessageForInvalidGender() {
        assertEquals("无效的性别值", PatientErrorCode.PATIENT_INVALID_GENDER.getMessage());
    }

    @Test
    void shouldReturnCodeForDuplicatePhone() {
        assertEquals("PATIENT_DUPLICATE_PHONE", PatientErrorCode.PATIENT_DUPLICATE_PHONE.getCode());
    }

    @Test
    void shouldReturnMessageForDuplicatePhone() {
        assertEquals("手机号已被占用", PatientErrorCode.PATIENT_DUPLICATE_PHONE.getMessage());
    }

    @Test
    void shouldImplementErrorCode() {
        for (PatientErrorCode code : PatientErrorCode.values()) {
            assertInstanceOf(ErrorCode.class, code);
        }
    }
}
