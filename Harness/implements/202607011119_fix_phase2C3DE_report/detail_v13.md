# 详细设计（v13 r2）

## 概述

修复 v12 验证遗留的 3 类 side-effect（F1-F7），仅涉及 medical-record 模块内的 2 个测试文件 + 1 个生产文件。无新增功能，均为机械性适配修正。

## 文件规划

| 文件路径（相对 `AIMedical/backend/modules/`） | 操作 | 涉及 |
|------|------|:----:|
| `medical-record/.../detector/MissingFieldDetectorImplTest.java` | 修改 | F1-F5 |
| `medical-record/.../service/impl/MedicalRecordServiceImplTest.java` | 修改 | F6, F7 |
| `medical-record/.../converter/MedicalRecordConverter.java` | 修改 | F7 |

## 类型定义

### 无新增类型

本任务不新增任何 class、interface 或 enum。所有变更均为既有方法体/声明的机械性适配。

### SameThreadExecutor（新增内部类）

**形态**：class（private static inner class）
**包路径**：`com.aimedical.modules.medicalrecord.service.impl.MedicalRecordServiceImplTest`
**职责**：同步执行器的简单实现，所有 `execute(Runnable)` 在调用线程上同步运行，用于 F6 确保 `resolveVisitId` 不消费中断标记。

**公开接口**：
- `void execute(Runnable command)` — 同步执行
- `void shutdown()` / `List<Runnable> shutdownNow()` — no-op
- `boolean isShutdown()` — 返回 false
- `boolean isTerminated()` — 返回 true
- `boolean awaitTermination(long, TimeUnit)` — 返回 true
- `<T> Future<T> submit(Callable<T>)` / `submit(Runnable, T)` / `submit(Runnable)` — 同步执行并返回已完成 future
- `<T> List<Future<T>> invokeAll(...)` / `T invokeAny(...)` — 委托 submit 或直接调用

**构造方式**：直接 `new SameThreadExecutor()`
**类型关系**：implements `java.util.concurrent.ExecutorService`

## F1-F5 — MissingFieldDetectorImplTest 模板字段集修正

**文件**：`MissingFieldDetectorImplTest.java`

**变更 1**：`setUp()` 第 29 行 — 过滤 MISSING_FIELDS/PARTIAL_CONTENT
```
旧：Stream.of(MedicalRecordField.values()).collect(Collectors.toSet())
新：Stream.of(MedicalRecordField.values())
       .filter(f -> f != MedicalRecordField.MISSING_FIELDS && f != MedicalRecordField.PARTIAL_CONTENT)
       .collect(Collectors.toSet())
```
效果：`template.requiredFields` 大小从 9 降为 7，与 `toFieldsMap` 返回值一致。

**变更 2**：`shouldReturnHintsForAllFieldsWhenAllNull`（第 89 行）
```
旧：assertEquals(9, hints.size())
新：assertEquals(7, hints.size())
```

**变更 3**：`shouldDetectMultipleMissingFields`（第 82 行）
```
旧：assertEquals(5, hints.size())
新：assertEquals(3, hints.size())
```
理由：resp 将 5 个字段设为 null（ChiefComplaint、SymptomDescription、PresentIllness、MissingFields、PartialContent），但 MissingFields/PartialContent 不在 fieldsMap 中，只有前 3 个业务字段被检出为缺失。

**变更 4**：`shouldResolveAllPlaceholdersForAllFields`（第 141、151-152 行）
```
旧：assertEquals(9, hints.size())
新：assertEquals(7, hints.size())
```
移除 expectedPrompts 中 MISSING_FIELDS 和 PARTIAL_CONTENT 的条目（第 151-152 行删除）。

**无需变更的测试**：其余测试的断言数据在 template 缩小后仍然成立：
- `shouldReturnEmptyHintsWhenAllFieldsAreFilled`：fullResponse() 填充全部 7 个业务字段 → `hints.isEmpty()` 恒成立
- `shouldReturnHintForNullField`：1 hint（ChiefComplaint null）→ `assertEquals(1, hints.size())` 不变
- `shouldReturnHintForEmptyStringField`：同上
- `shouldReturnHintForBlankStringField`：同上
- `shouldResolvePlaceholderInPromptMessage`：ChiefComplaint 唯一缺失 → `hints.get(0)` 恒为 ChiefComplaint 的 hint

## F6 — MedicalRecordServiceImplTest.shouldReturnInterruptedOnInterruptedException

**根因**：`generate()` 第 81 行先调用 `resolveVisitId()`，其内部 `future.get(visitFacadeTimeout, SECONDS)` 在需要等待时调用 `Thread.interrupted()` 消费中断标记。当 `shouldReturnInterruptedOnInterruptedException` 在当前线程设置中断标记后调用 `generate()`，`resolveVisitId` 抢先消费中断，导致 `callAiWithTimeout` 的 `future.get()` 无法感知中断。

