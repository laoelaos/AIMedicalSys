# 详细设计（v9）

## 概述

实现 5 项修复：A09（PrescriptionAuditServiceImpl 降级前置检查 + WARN 日志）、M01（MedicalRecordErrorCode 补充 4 错误码 + 测试更新）、A07（AiResult.success requireNonNull + 测试更新）、A11（移除 4 处冗余 `&& getData() != null` 防御检查）、M08（MedicalRecordServiceImpl.callAiWithTimeout 中 3 处 `new AiResult<>()` + setter 替换为 AiResultFactory 调用）。实施顺序：A09 → A07 → A11（M01/M08 无前后依赖）。

## 文件规划

| 文件路径 | 操作 | 职责 |
|---------|------|------|
| `modules/ai/ai-api/src/main/java/.../AiResult.java` | 修改 | A07: success() 增加 `Objects.requireNonNull(data)` |
| `modules/ai/ai-api/src/main/java/.../AiResultFactory.java` | 修改 | M08: 新增 `degraded(fallbackReason, errorCode, partialData)` 重载 |
| `modules/ai/ai-api/src/test/java/.../AiResultTest.java` | 修改 | A07: `shouldCreateSuccessResultWithNullData` → 验证 NPE |
| `modules/prescription/src/main/java/.../PrescriptionAuditServiceImpl.java` | 修改 | A09: 降级路径追加 WARN 日志；A11: 移除 L92、L333 的 `&& getData() != null` |
| `modules/prescription/src/main/java/.../PrescriptionAssistServiceImpl.java` | 修改 | A11: 移除 L86 的 `&& getData() != null` |
| `modules/consultation/src/main/java/.../TriageServiceImpl.java` | 修改 | A11: 移除 L107 的 `&& getData() != null` |
| `modules/medical-record/src/main/java/.../MedicalRecordErrorCode.java` | 修改 | M01: 新增 4 枚举值 |
| `modules/medical-record/src/test/java/.../MedicalRecordErrorCodeTest.java` | 修改 | M01: 常量计数 4→8，新增 4 组断言 |
| `modules/medical-record/src/main/java/.../MedicalRecordServiceImpl.java` | 修改 | M08: 3 处 `new AiResult<>()` + setter → `AiResultFactory.degraded()` |

## 类型定义

### AiResultFactory.degraded(fallbackReason, errorCode, partialData)

**形态**：静态方法（新增重载）
**包路径**：`com.aimedical.modules.ai.api.AiResultFactory`
**职责**：提供带 errorCode 参数的 degraded 工厂方法，供 M08 在超时场景保留 `MR_GEN_AI_TIMEOUT` 错误码

**公开接口**：
```java
public static <T> AiResult<T> degraded(String fallbackReason, String errorCode, T partialData)
```

**实现逻辑**：
```java
return new AiResult<>(false, partialData, errorCode, true, fallbackReason);
```

## 错误处理

- A07: `AiResult.success(T data)` 中 `Objects.requireNonNull(data)` 在 data==null 时抛出 `NullPointerException`（非受检异常），调用方无需 catch，按 Java 默认传播
- M01: 枚举值均为常量，无运行时异常路径；测试确保唯一码、正确码值

## 行为契约

### A09 — PrescriptionAuditServiceImpl.audit() 降级前置检查

```java
// 当前 L92:
if (aiResult != null && aiResult.isSuccess() && aiResult.getData() != null) {
// 改为:
if (aiResult != null && aiResult.isSuccess()) {
```
（A11 移除了 `&& aiResult.getData() != null`）

在 L92 `if` 的 `else` 分支（L94-104）中，`fromFallback = true;` 之前追加：
```java
log.warn("AI service unavailable, switching to local rule engine. aiResult={}",
    aiResult != null ? aiResult.getErrorCode() : "null");
```

现有降级路径构造的 `AuditResponse` 字段（riskLevel/alerts/interactions/suggestions/fromFallback）保持不变。

### M01 — MedicalRecordErrorCode 新增 4 枚举值

新增枚举值（按字母序插入）：
```java
MR_GEN_AI_UNAVAILABLE("MR_GEN_AI_UNAVAILABLE", "AI 服务不可用"),
MR_GEN_AI_INPUT_INVALID("MR_GEN_AI_INPUT_INVALID", "AI 输入参数不合法"),
MR_GEN_AI_OUTPUT_INCOMPLETE("MR_GEN_AI_OUTPUT_INCOMPLETE", "AI 输出不完整"),
MR_GEN_TEMPLATE_LOAD_FAILED("MR_GEN_TEMPLATE_LOAD_FAILED", "模板加载失败")
```

`MedicalRecordErrorCodeTest` 变更：
- `shouldHaveFourConstants` → `shouldHaveEightConstants`，断言 `assertEquals(8, ...)`
- `shouldReturnCorrectCodeAndMessage` 增加 4 组 `assertEquals(getCode()/getMessage())` 断言

### A07 — AiResult.success requireNonNull

```java
public static <T> AiResult<T> success(T data) {
    return new AiResult<>(true, Objects.requireNonNull(data), null, false, null);
}
```

需要 `import java.util.Objects;`。

`AiResultTest` 变更：
- 删除 `shouldCreateSuccessResultWithNullData` 方法
- 新增方法：
```java
@Test
void shouldThrowNpeWhenSuccessWithNullData() {
    assertThrows(NullPointerException.class, () -> AiResult.success(null));
}
```

### A11 — 移除 4 处 `&& getData() != null`

1. `PrescriptionAuditServiceImpl.java:92`:
   `aiResult != null && aiResult.isSuccess() && aiResult.getData() != null` → `aiResult != null && aiResult.isSuccess()`

2. `PrescriptionAuditServiceImpl.java:333`:
   `aiResult != null && aiResult.isSuccess() && aiResult.getData() != null` → `aiResult != null && aiResult.isSuccess()`

3. `PrescriptionAssistServiceImpl.java:86`:
   `aiResult.isSuccess() && aiResult.getData() != null` → `aiResult.isSuccess()`

4. `TriageServiceImpl.java:107`:
   `aiResult != null && aiResult.isSuccess() && aiResult.getData() != null` → `aiResult != null && aiResult.isSuccess()`

### M08 — MedicalRecordServiceImpl.callAiWithTimeout 替换 3 处 `new AiResult<>()` + setter

3 处相同结构（TimeoutException / InterruptedException / ExecutionException）统一替换为：
```java
return AiResultFactory.degraded("AI medical record generation timeout", "MR_GEN_AI_TIMEOUT", null);
```

需要 `import com.aimedical.modules.ai.api.AiResultFactory;`（如尚未引入）。

## 依赖关系

- M08 依赖 AiResultFactory 新增的 3 参 `degraded()` 重载（前置，本轮同步实现）
- A07 依赖 `java.util.Objects`（JDK 内置，无外部依赖）
- A09/A11 依赖 A07（实施顺序保证：A07 先于 A11，但代码层面无编译期依赖——A11 移除 `getData() != null` 后，`success()` 的 requireNonNull 保证 data 非空；A09 降级路径的 errorCode 日志不依赖 A07）
- M01 无外部依赖
