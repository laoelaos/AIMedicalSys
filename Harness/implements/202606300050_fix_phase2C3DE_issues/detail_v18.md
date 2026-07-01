# 详细设计（v18）

## 概述

实现 R18 — 在 `SubmitResponse` 中增加 `warnResult` 字段，新建 `WarnResult` 和 `WarnAlert` DTO，改写 `PrescriptionAuditServiceImpl` 中 WARN 路径使用 `warnResult` 替代 `RX_AUDIT_WARN_REQUIRES_FORCE_SUBMIT` 错误码，并移除该错误码。

## 文件规划

| 文件路径 | 操作 | 职责 |
|---------|------|------|
| `modules/prescription/src/main/java/.../dto/audit/WarnResult.java` | 新建 | WARN 路径统一响应 DTO，承载 riskLevel、alerts、auditRecordId、prescriptionHash |
| `modules/prescription/src/main/java/.../dto/audit/WarnAlert.java` | 新建 | WARN 告警条目 DTO，与 AuditAlert 为并行关系 |
| `modules/prescription/src/main/java/.../dto/audit/SubmitResponse.java` | 修改 | 新增 `warnResult` 字段 + getter/setter |
| `modules/prescription/src/main/java/.../service/audit/impl/PrescriptionAuditServiceImpl.java` | 修改 | WARN 路径 2 处改为填充 `warnResult` + errorCode=null；新增 4 个 private helper 方法 |
| `modules/prescription/src/main/java/.../PrescriptionErrorCode.java` | 修改 | 移除 `RX_AUDIT_WARN_REQUIRES_FORCE_SUBMIT` 枚举常量 |
| `modules/prescription/src/test/java/.../service/audit/impl/PrescriptionAuditServiceImplTest.java` | 修改 | L738-739 改为断言 `warnResult` 非空且字段正确 |
| `modules/prescription/src/test/java/.../PrescriptionErrorCodeTest.java` | 修改 | `assertEquals(11, ...)` → `assertEquals(10, ...)`；移除 L20-21 |
| `modules/prescription/src/test/java/.../api/PrescriptionAuditControllerTest.java` | 修改 | L101 错误码改为 `RX_AUDIT_PRESCRIPTION_MODIFIED` |

## 类型定义

### WarnAlert

**形态**：class
**包路径**：`com.aimedical.modules.prescription.dto.audit`
**职责**：单条 WARN 告警信息，与 `AuditAlert` 为并行 DTO 而非替代关系

```java
package com.aimedical.modules.prescription.dto.audit;

public class WarnAlert {
    private String alertCode;
    private String alertMessage;
    private AlertSeverity severity;

    public WarnAlert() {}
    public WarnAlert(String alertCode, String alertMessage, AlertSeverity severity) {}

    // getter/setter for each field
}
```

**公开接口**：
- `getAlertCode()` → `String`
- `setAlertCode(String)` → `void`
- `getAlertMessage()` → `String`
- `setAlertMessage(String)` → `void`
- `getSeverity()` → `AlertSeverity`
- `setSeverity(AlertSeverity)` → `void`

**构造方式**：无参构造 + 全参构造
**类型关系**：无继承/实现

### WarnResult

**形态**：class
**包路径**：`com.aimedical.modules.prescription.dto.audit`
**职责**：WARN 路径统一响应值，填充于 `SubmitResponse.warnResult`，此时 `errorCode=null`

```java
package com.aimedical.modules.prescription.dto.audit;

public class WarnResult {
    private AuditRiskLevel riskLevel;
    private List<WarnAlert> alerts;
    private Long auditRecordId;
    private String prescriptionHash;

    public WarnResult() {}
    public WarnResult(AuditRiskLevel riskLevel, List<WarnAlert> alerts, Long auditRecordId, String prescriptionHash) {}

    // getter/setter for each field
}
```

**公开接口**：
- `getRiskLevel()` → `AuditRiskLevel`
- `setRiskLevel(AuditRiskLevel)` → `void`
- `getAlerts()` → `List<WarnAlert>`
- `setAlerts(List<WarnAlert>)` → `void`
- `getAuditRecordId()` → `Long`
- `setAuditRecordId(Long)` → `void`
- `getPrescriptionHash()` → `String`
- `setPrescriptionHash(String)` → `void`

**构造方式**：无参构造 + 全参构造
**类型关系**：无继承/实现

### SubmitResponse 新增字段

