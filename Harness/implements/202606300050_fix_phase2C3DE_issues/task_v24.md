# 任务指令（v24）

## 动作
NEW（含前置修复步骤）

## 任务描述
### ⚠ 前置修复（必须先完成）
修复 `TriageServiceImplTest.java` 编译错误：将 `setUp()` 方法中声明的 `DialogueSessionManager sessionManager` 局部变量提升为类字段。

**问题根因**：`TriageServiceImplTest` 的 `setUp()` 方法第 77 行声明 `DialogueSessionManager sessionManager` 为局部变量，但 `shouldFallbackOnTimeout()` 测试方法（第 225 行）在类作用域引用该变量，导致编译错误 `cannot find symbol sessionManager`。该问题自 v19（commit 97b7477）即存在，因前序轮次未运行全量 reactor 构建而被掩盖。

**修改方案**：
1. 在类字段区域（第 57-67 行之间）增加：`private DialogueSessionManager sessionManager;`
2. `setUp()` 第 77 行：`DialogueSessionManager sessionManager = ...` → `sessionManager = ...`
3. 确认 `shouldFallbackOnTimeout`（第 225 行）及其它测试方法中的 `sessionManager` 引用正确

### 主任务：TTL+事件+定时任务（P03/S02+P04/E04+M02+M03+C11）

实现 `@Scheduled` 定时清理任务、药品/模板变更事件广播和监听、TTL 配置更新。

1. **ScheduledTaskConfig**：启用 `@EnableScheduling` 的配置类（consultation 模块 `SchedulingRetryConfig` 已含 `@EnableScheduling`；Spring 允许重复声明，此处为模块独立性保留，不影响功能）
2. **SuggestionCleanupTask**：`@Scheduled(cron = "0 0/5 * * * ?")` 每 5 分钟扫描清理 `SuggestionStore` 中 COMPLETED/FAILED 且 `consumed=true` 的过期条目（条目 TTL 60 分钟）
3. **DraftContextCleanupTask**：`@Scheduled(cron = "0 0/5 * * * ?")` 清理 `DraftContextStore` 中超时草稿（TTL 60min）。条目时间戳追踪方式：使用独立 `ConcurrentHashMap<String, Instant>` 作为写入时间追踪表，`put` 时同步记录 `Instant.now()`，`remove` 时移除对应时间戳；清理任务遍历 `keySet()` 时对比时间戳判定过期
4. **DrugContraindicationChangeEvent / DrugAllergyMappingChangeEvent / DrugCompositionDictChangeEvent**：共享基类 `DrugDictChangeEvent`（含 `changeType: CREATE|UPDATE|DELETE`、`drugCode`）；应用层 `@EventListener` 监听并调用 `Caffeine Cache` 的 `invalidate()` 清除对应缓存
5. **TemplateConfigChangeEvent**：admin 模块科室模板配置管理 Service 发布事件 → medical-record 模块 `DatabaseTemplateConfigManager` 监听并调用 `Caffeine Cache` 的 `invalidateAll()`
6. **VisitIdReconciledTask**：`@Scheduled(cron = "0 */30 * * * ?")` 每 30 分钟补偿 reconciliation
7. **DialogueSessionManager**：`evictExpiredSessions()` 的 `@Scheduled(fixedRate = 60000)` 改为 `fixedRate = 300000`（每 5 分钟扫描清理周期，与 C11/OOD 要求一致；`SESSION_TTL_MINUTES = 30` 无需变更，已正确满足 OOD §6.1 会话 TTL 设计要求）

**涉及文件**（新建）：
- `common/.../config/ScheduledTaskConfig.java`
- `prescription/.../task/SuggestionCleanupTask.java`
- `consultation/.../task/DraftContextCleanupTask.java`
- `prescription/.../event/DrugDictChangeEvent.java`（基类）
- `prescription/.../event/DrugContraindicationChangeEvent.java`
- `prescription/.../event/DrugAllergyMappingChangeEvent.java`
- `prescription/.../event/DrugCompositionDictChangeEvent.java`
- `medical-record/.../event/TemplateConfigChangeEvent.java`
- `medical-record/.../task/VisitIdReconciledTask.java`

**涉及文件**（修改）：
- `consultation/.../dialogue/DialogueSessionManager.java`（`@Scheduled` fixedRate 60000→300000）
- `medical-record/.../template/DatabaseTemplateConfigManager.java`（监听 TemplateConfigChangeEvent，调用 invalidateAll）
- `consultation/src/test/java/.../TriageServiceImplTest.java`（前置修复）

## 选择理由
1. R22+R23 异步 AI 调度 + SuggestionStore 状态映射已完成（P01/A03），但缺少 TTL 清理会导致 `SuggestionStore` 无限增长
2. 药品字典变更事件（M02/M03）是 prescription 模块缓存一致性的前提
3. 模板配置事件（E04）确保 admin 模块配置变更通知到缓存
4. TriageServiceImplTest 编译错误是 R23 全量构建暴露的预存 bug，必须先修复

