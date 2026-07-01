# 代码审查报告（v4 r1）

## 审查结果
REJECTED

## 发现

- **[严重]** `prescription/.../service/assist/impl/PrescriptionAssistServiceImplTest.java` — P14 clearCriticalAlerts 测试完全缺失且存在断言错误

  1. **测试 `assistShouldReturnNoRecommendationWhenAiReturnsEmptyDrugs`（line 154）使用 `verify(prescriptionDraftContext, never()).updateCriticalAlerts(anyString(), anyList())`，但生产代码在 `!hasDrugs` 路径（PrescriptionAssistServiceImpl.java:117）明确调用了 `clearCriticalAlerts`，后者内部调用 `prescriptionDraftContext.updateCriticalAlerts(prescriptionId, Collections.emptyList())`。该测试断言与实现矛盾，运行必定失败。**

  2. **其余 4 个 AI 失败/降级路径（InterruptedException/ExecutionException/TimeoutException/aiData==null）的测试均未验证 `clearCriticalAlerts` 是否被调用**——即未验证 `prescriptionDraftContext.updateCriticalAlerts(prescriptionId, Collections.emptyList())`。P14 是本次修改的核心修复目标，5 个调用路径中 0 个有正确的清除行为验证，测试覆盖不满足设计要求。

- **[轻微]** `prescription/.../service/audit/impl/PrescriptionAuditServiceImplTest.java` — 无 P14 相关测试变更（本次设计未要求 AuditService 侧修改，故无问题，仅标注确认）

- **[轻微]** `prescription/.../task/DraftContextCleanupTask.java` — 迁移后代码与设计完全一致，无偏差

- **[轻微]** `prescription/.../task/DraftContextCleanupTaskTest.java` — 迁移后代码与设计完全一致，无偏差

- **[轻微]** `prescription/.../service/assist/impl/PrescriptionAssistServiceImpl.java` — 生产代码实现正确：`clearCriticalAlerts` 私有方法封装合理，5 个调用点位置正确，构造器签名与字段列表符合设计，无 DrugFacade/enrichWithDrugInfo 残留

- **[轻微]** `prescription/.../service/audit/impl/PrescriptionAuditServiceImpl.java` — DrugFacade/enrichWithDrugInfo 已彻底移除，构造器签名与字段列表符合设计

## 修改要求

### 1. PrescriptionAssistServiceImplTest.java — 修正 `assistShouldReturnNoRecommendationWhenAiReturnsEmptyDrugs` 测试

**位置**：line 154  
**问题**：`verify(prescriptionDraftContext, never()).updateCriticalAlerts(anyString(), anyList())` 与生产代码矛盾。`!hasDrugs` 路径在 line 117 调用了 `clearCriticalAlerts`，内部调用 `updateCriticalAlerts(prescriptionId, emptyList())`。  
**期望修正**：将 `never()` 验证改为正向验证，确认调用了 `updateCriticalAlerts` 且参数为空列表：
```java
verify(prescriptionDraftContext).updateCriticalAlerts(anyString(), eq(Collections.emptyList()));
```

### 2. PrescriptionAssistServiceImplTest.java — 为其余 4 个 AI 失败/降级路径补充 clearCriticalAlerts 验证

**位置**：`assistShouldReturnEmptyOnTimeout`（line 97）、`assistShouldReturnEmptyWhenInterrupted`（line 113）、`assistShouldReturnEmptyWhenExecutionException`（line 127）、以及 `aiData == null || !aiResult.isSuccess()` 路径（需新增测试）  
**问题**：P14 核心修复目标是在 AI 失败/降级路径中清除旧 CRITICAL 告警，但 5 个路径中无一有正确的清除行为验证。  
**期望修正**：每个测试均增加 `verify(prescriptionDraftContext).updateCriticalAlerts(anyString(), eq(Collections.emptyList()))` 验证。对于 `aiData == null || !aiResult.isSuccess()` 路径，需新增独立测试用例覆盖 `aiResult.isSuccess() == false` 的场景。

**最低要求**：5 个路径全部有 `updateCriticalAlerts(prescriptionId, emptyList())` 的正向验证。