**修复方案**：使用同步执行器替换原有线程池，使 `resolveVisitId` 的 `supplyAsync` 在调用线程同步完成，`future.get()` 返回已完成 future（不检查 `Thread.interrupted()`），中断标记得以保留到 `callAiWithTimeout`。

**变更 1**：`setUp()` 第 56 行 — 替换 executor 为同步执行器

**文件**：`MedicalRecordServiceImplTest.java`

```
旧：medicalRecordExecutor = Executors.newSingleThreadExecutor();
新：medicalRecordExecutor = new SameThreadExecutor();
```

**理由**：
- `SameThreadExecutor.execute(Runnable)` 在调用线程同步运行任务
- `resolveVisitId` 内的 `CompletableFuture.supplyAsync(supplier, executor)` 提交任务后，任务立即在当前线程完成，future 即刻处于已完成状态
- `future.get(visitFacadeTimeout, SECONDS)` 返回已完成 future，**不调用 `Thread.interrupted()`**
- 中断标记完整保留，`callAiWithTimeout` 的 `future.get(aiTimeout, SECONDS)` 等待时正确感知中断 → `InterruptedException` → degraded 结果

**副作用分析**：同步执行器对所有测试的影响：

| 测试 | resolveVisitId 执行路径 | 是否受影响 |
|------|----------------------|:--------:|
| `shouldReturnVisitNotFoundWhenEncounterIdIsNull` | encounterId=null，直接 return null，不使用 executor | 无 |
| `shouldReturnVisitNotFoundWhenEncounterIdIsEmpty` | 同上 | 无 |
| `shouldUseFallbackWhenVisitFacadeTimesOut` | stub 抛 RuntimeException → future 异常完成 → get() 抛 ExecutionException → catch → fallback | 行为不变 |
| `shouldUseFallbackWhenVisitFacadeThrowsException` | 同上 | 行为不变 |
| `shouldReturnDegradedWhenAiTimesOut` | stub 返回值 → future 正常完成 → get() 返回 → 正常路径 | 行为不变 |
| `shouldReturnInterruptedOnInterruptedException` | **同步完成**，不消费中断 | 修复目标 |
| 其余测试 | 均使用 returnValue，同步完成，无异常 | 行为不变 |

**变更 2**：`shouldReturnInterruptedOnInterruptedException`（原第 137 行）— future 改为未完成

```
旧：aiService.resultFuture = CompletableFuture.completedFuture(AiResult.success(createAiResponse()));
新：aiService.resultFuture = new CompletableFuture<>();
```
理由：已预完成的 future 在 `callAiWithTimeout.get()` 中立即返回，不会进入等待状态触发中断检测。未完成的 future 使 `get(12, SECONDS)` 进入 WAITING 状态，此时 `Thread.interrupted()` 返回 true → 抛出 `InterruptedException`。

**变更 3**：`shouldReturnInterruptedOnInterruptedException`（第 146-149 行）— 补充 errorCode 断言

```
旧：
assertFalse(response.isSuccess());
assertTrue(response.isDegraded());

新：
assertFalse(response.isSuccess());
assertTrue(response.isDegraded());
assertEquals(MedicalRecordErrorCode.MR_GEN_AI_INTERRUPTED, response.getErrorCode());
```

**执行路径验证**：
1. `SameThreadExecutor` 使 `resolveVisitId` 的 `supplyAsync` 同步完成，`future.get()` 返回 "V001"（不检查中断）
2. `Thread.interrupted()` 仍为 true（未被消费）
3. `callAiWithTimeout` 的 `future.get(12, SECONDS)` 进入等待 → 检测到 `Thread.interrupted() == true` → 抛出 `InterruptedException`
4. `catch (InterruptedException e)` → `Thread.currentThread().interrupt()`（恢复中断标记）→ 返回 `AiResultFactory.degraded(...)` 含 `errorCode="MR_GEN_AI_INTERRUPTED"`
5. `F7a` 的 `MedicalRecordErrorCode.valueOf("MR_GEN_AI_INTERRUPTED")` 解析为 `MedicalRecordErrorCode.MR_GEN_AI_INTERRUPTED`
6. `success` 条件不匹配 `MR_GEN_AI_TIMEOUT` → `success=false`
7. 所有断言通过

## F7 — MedicalRecordServiceImplTest.shouldReturnDegradedWhenAiTimesOut + MedicalRecordConverter

### F7a — MedicalRecordConverter.toRecordGenerateResponse 错误码动态解析

**文件**：`MedicalRecordConverter.java:61-66`

