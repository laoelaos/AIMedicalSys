# 设计审查报告（v3 r1）

## 审查结果
REJECTED

## 发现

### [严重] 遗漏 `findBySessionId` 的第三方调用者 — `RegistrationEventListener`

设计在行为契约中声称："仅 `saveTriageRecord`（TransactionTemplate）和 `selectDepartment`（@Transactional）两条路径调用 `findBySessionId`"。**此断言与实际代码不符。**

`RegistrationEventListener.handleRegistrationEvent`（`RegistrationEventListener.java:45`）直接调用 `triageRecordRepository.findBySessionId(event.getSessionId())`，且该方法**没有 `@Transactional`**。在 v3 的设计中为 `findBySessionId` 增加了 `@Transactional(Propagation.MANDATORY)`，该调用将在无事务上下文中抛出 `IllegalTransactionStateException`。

**影响**：注册事件处理链路完全中断（即使 `selectDepartment` 已在事务内，`findBySessionId` 的调用发生在事务之外）。

**修正方向**：
- 方案 A：为 `handleRegistrationEvent` 增加 `@Transactional`（使其自身提供事务上下文，`selectDepartment` 内部 `@Transactional(REQUIRED)` 会加入同一事务，`findBySessionId(MANDATORY)` 正常运作）
- 方案 B：将 `findBySessionId` 的传播行为从 `MANDATORY` 改为 `REQUIRED`（调用方无论有无事务均可正常工作，PESSIMISTIC_WRITE 锁仍生效）
- 方案 C：重构 `RegistrationEventListener` 将 `findBySessionId` 逻辑移入事务上下文

### [一般] 未复用模块已有的 `TransactionTemplate` Bean

任务要求"注入 TransactionTemplate transactionTemplate（可通过构造器注入或 @Bean，同模块已有 TransactionTemplate 建议使用构造器注入）"。设计选择注入 `PlatformTransactionManager` 并在构造器内 `new TransactionTemplate(transactionManager)`。

若模块中已存在配置了自定义超时、隔离级别的 `TransactionTemplate` Bean，直接 `new` 创建的新实例会丢失这些配置，可能导致与模块其他部分行为不一致。

**修正方向**：直接注入 `TransactionTemplate` Bean（通过构造器参数），而非注入 `PlatformTransactionManager` 后自行创建。

### [轻微] `synchronized` 粗粒度锁影响并发性能

`DialogueSessionManager.createSession` 使用 `synchronized` 保护 `get` + `put` 的 check-then-act 原子性。由于 `SessionStore` 接口无 `putIfAbsent`，此方案在功能上正确，但锁定粒度为整个 Manager 实例，不同 sessionId 的创建无法并发执行。

### [轻微] `triage()` 方法中存在死代码

当前 `triage()`（`TriageServiceImpl.java:76`）检查 `session.getAdditionalResponses() == null` 并惰性初始化。设计将 `additionalResponses` 改为声明时 `= new CopyOnWriteArrayList<>()`，此 null 检查将变成死代码。虽不影响正确性，但产生不必要的分支。

## 修改要求（仅 REJECTED 时）

### 1. [严重] RegistrationEventListener 事务缺失
**问题**：`RegistrationEventListener.handleRegistrationEvent` 调用 `findBySessionId` 时无事务上下文，与 `@Transactional(Propagation.MANDATORY)` 冲突。

**为什么是问题**：设计断言仅两条调用路径，实际存在第三条路径。当前代码已调用 `findBySessionId`，v3 部署后将引发运行时异常，破坏注册事件处理链路（P0 级问题）。

**修正方向**：选择上述三个方案之一，在设计文档中明确注册事件链路的处理方式，并更新"行为契约—findBySessionId 悲观锁"中的调用路径说明。

### 2. [一般] TransactionTemplate 注入方式偏离任务要求
**问题**：设计使用 `PlatformTransactionManager` + `new TransactionTemplate()`，而非直接注入 `TransactionTemplate` Bean。

**为什么是问题**：可能遗漏已有 Bean 的自定义配置（如超时、隔离级别），与模块已有实践不一致。

**修正方向**：改为注入 `TransactionTemplate transactionTemplate`，移除 `PlatformTransactionManager` 依赖及其 import。
