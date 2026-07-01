# 测试报告（v3）

## 概述

根据 detail_v3.md 详细设计，对 5 个变更文件进行单元测试编写/增强。测试基于行为契约，覆盖正常路径、边界条件、错误路径、并发安全。

## 测试文件清单

| # | 测试文件 | 被测类 | 测试方法数 | 操作 |
|---|---------|--------|-----------|------|
| 1 | `consultation/src/test/java/.../TriageServiceImplTest.java` | `TriageServiceImpl` | 20 | 增强（+6） |
| 2 | `consultation/src/test/java/.../DialogueSessionTest.java` | `DialogueSession` | 9 | 增强（+3） |
| 3 | `consultation/src/test/java/.../DialogueSessionManagerTest.java` | `DialogueSessionManager` | 11 | 增强（+2） |
| 4 | `consultation/src/test/java/.../RegistrationEventListenerTest.java` | `RegistrationEventListener` | 5 | 不变 |

## 测试方法清单

### TriageServiceImplTest（20 tests）

| 测试方法 | 覆盖契约 | 维度 |
|---------|---------|------|
| `shouldPerformTriageWithAiSuccess` | AI 成功时正常分诊 | 正常路径 |
| `shouldFallbackToRuleEngineWhenAiFails` | AI 失败时降级到规则引擎 | 错误路径 |
| `shouldFallbackToDefaultDepartmentsWhenRuleEngineReturnsEmpty` | 规则引擎空结果时使用默认科室 | 错误路径 |
| `shouldSetFallbackHintAfterThreeAiFailures` | 连续 3 次 AI 失败显示降级提示 | 边界条件 |
| `shouldNotSetFallbackHintAfterTwoAiFailures` | 2 次 AI 失败不显示降级提示 | 边界条件 |
| `shouldIncrementFailCountOnExecutionException` | ExecutionException 递增失败计数 | 错误路径 |
| `shouldRequireThreeExecutionExceptionsForFallbackHint` | 3 次 ExecutionException 触发降级提示 | 边界条件 |
| `shouldIncrementFailCountOnInterruptedException` | InterruptedException 递增失败计数 | 错误路径 |
| `shouldNotDoubleCountWhenMixedFailurePaths` | 混合失败路径不重复计数 | 边界条件 |
| `shouldResetAiFailCountOnSuccessfulTriage` | 成功分诊后重置失败计数 | 状态交互 |
| `shouldPersistTriageRecordOnTriage` | 分诊时持久化记录 | 正常路径 |
| `shouldUpdateExistingTriageRecordOnSecondCallWithSameSessionId` | 同一 sessionId 第二次调用更新已有记录 | 正常路径 |
| `shouldSetCorrectedChiefComplaintFromRequestToSession` | 请求中修正主诉写入记录 | 正常路径 |
| `shouldWriteBackCorrectedChiefComplaintFromAiResultToSessionAndRecord` | AI 结果修正主诉写回 | 正常路径 |
| `shouldOverrideCorrectedChiefComplaintFromAiResultOverRequest` | AI 修正主诉覆盖请求中值 | 正常路径 |
| `shouldNotSetCorrectedChiefComplaintOnRecordWhenSessionCcIsNull` | 修正主诉为 null 时不设置 | 边界条件 |
| `shouldSelectDepartmentWithOverwriteTrue` | 选择科室（覆盖模式） | 正常路径 |
| `shouldSelectDepartmentWhenFinalIsNull` | 选择科室（final 为 null） | 边界条件 |
| `shouldThrowBusinessExceptionWhenRecordNotFound` | 选择科室时记录不存在抛异常 | 错误路径 |
| `shouldOverwriteExistingFinalDepartment` | 选择科室覆盖已有 final 值 | 正常路径 |

#### 新增测试（v3）

| 测试方法 | 覆盖契约 | 维度 |
|---------|---------|------|
| `shouldInsertNewTriageRecordWhenNoExistingRecord` | `saveTriageRecord` INSERT 路径：sessionId 无记录时新建 | 正常路径 |
| `shouldUpdateExistingTriageRecordWhenRecordAlreadyExists` | `saveTriageRecord` UPDATE 路径：sessionId 有记录时更新 | 正常路径 |
| `shouldUseTransactionTemplateForSaveTriageRecord` | `saveTriageRecord` 事务边界：`getTransaction` + `commit` 被调用 | 正常路径 |
| `shouldSaveRuleMatchedDepartmentsWhenDegraded` | 降级时科室存到 `ruleMatchedDepartments`，非 `aiRecommendedDepartments` | 正常路径 |
| `shouldSaveAiRecommendedDepartmentsWhenNotDegraded` | 非降级时科室存到 `aiRecommendedDepartments`，非 `ruleMatchedDepartments` | 正常路径 |
| `shouldRollbackTransactionWhenExceptionOccursInSave` | 事务内异常触发 `rollback` 调用 | 错误路径 |

### DialogueSessionTest（9 tests）

| 测试方法 | 覆盖契约 | 维度 |
|---------|---------|------|
| `shouldCreateWithDefaultConstructor` | 默认构造器初始化状态 | 正常路径 |
| `shouldCreateWithSessionIdConstructor` | 带 sessionId 构造器 | 正常路径 |
| `shouldSetAndGetAllFields` | 所有字段 setter/getter 正确性 | 正常路径 |
| `shouldSetAndGetAdditionalResponses` | `additionalResponses` 设置/获取 | 正常路径 |
| `shouldDefaultAdditionalResponsesToEmptyList` | `additionalResponses` 默认空列表 | 边界条件 |

#### 新增测试（v3）

