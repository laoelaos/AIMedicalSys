# 详细设计（v16）

## 概述

对 PrescriptionAuditServiceImpl + AuditRecordRepository 进行四项审核记录完善修复（P06/P07/P08/P16），确保降级路径告警信息不丢失、auditIssues 写入 record、forceSubmit 回写 prescriptionOrderId、以及按 prescriptionOrderId 的分组清理逻辑。

## 文件规划

| 文件路径 | 操作 | 职责 |
|---------|------|------|
| `.../service/audit/impl/PrescriptionAuditServiceImpl.java` | 修改 | P06/P07/P08/P16 共 4 处变更 |
| `.../repository/AuditRecordRepository.java` | 修改 | P16 新增 List 版本查询方法 |
| `.../service/audit/impl/PrescriptionAuditServiceImplTest.java` | 修改 | 为每项 P 新增测试方法 |
| `.../converter/AuditConverterTest.java` | 修改 | 可选：补充 P07 auditIssues 序列化相关测试 |

## 类型定义

无新增类型。以下为变更涉及已有类型及映射关系的详细说明。

### AuditRiskLevel → AlertSeverity 映射（P06/P07 共用）

**形态**：静态映射方法（private static）
**职责**：将 `AuditRiskLevel` 枚举值映射为 `AlertSeverity` 枚举值

| 输入 (AuditRiskLevel) | 输出 (AlertSeverity) |
|:--|:--|
| BLOCK | CRITICAL |
| WARN | WARNING |
| PASS | INFO |

```java
private static AlertSeverity toAlertSeverity(AuditRiskLevel riskLevel) {
    if (riskLevel == null) return AlertSeverity.INFO;
    switch (riskLevel) {
        case BLOCK: return AlertSeverity.CRITICAL;
        case WARN:  return AlertSeverity.WARNING;
        default:    return AlertSeverity.INFO;
    }
}
```

### String → AlertSeverity 映射（P07 AI 路径 DrugInteractionItem 用）

**形态**：静态映射方法（private static）
**职责**：将 AI 响应中 `DrugInteractionItem.severity`（String）映射为 `AlertSeverity` 枚举值

```java
private static AlertSeverity toAlertSeverity(String severity) {
    if (severity == null) return AlertSeverity.INFO;
    switch (severity.toUpperCase()) {
        case "CRITICAL": return AlertSeverity.CRITICAL;
        case "WARNING":
        case "WARN":     return AlertSeverity.WARNING;
        default:         return AlertSeverity.INFO;
    }
}
```

## 详细变更设计

### P06: 降级路径 LocalRuleResult → AuditAlert 转换

**位置**：`PrescriptionAuditServiceImpl.audit()` 降级分支（~L107-115）

**变更**：
- 删除 `response.setAlerts(Collections.emptyList())`
- 替换为：
  1. 从 `ruleResults` 中过滤 `!result.isPassed()` 的条目
  2. 对每个未通过结果构造 `AuditAlert`：
     - `alertCode = result.getRuleId()`
     - `alertMessage = result.getMessage()`
     - `severity = toAlertSeverity(result.getSeverity())`
  3. `response.setAlerts(alerts)`
- 保持 `setInteractions(Collections.emptyList())` 和 `setSuggestions(Collections.emptyList())` 不变

**行为契约**：
- 空列表或全通过时，alerts 为 `Collections.emptyList()`
- aggregateRiskLevel 仍在原位置调用，不变

### P07: auditIssues 写入 AuditRecord

**位置**：`PrescriptionAuditServiceImpl.persistAuditRecord()`（~L330-365）

**变更**：

在现有 record 字段赋值之后、`auditRecordRepository.save(record)` 之前，插入 AuditIssue 列表构建与序列化逻辑：

#### 降级路径（fromFallback=true）

从 `response.getAlerts()` 构建 `List<AuditIssue>`（P06 已先执行，alerts 已填充）：

| AuditIssue 字段 | 来源 |
|:--|:--|
| fieldName | null |
| issueDescription | alert.getAlertMessage() |
| ruleId | alert.getAlertCode() |
| severity | alert.getSeverity() |

#### AI 路径（aiResult != null && success）

从 `PrescriptionCheckResponse` 的 `alerts` 和 `interactions` 构建 `List<AuditIssue>`：

**AlertItem → AuditIssue：**

| AuditIssue 字段 | 来源 |
|:--|:--|
| fieldName | null |
| issueDescription | AlertItem.alertMessage |
| ruleId | AlertItem.alertCode |
| severity | toAlertSeverity(AlertItem.severity) |

**DrugInteractionItem → AuditIssue：**

