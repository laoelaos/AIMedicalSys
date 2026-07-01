# 测试报告（v4）

## 概述

基于详细设计 v4 的行为契约，为 3 项修改编写单元测试。测试覆盖 P14 CRITICAL 告警清除契约、DraftContextCleanupTask 迁移契约、enrichWithDrugInfo 死代码移除契约。

## 测试文件清单

| 测试文件 | 操作 | 测试数量 | 覆盖契约 |
|---------|------|---------|---------|
| `prescription/.../service/assist/impl/PrescriptionAssistServiceImplTest.java` | 修改（新增 5 个测试） | +5 | P14 clearCriticalAlerts 幂等性、prescriptionId 传递、正常路径不调用、构造器签名、aiData==null 路径 |
| `prescription/.../service/audit/impl/PrescriptionAuditServiceImplTest.java` | 修改（新增 1 个测试） | +1 | 构造器签名 |
| `prescription/.../task/DraftContextCleanupTaskTest.java` | 修改（新增 3 个测试） | +3 | 包路径契约、@Component 注解、TTL 边界 |

## 新增测试用例详情

### PrescriptionAssistServiceImplTest（+5）

| 测试方法 | 覆盖行为契约 | 维度 |
|---------|------------|------|
| `assistShouldClearCriticalAlertsIdempotently` | clearCriticalAlerts 幂等性：连续两次调用均安全，updateCriticalAlerts 被调用两次 | 正常路径/幂等性 |
| `clearCriticalAlertsShouldPassCorrectPrescriptionIdOnTimeout` | clearCriticalAlerts 传递正确的 prescriptionId | 正常路径/状态交互 |
| `assistShouldNotCallClearCriticalAlertsOnNormalPath` | 正常路径写入非空 CRITICAL 告警（非清除），由 updateCriticalAlerts 处理 | 正常路径/状态交互 |
| `constructorShouldAcceptNineParameters` | 构造器参数从 11 减少到 9（移除 DrugFacade + drugFacadeTimeout） | 边界条件 |
| `assistShouldClearCriticalAlertsWhenAiReturnsNullData` | aiData==null 时清除 CRITICAL 告警 | 错误路径 |

### PrescriptionAuditServiceImplTest（+1）

| 测试方法 | 覆盖行为契约 | 维度 |
|---------|------------|------|
| `constructorShouldAcceptEightParameters` | 构造器参数从 10 减少到 8（移除 DrugFacade + drugFacadeTimeout） | 边界条件 |

### DraftContextCleanupTaskTest（+3）

| 测试方法 | 覆盖行为契约 | 维度 |
|---------|------------|------|
| `taskShouldBeInPrescriptionPackage` | 包路径从 consultation.task 迁移到 prescription.task | 正常路径 |
| `taskShouldHaveComponentAnnotation` | @Component 注解确保 Spring 组件扫描注册 | 正常路径 |
| `cleanupShouldHandleBoundaryTtl` | TTL 边界条件：时间戳设为未来 3600 秒，`ts.plusSeconds(3600)` 在 `Instant.now()` 之后，条目不过期 | 边界条件 |

## 行为契约覆盖矩阵

### P14: assist() CRITICAL 告警清除契约

| assist() 执行路径 | 原有测试覆盖 | 新增测试覆盖 | 验证内容 |
|---|---|---|---|
| catch InterruptedException | `assistShouldReturnEmptyWhenInterrupted` | — | updateCriticalAlerts(id, emptyList) |
| catch ExecutionException | `assistShouldReturnEmptyWhenExecutionException` | — | updateCriticalAlerts(id, emptyList) |
| catch TimeoutException | `assistShouldReturnEmptyOnTimeout` | `clearCriticalAlertsShouldPassCorrectPrescriptionIdOnTimeout` | updateCriticalAlerts(id, emptyList) + 正确 id |
| aiData == null \|\| !aiResult.isSuccess() | `assistShouldClearCriticalAlertsWhenAiResultNotSuccess` | `assistShouldClearCriticalAlertsWhenAiReturnsNullData` | updateCriticalAlerts(id, emptyList)，两个测试覆盖同一执行路径的不同入口条件 |
| !hasDrugs | `assistShouldReturnNoRecommendationWhenAiReturnsEmptyDrugs` | — | updateCriticalAlerts(id, emptyList) |
| 正常路径 | `assistShouldReturnFullResponseWhenAiSuccessWithDrugs` | `assistShouldNotCallClearCriticalAlertsOnNormalPath` | updateCriticalAlerts(id, nonEmptyCriticalAlerts)，验证写入非空 CRITICAL 告警 |

### clearCriticalAlerts 方法契约