```java
// 新增字段
private WarnResult warnResult;

// 新增公开方法
public WarnResult getWarnResult() { return warnResult; }
public void setWarnResult(WarnResult warnResult) { this.warnResult = warnResult; }
```

### PrescriptionErrorCode 变更

**操作**：删除第 9 行枚举常量 `RX_AUDIT_WARN_REQUIRES_FORCE_SUBMIT`

```java
// 删除前（共 11 个常量）
RX_AUDIT_BLOCKED,
RX_AUDIT_PRESCRIPTION_MODIFIED,
RX_AUDIT_WARN_REQUIRES_FORCE_SUBMIT,  // ← 删除此行
RX_AUDIT_CONCURRENT_SUBMIT,
...

// 删除后（共 10 个常量）
RX_AUDIT_BLOCKED,
RX_AUDIT_PRESCRIPTION_MODIFIED,
RX_AUDIT_CONCURRENT_SUBMIT,
...
```

## 行为契约

### PrescriptionAuditServiceImpl WARN 路径改写

#### 位置 1：`handleStepThree` 方法 L227-230

**当前**：
```java
SubmitResponse resp = new SubmitResponse();
resp.setSubmitted(false);
resp.setErrorCode(PrescriptionErrorCode.RX_AUDIT_WARN_REQUIRES_FORCE_SUBMIT.getCode());
return resp;
```

**改为**：
```java
SubmitResponse resp = new SubmitResponse();
resp.setSubmitted(false);
resp.setWarnResult(buildWarnResultFromRecord(latestRecord));
resp.setErrorCode(null);
return resp;
```

**前置条件**：`!request.isForceSubmit() && riskLevel == AuditRiskLevel.WARN && prescriptionsMatch(...) == true`（处方未变更）

**后置条件**：`resp.submitted == false`, `resp.errorCode == null`, `resp.warnResult != null`

#### 位置 2：`buildStepThreeResponse` 方法 L315-319

**当前**：
```java
if (!request.isForceSubmit()) {
    SubmitResponse resp = new SubmitResponse();
    resp.setSubmitted(false);
    resp.setErrorCode(PrescriptionErrorCode.RX_AUDIT_WARN_REQUIRES_FORCE_SUBMIT.getCode());
    return resp;
}
```

**改为**：
```java
if (!request.isForceSubmit()) {
    SubmitResponse resp = new SubmitResponse();
    resp.setSubmitted(false);
    resp.setWarnResult(buildWarnResultFromAuditResponse(auditResp, record, request));
    resp.setErrorCode(null);
    return resp;
}
```

**前置条件**：`riskLevel == AuditRiskLevel.WARN && (record == null || prescriptionsMatch(...) == true) && !request.isForceSubmit()`

**后置条件**：`resp.submitted == false`, `resp.errorCode == null`, `resp.warnResult != null`

### 新增 private helper 方法

在 `PrescriptionAuditServiceImpl` 中新增以下 4 个 private 方法：

#### `buildWarnResultFromRecord(AuditRecord record) → WarnResult`

从已持久化的 `AuditRecord` 构建 `WarnResult`：
1. 解析 `record.getAuditIssues()` JSON → `List<AuditIssue>`
2. 映射 `AuditIssue` 列表 → `List<WarnAlert>`（ruleId→alertCode, issueDescription→alertMessage, severity→severity）
3. 调用 `computePrescriptionHash(record.getOriginalPrescription())` 计算哈希
4. 返回 `new WarnResult(AuditRiskLevel.WARN, alerts, record.getId(), hash)`

**auditIssues 为空或解析失败时**：`alerts` 为空列表

#### `buildWarnResultFromAuditResponse(AuditResponse auditResp, AuditRecord record, SubmitRequest request) → WarnResult`

从实时审核结果构建 `WarnResult`：
1. 映射 `auditResp.getAlerts()`（`List<AuditAlert>`） → `List<WarnAlert>`（对应字段直接复制）
2. `auditRecordId` = `record != null ? record.getId() : null`
3. `prescriptionHash` = `record != null` ? `computePrescriptionHash(record.getOriginalPrescription())` : `computePrescriptionHash(request.getPrescriptionItems())`
4. 返回 `new WarnResult(AuditRiskLevel.WARN, alerts, auditRecordId, hash)`

#### `computePrescriptionHash(String originalPrescriptionJson) → String`

