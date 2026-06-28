package com.aimedical.modules.doctor.dto.response;

/**
 * AI 接口统一响应包装。
 *
 * <p>对外暴露三类状态：
 * <ul>
 *   <li>{@code success=true, degraded=false}：AI 正常返回结果，{@code data} 有效</li>
 *   <li>{@code success=false, degraded=true}：AI 不可用已降级，{@code fallbackReason} 说明降级原因，
 *       {@code data} 携带兜底建议（如人工录入指引、通用检查项清单等），前端需显式展示降级标识</li>
 *   <li>{@code success=false, degraded=false}：AI 调用失败（非降级），{@code errorCode} 提供错误码</li>
 * </ul>
 *
 * <p>该包装与 {@link com.aimedical.modules.ai.api.AiResult} 对齐，
 * 由 doctor-ai 控制器在调用 AiService 后映射生成。
 *
 * @author AIMedical Team
 * @version 1.0.0
 */
public record AiResultResponse<T>(
    boolean success,
    boolean degraded,
    String fallbackReason,
    String errorCode,
    T data
) {

    public static <T> AiResultResponse<T> ok(T data) {
        return new AiResultResponse<>(true, false, null, null, data);
    }

    public static <T> AiResultResponse<T> degraded(T fallbackData, String reason) {
        return new AiResultResponse<>(false, true, reason, null, fallbackData);
    }

    public static <T> AiResultResponse<T> error(String errorCode) {
        return new AiResultResponse<>(false, false, null, errorCode, null);
    }
}
