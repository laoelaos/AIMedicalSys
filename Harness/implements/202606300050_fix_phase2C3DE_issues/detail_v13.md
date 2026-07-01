# 详细设计（v13）

## 概述

手术式定点修复 prescription 模块 5 个测试阻断项，解除全量构建阻塞。仅改动 5 个文件（4 测试 + 1 生产代码），不做 scope creep。

## 文件规划

| 文件路径 | 操作 | 职责 |
|---------|------|------|
| `AIMedical/backend/modules/prescription/src/test/java/com/aimedical/modules/prescription/PrescriptionErrorCodeTest.java` | 修改 | L21 错误码消息断言值与生产代码同步 |
| `AIMedical/backend/modules/prescription/src/test/java/com/aimedical/modules/prescription/rule/DosageLimitRuleTest.java` | 修改 | L145 期望严重级别从 BLOCK 改为 WARN |
| `AIMedical/backend/modules/prescription/src/main/java/com/aimedical/modules/prescription/service/audit/impl/PrescriptionAuditServiceImpl.java` | 修改 | `buildStepThreeResponse` 新增 BLOCK 分支 |
| `AIMedical/backend/modules/prescription/src/test/java/com/aimedical/modules/prescription/service/assist/impl/PrescriptionAssistServiceImplTest.java` | 修改 | L84 后新增 `allergyCheckRule.check()` mock stub |
| `AIMedical/backend/modules/prescription/src/test/java/com/aimedical/modules/prescription/service/audit/impl/PrescriptionAuditServiceImplTest.java` | 修改 | L378 mock 抛出的异常类型从 `OptimisticLockException` 改为 `ObjectOptimisticLockingFailureException` |

## 类型定义

### PrescriptionErrorCode（枚举）
**形态**：enum implements ErrorCode
**包路径**：`com.aimedical.modules.prescription`
**职责**：处方模块错误码定义

**相关枚举常量**：
```java
RX_AUDIT_WARN_REQUIRES_FORCE_SUBMIT("RX_AUDIT_WARN_REQUIRES_FORCE_SUBMIT", "WARN 审核未确认，需 forceSubmit=true 放行")
```
**当前生产消息**：`"WARN 审核未确认，需 forceSubmit=true 放行"`（PrescriptionErrorCode.java:9）

### DosageLimitRule（组件）
**形态**：class
**包路径**：`com.aimedical.modules.prescription.rule`
**公开方法**：
- `LocalRuleResult check(AuditRequest request)`

**剂量严重级别判定逻辑**：
- `dose > singleMax * 2` → BLOCK（严格大于）
- `dose > singleMax` → WARN
- 否则 → PASS

### AuditRiskLevel（枚举）
**形态**：enum
**包路径**：`com.aimedical.modules.prescription.service.audit`
**枚举值**：`PASS`, `WARN`, `BLOCK`

### BlockResponse（DTO）
**形态**：class
**包路径**：`com.aimedical.modules.prescription.dto.audit`
**构造方式**：
```java
new BlockResponse(List<String> blockReasons, String blockCode, LocalDateTime blockTime)
```

### SubmitResponse（DTO）
**形态**：class
**包路径**：`com.aimedical.modules.prescription.dto.audit`
**属性**：`boolean submitted`, `String prescriptionOrderId`, `BlockResponse blockInfo`, `String errorCode`

### LocalRuleResult（DTO）
**形态**：class
**包路径**：`com.aimedical.modules.prescription.rule`
**构造方式**：
```java
new LocalRuleResult(String ruleId, boolean passed, String message, AuditRiskLevel severity)
```

### AllergyCheckRule（组件）
**形态**：class
**包路径**：`com.aimedical.modules.prescription.rule`
**公开方法**：
- `LocalRuleResult check(AuditRequest request)`

### ObjectOptimisticLockingFailureException（Spring）
**全限定名**：`org.springframework.orm.ObjectOptimisticLockingFailureException`
**构造方式**：
```java
new ObjectOptimisticLockingFailureException(String resourceDescription, Throwable cause)
```

