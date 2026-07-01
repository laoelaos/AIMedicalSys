# 任务指令（v4）

## 动作
NEW

## 任务描述
修复 prescription 模块中 3 项问题：
1. **P14 CRITICAL 阻断写入**：`PrescriptionAssistServiceImpl.assist()` 各 catch 块（ExecutionException/TimeoutException/InterruptedException）和 AI 返回空结果路径在 AI 失败/降级时未调用 `prescriptionDraftContext.updateCriticalAlerts` 清除旧 CRITICAL 告警，导致提交端点步① CRITICAL 阻断检查可能残留过期告警
2. **DraftContextCleanupTask 模块迁移**：将 `DraftContextCleanupTask` 从 `consultation` 模块迁移到 `prescription` 模块（`com.aimedical.modules.prescription.task`）；迁移测试文件
3. **移除 enrichWithDrugInfo 死代码**：`PrescriptionAssistServiceImpl:230-243` 和 `PrescriptionAuditServiceImpl:518-532` 中 `enrichWithDrugInfo` 调用 `drugFacade.findByDrugCode(drugCode)` 结果赋给局部变量 `DrugInfo info` 但从不被读取，属于死代码；移除方法定义和调用点，移除 `DrugFacade` 构造器注入

涉及文件：
- `prescription/.../service/assist/impl/PrescriptionAssistServiceImpl.java` — P14 修复 + 移除 enrichWithDrugInfo + 移除 DrugFacade 注入
- `prescription/.../service/audit/impl/PrescriptionAuditServiceImpl.java` — 移除 enrichWithDrugInfo + 移除 DrugFacade 注入
- `prescription/.../service/assist/impl/PrescriptionAssistServiceImplTest.java` — 移除 DrugFacade 相关测试用例 + Mock 字段 + 构造器参数变更
- `prescription/.../service/audit/impl/PrescriptionAuditServiceImplTest.java` — 移除 DrugFacade 相关测试用例 + Mock 字段 + 构造器参数变更
- `consultation/.../task/DraftContextCleanupTask.java` — 删除（迁移到 prescription）
- `consultation/.../task/DraftContextCleanupTaskTest.java` — 删除（迁移到 prescription）
- `prescription/.../task/DraftContextCleanupTask.java` — 新建（从 consultation 迁入）
- `prescription/.../task/DraftContextCleanupTaskTest.java` — 新建（从 consultation 迁入，适配新包路径）

## 选择理由
T3 Store 基础设施修复完成，DraftContextStoreImpl 已独立实现。T4 中 P14 为 CRITICAL 级别阻断写入缺陷，直接影响处方安全闭环；DraftContextCleanupTask 迁移依赖 T3 的 DraftContextStoreImpl 独立实现（迁移后注入 DraftContextStore 接口获得 DraftContextStoreImpl bean）；enrichWithDrugInfo 死代码清理为独立任务可一并完成，减少技术债务。

## 任务上下文

