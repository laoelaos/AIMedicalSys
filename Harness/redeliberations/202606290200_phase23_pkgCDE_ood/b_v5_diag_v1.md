# 质量审查报告：Phase 2/3 包C/D-AI1/D-AI2/E OOD 设计方案

## 审查结论

产出整体质量较高，经过多轮迭代修订后，需求覆盖度和设计深度已显著提升。但仍存在以下问题需要在进入编码阶段前修复，主要集中在：架构强制约束与详细设计的不一致、异步 AI 结果映射的逻辑缺口、API 契约的形式化程度不足、以及需求参考文档的追迹缺失。

---

## 问题诊断

### 问题 1（严重）：§3.1 DialogueSessionManager 详细设计与 §1.1/§6.1 Store 抽象层强制约束矛盾

- **问题描述**：§1.1 和 §6.1 明确将 Store 抽象层定义为"设计强制项"——"三项存储必须通过 Store 接口（SessionStore、SuggestionStore、DraftContextStore）间接访问"，并说明 DialogueSessionManager 应使用 ConcurrentHashMapStore 实现。然而 §3.1 DialogueSessionManager 的详细设计仍表述为"内部维护 ConcurrentHashMap"，且以"interface 抽象收益在当前阶段不抵实现复杂度"为由论证不使用接口。这里存在两层次矛盾：(a) Store 抽象已从"建议"升级为"强制"后，该论证已失效但未删除；(b) 详细设计中 DialogueSessionManager 仍直接操作 ConcurrentHashMap 而非 SessionStore 接口，实现者按此设计编码将违背架构约束，导致 Phase 5 迁移时必须重构 DialogueSessionManager 而不仅仅是替换 Store 实现。

- **所在位置**：§3.1 DialogueSessionManager 描述（"内部维护 ConcurrentHashMap + ScheduledExecutorService"段 + "为何使用 class 而非 interface"段）；§1.1 设计目标 Store 抽象层段；§6.1 部署约束段

- **严重程度**：严重

- **改进建议**：将 §3.1 DialogueSessionManager 的内部存储引用从"ConcurrentHashMap"替换为"SessionStore 接口（注入 ConcurrentHashMapStore 实现）"；删除或改写"为何使用 class 而非 interface"段中否定 interface 抽象价值的论证，改为说明 DialogueSessionManager 作为管理器 class 的职责边界，同时声明内部通过 SessionStore 接口访问存储。同步在 §2.1 目录结构中补充 SessionStore / ConcurrentHashMapStore 的包路径，并确认 SuggestionStore 和 DraftContextStore 同样有对应的具体实现类位置。

---

### 问题 2（严重）：异步 AI 建议流程缺少 AiResult → AiSuggestionResult 的映射逻辑定义

- **问题描述**：§6.3 定义了 check-dose 的异步 AI 调度（`@Async / CompletableFuture.runAsync()` 调用 `AiService.prescriptionAssist()`），§3.4 和 §4.4 定义了 AiSuggestionResult（含 PENDING/COMPLETED/FAILED 状态）供前端查询。但 AiService.prescriptionAssist() 返回的是 `CompletableFuture<AiResult<...>>`，而 AiSuggestionResult 是独立的值对象——两者之间的映射逻辑未定义。具体缺失包括：(a) AiResult.success=true → AiSuggestionResult.status=COMPLETED 时，AiResult.data 如何映射到 AiSuggestionResult.suggestion（String 类型）？(b) AiResult.success=false → AiSuggestionResult.status=FAILED 时，AiResult.errorCode/fallbackReason 如何映射到 AiSuggestionResult.failReason？(c) AiResult.degraded=true → 此路径在异步场景下归入 COMPLETED 还是 FAILED？(d) AiSuggestionResult.partialData（JSON TEXT）何时写入、写入的具体内容格式未定义。此映射缺口直接阻碍编码实现——开发者无法确定异步 AI 结果如何转换为前端可查询的格式。

- **所在位置**：§2.3 AiService.prescriptionAssist() 定义（CompletableFuture 返回签名）；§3.4 AiSuggestionResult 条目；§6.3 异步 AI 建议与去重段；§4.4 异步 AI 调用完成后更新状态段

- **严重程度**：严重

- **改进建议**：在 §4.4 check-dose 流程中或 §3.4 AiSuggestionResult 附近新增明确的 AiResult → AiSuggestionResult 映射规则表，至少覆盖：COMPLETED 路径（AiResult.success=true → status=COMPLETED, suggestion=序列化 AiResult.data）、FAILED 路径（AiResult.success=false → status=FAILED, failReason=AiResult.fallbackReason ?? errorCode）、DEGRADED 路径（AiResult.success=false + degraded=true → status=COMPLETED with partial data 或 FAILED with fallbackReason，需决策）。同步补充 partialData 的写入时机和 JSON 格式定义。

---

### 问题 3（严重）：产出未参考需求中列明的 Phase 23 已有 OOD 草案，设计连续性无法验证