```
旧：
if (aiResult.getErrorCode() != null && MedicalRecordErrorCode.MR_GEN_AI_TIMEOUT.name().equals(aiResult.getErrorCode())) {
    response.setErrorCode(MedicalRecordErrorCode.MR_GEN_AI_TIMEOUT);
}
boolean success = (aiResult.isSuccess() && aiResult.getData() != null)
        || MedicalRecordErrorCode.MR_GEN_AI_TIMEOUT.name().equals(aiResult.getErrorCode());

新：
if (aiResult.getErrorCode() != null) {
    try {
        response.setErrorCode(MedicalRecordErrorCode.valueOf(aiResult.getErrorCode()));
    } catch (IllegalArgumentException ignored) {
    }
}
boolean success = (aiResult.isSuccess() && aiResult.getData() != null)
    || MedicalRecordErrorCode.MR_GEN_AI_TIMEOUT.name().equals(aiResult.getErrorCode());
```
关键点：
- `setErrorCode`：使用 `MedicalRecordErrorCode.valueOf()` 动态解析任意已知错误码字符串（MR_GEN_AI_TIMEOUT、MR_GEN_AI_INTERRUPTED、MR_GEN_AI_EXECUTION_ERROR 等）
- `success` 条件：**保持仅 MR_GEN_AI_TIMEOUT 视为 success**（与修订说明 r1 一致），其他错误码（含 MR_GEN_AI_EXECUTION_ERROR）视为失败
- `IllegalArgumentException` 捕获：当 aiResult 返回未知错误码字符串时静默忽略，errorCode 保持 null

### F7b — MedicalRecordServiceImplTest.shouldReturnDegradedWhenAiTimesOut 断言修正

**文件**：`MedicalRecordServiceImplTest.java:129-131`

```
旧：
assertTrue(response.isSuccess());
assertTrue(response.isDegraded());
assertEquals(MedicalRecordErrorCode.MR_GEN_AI_TIMEOUT, response.getErrorCode());

新：
assertFalse(response.isSuccess());
assertTrue(response.isDegraded());
assertEquals(MedicalRecordErrorCode.MR_GEN_AI_EXECUTION_ERROR, response.getErrorCode());
```
理由：`supplyAsync(() -> { throw ... })` 触发 `ExecutionException` 路径，返回 `MR_GEN_AI_EXECUTION_ERROR`，该错误码在 success 条件中不视为成功。

## 错误处理

| 场景 | 策略 |
|------|------|
| `MedicalRecordErrorCode.valueOf()` 未知错误码 | catch `IllegalArgumentException`，response.errorCode 保持 null |
| F1-F5 模板缩小后 | `detect()` 结果正确反映 7 个业务字段的缺失状态 |
| F6 同步执行器 | `resolveVisitId` 不消费中断标记，中断按预期传递到 `callAiWithTimeout` |

## 行为契约

- 变更后 `MissingFieldDetectorImplTest.setUp()` 创建的 template 仅含 7 个业务字段，`Detect` 结果不包含 MISSING_FIELDS/PARTIAL_CONTENT
- `MedicalRecordConverter.toRecordGenerateResponse` 动态解析 `aiResult.getErrorCode()` 到 `MedicalRecordErrorCode` 枚举；`success` 条件保持 `MR_GEN_AI_TIMEOUT` 为唯一视为成功的错误码
- `MedicalRecordServiceImplTest` 使用 `SameThreadExecutor`，使 `resolveVisitId` 的 `supplyAsync` 在调用线程同步完成，`future.get()` 返回已完成 future 不检查中断
- `shouldReturnInterruptedOnInterruptedException` 中 `callAiWithTimeout` 正确感知中断标记并返回 `MR_GEN_AI_INTERRUPTED` 降级结果

## 依赖关系

| 依赖 | 说明 |
|------|------|
| `MedicalRecordField.MISSING_FIELDS` / `PARTIAL_CONTENT` | F1-F5 模板过滤中使用的枚举常量 |
| `MedicalRecordErrorCode.MR_GEN_AI_EXECUTION_ERROR` | F7 测试断言使用的错误码枚举值 |
| `MedicalRecordErrorCode.MR_GEN_AI_INTERRUPTED` | F6 补充断言使用的错误码枚举值 |
| `MedicalRecordErrorCode.valueOf()` | F7 转换器中使用的 JDK 枚举反向解析方法 |
| `IllegalArgumentException` | F7 转换器中捕获未知错误码的异常类型 |
| `java.util.concurrent.ExecutorService` | SameThreadExecutor 实现的接口，替换原有线程池 |
| `CompletableFuture.supplyAsync(Supplier, Executor)` | F6 中 resolveVisitId 使用，同步执行器确保任务即刻完成 |

## 修订说明（v13 r2）

| 审查意见 | 修改措施 |
|---------|---------|
| [严重] F6 方案未解决 resolveVisitId 抢先消费中断标记的问题 | 将 executor 从 `Executors.newSingleThreadExecutor()` 改为 `SameThreadExecutor`（同步执行器），使 `resolveVisitId` 的 `supplyAsync` 在调用线程同步完成，`future.get()` 不检查 `Thread.interrupted()`。补充完整副作用分析表。 |
| [轻微] F6 缺少 errorCode 断言 | 在 `shouldReturnInterruptedOnInterruptedException` 中补充 `assertEquals(MedicalRecordErrorCode.MR_GEN_AI_INTERRUPTED, response.getErrorCode())` |
