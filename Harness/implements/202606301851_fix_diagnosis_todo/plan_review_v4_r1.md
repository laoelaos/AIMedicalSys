# 计划审查报告（v4 r1）

## 审查结果
REJECTED

## 发现

### **[严重]** P14 修复方案与 OOD 设计语义不一致——AI 不可用时写入 CRITICAL 告警缺乏设计依据

task_v4.md 的 P14 修复方案要求在 `assist()` 的每个失败/降级路径中构造一个 CRITICAL 级别的 DosageAlert 写入 PrescriptionDraftContext，具体为 `DosageAlert(severity="CRITICAL", message="AI辅助开方服务不可用，处方提交已阻断", drugCode="SYSTEM")`。

然而 OOD 设计文档（§4.4 辅助开方场景 + §PrescriptionDraftContext 行为契约）对 CRITICAL 告警写入的语义定义是：**"CRITICAL 告警同步写入 PrescriptionDraftContext（以 prescriptionId 为键全量覆盖，写入逻辑与 check-dose 路径一致——覆盖当前 prescriptionId 下所有 CRITICAL 标记，无 CRITICAL 时清除对应条目）"**。CRITICAL 告警的来源是 **DosageThresholdService 剂量阈值校验结果中 alertLevel=CRITICAL 的告警**，而非 AI 服务可用性状态。

OOD §4.4 对 AI 返回无可推荐药品场景的处理是：`→ 返回空 prescriptionDraft + 本地校验结果 + PrescriptionAssistResponse.errorCode=RX_ASSIST_AI_NO_RECOMMENDATION`，并未要求写入 CRITICAL 告警。OOD 对 AI 失败/超时场景（catch 块路径）甚至没有显式描述辅助开方的降级路径（与处方审核 audit 的降级路径不同——audit 有 LocalRuleEngine 降级，assist 没有）。

**问题本质**：将"AI 不可用"映射为 CRITICAL 剂量告警是任务设计者的推断，而非 OOD 的明确要求。这引入了一个新的语义——`drugCode="SYSTEM"` 的 CRITICAL 告警在 DosageAlert 模型中从未定义过（DosageAlert 的 drugCode 语义是药品编码，"SYSTEM" 不属于任何药品编码体系），且 PrescriptionDraftContext 的 `hasCriticalAlerts()` 和 `getCriticalAlerts()` 方法会被此"伪 CRITICAL"污染，导致提交端点步①的 CRITICAL 阻断检查误判——即使剂量校验实际无 CRITICAL 告警，AI 不可用也会阻断提交。

**正确的修复方向**：应重新审视 OOD 设计意图。P14 的核心问题是"AI 不可用时处方提交无法被阻断"——但 OOD 的阻断链路是 CRITICAL 剂量告警 → PrescriptionDraftContext → 提交端点步①检查。如果 OOD 确实要求 AI 不可用时阻断提交，则应在提交端点步①增加对 AI 可用性的独立检查维度（而非伪造 CRITICAL 剂量告警）；如果 OOD 不要求 AI 不可用时阻断提交，则 P14 的修复方案本身就是错误的。建议回到 OOD 文档确认设计意图后再决定修复方案。

### **[一般]** DraftContextCleanupTask 迁移后"对齐键命名"描述模糊且可能引入行为变更

task_v4.md 第 43 行要求"清理逻辑中增加对 `prescriptionId+:criticalAlerts` 键格式的识别"，但未说明具体如何"识别"以及识别后行为有何不同。

当前 `DraftContextCleanupTask.cleanupExpiredDrafts()` 的清理逻辑是：遍历 `draftContextStore.keySet()`，检查 `writeTimestamps` 中 TTL 过期条目并移除。该逻辑对所有键一视同仁，不区分键格式。`prescriptionId+:criticalAlerts` 格式的键与任何其他格式的键在清理逻辑上没有区别——都是检查 TTL 过期后移除。

"对齐键命名"的实际含义不明确：
- 如果仅是"确保清理逻辑能正确处理 `prescriptionId+:criticalAlerts` 格式的键"——当前逻辑已经能处理，无需任何修改
- 如果是"在清理时对 `prescriptionId+:criticalAlerts` 格式的键做特殊处理"——需要明确特殊处理是什么，且可能引入与 OOD 不一致的行为
- 如果是"让 DraftContextCleanupTask 的 recordWrite() 调用点使用 `prescriptionId+:criticalAlerts` 格式"——需要明确谁调用 recordWrite() 以及调用时机