| 契约项 | 测试覆盖 |
|-------|---------|
| 前置：prescriptionId 非 null | 5 个 AI 失败/降级路径测试 |
| 后置：getCriticalAlerts 返回 emptyList | 幂等性测试（间接验证） |
| 幂等性 | `assistShouldClearCriticalAlertsIdempotently`（连续两次 assist() 调用，验证 updateCriticalAlerts 被调用两次且无异常） |

### 构造器变更契约

| 契约项 | 测试覆盖 |
|-------|---------|
| PrescriptionAssistServiceImpl 9 参数 | `constructorShouldAcceptNineParameters` |
| PrescriptionAuditServiceImpl 8 参数 | `constructorShouldAcceptEightParameters` |

### DraftContextCleanupTask 迁移契约

| 契约项 | 测试覆盖 |
|-------|---------|
| 包路径变更 | `taskShouldBeInPrescriptionPackage` |
| @Component 注解 | `taskShouldHaveComponentAnnotation` |
| 行为与原文件一致 | 原有 9 个测试 + `cleanupShouldHandleBoundaryTtl` |

## 覆盖维度统计

| 维度 | 数量 |
|-----|------|
| 正常路径 | 6 |
| 边界条件 | 3 |
| 错误路径 | 1 |
| 状态交互 | 2 |
| **合计新增** | **9** |

## 实现偏差关注

实现报告声明"无偏差"，与详细设计一致。

## 修订说明（v4 r2）

| 审查意见 | 采纳 | 修改措施 |
|---------|------|---------|
| [严重] `assistShouldNotCallClearCriticalAlertsOnNormalPath` 的 `never().updateCriticalAlerts(anyString(), eq(Collections.emptyList()))` 与正常路径 `updateCriticalAlerts(id, emptyArrayList)` 矛盾 | 采纳 | 改为构造 `dosageThresholdService.check()` 返回含 CRITICAL 级别警报的场景，用 `ArgumentCaptor` 捕获 `updateCriticalAlerts` 参数列表，断言列表非空（即正常路径写入实际 CRITICAL 告警而非清除） |
| [严重] `assistShouldClearCriticalAlertsIdempotently` 仅调用一次 assist()，未测试幂等性 | 采纳 | 改为连续调用两次 `service.assist()`，验证 `updateCriticalAlerts` 被调用两次（`times(2)`）且无异常抛出，体现重复调用安全性 |
| [一般] `cleanupShouldHandleBoundaryTtl` 使用 `Instant.now().minusSeconds(3600)` 存在纳秒级时间竞争 | 采纳 | 改为使用固定时间点 `Instant.parse("2026-06-30T12:00:00Z")` 构造 boundary，消除 `Instant.now()` 的时间依赖 |
| [一般] 正常路径验证策略应构造含 CRITICAL 警告的场景 | 采纳 | 已在第一条修改中一并解决 |
| [轻微] `assistShouldClearCriticalAlertsWhenAiReturnsNullData` 与 `assistShouldClearCriticalAlertsWhenAiResultNotSuccess` 覆盖同一执行路径的不同入口条件 | 部分采纳 | 两个测试保留（覆盖不同入口条件是合理的策略），但覆盖矩阵中已修正描述，明确两者覆盖同一执行路径的不同入口条件，而非不同执行路径 |

## 修订说明（v4 r3）

| 审查意见 | 采纳 | 修改措施 |
|---------|------|---------|
| [严重] `cleanupShouldHandleBoundaryTtl` 使用固定历史时间点 `Instant.parse("2026-06-30T12:00:00Z")`，生产代码 `cleanupExpiredDrafts()` 内部调用 `Instant.now()` 不可控，测试在特定日期后将确定性失败 | 部分采纳 | 审查建议方案一（注入 Clock）需修改生产代码，超出测试编写职责范围，不采纳。采用方案二思路：将 boundary 时间戳设为 `Instant.now().plusSeconds(3600)`（未来时间），确保 `ts.plusSeconds(3600)` 在 `Instant.now()` 之后，条目不会被判定过期。此方案无法精确测试 boundary 等值条件（即 TTL 恰好 3600 秒的临界情况），但能可靠验证"未过期条目不被清除"的行为。精确 boundary 测试需生产代码引入 Clock 依赖后方可实现 |
| [一般] `assistShouldNotCallClearCriticalAlertsOnNormalPath` 缺少 `dedupTaskScheduler.schedule(anyString())` mock 配置 | 采纳 | 添加 `when(dedupTaskScheduler.schedule(anyString())).thenReturn("test-task-id")`，与 `assistShouldReturnFullResponseWhenAiSuccessWithDrugs` 保持一致 |
| [轻微] `assistShouldClearCriticalAlertsWhenAiReturnsNullData` 隐含依赖 `scheduleSuggestionAsync(null, request)` 不抛异常 | 采纳 | 添加 `when(dedupTaskScheduler.schedule(anyString())).thenReturn("test-task-id")`，使测试意图与实际执行路径对齐，避免隐含依赖 mock 默认行为 |
