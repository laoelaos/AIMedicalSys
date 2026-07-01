package com.aimedical.modules.medicalrecord.enums;

import com.aimedical.common.exception.ErrorCode;
import com.aimedical.modules.medicalrecord.exception.MedicalRecordErrorCode;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MedicalRecordErrorCodeTest {

    @Test
    void shouldImplementErrorCodeInterface() {
        for (MedicalRecordErrorCode code : MedicalRecordErrorCode.values()) {
            assertInstanceOf(ErrorCode.class, code);
        }
    }

    @Test
    void shouldHaveTenConstants() {
        assertEquals(10, MedicalRecordErrorCode.values().length);
    }

    @Test
    void shouldReturnCorrectCodeAndMessage() {
        assertEquals("MR_GEN_VISIT_NOT_FOUND", MedicalRecordErrorCode.MR_GEN_VISIT_NOT_FOUND.getCode());
        assertEquals("未找到就诊记录", MedicalRecordErrorCode.MR_GEN_VISIT_NOT_FOUND.getMessage());

        assertEquals("MR_GEN_AI_TIMEOUT", MedicalRecordErrorCode.MR_GEN_AI_TIMEOUT.getCode());
        assertEquals("AI 病历生成超时", MedicalRecordErrorCode.MR_GEN_AI_TIMEOUT.getMessage());

        assertEquals("MR_GEN_STREAM_NOT_SUPPORTED", MedicalRecordErrorCode.MR_GEN_STREAM_NOT_SUPPORTED.getCode());
        assertEquals("流式模式暂不支持", MedicalRecordErrorCode.MR_GEN_STREAM_NOT_SUPPORTED.getMessage());

        assertEquals("MR_GEN_CONCURRENT_MODIFICATION", MedicalRecordErrorCode.MR_GEN_CONCURRENT_MODIFICATION.getCode());
        assertEquals("病历数据并发修改", MedicalRecordErrorCode.MR_GEN_CONCURRENT_MODIFICATION.getMessage());

        assertEquals("MR_GEN_AI_UNAVAILABLE", MedicalRecordErrorCode.MR_GEN_AI_UNAVAILABLE.getCode());
        assertEquals("AI 服务不可用", MedicalRecordErrorCode.MR_GEN_AI_UNAVAILABLE.getMessage());

        assertEquals("MR_GEN_AI_INPUT_INVALID", MedicalRecordErrorCode.MR_GEN_AI_INPUT_INVALID.getCode());
        assertEquals("AI 输入参数不合法", MedicalRecordErrorCode.MR_GEN_AI_INPUT_INVALID.getMessage());

        assertEquals("MR_GEN_AI_OUTPUT_INCOMPLETE", MedicalRecordErrorCode.MR_GEN_AI_OUTPUT_INCOMPLETE.getCode());
        assertEquals("AI 输出不完整", MedicalRecordErrorCode.MR_GEN_AI_OUTPUT_INCOMPLETE.getMessage());

        assertEquals("MR_GEN_TEMPLATE_LOAD_FAILED", MedicalRecordErrorCode.MR_GEN_TEMPLATE_LOAD_FAILED.getCode());
        assertEquals("模板加载失败", MedicalRecordErrorCode.MR_GEN_TEMPLATE_LOAD_FAILED.getMessage());

        assertEquals("MR_GEN_AI_INTERRUPTED", MedicalRecordErrorCode.MR_GEN_AI_INTERRUPTED.getCode());
        assertEquals("AI 病历生成被中断", MedicalRecordErrorCode.MR_GEN_AI_INTERRUPTED.getMessage());

        assertEquals("MR_GEN_AI_EXECUTION_ERROR", MedicalRecordErrorCode.MR_GEN_AI_EXECUTION_ERROR.getCode());
        assertEquals("AI 病历生成执行异常", MedicalRecordErrorCode.MR_GEN_AI_EXECUTION_ERROR.getMessage());
    }

    @Test
    void shouldHaveUniqueCodes() {
        java.util.Set<String> codes = new java.util.HashSet<>();
        for (MedicalRecordErrorCode code : MedicalRecordErrorCode.values()) {
            assertTrue(codes.add(code.getCode()), "Duplicate code: " + code.getCode());
        }
    }
}