TODO 清单 4.2 的原始描述是"清理的是 DraftContextStore 通用键空间，与 PrescriptionDraftContext 的 `prescriptionId+:criticalAlerts` 命名不一致"——这描述的是键命名风格不一致的问题，而非功能缺陷。迁移后 DraftContextCleanupTask 仍在清理 DraftContextStore 的键空间（T3 后 DraftContextStoreImpl 已独立，键空间仅包含 DraftContext 相关数据），键命名风格问题不影响功能正确性。

**修正方向**：明确"对齐键命名"的具体修改内容和预期行为，或承认此为风格改进而非功能修复，降低优先级或移出本任务。

### **[一般]** 移除 enrichWithDrugInfo 死代码未提及测试文件的配套修改

task_v4.md 列出了源码文件的修改（移除方法定义、调用点、DrugFacade 注入、字段、import），但未提及两个测试文件中已有的 DrugFacade 相关测试用例需要同步修改：

1. `PrescriptionAssistServiceImplTest.java`：4 个 DrugFacade 测试用例（`assistShouldCallDrugFacadeForEachDraftItem`、`assistShouldNotBlockWhenDrugFacadeThrows`、`assistShouldLogWarnWhenDrugFacadeFails`、`assistShouldSkipDraftItemsWithNullDrugId`）+ `@Mock DrugFacade drugFacade` 字段 + 构造器调用中的 `drugFacade` 参数
2. `PrescriptionAuditServiceImplTest.java`：4 个 DrugFacade 测试用例（`auditShouldCallDrugFacadeForEachItem`、`auditShouldNotBlockWhenDrugFacadeThrows`、`auditShouldLogWarnWhenDrugFacadeFails`、`auditShouldSkipItemsWithNullDrugId`）+ `@Mock DrugFacade drugFacade` 字段 + 构造器调用中的 `drugFacade` 参数

移除 DrugFacade 注入后，两个测试文件的构造器调用签名将变更（减少 drugFacade 和 drugFacadeTimeout 参数），8 个 DrugFacade 测试用例需删除，`@Mock DrugFacade` 字段和 import 需移除。遗漏这些修改将导致编译失败。

**修正方向**：在涉及文件清单中补充两个测试文件的修改说明。

### **[轻微]** P14 修复方案中 `buildEmptyResponse()` 保持"纯构建方法语义"的决策值得商榷

task_v4.md 第 32 行明确"buildEmptyResponse() 本身不修改（保持纯构建方法语义），由调用方在返回前写入 CRITICAL"。但当前 `buildEmptyResponse()` 已经设置了 `errorCode=RX_ASSIST_AI_NO_RECOMMENDATION`，这本身就是一个业务语义标记。如果 P14 修复方案最终被采纳（写入 CRITICAL 告警），将 CRITICAL 写入逻辑封装在 `buildEmptyResponse()` 内部（或新建一个 `buildEmptyResponseWithCriticalBlock()` 方法）可能更符合单一调用点原则，避免每个 catch 块都重复 CRITICAL 写入逻辑。

当前方案要求在 3 个 catch 块 + 1 个 aiData==null 路径 + 1 个无推荐药品路径共 5 个位置分别写入 CRITICAL，代码重复度高且容易遗漏。

## 修改要求（仅 REJECTED 时）

1. **P14 修复方案**：回到 OOD 设计文档确认 AI 不可用时辅助开方场景的设计意图。如果 OOD 确实要求 AI 不可用时阻断处方提交，应设计独立的阻断维度（而非伪造 CRITICAL 剂量告警）；如果 OOD 不要求，则 P14 修复方案应重新定义。当前方案将"AI 可用性"与"剂量 CRITICAL"两个不同维度混入同一数据结构，违反 OOD 的 PrescriptionDraftContext 语义定义。

2. **DraftContextCleanupTask 对齐键命名**：明确"对齐键命名"的具体修改内容、预期行为变更，或将其降级为风格改进项移出本任务。

3. **测试文件配套修改**：在涉及文件清单中补充 `PrescriptionAssistServiceImplTest.java` 和 `PrescriptionAuditServiceImplTest.java` 的修改说明，包括构造器签名变更、DrugFacade 相关测试用例删除、Mock 字段和 import 移除。