| AuditIssue 字段 | 来源 |
|:--|:--|
| fieldName | DrugInteractionItem.drugPair |
| issueDescription | DrugInteractionItem.description |
| ruleId | "DRUG_INTERACTION_" + drugPair |
| severity | toAlertSeverity(DrugInteractionItem.severity) |

**注意**：SuggestionItem 未纳入 AuditIssue 构建——SuggestionItem 仅有 `suggestionCode` 和 `suggestionText`，无 `severity` 字段（AuditIssue 必需的复合属性），且任务描述仅提及 alert/interaction 两类来源。

#### 序列化与异常处理

- `objectMapper.writeValueAsString(issues)` → `record.setAuditIssues(jsonString)`
- 捕获 `JsonProcessingException` → `log.warn("Failed to serialize audit issues", e)`，不抛出异常
- AI 路径中两个来源的 AuditIssue 合并为同一个列表后统一序列化

**伪代码**：
```java
List<AuditIssue> issues = new ArrayList<>();
if (fromFallback && response.getAlerts() != null) {
    for (AuditAlert alert : response.getAlerts()) {
        AuditIssue issue = new AuditIssue();
        issue.setFieldName(null);
        issue.setIssueDescription(alert.getAlertMessage());
        issue.setRuleId(alert.getAlertCode());
        issue.setSeverity(alert.getSeverity());
        issues.add(issue);
    }
} else if (aiResult != null && aiResult.isSuccess() && aiResult.getData() != null) {
    if (aiResult.getData().getAlerts() != null) {
        for (AlertItem alert : aiResult.getData().getAlerts()) {
            AuditIssue issue = new AuditIssue();
            issue.setFieldName(null);
            issue.setIssueDescription(alert.getAlertMessage());
            issue.setRuleId(alert.getAlertCode());
            issue.setSeverity(toAlertSeverity(alert.getSeverity()));
            issues.add(issue);
        }
    }
    if (aiResult.getData().getInteractions() != null) {
        for (DrugInteractionItem item : aiResult.getData().getInteractions()) {
            AuditIssue issue = new AuditIssue();
            issue.setFieldName(item.getDrugPair());
            issue.setIssueDescription(item.getDescription());
            issue.setRuleId("DRUG_INTERACTION_" + item.getDrugPair());
            issue.setSeverity(toAlertSeverity(item.getSeverity()));
            issues.add(issue);
        }
    }
}
if (!issues.isEmpty()) {
    try {
        record.setAuditIssues(objectMapper.writeValueAsString(issues));
    } catch (JsonProcessingException e) {
        log.warn("Failed to serialize audit issues", e);
    }
}
```

### P08: forceSubmit 回写 prescriptionOrderId

**位置**：`PrescriptionAuditServiceImpl.handleStepThree()` forceSubmit 成功路径（~L244-252）

**变更**：
在 `latestRecord.setForceSubmitted(true)` 之前插入 prescriptionOrderId 赋值，同时将 orderId 提取为局部变量以在 resp 中复用：

```java
try {
    String orderId = "RX-" + System.currentTimeMillis();
    latestRecord.setPrescriptionOrderId(orderId);
    latestRecord.setForceSubmitted(true);
    latestRecord.setForceSubmitTime(LocalDateTime.now());
    auditRecordRepository.save(latestRecord);

    SubmitResponse resp = new SubmitResponse();
    resp.setSubmitted(true);
    resp.setPrescriptionOrderId(orderId);
    return resp;
}
```

**说明**：原代码 `resp.setPrescriptionOrderId("RX-" + System.currentTimeMillis())` 移至块首生成、复用于 record 回写与 response 设置，避免 timestamp 不一致。

### P16: AuditRecordRepository 新增 List 版本查询 + forceSubmit 路径分组清理

#### 新增 Repository 方法

**文件**：`AuditRecordRepository.java`

```java
List<AuditRecord> findByPrescriptionOrderIdAndIsLatestTrue(String prescriptionOrderId);
```

与现有 `findTopByPrescriptionOrderIdAndIsLatestTrue`（返回 Optional）互补，List 版本用于批量清理。

#### forceSubmit 路径分组清理

**位置**：`PrescriptionAuditServiceImpl.handleStepThree()` forceSubmit try 块内，在 `auditRecordRepository.save(latestRecord)` 之后

**理由**：`persistAuditRecord()` 仅在 `audit()` 方法中被调用，而 audit 执行时 prescriptionOrderId 尚未生成（该值在 forceSubmit 流程中才创建）。因此 prescriptionOrderId 的分组清理逻辑实际执行点在 `handleStepThree()` forceSubmit 路径中，而非 `persistAuditRecord()` 内部。

