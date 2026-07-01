# 任务指令（v9）

## 动作
NEW

## 任务描述
**R9: A09+M01+A07+A11+M08 — AuditConverter前置检查+错误码+AiResult契约**

实现 5 项修复，**实施顺序**：A09 → A07 → A11（M01/M08 无前后依赖）：

1. **A09**: `PrescriptionAuditServiceImpl.audit()` 中调用 `toAuditResponse()` 前检查 `aiResult.isSuccess() != true || aiResult.getData() == null` 时走降级路径（追加 WARN 日志 + 返回降级 AuditResponse）
2. **M01**: `MedicalRecordErrorCode` 补充 4 个错误码：`MR_GEN_AI_UNAVAILABLE` / `MR_GEN_AI_INPUT_INVALID` / `MR_GEN_AI_OUTPUT_INCOMPLETE` / `MR_GEN_TEMPLATE_LOAD_FAILED`；同步更新 `MedicalRecordErrorCodeTest`（常量计数 4→8，新增断言）
3. **A07**: `AiResult.success(T data)` 增加 `Objects.requireNonNull(data)`
4. **A11**: 移除业务层 `all && getData() != null` 冗余防御检查（共 4 处：`PrescriptionAuditServiceImpl.java:92` / `:333`、`PrescriptionAssistServiceImpl.java:86`、`TriageServiceImpl.java:107`）
5. **M08**: `MedicalRecordServiceImpl.callAiWithTimeout` 中 3 处 `new AiResult<>()` + setter 替换为 `AiResultFactory` 静态工厂方法调用

## 选择理由
A09(P1) 降级前置检查是 AiResult 契约修复的前置条件。M01(P0) 缺失错误码。A07(P2)+A11(P2)+M08(P2) 为 AiResult 契约增强。此轮将 A09 前移以满足 A09→A07→A11 修复顺序。R8 已完成，consultation 模块 140 测试通过。

## 任务上下文
### A09 — PrescriptionAuditServiceImpl 降级前置检查
当前 `PrescriptionAuditServiceImpl.audit()` L92-104 已有降级分支，但 `else` 块缺少 `log.warn()`。需在 `fromFallback = true;` 之前追加 WARN 日志：
```java
log.warn("AI service unavailable, switching to local rule engine. aiResult={}", 
    aiResult != null ? aiResult.getErrorCode() : "null");
```
同时确认 fallback 路径构造的 `AuditResponse` 完整（riskLevel/alerts/interactions/suggestions/fromFallback）。

涉及文件：`AIMedical/backend/modules/prescription/src/main/java/com/aimedical/modules/prescription/service/audit/impl/PrescriptionAuditServiceImpl.java`

### M01 — MedicalRecordErrorCode 补充 4 错误码
当前枚举（4 个）：
- MR_GEN_VISIT_NOT_FOUND / MR_GEN_AI_TIMEOUT / MR_GEN_STREAM_NOT_SUPPORTED / MR_GEN_CONCURRENT_MODIFICATION

新增：
- `MR_GEN_AI_UNAVAILABLE("MR_GEN_AI_UNAVAILABLE", "AI 服务不可用")`
- `MR_GEN_AI_INPUT_INVALID("MR_GEN_AI_INPUT_INVALID", "AI 输入参数不合法")`
- `MR_GEN_AI_OUTPUT_INCOMPLETE("MR_GEN_AI_OUTPUT_INCOMPLETE", "AI 输出不完整")`
- `MR_GEN_TEMPLATE_LOAD_FAILED("MR_GEN_TEMPLATE_LOAD_FAILED", "模板加载失败")`

涉及文件：
- `AIMedical/backend/modules/medical-record/src/main/java/com/aimedical/modules/medicalrecord/exception/MedicalRecordErrorCode.java`
- `AIMedical/backend/modules/medical-record/src/test/java/com/aimedical/modules/medicalrecord/enums/MedicalRecordErrorCodeTest.java` — 更新 `shouldHaveFourConstants` → `shouldHaveEightConstants`，增加 4 组 getCode/getMessage 断言

### A07 — AiResult.success requireNonNull
```java
// 改前
public static <T> AiResult<T> success(T data) {
    return new AiResult<>(true, data, null, false, null);
}
// 改后
public static <T> AiResult<T> success(T data) {
    return new AiResult<>(true, Objects.requireNonNull(data), null, false, null);
}
```
需要 `import java.util.Objects;`。
涉及文件：`AIMedical/backend/modules/ai/ai-api/src/main/java/com/aimedical/modules/ai/api/AiResult.java`
同步更新 `AiResultTest.java`：`shouldCreateSuccessResultWithNullData` 测试需移除（null data 不再合法）或改为 `assertThrows(NullPointerException.class, () -> AiResult.success(null))`。

