# 质量审查报告（第 9 轮）

## 审查概要

- **待审查产出**：`a_v9_copy_from_v8.md`（含 v8–v27 共 19 轮修订注释）
- **审查维度**：需求响应充分度、事实错误/逻辑矛盾、深度完整性、落地可行性
- **内部审议已覆盖维度**：技术可行性、错误码完备性、并发安全、边界条件、TTL 清理、跨模块门面降级等（迭代第 6–8 轮）

---

## 一、需求响应充分度评估

### 1.1 包C 智能分诊（3.4.1）

| 需求项 | 响应状态 | 说明 |
|--------|---------|------|
| 单轮/多轮双对话 | ✅ 充分覆盖 | §3.1 TriageService 完整定义单轮/多轮流程；§4.1 提供 JSON 示例；全量拼接策略 + 上下文截断 | 
| 规则可配置 | ✅ 充分覆盖 | TriageRuleEngine + TriageRule JPA @Entity + admin 模块规则 CRUD（§9.3） |
| Mock 兜底回退科室列表 | ✅ 充分覆盖 | DepartmentFallbackProvider + StaticDepartmentFallbackProvider（§3.1）|

### 1.2 包D-AI1 处方审核（3.4.2）

| 需求项 | 响应状态 | 说明 |
|--------|---------|------|
| 风险等级差异化阻断 | ✅ 充分覆盖 | AuditRiskLevel (PASS/WARN/BLOCK) + PrescriptionAuditEnforcer + HTTP 422（§3.2, §4.2）|
| AI 超时回退本地规则校验打标 | ✅ 充分覆盖 | LocalRuleEngine 5 条独立规则 + fromFallback 标记（§3.2）|

### 1.3 包D-AI2 病历生成（3.4.3）

| 需求项 | 响应状态 | 说明 |
|--------|---------|------|
| 对话转结构化病历 | ✅ 充分覆盖 | MedicalRecordService + AiService.generateMedicalRecord() + 7 个 MedicalRecordField（§3.3）|
| 按科室配置规则 | ✅ 充分覆盖 | TemplateConfigManager + DepartmentTemplateConfig + DEFAULT 兜底（§3.3, §9.1）|
| 关键字段缺失提示补全 | ✅ 充分覆盖 | MissingFieldDetector（差集比对）+ FieldMissingHint（模板驱动）（§3.3）|

### 1.4 包E 辅助开方（3.4.10）

| 需求项 | 响应状态 | 说明 |
|--------|---------|------|
| 剂量阈值告警 | ✅ 充分覆盖 | DosageThresholdService + 6 级匹配优先级 + 3 种 warningType（§3.4, §8.4）|
| 与处方审核强耦合同步落地 | ✅ 充分覆盖 | 同属 prescription 模块；DosageStandard 迁至 common 模块共享（§1.2, §2.2）|

### 1.5 约束

| 约束 | 响应状态 | 说明 |
|------|---------|------|
| 所有包直接落地在底座 | ✅ 充分覆盖 | §1.1 完整论证 + §6.1 Store 抽象层强制隔离 + §7 设计决策 |
| 避免 Phase 5 迁移成本 | ✅ 充分覆盖 | AiService 接口编译期依赖 + Store 接口隔离 ConcurrentHashMap→Redis 透明替换 |

**需求响应评估结论**：所有需求项均已充分覆盖，字段级对齐可追溯。未发现需求遗漏。

---

## 二、事实错误与逻辑矛盾

### [问题 1] 一般 — consumed 标记设置职责在 §3.4 和 §4.4 之间矛盾

- **位置**：§3.4 AiSuggestionResult 状态图（line 665–666）vs §4.4 异步建议查询端点（line 940–943）
- **描述**：§3.4 陈述"查询端点返回结果时将 consumed 置为 true"（后端自动设置），而 §4.4 陈述"前端标记 consumed=true 通知后端"（前端显式通知）。两者语义矛盾，将导致实现分歧。
- **影响**：开发者无法确定 consumed 标记由谁触发。若后端自动设置，前端崩溃场景会导致 consumed 状态错误提前；若前端通知，需额外 API 调用且存在前端忘记通知的风险。
- **改进建议**：统一为后端在返回 COMPLETED 状态时自动设 consumed=true（PENDING/FAILED/NOT_FOUND 路径不设），并在 §3.4 中补充文档说明"前端崩溃后重新发起 check-dose 将创建新 task"作为可接受的边界行为。

### [问题 2] 一般 — DosageStandard 缺少变更事件通知，与同级配置实体的事件驱动刷新机制不一致