- **问题描述**：需求文档 §参考文档 明确列出了"本阶段已有 OOD 草案：`Harness\redeliberations\202606281422_phase23_ood\a_v19_copy_from_v18.md`"作为输入参考。然而当前产出全文中未引用或提及该草案，无法判断：(a) 当前设计是否继承了已有草案中的合理决策；(b) 是否有意偏离已有草案及理由；(c) 设计范围是否有重叠或遗漏。需求文档列出此参考的目的显然是为了保证设计连续性，而非忽略已有工作重新设计。此遗漏使审查者无法验证"本阶段设计是否在已有基础上合理演进"这一基本质量属性。

- **所在位置**：全文均未引用，尤其应在 §1 概述或 §7 设计决策中有追迹说明

- **严重程度**：严重

- **改进建议**：在 §1 概述中增加对 Phase 23 已有 OOD 草案的追迹说明——例如：在 §1.1 后追加一段"与 Phase 23 OOD 草案的关系"，说明哪些部分继承自草案、哪些部分新增/修改及理由。或在 §7 设计决策表中为关键决策标注与草案的一致/差异关系。

---

### 问题 4（一般）：§1.3 DosageCheckRequest.prescriptionId 的"必填"约束与 §3.4 后端自动生成 fallback 路径矛盾

- **问题描述**：§1.3 DosageCheckRequest 条目将 prescriptionId 描述为"必填，String"，但 §3.4 "prescriptionId 分配时机"段明确定义了后端按需生成 fallback 路径——"若极端场景下前端未预创建 prescriptionId（如离线客户端异常），后端在首次 check-dose 调用检测到 prescriptionId 为空时自动生成 UUID v4"。如果 prescriptionId 是 API 契约层面的必填字段，API 验证层（如 @NotNull / @NotBlank）将在请求到达 Service 层前拒绝空值，后端自动生成逻辑永远不会触发。"必填"语义与 fallback 逻辑互斥。

- **所在位置**：§1.3 DosageCheckRequest 条目（"处方标识（prescriptionId，必填，String）"段）；§3.4 "prescriptionId 分配时机"段；§4.4 check-dose 请求参数描述

- **严重程度**：一般

- **改进建议**：将 §1.3 和 §4.4 中 DosageCheckRequest.prescriptionId 的约束改为"主路径必填（前端预创建场景），空值时由后端自动生成并回写"，或拆分为两个子场景明确定义：①前端预创建场景（prescriptionId 必填）和②后端按需生成场景（prescriptionId 可选，为空时后端生成）。同时在 §3.4 中明确 API 验证层应跳过 prescriptionId 的 @NotNull 校验以允许 fallback 路径工作。

---

### 问题 5（一般）：所有 API 端点缺少结构化契约定义，前端/QA 无法直接消费

- **问题描述**：当前产出的所有端点契约均以自然语言描述字段列表的方式呈现（§1.3 各 DTO 字段描述 + §4 各场景的行为描述），缺少结构化的请求/响应 JSON Schema 定义或 OpenAPI/Swagger 规格。具体缺失包括：(a) 请求 JSON 顶层结构和嵌套层级未以 JSON 示例或 Schema 明确；(b) HTTP 响应错误体格式未定义——GlobalExceptionHandler 的输出格式（如 `{"code": "...", "message": "...", "details": {...}}`）未在产出中说明；(c) 各端点的 HTTP 响应状态码集合和条件未完整枚举（如除 200/400/422 外是否有 401/403/500 等）；(d) 所有端点是否需要认证头、Content-Type 等未说明。这导致前端开发者和测试工程师无法仅凭此设计文档独立工作，必须通过阅读代码或额外沟通才能获取完整的 API 契约。

- **所在位置**：§1.3 核心抽象一览（各 DTO 条目）；§4.1~§4.4 关键行为契约；全文无 OpenAPI/Swagger 章节

- **严重程度**：一般

- **改进建议**：在 §4 后或新增 §11 集中给出各端点的完整请求/响应 JSON 示例（至少每个主端点一条示例），并明确定义错误响应包装格式（errorCode + message + details 的结构）。如果项目使用 Swagger/OpenAPI 注解驱动，至少应在设计中约定注解位置和格式规范。如果团队认为 DTO 字段定义 + 行为描述已足够，需明确说明前端/QA 如何获取完整 API 契约（如"从 Swagger 注解生成"），以确保下游可行动。

---

### 问题 6（一般）：AiSuggestionResult.TTL 与 PrescriptionDraftContext.TTL 一致性问题已在 v17 修复，但 DedupTaskScheduler 的去重判定仍可能因 TTL 差异导致逻辑残差

- **问题描述**：v17 修订将 AiSuggestionResult TTL 从 30 分钟调整为 60 分钟与 PrescriptionDraftContext 一致，消除了"AiSuggestionResult 先过期但 CRITICAL 标记残留"的状态残差。然而去重逻辑（§3.4、§6.3）依赖前端 consumed 标记和 TTL 过期清理恢复初始状态，存在以下时序残差：当 AiSuggestionResult 在 TTL 过期（60 分钟）后被清理，但前端尚未消费结果（consumed=false），此时新的 check-dose 调用因无法找到已有 AiSuggestionResult 而创建新 task——但前端仍可能持有旧 taskId 并发起查询，导致 RX_ASSIST_SUGGESTION_NOT_FOUND。设计中未定义 TTL 过期后的前端重试/刷新策略。这不是 TTL 值本身的问题，而是"TTL 清理"与"consumed 标记"两条状态回收路径的时序协调缺口。