## 任务上下文
### 需求引用
- **P03**：SuggestionStore 条目 TTL 清理（60 分钟，每 5 分钟扫描）
- **S02**：SuggestionCleanupTask 定时任务
- **P04**：DraftContextCleanupTask 定时任务（草稿 60 分钟 TTL）
- **E04**：TemplateConfigChangeEvent 发布（admin 模块 CRUD Service）与监听（DatabaseTemplateConfigManager）
- **M02**：DrugContraindicationChangeEvent + Caffeine invalidate
- **M03**：DrugAllergyMappingChangeEvent + DrugCompositionDictChangeEvent + Caffeine invalidate
- **C11**：DialogueSessionManager.evictExpiredSessions() 清理扫描周期 1min → 5min（`@Scheduled` fixedRate 60000→300000）

### 技术约束
- 所有 ChangeEvent 使用 Spring `ApplicationEventPublisher.publishEvent()`
- `@EventListener` 需保证弱一致性（最终一致即可）
- Caffeine invalidate 后下次访问自动重新加载
- 定时任务使用 `@Scheduled`，配置类需 `@EnableScheduling`
- Drug 类 ChangeEvent 位于 `prescription/.../event/` 包，共享 `DrugDictChangeEvent` 基类以统一监听处理
- TemplateConfigChangeEvent 位于 `medical-record/.../event/` 包，admin 模块 CRUD Service 作为发布方，DatabaseTemplateConfigManager 作为监听方

## 已有代码上下文
- `SuggestionStore` 实现了 `SessionStore<String, Object>`，条目包含状态标记（PENDING/COMPLETED/FAILED）和 `consumed` 标记（R22 新增）
- `DraftContextStore` 当前无 TTL 清理机制
- `DatabaseTemplateConfigManager` 已有 Caffeine 缓存配置且提供 `refreshTemplate()` 方法，需补充 `@EventListener` 监听 `TemplateConfigChangeEvent` 并调用 `invalidateAll()`
- `DialogueSessionManager` 当前 `@Scheduled(fixedRate = 60000)` 每 1 分钟扫描（C11 要求改为每 5 分钟）；`SESSION_TTL_MINUTES = 30` 已正确
- 课程参考：`common-module-impl` 已有全局事件配置模式，`prescription` 模块有 `Caffeine Cache` 配置

## 修订说明（v24 r1）
| 审查意见 | 修改措施 |
|---------|---------|
| VisitIdReconciledTask 频率应为 30 分钟而非 60 秒 | `fixedDelay = 60000` → `cron = "0 */30 * * * ?"` |
| VisitIdReconciledTask 模块应为 medical-record 而非 consultation | 文件路径从 `consultation/.../task/` 移至 `medical-record/.../task/` |
| SuggestionCleanupTask 条目 TTL 应为 60 分钟而非 5 分钟 | "TTL 5min" → "条目 TTL 60 分钟，每 5 分钟扫描" |
| DraftContextCleanupTask 条目 TTL 应为 60 分钟而非 30 分钟 | "TTL 30min" → "TTL 60min" |
| TemplateConfigChangeEvent 发布/消费关系反转：admin 发布，DatabaseTemplateConfigManager 监听 | 修正为"admin 模块科室模板配置管理 Service 发布 → DatabaseTemplateConfigManager 监听并 invalidateAll" |
| Drug 事件类应位于 prescription/event/ 而非 common/event/ | 路径从 `common/.../event/` 移至 `prescription/.../event/` |
| TemplateConfigChangeEvent 应位于 medical-record/event/ 而非 admin/event/ | 路径从 `admin/.../event/` 移至 `medical-record/.../event/` |

## 修订说明（v24 r2）
| 审查意见 | 修改措施 |
|---------|---------|
| **[严重]** DialogueSessionManager 变更指令与代码现状不符：无 `TTL_DURATION` 字段（实际为 `SESSION_TTL_MINUTES = 30`），C11 实际意图为 `@Scheduled` 清理周期从 1 分钟改为 5 分钟 | 任务第 7 项从"TTL_DURATION 1→5"修正为"`evictExpiredSessions()` 的 `@Scheduled(fixedRate = 60000)` 改为 `fixedRate = 300000`（每 5 分钟扫描清理周期）"；`SESSION_TTL_MINUTES = 30` 无需变更 |
| **[一般]** DraftContextCleanupTask 缺少条目时间戳机制：`DraftContextStore` 值类型为 `List<DosageAlert>` 等纯业务对象，无时间戳，清理任务无法判断过期 | 任务第 3 项补充实现方式说明：使用独立 `ConcurrentHashMap<String, Instant>` 追踪写入时间，`put`/`remove` 同步维护，清理时对比时间戳 |
| **[轻微]** ScheduledTaskConfig 与现有 `SchedulingRetryConfig` 重复 `@EnableScheduling` | 任务第 1 项补充注释：Spring 允许多次声明，此处为模块独立性保留，不影响功能 |