- **位置**：§9.3（line 1526）；§8 DosageStandard 定义
- **描述**：DrugAllergyMapping、DrugContraindicationMapping、DrugCompositionDict、TriageRule、DepartmentTemplateConfig 等全部配置实体均定义了变更事件 + Caffeine 定时刷新的双重缓存失效策略（§9.3）。DosageStandard 与这些实体同属 admin 模块管理，但未定义任何 DosageStandardChangeEvent。当前设计未明确 DosageStandard 是否被 prescription 模块缓存，但未来引入缓存后（高频 check-dose 场景下很可能发生），admin 模块的变更将无法及时传播。
- **影响**：未来引入缓存时存在设计缺口，admin 更新剂量标准后 prescription 模块可能使用过期数据，导致剂量告警不准确。
- **改进建议**：在 §9.3 事件声明表中补充 `DosageStandardChangeEvent`（可标注 Phase 2/3 预留），或在 §8 中显式声明"DosageStandard 当前无缓存，变更事件推迟至引入缓存时实现"，消除未来实现者的猜测。

---

## 三、深度与完整性评估

### 3.1 整体评价

产出总篇幅 1844 行，覆盖架构、模块、核心抽象、行为契约、错误处理、并发设计、设计决策、配置定义、API 扩展规格九个维度，经历 19 轮定向修订，深度和完整性在同类 OOD 文档中属优秀水平。

### 3.2 完整性缺口

#### [问题 3] 轻微 — DoctorFacade 返回值 AvailableDoctor 缺少正式 DTO 定义

- **位置**：§1.3 DoctorFacade 条目（line 73）；§3.1 DoctorFacade（line 463–470）
- **描述**：AvailableDoctor 作为 DoctorFacade.findAvailableDoctorsByDepartment() 的返回值类型被引用（含 doctorId / doctorName / departmentId / availableSlotCount 四个字段），但未在 §1.3 核心抽象表或 §2.1 目录结构中作为正式 DTO 列出。开发者需要自行推断其结构。
- **影响**：编码阶段可能因 AvailableDoctor 字段定义不一致导致类型不匹配问题。
- **改进建议**：在 §1.3 包C 核心抽象表或跨模块门面部分新增 AvailableDoctor DTO 条目，与 RecommendedDoctor、MatchedRule 等并列定义。

#### [问题 4] 轻微 — VisitFacade fallback 路径中"visitId 格式约束"未定义

- **位置**：§3.3 RecordGenerateRequest（line 627）
- **描述**：fallback 路径 (a) 定义为"将 encounterId 直接作为 visitId 的 fallback 写入（当 encounterId 非空且满足 visitId 格式约束时）"。但"visitId 格式约束"的具体规则（正则表达式、字符长度、允许字符集等）未定义，开发者无法判断该 fallback 分支是否可执行。
- **影响**：编码时 fallback (a) 可能因判断条件模糊而被跳过，实际退化为 fallback (b)，导致 VisitFacade 降级场景下部分病历内容也可能无法返回。
- **改进建议**：定义 visitId 格式约束规则（如"符合 UUID v4 格式"或"长度 8–36 的字母数字字符串"），或显式移除该条件约束，直接使用 encounterId 作为 visitId fallback。

---

## 四、落地可行性评估

### 4.1 编码指导能力

产出提供了：
- 完整的方法签名（经 v27 修订后四个业务 Service 接口均补充了显式签名）
- 详细的实体/JPA 字段定义（含 @Id、@Column、@Version、@Convert 等注解规格）
- 结构化的 API 契约示例（§4.6 各端点 JSON 示例）
- 六套错误码表 + 命名规则

**结论**：可直接指导编码实现，开发者拿到文档后可以独立完成模块开发。

### 4.2 下游消费者支持

产出定义的 API 端点均提供请求/响应 JSON 示例（§4.6）、HTTP 状态码约定、统一错误包装格式、BLOCK 阻断的独立 BlockResponse 格式。前端开发者可基于此文档编写集成代码。

**结论**：接口定义足以支持前端、QA 等下游消费者。

### 4.3 异常场景与边界条件

产出覆盖了以下异常场景：
- AI 超时降级（4 种 AI 能力分别配置超时阈值）
- AI 不可用时本地规则链兜底
- 跨模块门面（DoctorFacade/DrugFacade/VisitFacade）独立超时降级
- TTL 清理竞态（ConcurrentHashMap.remove 原子性）
- 并发写冲突（@Version 乐观锁）
- TOCTOU 去重竞态（SuggestionStore.createIfNotExists 原子操作）
- 阻断竞态防护（SubmitContext 快照 + 二次验证）
- 事件消费失败补偿（@Retryable + DeadLetterEvent 死信队列）
- 规则快照失效降级
- 上下文 Token 超限截断

**结论**：异常场景覆盖充分。

---

## 五、总体评价

该设计文档质量较高，需求响应完整、逻辑一致、深度充分，可直接指导编码实现。内部审议（第 6–8 轮）已覆盖技术可行性、并发安全、错误码完备性等关键维度。本次审查发现的 4 个问题均为一般/轻微级别，不影响设计总体可用性。

建议修复优先级：
1. **[问题 1]** consumed 职责矛盾 — 编码前必须统一，否则实现的去重逻辑可能行为异常
2. **[问题 2]** DosageStandard 事件缺失 — 建议在 Phase 2/3 范围内解决，避免未来引入缓存的回退成本
3. **[问题 3–4]** 轻微完整性缺口 — 可在编码阶段同步补全
