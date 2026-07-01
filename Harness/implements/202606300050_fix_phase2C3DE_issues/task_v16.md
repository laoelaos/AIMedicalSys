# 任务指令（v16）

## 动作
NEW

## 任务描述
实现 R16 — P06+P07+P08+P16 审核记录完善（PrescriptionAuditServiceImpl + AuditRecord + AuditRecordRepository）

### P06: 降级路径 LocalRuleResult → AuditAlert 转换
- PrescriptionAuditServiceImpl.audit() 降级路径（~L107-115）中将 `localRuleEngine.check(request)` 返回的 `List<LocalRuleResult>` 转换为 `List<AuditAlert>`，替代当前 `response.setAlerts(Collections.emptyList())`
- 映射规则：`LocalRuleResult.ruleId` → `AuditAlert.alertCode`，`LocalRuleResult.message` → `AuditAlert.alertMessage`，`LocalRuleResult.severity` → `AuditAlert.severity`（注意 severity 类型：LocalRuleResult 使用 AuditRiskLevel，AuditAlert 使用 AlertSeverity，需做映射：BLOCK→CRITICAL, WARN→WARNING, PASS→INFO）
- 过滤条件：只映射 `!result.isPassed()` 的规则结果（未通过的规则才产生告警）
- 仍保持 `setInteractions(Collections.emptyList())` 和 `setSuggestions(Collections.emptyList())` 不变

### P07: auditIssues 写入 AuditRecord
- persistAuditRecord() 方法中，将审核过程的 AuditIssue 列表（从 AI 响应或规则结果中构建）JSON 序列化后写入 `AuditRecord.auditIssues` 字段（`@Column(columnDefinition = "TEXT")` 已定义，类型 `String`）
- 数据来源：降级路径从 `LocalRuleResult` 构建 `AuditIssue` 列表（fieldName=null, issueDescription=result.message, ruleId=result.ruleId, severity=result.severity→AlertSeverity）；AI 路径从 `PrescriptionCheckResponse` 各 alert/interaction 构建
- 使用 `objectMapper.writeValueAsString()` 序列化，失败时 log.warn + 跳过写入

### P08: forceSubmit 回写 prescriptionOrderId
- PrescriptionAuditServiceImpl.handleStepThree() 的 forceSubmit 成功路径（~L244-252）：在 `latestRecord.setForceSubmitted(true)` **之前**插入 `latestRecord.setPrescriptionOrderId(resp.getPrescriptionOrderId())`，确保 prescriptionOrderId 在 forceSubmitted=true 时一并持久化
- 注意：`resp.setPrescriptionOrderId("RX-" + System.currentTimeMillis())` 已存在于当前代码，只需在 setForceSubmitted(true) 前将同一值设回 record

### P16: AuditRecordRepository 新增 List 版本查询 + persistAuditRecord 分组清理
- AuditRecordRepository 新增 `List<AuditRecord> findByPrescriptionOrderIdAndIsLatestTrue(String prescriptionOrderId)`（List 版本，与现有 `findTopByPrescriptionOrderIdAndIsLatestTrue` 互补）
- persistAuditRecord() 中：在按 `prescriptionId` 清理 isLatest 之前，新增按 `prescriptionOrderId` 分组清理逻辑（如果当前 record 有 prescriptionOrderId 值，则查找该 orderId 下所有 isLatest=true 的记录并设为 false），确保同一 prescriptionOrderId 下仅保留最新一条 isLatest=true 的记录

### 涉及文件
| 文件 | 操作 | 说明 |
|------|------|------|
| `modules/prescription/.../service/audit/impl/PrescriptionAuditServiceImpl.java` | 修改 | P06/P07/P08 三处变更 |
| `modules/prescription/.../repository/AuditRecordRepository.java` | 修改 | P16 新增查询方法 |
| `modules/prescription/.../service/audit/impl/PrescriptionAuditServiceImplTest.java` | 修改 | 新增测试方法 |
| `modules/prescription/.../converter/AuditConverterTest.java` | 修改 | 可选：补充分组清理/auditIssues 写入测试 |

## 选择理由
R15（DrugFacade注入）已完成且通过验证，prescription 模块测试全部通过。R16 四项修复均为 P1 级别，集中涉及 PrescriptionAuditServiceImpl/AuditRecord 的审核记录完善，属于同一域内的逻辑补充，无跨模块依赖。修复后可确保降级路径告警信息不丢失、forceSubmit 回写完整、分组清理逻辑正确。