**签名**：`private String computePrescriptionHash(String originalPrescriptionJson)`
**行为**：对 JSON 字符串计算 SHA-256 摘要，返回 64 字符小写十六进制字符串
**异常**：`RuntimeException`（包装 `NoSuchAlgorithmException`）

```java
MessageDigest digest = MessageDigest.getInstance("SHA-256");
byte[] hash = digest.digest(originalJson.getBytes(StandardCharsets.UTF_8));
StringBuilder hex = new StringBuilder();
for (byte b : hash) { hex.append(String.format("%02x", b)); }
return hex.toString();
```

#### `computePrescriptionHash(List<PrescriptionItem> items) → String`

**签名**：`private String computePrescriptionHash(List<PrescriptionItem> items)`
**行为**：将 `items` 通过 `objectMapper.writeValueAsString()` 序列化为 JSON，再调用 `computePrescriptionHash(String)` 计算哈希
**异常**：`RuntimeException`（包装 `JsonProcessingException`）

### 字段约束表（参考 OOD §4.6）

| 路径 | submitted | blockInfo | errorCode | warnResult | HTTP |
|------|----------|-----------|-----------|------------|------|
| 步③ forceSubmit=false + WARN（未变更） | false | null | null | WarnResult | 200 |
| 步③ forceSubmit=false + WARN（已变更） | false | null | RX_AUDIT_PRESCRIPTION_MODIFIED | null | 409 |

## 错误处理

- `computePrescriptionHash(String)` 中的 `NoSuchAlgorithmException` 包装为 `RuntimeException` 抛出（不应发生，SHA-256 为 JDK 标准算法）
- `computePrescriptionHash(List<PrescriptionItem>)` 中的 `JsonProcessingException` 包装为 `RuntimeException` 抛出
- `buildWarnResultFromRecord` 中 `auditIssues` JSON 解析失败：静默处理，alerts 返回空列表（`try-catch`，log warn）

## 依赖关系

### 新增类型依赖
- `WarnResult` → `AuditRiskLevel`（`com.aimedical.modules.prescription.service.audit`）
- `WarnResult` → `WarnAlert`（同包）
- `WarnAlert` → `AlertSeverity`（同包）

### PrescriptionAuditServiceImpl 新增 import
- `java.security.MessageDigest`
- `java.nio.charset.StandardCharsets`
- `com.aimedical.modules.prescription.dto.audit.WarnResult`
- `com.aimedical.modules.prescription.dto.audit.WarnAlert`
- `com.fasterxml.jackson.core.type.TypeReference`（用于解析 `auditIssues` JSON → `List<AuditIssue>`）

### 测试文件适应性变更

#### PrescriptionAuditServiceImplTest.java L738-739

**修改点**：`submitShouldRequireForceSubmitWhenWarnAndPrescriptionUnchanged`

**当前**：
```java
assertFalse(result.isSubmitted());
assertEquals(PrescriptionErrorCode.RX_AUDIT_WARN_REQUIRES_FORCE_SUBMIT.getCode(), result.getErrorCode());
```

**改为**：
```java
assertFalse(result.isSubmitted());
assertNull(result.getErrorCode());
assertNotNull(result.getWarnResult());
assertEquals(AuditRiskLevel.WARN, result.getWarnResult().getRiskLevel());
assertNotNull(result.getWarnResult().getAlerts());
assertNotNull(result.getWarnResult().getAuditRecordId());
assertNotNull(result.getWarnResult().getPrescriptionHash());
```

#### PrescriptionErrorCodeTest.java

**L11 修改**：`assertEquals(11, PrescriptionErrorCode.values().length)` → `assertEquals(10, ...)`

**L20-21 删除**（两行）：
```java
assertEquals("RX_AUDIT_WARN_REQUIRES_FORCE_SUBMIT", PrescriptionErrorCode.RX_AUDIT_WARN_REQUIRES_FORCE_SUBMIT.getCode());
assertEquals("WARN 审核未确认，需 forceSubmit=true 放行", PrescriptionErrorCode.RX_AUDIT_WARN_REQUIRES_FORCE_SUBMIT.getMessage());
```

#### PrescriptionAuditControllerTest.java L101

**当前**：
```java
response.setErrorCode("RX_AUDIT_WARN_REQUIRES_FORCE_SUBMIT");
```

**改为**：
```java
response.setErrorCode("RX_AUDIT_PRESCRIPTION_MODIFIED");
```
