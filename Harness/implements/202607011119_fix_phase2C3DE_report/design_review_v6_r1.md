# 设计审查报告（v6 r1）

## 审查结果
APPROVED

## 发现

无严重/一般问题。

所有 5 项缺陷的修改方案均完整覆盖 task_v6.md 和 requirement.md 的要求：

- **1a (DraftContextCleanupTask)**：`PrescriptionDraftContext` 新增 `DraftContextCleanupTask` 字段注入（两 bean 均为 `@Component`，Spring 自动装配有效），`updateCriticalAlerts()` 中 `put` 后调 `recordWrite`、`remove` 后调 `removeTimestamp`，行为契约明确。测试修改涵盖 mock 字段、构造函数、verify 断言。

- **1b (SuggestionCleanupTask)**：`PrescriptionAssistServiceImpl` 补充 `setCreateTime(LocalDateTime.now())` 消除 NPE；`isExpiredAndConsumed()` FAILED 分支不要求 `consumed`，null-safe `getTimestamp()` 处理。测试重命名 + 数据修改 + 新增未过期 FAILED 用例。

- **2 (MockAiService)**：`@Profile("mock")` → `@ConditionalOnProperty(name = "ai.mock.enabled", havingValue = "true")`，import 增减明确，`matchIfMissing=false` 默认关闭行为正确。

- **3 (RegistrationEventListener)**：`@Transactional` 注解、`e.getMessage()` null 防护（`"Unknown failure reason"`）、`event.getSessionId()` null 防护（`"unknown"`）三项修改完整，import 无需新增。

- **4 (MedicalRecord)**：`visitId` 加 `unique=true` 约束；`DataIntegrityViolationException` catch 分支置于 `ObjectOptimisticLockingFailureException` 之后，INSERT 与 UPDATE 两路径互补保护。`MR_GEN_CONCURRENT_MODIFICATION` 错误码确已存在于枚举。

设计文档结构清晰，修改范围精确到行号，未超出 task 定义的边界，各缺陷互不依赖可并行实现。
