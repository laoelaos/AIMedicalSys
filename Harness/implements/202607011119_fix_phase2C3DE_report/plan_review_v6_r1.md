# 计划审查报告（v6 r1）

## 审查结果
REJECTED

## 发现

### **[严重] R6 NEW 计划缺少必要的实施细节，与项目已建立的计划详细程度标准严重背离**

plan.md R5 NEW 节包含超过 80 行的详尽实施计划，含问题定位（精确到行号）、变更明细（具体代码改动）、伪码、完整文件清单、测试修改策略。而 R6 NEW 节仅用 10 行列出任务名称和上下文文件路径，没有任何可指导编码环节的操作细节。

**风险**：编码环节无法仅依据此计划执行，仍需回到 task_v6.md 自行梳理实现细节。这违背了 plan 作为编码直接参考的定位，增加了实现偏差风险。

**期望修正**：R6 NEW 节应参照 R5 NEW 节的质量标准，对每个缺陷补充：
- 问题定位（涉及文件、行号、原因）
- 变更明细（具体修改内容、代码示例）
- 涉及文件清单（精确路径）
- 测试修改细节

---

### **[一般] 缺少 P03/S02 — DraftContextCleanupTask 调用方的具体修改方案**

plan.md 指出 "recordWrite() 无调用方"，但未说明需要在 `PrescriptionDraftContext.updateCriticalAlerts()` 中新增调用。task_v6.md 明确指定了：
- `updateCriticalAlerts()` 中 put 后调用 `cleanupTask.recordWrite(key, Instant.now())`
- `updateCriticalAlerts()` 中 remove 后调用 `cleanupTask.removeTimestamp(key)`
- 新增 `PrescriptionDraftContext` 对 `DraftContextCleanupTask` 的字段注入

**期望修正**：补充 PrescriptionDraftContext 的完整修改说明。

---

### **[一般] 缺少 P03/S02 — SuggestionCleanupTask 测试修改方案**

task_v6.md 明确指定：
- 重命名 `shouldRemoveExpiredFailedAndConsumedEntry` → `shouldRemoveExpiredFailedEntryEvenIfNotConsumed`
- 修改测试数据 `consumed=false`
- 新增测试 `shouldNotRemoveFailedEntryWhenNotExpired`

plan.md 未提及任何测试修改。

**期望修正**：列出 SuggestionCleanupTaskTest 的修改清单。

---

### **[一般] 缺少 T40/E05 — null sessionId 保护的修改细节**

plan.md 指出 "null sessionId" 问题，但未说明修改方式。task_v6.md 指定：
```java
String sid = event.getSessionId() != null ? event.getSessionId() : "unknown";
deadLetter.setEventPayload("{\"sessionId\":\"" + sid + "\"}");
```

**期望修正**：补充 JSON 兜底中 null sessionId 保护的代码细节。

---

### **[一般] 缺少 M04 — 完整修改方案**

plan.md 仅提及 "乐观锁不可触发"，未说明需做两个具体修改：
1. `MedicalRecord.visitId` 添加 `unique = true` 约束
2. `MedicalRecordServiceImpl` catch 块追加 `DataIntegrityViolationException` 捕获

**期望修正**：补充 M04 变更的文件、字段、代码细节。

---

### **[轻微] 文件路径使用通配符**

R6 NEW 上下文列表中 `ai/ai-impl/.../mock/MockAiService.java` 使用 `...` 通配符，不符合精确路径要求。R5 节均使用完整精确路径。

**期望修正**：使用完整路径替换通配符。

## 修改要求

1. **[严重]** 参照 R5 NEW 节的质量标准，将 R6 NEW 节扩充为包含问题定位、变更明细、文件清单、测试修改的完整实现计划
2. **[一般]** 补充 P03/S02 DraftContextCleanupTask + PrescriptionDraftContext 修改细节
3. **[一般]** 补充 SuggestionCleanupTaskTest 测试修改清单
4. **[一般]** 补充 T40/E05 null sessionId 防护代码细节
5. **[一般]** 补充 M04 unique=true + DataIntegrityViolationException 捕获细节
6. **[轻微]** 统一使用精确文件路径