### A11 — 移除冗余 `&& getData() != null` 防御检查
A07 确保 `success()` 返回的 data 永不 null，以下 4 处可安全移除：
1. `PrescriptionAuditServiceImpl.java:92`: `aiResult != null && aiResult.isSuccess() && aiResult.getData() != null` → `aiResult != null && aiResult.isSuccess()`
2. `PrescriptionAuditServiceImpl.java:333`: `aiResult != null && aiResult.isSuccess() && aiResult.getData() != null` → `aiResult != null && aiResult.isSuccess()`
3. `PrescriptionAssistServiceImpl.java:86`: `aiResult.isSuccess() && aiResult.getData() != null` → `aiResult.isSuccess()`
4. `TriageServiceImpl.java:107`: `aiResult != null && aiResult.isSuccess() && aiResult.getData() != null` → `aiResult != null && aiResult.isSuccess()`

### M08 — 替换 `new AiResult<>()` + setter 为 AiResultFactory（补充 errorCode 参数重载）

**前置：`AiResultFactory` 增加 `degraded(fallbackReason, errorCode, partialData)` 重载**

```java
// AiResultFactory 新增
public static <T> AiResult<T> degraded(String fallbackReason, String errorCode, T partialData) {
    return new AiResult<>(false, partialData, errorCode, true, fallbackReason);
}
```

`MedicalRecordServiceImpl.callAiWithTimeout()` 中 3 处相同的 `new AiResult<>()` + setter 模式：
```java
AiResult<MedicalRecordGenResponse> result = new AiResult<>();
result.setSuccess(false);
result.setErrorCode("MR_GEN_AI_TIMEOUT");
result.setDegraded(true);
result.setData(null);
return result;
```
替换为：
```java
return AiResultFactory.degraded("AI medical record generation timeout", "MR_GEN_AI_TIMEOUT", null);
```

选择3参重载而非默认的2参形式，是因为 `MedicalRecordConverter.toRecordGenerateResponse()`（L53-55）显式检查 `"MR_GEN_AI_TIMEOUT".equals(aiResult.getErrorCode())` 来设置响应中的 errorCode，测试 `toRecordGenerateResponseShouldSetTimeoutErrorCode` 也验证此行为。若丢失 errorCode，超时场景下 `response.getErrorCode()` 将为 null。

涉及文件：
- `AIMedical/backend/modules/ai/ai-api/src/main/java/com/aimedical/modules/ai/api/AiResultFactory.java`
- `AIMedical/backend/modules/medical-record/src/main/java/com/aimedical/modules/medicalrecord/service/impl/MedicalRecordServiceImpl.java`

## 已有代码上下文

### 当前 PrescriptionAuditServiceImpl.audit() 降级分支（L92-104）
```java
if (aiResult != null && aiResult.isSuccess() && aiResult.getData() != null) {
    response = auditConverter.toAuditResponse(aiResult);
} else {
    fromFallback = true;
    List<LocalRuleResult> ruleResults = localRuleEngine.check(request);
    AuditRiskLevel aggregated = aggregateRiskLevel(ruleResults);
    response = new AuditResponse();
    response.setRiskLevel(aggregated);
    response.setAlerts(Collections.emptyList());
    response.setInteractions(Collections.emptyList());
    response.setSuggestions(Collections.emptyList());
    response.setFromFallback(true);
}
```

### 当前 MedicalRecordServiceImpl.callAiWithTimeout()（L135-163）
3 处 `new AiResult<>()` + setter 模式（TimeoutException / InterruptedException / ExecutionException）。

### 当前 MedicalRecordErrorCode（4 个枚举值）
详见文件。

### 当前 AiResult.success()
无 null 检查。

## 验收标准
1. **编译通过**：全量 `mvn compile` 无错误
2. **A09**: PrescriptionAuditServiceImpl 降级路径输出 WARN 日志含 errorCode
3. **M01**: `MedicalRecordErrorCode.values().length == 8`，新 4 码 getCode/getMessage 正确
4. **A07**: `AiResult.success(null)` 抛出 `NullPointerException`
5. **A11**: 4 处 `getData() != null` 防御检查已移除，业务逻辑不变
6. **M08**: `MedicalRecordServiceImpl` 中无 `new AiResult<>()` + setter 模式
7. **测试**: MedicalRecordErrorCodeTest（8 常量 + 唯一码）、AiResultTest（NPE 版本）、全量回归（prescription 预存失败除外）

---

## 修订说明（v9 r1）
| 审查意见 | 修改措施 |
|---------|---------|
| M08: `AiResultFactory.degraded()` 丢失 errorCode `"MR_GEN_AI_TIMEOUT"`，导致调用方 `MedicalRecordConverter.toRecordGenerateResponse()` 无法设置错误码 | `AiResultFactory` 新增 `degraded(fallbackReason, errorCode, partialData)` 3参重载，M08 使用 `AiResultFactory.degraded("AI medical record generation timeout", "MR_GEN_AI_TIMEOUT", null)` 保留 errorCode |
| M01 测试方法 `shouldHaveFourConstants` 需同步重命名 | 任务已正确要求重命名为 `shouldHaveEightConstants`，无需额外修改 |
| A09 WARN 日志格式未精确定义 | 任务上下文 A09 补充了明确的 WARN 日志格式：`log.warn("AI service unavailable, switching to local rule engine. aiResult={}", aiResult != null ? aiResult.getErrorCode() : "null")` |