## 详细变更规格

### 变更 1：PrescriptionErrorCodeTest.java L21

**文件**：`src/test/java/.../PrescriptionErrorCodeTest.java`
**位置**：`shouldExposeCodeAndMessage()` 方法内 L21
**当前代码**：
```java
assertEquals("WARN审核未确认", PrescriptionErrorCode.RX_AUDIT_WARN_REQUIRES_FORCE_SUBMIT.getMessage());
```
**改为**：
```java
assertEquals("WARN 审核未确认，需 forceSubmit=true 放行", PrescriptionErrorCode.RX_AUDIT_WARN_REQUIRES_FORCE_SUBMIT.getMessage());
```
**说明**：生产代码 PrescriptionErrorCode 枚举的 message 已在之前轮次更新为更详细文案，测试断言值未同步。

### 变更 2：DosageLimitRuleTest.java L145

**文件**：`src/test/java/.../rule/DosageLimitRuleTest.java`
**位置**：`shouldMatchByAgeWhenPatientInfoAvailable()` 方法内 L145
**当前代码**：
```java
assertEquals(AuditRiskLevel.BLOCK, result.getSeverity());
```
**改为**：
```java
assertEquals(AuditRiskLevel.WARN, result.getSeverity());
```
**说明**：测试数据 dose=100, singleMax=50, dose 恰好等于 2×singleMax。生产代码条件 `dose.compareTo(singleMax * 2) > 0`（DosageLimitRule.java:43）在 dose 等于 2×singleMax 时 evaluate 为 false，落入 WARN 分支（dose > singleMax 为 true）。因此正确期望为 WARN 而非 BLOCK。

### 变更 3：PrescriptionAuditServiceImpl.java buildStepThreeResponse 新增 BLOCK 分支

**文件**：`src/main/java/.../service/audit/impl/PrescriptionAuditServiceImpl.java`
**方法**：`buildStepThreeResponse(SubmitRequest, AuditResponse, AuditRecord)` — L256-L282
**当前代码**（L258-L277 两个 if 之间）：
```java
if (riskLevel == AuditRiskLevel.PASS) {
    SubmitResponse resp = new SubmitResponse();
    resp.setSubmitted(true);
    resp.setPrescriptionOrderId("RX-" + System.currentTimeMillis());
    return resp;
}
// ← 此处插入 BLOCK 分支
if (riskLevel == AuditRiskLevel.WARN) {
    ...
}
```
**插入代码**（在 PASS 和 WARN 两个 `if` 块之间）：
```java
if (riskLevel == AuditRiskLevel.BLOCK) {
    SubmitResponse resp = new SubmitResponse();
    resp.setSubmitted(false);
    BlockResponse blockInfo = new BlockResponse(
        List.of("Prescription audit blocked"),
        "RX_BLOCK_AUDIT",
        LocalDateTime.now());
    resp.setBlockInfo(blockInfo);
    return resp;
}
```
**说明**：R9 A09 变更后 AI 返回 BLOCK 时进入 `buildStepThreeResponse`，但该方法缺少 BLOCK 分支，误落入兜底 `submitted=true` 逻辑。测试 `submitShouldReAuditWhenNoLatestRecordFoundThenReturnBlock`（L524）期望 `assertFalse(result.isSubmitted())` 并检查 `blockInfo.getBlockCode() == "RX_BLOCK_AUDIT"`。新增分支即可匹配期望。

**所需新增 import**：（无需新增，`java.util.List` 和 `java.time.LocalDateTime` 已在文件顶部 import）

### 变更 4：PrescriptionAssistServiceImplTest.java 新增 allergyCheckRule stub