### P14 CRITICAL 阻断写入
- **TODO 清单描述**：`PrescriptionAssistServiceImpl.java:97-115, 245-254` 的 `assist()` 各 catch 块在 failReason 计算后未调用 `prescriptionDraftContext.updateCriticalAlerts` 写入 CRITICAL 阻断；`buildEmptyResponse()` 同样未写
- **OOD 设计要求**（§4.4 辅助开方场景）：CRITICAL 告警同步写入 PrescriptionDraftContext（以 prescriptionId 为键全量覆盖，写入逻辑与 check-dose 路径一致——覆盖当前 prescriptionId 下所有 CRITICAL 标记，无 CRITICAL 时清除对应条目）
- **OOD 设计要求**（§4.2 处方提交端点步①）：CRITICAL 阻断检查（最高优先级）：检查 PrescriptionDraftContext 中该 prescriptionId 是否存在 CRITICAL 级别剂量告警。若存在，立即返回 HTTP 422 + SubmitResponse(submitted=false, blockInfo=BlockResponse)
- **OOD 语义分析**：OOD §4.4 明确 CRITICAL 告警的来源是 **DosageThresholdService 剂量阈值校验结果中 alertLevel=CRITICAL 的告警**（§3.4 PrescriptionAssistService 职责："校验后将 alertLevel=CRITICAL 的剂量告警同步写入 PrescriptionDraftContext"），而非 AI 服务可用性状态。OOD §4.4 对 AI 返回无可推荐药品场景的处理是"返回空 prescriptionDraft + 本地校验结果 + PrescriptionAssistResponse.errorCode=RX_ASSIST_AI_NO_RECOMMENDATION"，未要求写入 CRITICAL 告警。OOD 对 AI 失败/超时场景（catch 块路径）甚至没有显式描述辅助开方的降级路径（与处方审核 audit 的降级路径不同——audit 有 LocalRuleEngine 降级，assist 没有）。将"AI 不可用"映射为 CRITICAL 剂量告警（如 `drugCode="SYSTEM"`）违反 DosageAlert 模型的语义定义（drugCode 语义是药品编码，"SYSTEM" 不属于任何药品编码体系），且会污染 PrescriptionDraftContext 的 `hasCriticalAlerts()` 和 `getCriticalAlerts()` 方法
- **当前代码问题**：`assist()` 方法在 AI 调用失败（catch 块 line 98-104）和 AI 返回空结果（line 113-114）时直接调用 `buildEmptyResponse()` 返回，**不调用 `prescriptionDraftContext.updateCriticalAlerts`**。这意味着：
  - 如果之前 check-dose 路径已写入 CRITICAL 告警，AI 失败路径不会清除旧告警，提交端点步①仍会检测到残留的 CRITICAL 告警（这是正确行为——旧告警应保留直到新的 check-dose 重算覆盖）
  - 如果之前无 CRITICAL 告警，AI 失败路径也不应写入伪 CRITICAL 告警（OOD 未要求）
- **修复方案**：在 `assist()` 的每个失败/降级路径中，调用 `prescriptionDraftContext.updateCriticalAlerts(prescriptionId, Collections.emptyList())` 清除该 prescriptionId 下可能残留的旧 CRITICAL 告警。这确保：
  - AI 失败时，如果之前 check-dose 写入的 CRITICAL 告警已因处方草案变更而不再适用，旧告警被清除
  - 不引入"AI 不可用"伪 CRITICAL 告警，保持 PrescriptionDraftContext 语义与 OOD 一致
  - 具体修改点：
    - catch 块（ExecutionException/TimeoutException/InterruptedException）中，在返回 `buildEmptyResponse()` 前调用 `prescriptionDraftContext.updateCriticalAlerts(prescriptionId, Collections.emptyList())`
    - AI 返回空结果（line 113-114 `aiData == null || !aiResult.isSuccess()`）时，同样清除旧告警
    - AI 返回无可推荐药品时（line 118-122），同样清除旧告警（此场景 OOD 明确为"AI 有效结果"，本地校验结果为空，无 CRITICAL 告警应写入）
  - `buildEmptyResponse()` 本身不修改（保持纯构建方法语义），由调用方在返回前清除旧告警
  - **封装优化**：为避免 5 个路径重复调用 `updateCriticalAlerts(prescriptionId, emptyList())`，提取私有方法 `clearCriticalAlerts(String prescriptionId)` 封装清除逻辑，各失败路径统一调用

### DraftContextCleanupTask 模块迁移
- **TODO 清单描述**：实际位置 `modules/consultation/.../task/DraftContextCleanupTask.java`，计划要求 `modules/prescription/.../task/`；依赖类型为 `DraftContextStore`（common-module），非 `PrescriptionDraftContext`
- **当前代码**：`DraftContextCleanupTask` 在 consultation 模块，注入 `DraftContextStore`，遍历 `draftContextStore.keySet()` 清理 TTL 过期条目
- **修复方案**：
  1. 将 `DraftContextCleanupTask` 从 `com.aimedical.modules.consultation.task` 迁移到 `com.aimedical.modules.prescription.task`
  2. 包声明改为 `package com.aimedical.modules.prescription.task`
  3. 注入 `DraftContextStore`（T3 已创建独立 `DraftContextStoreImpl` bean，迁移后自动获得该 bean）
  4. 清理逻辑不变——当前 `cleanupExpiredDrafts()` 对所有键一视同仁（遍历 keySet 检查 TTL 过期后移除），`prescriptionId+:criticalAlerts` 格式的键与任何其他格式的键在清理逻辑上没有区别，无需特殊处理
  5. 删除 consultation 模块中的原文件和测试文件
  6. 在 prescription 模块新建测试文件，包路径对齐
