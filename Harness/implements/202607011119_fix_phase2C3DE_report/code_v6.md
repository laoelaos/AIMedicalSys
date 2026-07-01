# 实现报告（v6）

## 概述

修复 4 个模块的 5 项 P1/P2 缺陷，涉及 8 个源文件和 2 个测试文件的修改。

## 文件变更清单

| 操作 | 文件路径 | 说明 |
|------|---------|------|
| 修改 | `prescription/context/PrescriptionDraftContext.java` | 新增 `DraftContextCleanupTask` 字段注入，`updateCriticalAlerts()` 中调用 `recordWrite`/`removeTimestamp` |
| 修改 | `prescription/service/assist/impl/PrescriptionAssistServiceImpl.java` | `scheduleSuggestionAsync()` 中 `result` 补充 `setCreateTime(LocalDateTime.now())` |
| 修改 | `prescription/task/SuggestionCleanupTask.java` | `isExpiredAndConsumed()` 重构：FAILED 不要求 consumed，null-safe timestamp 处理 |
| 修改 | `ai/ai-impl/.../mock/MockAiService.java` | `@Profile("mock")` 替换为 `@ConditionalOnProperty(name = "ai.mock.enabled", havingValue = "true")` |
| 修改 | `consultation/event/RegistrationEventListener.java` | `recover()` 追加 `@Transactional`；null 防护 `e.getMessage()` 和 `event.getSessionId()` |
| 修改 | `medical-record/entity/MedicalRecord.java` | `visitId` 字段追加 `unique = true` 约束 |
| 修改 | `medical-record/service/impl/MedicalRecordServiceImpl.java` | `save()` try-catch 追加 `DataIntegrityViolationException` 分支 |
| 修改 | `prescription/context/PrescriptionDraftContextTest.java` | 新增 `cleanupTask` mock 字段、构造函数改为 2 参数、`updateCriticalAlerts` 测试追加 verify 断言 |
| 修改 | `prescription/task/SuggestionCleanupTaskTest.java` | 重命名测试、修改 FAILED 测试数据 `consumed=false`、新增未过期 FAILED 不清理测试 |

## 编译验证

未执行编译验证。

## 设计偏差说明

无偏差。所有修改严格按详细设计 v6 规格实现，包括：

- **1a**: `PrescriptionDraftContext` 构造函数扩展为 2 参数，`updateCriticalAlerts()` 中 put/remove 后正确调用 `recordWrite`/`removeTimestamp`
- **1b**: `PrescriptionAssistServiceImpl.scheduleSuggestionAsync()` 中 `result.setCreateTime(LocalDateTime.now())` 追加
- **1c**: `SuggestionCleanupTask.isExpiredAndConsumed()` 重构，FAILED 仅要求过期不要求 consumed，添加 null-safe timestamp 检查
- **2**: `MockAiService` 注解替换，`@Profile` import 移除，`@ConditionalOnProperty` import 新增
- **3**: `RegistrationEventListener.recover()` 添加 `@Transactional`，null 防护 `e.getMessage()` → "Unknown failure reason"，`event.getSessionId()` → "unknown"
- **4**: `MedicalRecord.visitId` 追加 `unique = true`，`MedicalRecordServiceImpl.save()` 追加 `DataIntegrityViolationException` catch 分支
- **测试**: `PrescriptionDraftContextTest` 新增 `cleanupTask` mock/constructor 参数/verify 断言；`SuggestionCleanupTaskTest` 重命名测试 + 新增 `shouldNotRemoveFailedEntryWhenNotExpired`