**文件**：`src/test/java/.../service/assist/impl/PrescriptionAssistServiceImplTest.java`
**位置**：`assistShouldGeneratePrescriptionIdWhenBlank()` 方法内，在 L84 `when(assistConverter.toPrescriptionAssistResponse(aiResult)).thenReturn(new PrescriptionAssistResponse());` 之后
**插入代码**：
```java
when(allergyCheckRule.check(any())).thenReturn(
    new LocalRuleResult("ALLERGY_CHECK", true, null, AuditRiskLevel.PASS));
```
**说明**：`allergyCheckRule` 是 `@Mock`，`check()` 默认返回 null。`assist()` 流程中 `checkAllergies()`（L269）调用 `ruleResult.isPassed()` 抛出 NPE。此 stub 确保 `checkAllergies` 返回 PASS 结果，避免 NPE。

**所需新增 import**：无需新增（`LocalRuleResult`、`AuditRiskLevel`、`any()` 均已在文件顶部导入）

### 变更 5：PrescriptionAuditServiceImplTest.java L378 异常类型

**文件**：`src/test/java/.../service/audit/impl/PrescriptionAuditServiceImplTest.java`
**位置**：`submitShouldReturnConcurrentSubmitErrorWhenOptimisticLockException()` 方法内 L378
**当前代码**：
```java
when(auditRecordRepository.save(any())).thenThrow(new jakarta.persistence.OptimisticLockException());
```
**改为**：
```java
when(auditRecordRepository.save(any())).thenThrow(
    new ObjectOptimisticLockingFailureException(
        "com.aimedical.modules.prescription.entity.AuditRecord",
        new jakarta.persistence.OptimisticLockException()));
```
**说明**：生产代码 L242 `catch (ObjectOptimisticLockingFailureException e)` 只捕获 Spring 包装后的乐观锁异常。Mockito mock 的 `save()` 直接抛 `jakarta.persistence.OptimisticLockException` 不会被 catch 块捕获，导致异常向上传播到测试框架而非返回 `submitted=false` 响应。需要构造 Spring 的 `ObjectOptimisticLockingFailureException`，传入可序列化的 resource description 字符串和原始异常作为 cause。

**所需新增 import**：
```java
import org.springframework.orm.ObjectOptimisticLockingFailureException;
```

## 错误处理

| 变更 | 错误处理说明 |
|------|-------------|
| 变更 1 | 不涉及 |
| 变更 2 | 不涉及 |
| 变更 3 | 新增 BLOCK 路径返回 `submitted=false` + blockInfo，与 submit() 方法中其他 BLOCK 路径的返回模式一致 |
| 变更 4 | 不涉及 |
| 变更 5 | 修正 mock 异常类型以匹配生产代码的 catch 语义 |

## 行为契约

### buildStepThreeResponse（变更 3 后完整流程）
- `riskLevel == PASS` → submitted=true
- `riskLevel == BLOCK` → submitted=false + blockInfo（新增分支）
- `riskLevel == WARN` → 检查 prescription match / forceSubmit，按现有逻辑
- 兜底 → submitted=true（保留，实际不会被覆盖类型到达）

### submit() 中 forceSubmit 路径乐观锁处理（变更 5 相关）
- `auditRecordRepository.save(latestRecord)` 抛出 `ObjectOptimisticLockingFailureException` → 返回 `submitted=false` + `errorCode=RX_AUDIT_CONCURRENT_SUBMIT`
- `ObjectOptimisticLockingFailureException` 是 Spring 对 `jakarta.persistence.OptimisticLockException` 的包装

## 依赖关系

| 变更 | 依赖的已有类型 | 暴露给后续任务 |
|------|---------------|---------------|
| 变更 1 | `PrescriptionErrorCode.RX_AUDIT_WARN_REQUIRES_FORCE_SUBMIT` | 无 |
| 变更 2 | `AuditRiskLevel.WARN` | 无 |
| 变更 3 | `SubmitResponse`, `BlockResponse`, `AuditRiskLevel.BLOCK`, `List.of()`, `LocalDateTime.now()` | `buildStepThreeResponse` 新增 BLOCK 返回路径 |
| 变更 4 | `LocalRuleResult(String, boolean, String, AuditRiskLevel)`, `allergyCheckRule.mock`, `any()` | 无 |
| 变更 5 | `ObjectOptimisticLockingFailureException(String, Throwable)` | 无 |