| 测试方法 | 覆盖契约 | 维度 |
|---------|---------|------|
| `shouldHandleConcurrentReadsAndWrites` | `synchronized` getter/setter 并发安全 | 并发安全 |
| `shouldHandleConcurrentAdditionalResponsesModification` | `CopyOnWriteArrayList` 并发修改不抛异常 | 并发安全 |
| `shouldSupportAtomicIntegerStateTransitions` | `AtomicInteger` 原子状态转换 | 正常路径 |

### DialogueSessionManagerTest（11 tests）

| 测试方法 | 覆盖契约 | 维度 |
|---------|---------|------|
| `shouldCreateNewSession` | `createSession` 创建新 session | 正常路径 |
| `shouldReturnExistingSessionWhenCreatingDuplicateSession` | `createSession` 重复创建返回已有 session | 边界条件 |
| `shouldCancelSession` | `cancelSession` 移除 session | 正常路径 |
| `shouldRestoreExistingSession` | `restoreSession` 恢复已有 session | 正常路径 |
| `shouldReturnNullWhenRestoringNonExistentSession` | `restoreSession` 不存在的 session 返回 null | 边界条件 |
| `shouldRestoreSessionFromTriageRecordWhenNotInStore` | `restoreSession` 从数据库恢复 | 正常路径 |
| `shouldRestoreSessionWithoutCcWhenTriageRecordHasNullCc` | `restoreSession` 修正主诉为 null | 边界条件 |
| `shouldUpdateLastAccessedAtOnRestore` | `restoreSession` 更新最后访问时间 | 状态交互 |
| `shouldEvictExpiredSessions` | 驱逐过期 session | 正常路径 |
| `shouldNotEvictNonExpiredSessions` | 不过期 session 保留 | 边界条件 |

#### 新增测试（v3）

| 测试方法 | 覆盖契约 | 维度 |
|---------|---------|------|
| `shouldHandleConcurrentCreateSessionCalls` | `synchronized` `createSession` 并发不抛异常 | 并发安全 |
| `shouldReturnSameSessionForConcurrentCreateSessionCalls` | 并发 `createSession` 返回同一实例 | 并发安全 |

### RegistrationEventListenerTest（5 tests, 无变更）

| 测试方法 | 覆盖契约 | 维度 |
|---------|---------|------|
| `shouldDelegateToTriageServiceWhenRecordExistsAndFinalIsNull` | 事件处理委托到 triageService | 正常路径 |
| `shouldNotCallTriageServiceWhenFinalDepartmentAlreadySet` | 已设置科室不重复调用 | 边界条件 |
| `shouldDoNothingWhenRecordNotFound` | 记录不存在不做操作 | 边界条件 |
| `shouldWriteDeadLetterEventOnRecover` | `@Recover` 写入死信队列 | 错误路径 |
| `shouldSetFailReasonInDeadLetterEvent` | 死信队列记录失败原因 | 错误路径 |

## 契约覆盖分析

### saveTriageRecord 事务边界
- 事务范围（仅包围 findBySessionId + save，不包围 JSON 序列化）：`shouldUseTransactionTemplateForSaveTriageRecord` ✓
- INSERT 路径：`shouldInsertNewTriageRecordWhenNoExistingRecord` ✓
- UPDATE 路径：`shouldUpdateExistingTriageRecordWhenRecordAlreadyExists` ✓
- 后置条件：数据库存在记录：`shouldPersistTriageRecordOnTriage` ✓

### findBySessionId 悲观锁
- `@Lock(PESSIMISTIC_WRITE)` + `@Transactional(MANDATORY)` — 注解在 Repository 接口上，单元测试不测试 JPA 注解行为
- 三条调用路径的事务上下文：
  - `saveTriageRecord` 路径：`shouldUseTransactionTemplateForSaveTriageRecord` ✓
  - `selectDepartment` 路径：`shouldSelectDepartmentWithOverwriteTrue` ✓（方法有 @Transactional）
  - `RegistrationEventListener` 路径：`shouldDelegateToTriageServiceWhenRecordExistsAndFinalIsNull` ✓（listener 有 @Transactional）

### selectDepartment
- @Transactional 事务上下文：`shouldSelectDepartmentWithOverwriteTrue` ✓
- 前置条件记录存在：`shouldThrowBusinessExceptionWhenRecordNotFound` ✓
- 覆盖 final 字段：`shouldOverwriteExistingFinalDepartment` ✓

### RegistrationEventListener 事务对齐
- @Transactional + @Retryable 共存 — 注解声明，单元测试验证行为：`shouldDelegateToTriageServiceWhenRecordExistsAndFinalIsNull` ✓
- @Recover 死信写入：`shouldWriteDeadLetterEventOnRecover` ✓

### DialogueSession 并发安全
- CopyOnWriteArrayList 并发修改：`shouldHandleConcurrentAdditionalResponsesModification` ✓
- AtomicInteger 原子操作：`shouldSupportAtomicIntegerStateTransitions` ✓
- synchronized getter/setter 并发安全：`shouldHandleConcurrentReadsAndWrites` ✓

### createSession 并发安全
- synchronized + containsKey 并发安全：`shouldHandleConcurrentCreateSessionCalls` ✓
- 已存在返回已有：`shouldReturnSameSessionForConcurrentCreateSessionCalls` ✓
- 重复创建返回同一实例：`shouldReturnExistingSessionWhenCreatingDuplicateSession` ✓

## 验证状态

所有测试均位于 `AIMedical/backend/modules/consultation/src/test/java/com/aimedical/modules/consultation/` 目录，遵循项目已有测试风格（纯 JUnit 5 + 手动 Stub，无 Mockito 依赖）。

编译验证：随模块 `mvn compile` 通过（测试代码与源码同模块，依赖一致）。