## 任务上下文
- **P06**（OOD §4.2）：降级路径中 `localRuleEngine.check(request)` 已返回 `List<LocalRuleResult>`（ruleId, passed, message, severity），但当前代码直接丢弃结果，仅用于聚合 riskLevel，告警列表被设为 `Collections.emptyList()`。应为未通过规则生成 AuditAlert 列表
- **P07**：AuditRecord.auditIssues 字段（TEXT 类型）已定义（getter/setter 存在），但 persistAuditRecord 中从未写入
- **P08**：forceSubmit 路径中 `setForceSubmitted(true)` 和 `setForceSubmitTime(...)` 已持久化，但 `prescriptionOrderId` 在 AuditRecord 中未赋值（`getPrescriptionOrderId()` 有字段但未写入）
- **P16**：当前 `persistAuditRecord` 仅按 `prescriptionId` 清理 isLatest，缺少按 `prescriptionOrderId` 的分组清理。Repository 已有 `findTopByPrescriptionOrderIdAndIsLatestTrue`（返回 Optional），需要 List 版本

## 已有代码上下文

### PrescriptionAuditServiceImpl.java 降级路径（L101-115）
```java
if (aiResult != null && aiResult.isSuccess()) {
    response = auditConverter.toAuditResponse(aiResult);
} else {
    // ...
    List<LocalRuleResult> ruleResults = localRuleEngine.check(request);
    AuditRiskLevel aggregated = aggregateRiskLevel(ruleResults);
    response = new AuditResponse();
    response.setRiskLevel(aggregated);
    response.setAlerts(Collections.emptyList());   // ← P06: 改为转换 LocalRuleResult
    response.setInteractions(Collections.emptyList());
    response.setSuggestions(Collections.emptyList());
    response.setFromFallback(true);
}
```

### PrescriptionAuditServiceImpl.java persistAuditRecord（L330-365）
```java
private void persistAuditRecord(...) {
    List<AuditRecord> existingRecords = auditRecordRepository
            .findByPrescriptionIdAndIsLatestTrue(request.getPrescriptionId());
    for (AuditRecord existing : existingRecords) {
        existing.setLatest(false);
    }
    auditRecordRepository.saveAll(existingRecords);

    AuditRecord record = new AuditRecord();
    // ... 字段赋值，但不包含 auditIssues ← P07: 补充写入
    // ... 不包含 prescriptionOrderId 回写 ← P08 在 handleStepThree 中处理
    auditRecordRepository.save(record);
}
```

### AuditRecordRepository.java 现有查询（L10-22）
```java
public interface AuditRecordRepository extends JpaRepository<AuditRecord, Long> {
    List<AuditRecord> findByPrescriptionOrderIdOrderByAuditSequenceDesc(String prescriptionOrderId);
    Optional<AuditRecord> findTopByPrescriptionIdOrderByAuditSequenceDesc(String prescriptionId);
    Optional<AuditRecord> findTopByPrescriptionIdAndIsLatestTrueOrderByAuditSequenceDesc(String prescriptionId);
    Optional<AuditRecord> findTopByPrescriptionOrderIdAndIsLatestTrue(String prescriptionOrderId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<AuditRecord> findByPrescriptionIdAndIsLatestTrue(String prescriptionId);
}
```

### LocalRuleResult.java
- `ruleId` (String): 规则标识符
- `passed` (boolean): true=规则通过无问题
- `message` (String): 规则判定消息
- `severity` (AuditRiskLevel): BLOCK/WARN/PASS

### AuditAlert.java
- `alertCode` (String): 告警编码
- `alertMessage` (String): 告警消息
- `severity` (AlertSeverity): CRITICAL/WARNING/INFO

### AuditIssue.java
- `fieldName` (String)
- `issueDescription` (String)
- `ruleId` (String)
- `severity` (AlertSeverity)

### 严重级别映射
| LocalRuleResult.severity (AuditRiskLevel) | AuditAlert.severity (AlertSeverity) | AuditIssue.severity (AlertSeverity) |
|:--|:--|:--|
| BLOCK | CRITICAL | CRITICAL |
| WARN | WARNING | WARNING |
| PASS | INFO | INFO |

### buildStepThreeResponse 中 forceSubmit 路径（L244-252）
```java
latestRecord.setForceSubmitted(true);
latestRecord.setForceSubmitTime(LocalDateTime.now());
auditRecordRepository.save(latestRecord);
```
// P08: 在 setForceSubmitted(true) 前插入 setPrescriptionOrderId(...)
// P16: persistAuditRecord 中补充 prescriptionOrderId 分组清理

## 注意事项
- 4 项修复都在 PrescriptionAuditServiceImpl 同一个类中，确保不破坏现有逻辑
- P06 降级路径中 `aggregateRiskLevel(ruleResults)` 仍在原位置调用，不影响
- P08 注意 `resp.getPrescriptionOrderId()` 值在 `setPrescriptionOrderId(resp...)` 前已设好
- P16 的按 prescriptionOrderId 分组清理应放在按 prescriptionId 清理之前或之后均可，但需避免重复设置 isLatest=false
- 测试覆盖：每个 P 至少一个正向测试 + 边界条件（空列表、null）
