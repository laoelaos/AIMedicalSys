package com.aimedical.modules.medicalrecord.exception;

import com.aimedical.common.exception.ErrorCode;

public enum MedicalRecordErrorCode implements ErrorCode {
    MR_GEN_AI_INPUT_INVALID("MR_GEN_AI_INPUT_INVALID", "AI 输入参数不合法"),
    MR_GEN_AI_OUTPUT_INCOMPLETE("MR_GEN_AI_OUTPUT_INCOMPLETE", "AI 输出不完整"),
    MR_GEN_AI_TIMEOUT("MR_GEN_AI_TIMEOUT", "AI 病历生成超时"),
    MR_GEN_AI_INTERRUPTED("MR_GEN_AI_INTERRUPTED", "AI 病历生成被中断"),
    MR_GEN_AI_EXECUTION_ERROR("MR_GEN_AI_EXECUTION_ERROR", "AI 病历生成执行异常"),
    MR_GEN_AI_UNAVAILABLE("MR_GEN_AI_UNAVAILABLE", "AI 服务不可用"),
    MR_GEN_CONCURRENT_MODIFICATION("MR_GEN_CONCURRENT_MODIFICATION", "病历数据并发修改"),
    MR_GEN_STREAM_NOT_SUPPORTED("MR_GEN_STREAM_NOT_SUPPORTED", "流式模式暂不支持"),
    MR_GEN_TEMPLATE_LOAD_FAILED("MR_GEN_TEMPLATE_LOAD_FAILED", "模板加载失败"),
    MR_GEN_VISIT_NOT_FOUND("MR_GEN_VISIT_NOT_FOUND", "未找到就诊记录");

    private final String code;
    private final String message;

    MedicalRecordErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
