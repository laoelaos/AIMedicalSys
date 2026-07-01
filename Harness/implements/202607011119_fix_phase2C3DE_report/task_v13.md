# 任务指令（v13）

## 动作
RETRY

## 任务描述

修复 medical-record 模块 v12 验证失败的 7 项测试（F1-F7），涉及 2 个测试文件 + 1 个生产文件的适配修改。无新增功能。

## 选择理由

R12 完成 17 项生产代码变更（9a-9q），但遗留了 3 类 side-effect：
1. 9e 排除了 MISSING_FIELDS/PARTIAL_CONTENT 元数据字段，但 MissingFieldDetectorImplTest 测试模板仍包含全部 9 个枚举值（F1-F5）
2. 9c 新增 InterruptedException 处理，但测试 mock 使用已预完成的 future 无法触发等待中断（F6）
3. 9c 新增 MR_GEN_AI_EXECUTION_ERROR 错误码，但 toRecordGenerateResponse 只识别 MR_GEN_AI_TIMEOUT（F7）

本任务仅局限在 medical-record 模块内修复，风险低、范围小。

## 任务上下文

### 失败详情

| # | 测试类 | 方法 | 行号 | 根因 |
|---|--------|------|:----:|------|
| F1 | MissingFieldDetectorImplTest | shouldReturnHintForEmptyStringField | 60 | setUp() 创建包含全部 9 个 MedicalRecordField 的 template，但 toFieldsMap 9e 后仅返回 7 个业务字段，MISSING_FIELDS/PARTIAL_CONTENT 始终被 detect 为缺失 |
| F2 | MissingFieldDetectorImplTest | shouldReturnHintForNullField | 51 | 同 F1 |
| F3 | MissingFieldDetectorImplTest | shouldResolvePlaceholderInPromptMessage | 97 | 同 F1，PARTIAL_CONTENT 被当作字段名解析 |
| F4 | MissingFieldDetectorImplTest | shouldReturnHintForBlankStringField | 69 | 同 F1 |
| F5 | MissingFieldDetectorImplTest | shouldReturnEmptyHintsWhenAllFieldsAreFilled | 43 | 同 F1，9 field template 中 MISSING_FIELDS/PARTIAL_CONTENT 不在 fieldsMap → detect 返回 2 hint |
| F6 | MedicalRecordServiceImplTest | shouldReturnInterruptedOnInterruptedException | 146 | aiService.resultFuture 使用 `completedFuture(...)` 已预完成，callAiWithTimeout 中 `.get()` 不等待也不检查中断标记 → 返回成功结果而非 degraded |
| F7 | MedicalRecordServiceImplTest | shouldReturnDegradedWhenAiTimesOut | 129 | supplyAsync 抛出 RuntimeException → ExecutionException 路径返回 MR_GEN_AI_EXECUTION_ERROR，但 toRecordGenerateResponse 只识别 MR_GEN_AI_TIMEOUT → success=false |

### F1-F5 修复方案: MissingFieldDetectorImplTest 模板字段集修正

**文件**：`MissingFieldDetectorImplTest.java`

**变更 1**：setUp() 第29行过滤 MISSING_FIELDS/PARTIAL_CONTENT

```java
// 旧
Set<MedicalRecordField> allFields = Stream.of(MedicalRecordField.values()).collect(Collectors.toSet());

// 新
Set<MedicalRecordField> allFields = Stream.of(MedicalRecordField.values())
    .filter(f -> f != MedicalRecordField.MISSING_FIELDS && f != MedicalRecordField.PARTIAL_CONTENT)
    .collect(Collectors.toSet());
```

**变更 2**：shouldReturnHintsForAllFieldsWhenAllNull 第89行
- `assertEquals(9, hints.size())` → `assertEquals(7, hints.size())`

**变更 3**：shouldDetectMultipleMissingFields 第82行
- `assertEquals(5, hints.size())` → `assertEquals(3, hints.size())`