```java
try {
    String orderId = "RX-" + System.currentTimeMillis();
    latestRecord.setPrescriptionOrderId(orderId);
    latestRecord.setForceSubmitted(true);
    latestRecord.setForceSubmitTime(LocalDateTime.now());
    auditRecordRepository.save(latestRecord);

    // P16: 按 prescriptionOrderId 分组清理 isLatest
    // 确保该 orderId 下仅此记录保留 isLatest=true
    List<AuditRecord> orderRecords = auditRecordRepository
            .findByPrescriptionOrderIdAndIsLatestTrue(orderId);
    for (AuditRecord orderRecord : orderRecords) {
        orderRecord.setLatest(false);
    }
    if (!orderRecords.isEmpty()) {
        auditRecordRepository.saveAll(orderRecords);
    }

    SubmitResponse resp = new SubmitResponse();
    resp.setSubmitted(true);
    resp.setPrescriptionOrderId(orderId);
    return resp;
}
```

**边界情况**：
- `findByPrescriptionOrderIdAndIsLatestTrue` 返回空列表时，`saveAll(emptyList)` 是安全操作（JPA 忽略空集合）
- 由于 orderId 为 `System.currentTimeMillis()` 实时生成，同一 orderId 被多个 record 持有的可能性极低；此清理为防御性设计

## 错误处理

- P06：无特殊错误处理；LocalRuleResult 字段 guaranteed non-null（由 LocalRuleEngine 产出）
- P07：`JsonProcessingException` → `log.warn("Failed to serialize audit issues", e)`，不抛出异常，不阻断流程
- P08：prescriptionOrderId 生成与回写均在 forceSubmit try 块内，受 `ObjectOptimisticLockingFailureException` 保护
- P16：`findByPrescriptionOrderIdAndIsLatestTrue` 返回空列表时 `saveAll` 安全处理空列表

## 行为契约

### persistAuditRecord 调用后的状态

| 字段 | 条件 | 值 |
|------|------|-----|
| auditIssues | 降级路径 | response.getAlerts() 中各 alert 的 AuditIssue JSON |
| auditIssues | AI 路径 | aiResult.data 的 alerts + interactions 合并后的 AuditIssue JSON |
| prescriptionOrderId | forceSubmit 路径 | "RX-" + timestamp（与 resp 中一致） |
| isLatest | 同一 prescriptionId 旧记录 | false |
| isLatest | 同一 prescriptionOrderId 旧记录 | false（forceSubmit 路径执行后） |

### 调用顺序约束

1. P06 只影响 `audit()` 降级分支，在 `persistAuditRecord()` 调用前执行
2. P07 影响 `persistAuditRecord()` 所有路径，在 record save 前执行
3. P08 只影响 `handleStepThree()` forceSubmit 分支
4. P16 新增 Repository 方法；分组清理逻辑在 `handleStepThree()` forceSubmit 路径中执行

## 依赖关系

- 依赖已有类型：`LocalRuleResult`, `AuditAlert`, `AuditIssue`, `AlertSeverity`, `AuditRiskLevel`, `AuditResponse`, `AuditRecord`, `AlertItem`, `DrugInteractionItem`
- 无新增依赖
- 无暴露给后续任务的新公开接口

## 修订说明（v16 r1）
| 审查意见 | 修改措施 |
|---------|---------|
| **[严重]** P16 设计引用 `response.getPrescriptionOrderId()` 但 AuditResponse 无此方法 | 重新分析代码结构后发现 `persistAuditRecord()` 仅在 `audit()` 中被调用（此时无 prescriptionOrderId），而 forceSubmit 流程在 `handleStepThree()` 中直接 `save(latestRecord)`。因此将 P16 分组清理逻辑移至 `handleStepThree()` forceSubmit try 块内 `save(latestRecord)` 之后，避免引入不存在的 API |
| **[一般]** P07 AI 路径未从 DrugInteractionItem 构建 AuditIssue | 补充 DrugInteractionItem → AuditIssue 映射规则（fieldName=drugPair, issueDescription=description, ruleId="DRUG_INTERACTION_"+drugPair, severity=toAlertSeverity(String)）；新增 `toAlertSeverity(String)` 重载方法处理 String → AlertSeverity 映射 |
| **[轻微]** P16 分组清理顺序与任务要求不一致（任务要求 orderId 清理在前，设计在后） | 已修正：prescriptionOrderId 清理先于 prescriptionId 清理。同时在修订中阐明因实际代码结构差异，orderId 分组清理逻辑实际放置于 `handleStepThree()` forceSubmit 路径中 |
