# 计划审查报告（v9 r1）

## 审查结果
REJECTED

## 发现

### [一般] 1. 缺少测试修改计划

plan.md R9 章节未包含任何测试文件的修改方案。此前每轮（R1-R8）均明确列出测试文件修改清单：
- R5 列出了 10 个 PrescriptionAssistServiceImplTest 受影响测试名 + mock 策略，以及 5 个 DedupTaskSchedulerTest 测试名
- R6 列出了 PrescriptionDraftContextTest 构造函数变更 + SuggestionCleanupTaskTest 重命名/新增

R9 作为涉及 3 个源文件、5 项缺陷修改的批量任务，缺少测试修改计划将导致：新增代码无法被测试覆盖、现有测试可能因代码变更而失败、修复有效性无法验证。

期望：补充每项修改对应的测试验证方案，至少包括现有测试的适配说明（如因 TriageServiceImpl 降级路径重构需调整 mock 的测试）和关键场景的新增测试。

---

### [一般] 2. 子项 7a (C13) — MatchResult 已有 ruleVersionMismatch 标记，计划描述与代码现状脱节

经检查实际代码：
- `MatchResult.java:9` 已有 `private boolean ruleVersionMismatch` 字段，`isRuleVersionMismatch()` getter 存在
- `DefaultTriageRuleEngine.match():82` 已通过构造函数传入 `ruleVersionMismatch` 标记
- `TriageServiceImpl.java:183` 已在降级路径中读取并使用 `matchResult.isRuleVersionMismatch()`

计划要求「检查 MatchResult 类是否有回退标记字段」——此检查已在现有代码中完成。计划应基于现状定义真正的增量变更，而非将已存在的功能作为待检查项。当前描述模糊（"追加注释或标记"），注释不产生功能性变更，无法被测试验证。

期望：基于代码现状明确增量变更——确认 `ruleVersionMismatch` 标记是否需要在正常路径（非降级路径）也传播到 `TriageResponse`，或者是否需要额外标记行为（如日志输出）。移除已完成的检查描述。

---

### [一般] 3. 子项 7c (T42) — RuleChangeEvent 不存在，TODO 方案不解决缓存失效问题

经搜索代码库，`RuleChangeEvent` 在任何模块中均不存在。计划的备选方案「若事件类不存在，则添加 TODO 注释标记」无法解决实际问题——`DefaultTriageRuleEngine` 的 Caffeine 缓存在规则变更后最长 60 秒才通过 `refreshAfterWrite` 刷新，TODO 注释不触发任何失效行为。

期望：在 plan 阶段完成代码搜索并给出可行方案，例如：
- 若 OOD 定义了规则变更事件发布机制，创建事件类并在规则管理接口发布事件
- 或使用定时刷新 `expireAfterWrite` 缩短不一致窗口
- 或添加手动缓存管理端点
- 不可将「添加 TODO」作为实现方案。

---

### [一般] 4. 子项 7e (T45) — 异常处理方案二选一未做决断

计划中「抛 IllegalArgumentException 或记录错误并跳过」给出两个互斥选项但未选择。经查 `RegistrationEventListener`：
- 第44行 `handleRegistrationEvent(RegistrationEvent event)` 中 `event.getSessionId()` 可能为 null
- 第45行 null sessionId 传入 `findBySessionId(null)` 在 JPA 中可能导致 NPE
- `@Retryable` 注解配置了 `noRetryFor = {IllegalArgumentException.class, NullPointerException.class}`

两种选项的行为差异显著：抛异常会冒泡到调用方，记录并跳过则静默忽略。应基于 OOD 要求和业务语义选定唯一方案，而非留到实现时决策。

期望：选择唯一方案并在 plan 中明确。

---

### [轻微] 5. 子项 7b (T4) — 降级路径复用 TriageConverter 的兼容性未验证

计划要求将手工构造替换为 `triageConverter.toTriageResponse(...)`，但未确认 `toTriageResponse` 的方法签名能否接受降级场景下的参数（当前正常路径调用在第191行：`triageConverter.toTriageResponse(aiResult, doctors, session)`，而降级路径无 `aiResult`）。若签名不兼容，需先调整 Converter 或提供重载方法。