- **关于"对齐键命名"**：TODO 清单 4.2 原始描述"清理的是 DraftContextStore 通用键空间，与 PrescriptionDraftContext 的 prescriptionId+:criticalAlerts 命名不一致"是键命名风格不一致的问题，而非功能缺陷。DraftContextCleanupTask 清理 DraftContextStore 的键空间，T3 后 DraftContextStoreImpl 已独立，键空间仅包含 DraftContext 相关数据。清理逻辑对所有键格式一视同仁，键命名风格不影响功能正确性。因此本任务仅做模块迁移，不做键命名变更

### 移除 enrichWithDrugInfo 死代码
- **TODO 清单描述**：`PrescriptionAssistServiceImpl:230-243` 和 `PrescriptionAuditServiceImpl:518-532` 调用 `drugFacade.findByDrugCode(drugCode)` 结果赋给局部 `DrugInfo info` 但从不被读取
- **当前代码**：两个 Service 均有 `enrichWithDrugInfo` 私有方法，调用 `drugFacade.findByDrugCode(drugCode)` 仅捕获异常 + WARN 日志，返回值 `DrugInfo info` 未被使用
- **修复方案**：
  1. 移除 `PrescriptionAssistServiceImpl.enrichWithDrugInfo()` 方法定义和 line 132 的调用
  2. 移除 `PrescriptionAuditServiceImpl.enrichWithDrugInfo()` 方法定义和 line 147 的调用
  3. 移除两个 Service 构造器中的 `DrugFacade drugFacade` 参数和 `drugFacadeTimeout` 参数
  4. 移除两个 Service 中的 `drugFacade` 和 `drugFacadeTimeout` 字段
  5. 移除相关 import（`DrugFacade`、`DrugInfo`）
  6. 注意：DrugFacade 在 OOD §2.2 中定义为"跨模块药品信息查询门面"，当前仅用于 enrichWithDrugInfo 死代码。移除后 DrugFacade 接口定义保留在 common-module-api 中（不删除接口本身），仅移除 prescription 模块中的注入和调用
- **测试文件配套修改**：
  - `PrescriptionAssistServiceImplTest.java`：
    - 移除 `@Mock DrugFacade drugFacade` 字段
    - 移除 4 个 DrugFacade 测试用例：`assistShouldCallDrugFacadeForEachDraftItem`、`assistShouldNotBlockWhenDrugFacadeThrows`、`assistShouldLogWarnWhenDrugFacadeFails`、`assistShouldSkipDraftItemsWithNullDrugId`
    - 构造器调用签名变更：移除 `drugFacade` 和 `2L`（drugFacadeTimeout）参数
    - 移除 `DrugFacade` import
  - `PrescriptionAuditServiceImplTest.java`：
    - 移除 `@Mock DrugFacade drugFacade` 字段
    - 移除 4 个 DrugFacade 测试用例：`auditShouldCallDrugFacadeForEachItem`、`auditShouldNotBlockWhenDrugFacadeThrows`、`auditShouldLogWarnWhenDrugFacadeFails`、`auditShouldSkipItemsWithNullDrugId`
    - 构造器调用签名变更：移除 `drugFacade` 和 `2L`（drugFacadeTimeout）参数
    - 移除 `DrugFacade` import

## 已有代码上下文

### PrescriptionAssistServiceImpl（当前状态）
- 构造器注入：AiService, AssistConverter, AllergyCheckRule, DosageThresholdService, PrescriptionDraftContext, DedupTaskScheduler, SuggestionStore, ObjectMapper, DrugFacade, drugFacadeTimeout, aiTimeout
- `assist()` 方法：line 86-180，AI 调用失败时 catch 块直接返回 `buildEmptyResponse()`，不调用 `updateCriticalAlerts`
- `buildEmptyResponse()`：line 245-254，纯构建方法
- `enrichWithDrugInfo()`：line 230-243，死代码
- `checkDose()`：line 183-213，正常路径已调用 `prescriptionDraftContext.updateCriticalAlerts()`

