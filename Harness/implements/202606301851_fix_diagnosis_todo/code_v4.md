# 实现报告（v4）

## 概述
实现了 prescription 模块 3 项修复：P14 CRITICAL 告警清除（5 个 AI 失败/降级路径）、DraftContextCleanupTask 从 consultation 迁移到 prescription 模块、移除 enrichWithDrugInfo 死代码及 DrugFacade 注入。

## 文件变更清单
| 操作 | 文件路径 | 说明 |
|------|---------|------|
| 修改 | prescription/.../service/assist/impl/PrescriptionAssistServiceImpl.java | P14 clearCriticalAlerts + 移除 enrichWithDrugInfo + 移除 DrugFacade |
| 修改 | prescription/.../service/audit/impl/PrescriptionAuditServiceImpl.java | 移除 enrichWithDrugInfo + 移除 DrugFacade |
| 删除 | consultation/.../task/DraftContextCleanupTask.java | 迁移到 prescription 模块 |
| 删除 | consultation/.../task/DraftContextCleanupTaskTest.java | 迁移到 prescription 模块 |
| 新建 | prescription/.../task/DraftContextCleanupTask.java | 从 consultation 迁入，包路径变更 |
| 新建 | prescription/.../task/DraftContextCleanupTaskTest.java | 从 consultation 迁入，包路径变更 |
| 修改 | prescription/.../service/assist/impl/PrescriptionAssistServiceImplTest.java | 移除 DrugFacade 相关测试 + Mock 字段 + 构造器签名变更 + P14 clearCriticalAlerts 验证 |
| 修改 | prescription/.../service/audit/impl/PrescriptionAuditServiceImplTest.java | 移除 DrugFacade 相关测试 + Mock 字段 + 构造器签名变更 |

## 编译验证
未执行编译验证

## 设计偏差说明
无偏差

## 修订说明（v4 r1）
| 审查意见 | 修改措施 |
|---------|---------|
| `assistShouldReturnNoRecommendationWhenAiReturnsEmptyDrugs` 使用 `verify(..., never()).updateCriticalAlerts(...)` 与生产代码矛盾 | 将 `never()` 验证改为正向验证 `verify(prescriptionDraftContext).updateCriticalAlerts(anyString(), eq(Collections.emptyList()))` |
| 4 个 AI 失败/降级路径（Timeout/Interrupted/ExecutionException/aiResult not success）缺少 clearCriticalAlerts 验证 | 为 `assistShouldReturnEmptyOnTimeout`、`assistShouldReturnEmptyWhenInterrupted`、`assistShouldReturnEmptyWhenExecutionException` 3 个已有测试各增加 `verify(prescriptionDraftContext).updateCriticalAlerts(anyString(), eq(Collections.emptyList()))` 验证；新增 `assistShouldClearCriticalAlertsWhenAiResultNotSuccess` 测试覆盖 `aiResult.isSuccess() == false` 路径 |
