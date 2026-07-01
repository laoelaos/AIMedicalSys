# 设计审查报告（v24 r1）

## 审查结果
APPROVED

## 发现

### 已核实事项

- **SuggestionStore** ✓ — 位于 `common-module-api/.../store/SuggestionStore.java`，继承 `SessionStore<String, Object>`
- **DraftContextStore** ✓ — 位于 `common-module-api/.../store/DraftContextStore.java`，继承 `SessionStore<String, Object>`
- **ConcurrentHashMapStore** ✓ — 同时实现 SuggestionStore 和 DraftContextStore，值存储为 `Object`，无内置 TTL/时间戳
- **DrugContraindicationMappingRepository** ✓ — 存在，提供 `findByDrugCode(String)`
- **DrugAllergyMappingRepository** ✓ — 存在，提供 `findByDrugCode(String)`
- **DrugCompositionDictRepository** ✓ — 存在，提供 `findByDrugCode(String)`
- **DatabaseTemplateConfigManager** ✓ — 存在，含 `templateCache`（`LoadingCache`），已有 `refreshTemplate(departmentId)` 调 `invalidate(departmentId)`
- **DialogueSessionManager** ✓ — `@Scheduled(fixedRate = 60000)` 当前值确认为 1 分钟，`SESSION_TTL_MINUTES = 30`
- **SchedulingRetryConfig** ✓ — consultation 模块已有 `@EnableScheduling` + `@ConditionalOnProperty`
- **TriageServiceImplTest** ✓ — `shouldFallbackOnTimeout`（第 221-231 行）引用了 `setUp()` 中的局部变量 `sessionManager`（第 77 行），确认编译错误存在，修复方案正确

### 设计完整性

设计覆盖了 task_v24.md 中所有 7 项主任务和前置修复任务，类型定义清晰、行为契约完整、错误处理覆盖全面，无遗漏。

### 未发现严重或一般问题

- 文件路径使用了项目约定格式（与 task 一致），无实质偏差
- DraftContextCleanupTask 模块定位（consultation）与 task 一致，独立 ConcurrentHashMap 时间戳追踪方案合理解决了 DraftContextStore 值类型无时间戳的问题
- DrugDictCacheManager 的 LoadingCache + invalidate 方案与现有 Caffeine 使用模式一致
- 所有事件定义继承结构和模块归属正确

## 结论

设计完整且与任务需求一致，无 **[严重]** 或 **[一般]** 级别缺陷，批准进入实施阶段。