### PrescriptionAuditServiceImpl（当前状态）
- 构造器注入：AiService, LocalRuleEngine, AuditRecordRepository, AuditConverter, PrescriptionDraftContext, CurrentUser, ObjectMapper, DrugFacade, drugFacadeTimeout, aiTimeout
- `enrichWithDrugInfo()`：line 518-532，死代码

### PrescriptionAssistServiceImplTest（当前状态）
- `@Mock DrugFacade drugFacade` 字段（line 48）
- 构造器调用含 `drugFacade, 2L` 参数（line 59, 110, 851）
- 4 个 DrugFacade 测试用例（line 244-350）
- DrugFacade import（line 6）

### PrescriptionAuditServiceImplTest（当前状态）
- `@Mock DrugFacade drugFacade` 字段（line 66）
- 构造器调用含 `drugFacade, 2L` 参数（line 77, 404, 464）
- 4 个 DrugFacade 测试用例（line 568-654）
- DrugFacade import（line 13）

### DraftContextCleanupTask（当前状态，consultation 模块）
- 包路径：`com.aimedical.modules.consultation.task`
- 注入：`DraftContextStore`
- 清理逻辑：遍历 `draftContextStore.keySet()`，检查 `writeTimestamps` 中 TTL 过期条目并移除
- 测试：`DraftContextCleanupTaskTest`（9 个用例，StubDraftContextStore 实现 DraftContextStore）

### PrescriptionDraftContext（当前状态）
- 注入 `DraftContextStore`（T3 后获得 `DraftContextStoreImpl` bean）
- 键格式：`prescriptionId + ":criticalAlerts"`
- `updateCriticalAlerts(prescriptionId, alerts)`：alerts 为空时 remove，非空时 put

### DosageAlert（prescription context 包）
- 字段：severity (String), message (String), drugCode (String)
- 用于 PrescriptionDraftContext 中 CRITICAL 告警存储

## 修订说明（v4 r1）
| 审查意见 | 修改措施 |
|---------|---------|
| **[严重]** P14 修复方案与 OOD 设计语义不一致——AI 不可用时写入 CRITICAL 告警缺乏设计依据。OOD CRITICAL 告警来源是 DosageThresholdService 剂量阈值校验结果，而非 AI 服务可用性状态。`drugCode="SYSTEM"` 的 CRITICAL 告警在 DosageAlert 模型中从未定义过，会污染 PrescriptionDraftContext 语义 | 采纳。P14 修复方案改为：在 `assist()` 的每个失败/降级路径中调用 `prescriptionDraftContext.updateCriticalAlerts(prescriptionId, Collections.emptyList())` 清除旧 CRITICAL 告警，而非写入伪 CRITICAL 告警。这确保 AI 失败时旧告警被正确清除（与 OOD "无 CRITICAL 时清除对应条目"语义一致），不引入与 OOD 不一致的"AI 不可用"伪 CRITICAL 告警。提取 `clearCriticalAlerts(prescriptionId)` 私有方法封装清除逻辑，避免 5 个路径重复调用 |
| **[一般]** DraftContextCleanupTask 迁移后"对齐键命名"描述模糊且可能引入行为变更。当前清理逻辑对所有键一视同仁，`prescriptionId+:criticalAlerts` 格式的键无需特殊处理 | 采纳。移除"对齐键命名"要求。DraftContextCleanupTask 迁移仅做模块归属修正（从 consultation 迁到 prescription），清理逻辑不变。键命名风格不一致是非功能性问题，不影响清理正确性 |
| **[一般]** 移除 enrichWithDrugInfo 死代码未提及测试文件的配套修改。两个测试文件中 8 个 DrugFacade 测试用例需删除，Mock 字段和 import 需移除，构造器签名需变更 | 采纳。在涉及文件清单中补充 `PrescriptionAssistServiceImplTest.java` 和 `PrescriptionAuditServiceImplTest.java` 的修改说明，包括构造器签名变更、DrugFacade 相关测试用例删除、Mock 字段和 import 移除 |
| **[轻微]** P14 修复方案中 `buildEmptyResponse()` 保持"纯构建方法语义"的决策值得商榷，5 个位置重复 CRITICAL 写入逻辑代码重复度高 | 采纳。提取 `clearCriticalAlerts(String prescriptionId)` 私有方法封装清除逻辑，各失败路径统一调用该方法，避免重复代码 |
