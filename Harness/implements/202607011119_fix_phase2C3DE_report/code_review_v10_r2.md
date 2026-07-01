# 代码审查报告（v10 r2）

## 审查结果
APPROVED

## 发现
无严重或一般问题。所有变更严格按详细设计 v10 规格实现，逐一验证 13 个源文件与设计吻合。

### 逐项验证结果

| 设计项 | 文件 | 状态 |
|--------|------|------|
| 8a | DosageThresholdService.java — catch 中 log.warn + Logger 字段 | ✓ |
| 8b | PrescriptionDraftContext.java — 删除 @SuppressWarnings + instanceof List<?> | ✓ |
| 8c | PrescriptionItem.java — dose double→BigDecimal，getter/setter/import | ✓ |
| 8c | DosageLimitRule.java:40 — BigDecimal.valueOf(item.getDose()) → item.getDose() | ✓ |
| 8c | AuditConverter.java:70 — .doubleValue() 转换 | ✓ |
| 8c | PrescriptionAssistServiceImpl.java:146 — setDosage(.doubleValue()) | ✓ |
| 8c | PrescriptionAssistServiceImpl.java:275 — setDose(BigDecimal.valueOf()) | ✓ |
| 8c | SpecialPopulationDosageRule.java:61 — item.getDose() 直接引用 BigDecimal | ✓ |
| 8d | PrescriptionAuditServiceImpl.java — 5 处 UUID ID 替换 | ✓ |
| 8e | PrescriptionAuditServiceImpl.java — 死代码块 + hasNewAlerts 已删除 | ✓ |
| 8f | DosageLimitRule.java — findBestMatch null 回退 log.warn + fallback | ✓ |
| 8g | AllergyCheckRule.java — 单词边界正则 + hasNegationPrefix 方法 | ✓ |
| 8h | AuditConverter.java — setUnit + setWeight | ✓ |
| 8h | PrescriptionCheckItem.java (ai-api) — 新增 unit 字段 | ✓ |
| 8h | PatientInfo.java (ai-api) — 新增 weight 字段 | ✓ |
| 8i | DrugInteractionRule.java — @ConditionalOnProperty | ✓ |
| 8j | PrescriptionAuditServiceImpl.doSubmit — reasons 从 snapshot.alerts 提取 | ✓ |
| 8j | PrescriptionAuditController.java — BLOCK 路径 reasons 提取 | ✓ |
| 8k | DosageThresholdService.java — matchesBothRanges + findFirstCandidate 抽取，5 循环替换 | ✓ |
| 8l | PrescriptionAssistServiceImpl.java — exceptionally 追加 clearCriticalAlerts | ✓ |
| 8m | PrescriptionAuditServiceImpl.java — ReentrantLock + doSubmit 提取 + imports | ✓ |
| 8n | PrescriptionDraftContext.java — snapshotCriticalAlerts + SnapshotResult | ✓ |
| 8n | PrescriptionAuditServiceImpl.doSubmit — TOCTOU 消除，单次 snapshot | ✓ |
| 8n | PrescriptionDraftContext — hasCriticalAlerts 已删除 | ✓ |

### 设计偏差
无。实现与设计完全吻合。

### 轻微观察
- **[轻微]** PrescriptionAuditServiceImpl.java: `@Transactional` 包裹 `lock.lock()`，等待锁时持有 DB 连接，极端高并发下可能消耗连接池。此模式由设计规格明确指定，属已知权衡。
