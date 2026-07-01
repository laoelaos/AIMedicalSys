# 计划审查报告（v9 r1）

## 审查结果
REJECTED

## 发现

### **[一般]** M08: `AiResultFactory.degraded()` 丢失 errorCode `"MR_GEN_AI_TIMEOUT"`

**问题**: M08 任务将 `MedicalRecordServiceImpl.callAiWithTimeout()` 中 3 处 `new AiResult<>()` + setter 替换为 `AiResultFactory.degraded("AI medical record generation timeout", null)`。但 `AiResultFactory.degraded()` 的 errorCode 为 `null`，而原始代码显式设置 `errorCode = "MR_GEN_AI_TIMEOUT"`。

**为什么是问题**: `MedicalRecordConverter.toRecordGenerateResponse()`（MedicalRecordConverter.java:53-55）明确检查 `"MR_GEN_AI_TIMEOUT".equals(aiResult.getErrorCode())` 来设置响应中的 `errorCode`，测试 `toRecordGenerateResponseShouldSetTimeoutErrorCode` 也验证此行为。如果 M08 简单替换为 `degraded()`，则超时场景下 `response.getErrorCode()` 为 `null`，调用方无法获知超时错误。现有的 AI 超时专用错误码 `MR_GEN_AI_TIMEOUT` 将被静默丢失。

**期望修正方向**: 在 `AiResultFactory` 中增加一个工厂方法（例如 `timeout(String errorCode, T partialData)`），同时设置 `degraded=true` 和 `errorCode`，或在 `AiResultFactory` 增加 `degraded(String fallbackReason, String errorCode, T partialData)` 重载。然后 M08 使用该新工厂方法以保留 errorCode 语义。

### **[轻微]** M01 测试方法命名需要同步更新

`MedicalRecordErrorCodeTest.shouldHaveFourConstants` 需要重命名为 `shouldHaveEightConstants`（task_v9.md 已明确要求，属于正确指引）。

### **[轻微]** A09 WARN 日志格式未精确定义

任务要求追加 WARN 日志"含 errorCode/异常原因"，但未指定具体格式。实施时需确保日志内容可追溯定位。

## 修改要求（仅 REJECTED 时）

1. **M08 errorCode 丢失问题**（一般）：请明确 M08 的实施方案——是在 `AiResultFactory` 新增 timeout 工厂方法同时保留 errorCode，还是明确接受 degraded 语义并同步修改 `MedicalRecordConverter.toRecordGenerateResponse()` 不再依赖 errorCode 字符串匹配。当前 "可考虑" 的措辞给实施者留下歧义，可能导致错误码丢失的缺陷。