**变更 4**：shouldResolveAllPlaceholdersForAllFields 第141行
- `assertEquals(9, hints.size())` → `assertEquals(7, hints.size())`
- 第151-152行 expectedPrompts 中移除 MISSING_FIELDS 和 PARTIAL_CONTENT 条目

### F6 修复方案: MedicalRecordServiceImplTest.shouldReturnInterruptedOnInterruptedException

**文件**：`MedicalRecordServiceImplTest.java:137`

```java
// 旧
aiService.resultFuture = CompletableFuture.completedFuture(AiResult.success(createAiResponse()));

// 新
aiService.resultFuture = new CompletableFuture<>();
```

理由：已预完成的 future 调用 `.get()` 立即返回（不检查中断标记），从而 `callAiWithTimeout` 不会进入 InterruptedException catch 块。用未完成的 future 使 `.get(12, SECONDS)` 等待期间检测到中断标记 → 抛出 InterruptedException → 返回 degraded 结果。

### F7 修复方案: MedicalRecordServiceImplTest + MedicalRecordConverter

**文件 1**：`MedicalRecordServiceImplTest.java:129-131`

```java
// 旧
assertTrue(response.isSuccess());
assertTrue(response.isDegraded());
assertEquals(MedicalRecordErrorCode.MR_GEN_AI_TIMEOUT, response.getErrorCode());

// 新
assertFalse(response.isSuccess());
assertTrue(response.isDegraded());
assertEquals(MedicalRecordErrorCode.MR_GEN_AI_EXECUTION_ERROR, response.getErrorCode());
```

**文件 2**：`MedicalRecordConverter.java:61-66` — toRecordGenerateResponse 错误码识别

```java
// 旧
if (aiResult.getErrorCode() != null && MedicalRecordErrorCode.MR_GEN_AI_TIMEOUT.name().equals(aiResult.getErrorCode())) {
    response.setErrorCode(MedicalRecordErrorCode.MR_GEN_AI_TIMEOUT);
}
boolean success = (aiResult.isSuccess() && aiResult.getData() != null)
        || MedicalRecordErrorCode.MR_GEN_AI_TIMEOUT.name().equals(aiResult.getErrorCode());

// 新
if (aiResult.getErrorCode() != null) {
    try {
        response.setErrorCode(MedicalRecordErrorCode.valueOf(aiResult.getErrorCode()));
    } catch (IllegalArgumentException ignored) {
    }
}
boolean success = (aiResult.isSuccess() && aiResult.getData() != null)
    || MedicalRecordErrorCode.MR_GEN_AI_TIMEOUT.name().equals(aiResult.getErrorCode());
```

## 已有代码上下文

- `MissingFieldDetectorImplTest.java:29` → setUp 创建含全部 9 枚举值的 template
- `MissingFieldDetectorImplTest.java:51,60,69,82,89,97,141` → 受影响断言行
- `MedicalRecordServiceImplTest.java:137` → F6 aiService.resultFuture mock
- `MedicalRecordServiceImplTest.java:129-131` → F7 断言块
- `MedicalRecordConverter.java:61-66` → toRecordGenerateResponse 硬编码只识别 MR_GEN_AI_TIMEOUT
- `MedicalRecordErrorCode` 枚举值：MR_GEN_AI_TIMEOUT, MR_GEN_AI_INTERRUPTED, MR_GEN_AI_EXECUTION_ERROR 等，`valueOf()` 可反向解析错误码字符串到枚举常量

## 修订说明（v13 r1）
| 审查意见 | 修改措施 |
|---------|---------|
| [严重] F7 success 计算逻辑：`response.getErrorCode() != null` 使任意错误码均算 success，与测试 `assertFalse(response.isSuccess())` 矛盾 | success 条件恢复为仅 `MR_GEN_AI_TIMEOUT.name().equals(aiResult.getErrorCode())`，保留动态错误码解析（`MedicalRecordErrorCode.valueOf`）用于 `setErrorCode` |