- **所在位置**：§3.4 AiSuggestionResult "TTL（60 分钟）"描述；§3.4 异步 AI 调用去重策略段；§6.3 prescriptionId 级去重段

- **严重程度**：一般

- **改进建议**：定义 TTL 过期与 consumed 标记的协调策略——例如：(a) 前端查询端点 GET /api/prescription/assist/suggestion/{taskId} 在遇到 TTL 过期（AiSuggestionResult 已清理）时，返回特定错误码（如 RX_ASSIST_SUGGESTION_EXPIRED）而非 RX_ASSIST_SUGGESTION_NOT_FOUND，前端据此触发新的 check-dose 重新获取 taskId；(b) 或延长 AiSuggestionResult TTL 至足够覆盖前端消费窗口（如 120 分钟），以 consumed 标记作为唯一的释放条件，TTL 仅作为异常兜底。

---

### 问题 7（一般）：§5.1 错误码表缺少若干边界场景的错误码

- **问题描述**：§5.1 错误码表在历次修订中已逐步补充，但仍有以下边界场景缺少对应错误码：(a) 分诊场景中 DoctorFacade 调用超时——当前仅记录 WARN 日志并将 doctors 置为空列表（§3.1/§4.1），但响应中无错误码告知前端"医生列表不可用"的原因；(b) 病历生成场景中 TemplateConfigManager 加载科室模板失败——仅兜底返回 DEFAULT 模板（§3.3），但无错误码标识模板加载异常；(c) 辅助开方场景中 AI 异步建议创建失败（如去重失败、AiSuggestionResult 存储异常）——无对应错误码；(d) 处方提交端点 forceSubmit=true 时处方版本校验一致但 forceSubmit 已被其他线程处理——可能返回什么错误码未定义。缺少这些错误码导致前端无法针对性地展示错误信息和提示，降低了设计的前端可消费性。

- **所在位置**：§5.1 模块级错误码表

- **严重程度**：一般

- **改进建议**：补充以下错误码至 §5.1 并关联各章节：(a) 分诊（非AI）新增 TRIAGE_DOCTOR_FACADE_UNAVAILABLE；(b) 病历（非AI）新增 MR_GEN_TEMPLATE_LOAD_FAILED；(c) 开方辅助（AI）新增 RX_ASSIST_AI_SUGGEST_CREATE_FAILED；(d) 审核（非AI）新增 RX_AUDIT_CONCURRENT_SUBMIT（并发提交冲突）。或在不新增的情况下，在相关章节显式声明复用哪个现有错误码。

---

### 问题 8（轻微）：DeadLetterEventRepository 在 §2.1 目录结构中遗漏

- **问题描述**：§2.2 明确定义了 DeadLetterEvent 实体和 DeadLetterEventRepository 的存在（"DeadLetterEvent 实体定义在 consultation 模块...对应 Repository 为 DeadLetterEventRepository"），同时 DeadLetterCompensationService 也依赖此 Repository。但 §2.1 consultation 模块的目录树（`repository/TriageRecordRepository.java, TriageRuleRepository.java`）中未列出 DeadLetterEventRepository.java，存在不一致。

- **所在位置**：§2.1 consultation 模块目录结构（repository/ 行）；§2.2 DeadLetterEventRepository 引用段

- **严重程度**：轻微

- **改进建议**：在 §2.1 consultation/repository/ 目录行中补充 DeadLetterEventRepository.java 条目。

---

## 问题汇总

| 编号 | 严重程度 | 类别 | 简要描述 |
|------|---------|------|---------|
| 1 | 严重 | 逻辑矛盾 | §3.1 DialogueSessionManager 直接使用 ConcurrentHashMap，与 §1.1/§6.1 Store 抽象层强制约束矛盾 |
| 2 | 严重 | 关键遗漏 | 异步 AI 建议流程缺少 AiResult → AiSuggestionResult 映射逻辑，编码无法闭环 |
| 3 | 严重 | 需求响应 | 未引用需求列明的 Phase 23 OOD 草案，设计连续性不可验证 |
| 4 | 一般 | 逻辑矛盾 | DosageCheckRequest.prescriptionId "必填"与后端自动生成 fallback 路径互斥 |
| 5 | 一般 | 深度不足 | 所有 API 端点缺少结构化契约（JSON Schema/OpenAPI），前端/QA 无法直接消费 |
| 6 | 一般 | 关键遗漏 | TTL 清理与 consumed 标记的时序协调策略未定义，功能残差 |
| 7 | 一般 | 深度不足 | 错误码表缺少多个边界场景错误码，降低前端可消费性 |
| 8 | 轻微 | 事实错误 | §2.1 目录结构遗漏 DeadLetterEventRepository.java 条目 |
