# 代码审查报告（v8 r1）

## 审查结果
REJECTED

## 发现

- **[严重]** `AIMedical/backend/modules/consultation/` — 缺失 `@EnableScheduling` 和 `@EnableRetry` 启用注解。`DialogueSessionManager.evictExpiredSessions()`、`DeadLetterCompensationService.compensateDeadLetters()` 的 `@Scheduled` 以及 `RegistrationEventListener.handleRegistrationEvent()` 的 `@Retryable`/`@Recover` 依赖这些注解才能生效。全项目搜索未发现任何 `@Configuration` 类提供了这些注解。核心设计功能无法运行。

- **[一般]** `AIMedical/backend/modules/consultation/service/impl/TriageServiceImpl.java:178-208` — `saveTriageRecord()` 在 AI 降级场景（`aiResult==null || !aiResult.isSuccess()`）下将规则引擎/兜底匹配的科室通过 `objectMapper.writeValueAsString(departments)` 写入 `aiRecommendedDepartments` 字段。降级场景下科室来源非 AI，应写入 `ruleMatchedDepartments` 字段（该字段在 `TriageRecord` 中已定义但从未被赋值）。违反设计 §TriageRecord 中 `aiRecommendedDepartments` vs `ruleMatchedDepartments` 的语义分离。

- **[一般]** `AIMedical/backend/modules/consultation/rule/DefaultTriageRuleEngine.java:23-25` — 使用 `Caffeine.newBuilder().refreshAfterWrite(60, TimeUnit.SECONDS).build(Function)`，返回 `Cache` 而非 `LoadingCache`。`refreshAfterWrite` 仅对 `LoadingCache`（`build(CacheLoader)`）生效，此处实际无效。不满足设计 §DefaultTriageRuleEngine 行为契约的"60s 刷新"要求。

- **[一般]** `AIMedical/backend/modules/consultation/service/impl/TriageServiceImpl.java`、`service/DeadLetterCompensationService.java`、`event/RegistrationEventListener.java` — 三个类的构造器均补充了设计文档未列的 `ObjectMapper` 依赖。虽然功能上合理，但属于设计偏差，表明设计文档与实现之间存在不一致。

- **[轻微]** `AIMedical/backend/modules/consultation/entity/TriageRecord.java:31` — `ruleMatchedDepartments` 字段被定义但全局未使用（仅在 `saveTriageRecord` 中写入 `aiRecommendedDepartments`，从不写入 `ruleMatchedDepartments`）。

- **[轻微]** `AIMedical/backend/modules/consultation/dialogue/DialogueSessionManager.java:41-46` — `evictExpiredSessions()` 在迭代 `sessionStore.keySet()` 的过程中执行 `sessionStore.remove(key)`，若 `SessionStore` 实现非 `ConcurrentHashMap` 会抛出 `ConcurrentModificationException`。

- **[轻微]** `AIMedical/backend/modules/consultation/rule/DefaultTriageRuleEngine.java:56-62` — `currentRuleVersion()` 和 `currentRuleSetId()` 返回硬编码字符串 `"latest"` 和 `"default"`，无实际计算逻辑。

## 修改要求（仅 REJECTED 时）

1. **`AIMedical/backend/modules/consultation/config/SchedulingRetryConfig.java`** (新建) — 创建 `@Configuration @EnableScheduling @EnableRetry` 配置类，或在已有配置类上补充这两个注解。`AspectJ` 代理自动配置依赖 `spring-boot-starter-aop`，需确认父 POM 已包含。**严重**

2. **`TriageServiceImpl.java:191-206`** — 在 `saveTriageRecord()` 的降级分支（`aiResult==null || !aiResult.isSuccess()`）中将规则/兜底科室写入 `record.setRuleMatchedDepartments(...)` 而非 `setAiRecommendedDepartments(...)`；AI 成功分支继续写入 `setAiRecommendedDepartments(...)`。**一般**

3. **`DefaultTriageRuleEngine.java:23-25`** — 将 `.build(key -> triageRuleRepository.findByEnabledTrue())` 改为 `.build(new CacheLoader<String, List<TriageRule>>() { ... })` 以确保 `refreshAfterWrite` 生效；或在构造器中使用 `LoadingCache` 类型。**一般**

4. **`TriageServiceImpl.java`、`DeadLetterCompensationService.java`、`RegistrationEventListener.java`** — 将 `ObjectMapper` 作为设计文档的一部分补充到设计规格中，或考虑使用 Spring Boot 自动注入的 `ObjectMapper` bean（目前已隐式注入但设计未记录）。**一般**

5. **`DialogueSessionManager.java:41-46`** — 使用显式的迭代器（如 `sessionStore.keySet().toArray()` 或 `new ArrayList<>(sessionStore.keySet())`）避免并发修改异常。**轻微**

6. **`DefaultTriageRuleEngine.java:56-62`** — 实现实际规则版本/规则集查询逻辑或文档说明当前为占位实现。**轻微**
