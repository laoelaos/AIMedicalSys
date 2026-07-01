# 质量审查报告 — 第 2 轮

**审查对象**：`a_v2_copy_from_v1.md`（Phase 2/3 包C/D-AI1/D-AI2/E 架构级 OOD）
**审查视角**：需求响应充分度、深度与完整性、事实与逻辑正确性
**审查原则**：侧重内部审议（10 项问题，见迭代历史）未覆盖的维度，避免重复验证

---

## 发现的问题

### 问题 1：[严重] TriageRecord、AuditRecord、DeadLetterEvent 实体未定义 JPA @Id 主键字段

**所在位置**：§3.1 TriageRecord（line 413-416）、§3.2 AuditRecord（line 436-448）、§2.2 DeadLetterEvent（line 278）

**问题描述**：
三个 JPA @Entity 定义了丰富的业务字段，但均未声明 `@Id` 主键字段：
- TriageRecord：sessionId 可为候选但非主键（同一会话可多次分诊），需 `@GeneratedValue` 的 surrogate key
- AuditRecord：prescriptionId 非唯一（多次审核），需 `auditId` 作为 PK；prescriptionId + auditSequence 复合键也可行但至少需显式定义
- DeadLetterEvent：无 ID 字段声明，定时补偿任务无法定位具体记录

缺少 PK 使实体定义不完整，无法直接编码。设计与已有模块不一致——已有模块（如 UserFacade 依赖的 entity）均有显式主键定义。

**改进建议**：
- TriageRecord 补充 `recordId`（Long，@GeneratedValue）或显式声明 sessionId 为主键并补充唯一约束
- AuditRecord 补充 `auditId`（Long，@GeneratedValue）；若使用 prescriptionId + auditSequence 复合键需用 @IdClass 或 @EmbeddedId 显式定义
- DeadLetterEvent 补充 `id`（Long，@GeneratedValue），使补偿任务可按 id 检索更新

---

### 问题 2：[严重] 手动选科与 RegistrationEvent 自动写入的覆盖优先级缺少强制执行机制

**所在位置**：§2.2（line 274-275）、§3.1 TriageService.selectDepartment（line 349）

**问题描述**：
设计声明"当两路径均命中时以手动选科值为准（覆盖语义）"，但强制执行机制缺失：
- §3.1 明确 selectDepartment() 方法由**手动选科端点和 RegistrationEventListener 共同调用**
- selectDepartment 方法始终执行"更新 finalDepartmentId 和 finalDepartmentName"（覆盖写入）
- RegistrationEvent 可能迟到（死信补偿 30 分钟兜底）：若患者先手动选科，30 分钟后 RegistrationEvent 死信补偿到达并调用 selectDepartment，手动选科值会被 RegistrationEvent 的 departmentId 覆盖

即 design 声明手动选科优先，但 selectDepartment 被两路径共用且无调用方感知，迟到的 RegistrationEvent 会无声覆盖手动选择，导致患者最终科室不正确。

**改进建议**：
方案 A（推荐）：RegistrationEventListener 在调用 selectDepartment 前检查 finalDepartmentId，仅在为空时写入；手动选科端点调用时始终覆盖。
方案 B：selectDepartment 增加 callerContext 参数（如 enum SELECTOR {MANUAL, EVENT}），MANUAL 始终覆盖，EVENT 仅在 finalDepartmentId 为空时覆盖。
需在 §2.2 事件传递机制和 §3.1 selectDepartment 中同步补充。

---

### 问题 3：[严重] 全量拼接上下文截断缺少 AI 感知截断标记

**所在位置**：§3.1 TriageService（line 339）"全量拼接 Token 超限风险评估"

**问题描述**：
设计定义上下文截断策略：累计字符超阈值（默认 3000）时"保留首轮主诉 + 最近 N 轮 QA（窗口策略），丢弃中间轮次"。但截断后传递给 AiService.triage() 的上下文中**没有任何标记告知 AI 中间轮次已被丢弃**。

AI 模型接收到的对话历史是断裂的：主诉 → 最近 N 轮 QA，缺少中间轮次。AI 无法感知被丢弃的患者回复内容，可能产生错误推理（例如中间轮次中患者已修正过主诉，但截断后 AI 基于旧主诉推理）。

**改进建议**：在拼接上下文中显式插入截断标记，例如在首轮主诉和最近 N 轮之间追加 `[NOTE: 部分对话内容因长度限制已省略]`，使 AI 感知到信息缺口。截断标记内容为 Prompt 工程细节，但 OOD 层面至少应定义此策略，否则进入编码阶段易被忽略。

---

### 问题 4：[严重] DrugFacade 接口有引用无定义

**所在位置**：§8.2（line 1063）

**问题描述**：
§8.2 跨模块药品信息查询描述中显式声明"跨模块的药品名称/规格信息查询通过 Service 层按需调用 DrugFacade（定义在 common-module-api 中）获取"，但：
- DrugFacade 未出现在 §1.3 核心抽象一览中
- DrugFacade 未出现在 §2.1 目录结构中
- DrugFacade 未出现在 §2.3 依赖关系或跨模块协作图中
- DrugFacade 的接口方法签名完全未定义

这与 DoctorFacade、VisitFacade、UserFacade 的完整定义形成对比——后三者均有接口方法签名、包路径和实现模式说明。

**改进建议**：在 §1.3 核心抽象表中新增 DrugFacade 条目（interface，定义在 common-module-api/drug/ 包下，提供 findByDrugCode(drugCode) 等方法）；在 §2.1 目录结构中补充 DrugFacade 的包路径；在 §2.3 跨模块数据获取机制中说明与 DoctorFacade 一致的模式。

---

### 问题 5：[严重] Registration 模块和就诊模块作为外部依赖未在设计范围中说明

**所在位置**：§2.2 RegistrationEvent（line 274）、§3.3 RecordGenerateRequest（line 544-546）

**问题描述**：
设计依赖两个当前代码库中不存在的模块：
1. **registration 模块**：需发布 RegistrationEvent 并填充 sessionId 字段，但当前模块列表中无此模块（现有模块：admin、ai、common-module、diagnosis、doctor、patient）
2. **就诊/visit 模块**：需实现 VisitFacade.findVisitIdByEncounterId() 提供 encounterId→visitId 转换，但当前无此模块

设计将其视为已拥有的基础设施，要求这些模块在 Phase 2/3 交付时同步就绪，但 OOD 中未将此项列为设计前提/风险/依赖。若这些模块由其他团队开发且未同步排期，Phase 2/3 的 consultation 和 medical-record 模块无法独立交付。

**改进建议**：
- 在 §1.1 设计目标或末尾新增"外部依赖与前提条件"章节，显式列出：
  - registration 模块（需提供 RegistrationEvent 发布能力 + sessionId 填充逻辑）
  - visit 模块（需提供 VisitFacade 实现）
- 标注这些依赖的实施时间线要求，纳入设计风险

---

### 问题 6：[一般] DeadLetterEvent 实体缺少精确的 JPA 字段定义

**所在位置**：§2.2（line 276-278）

**问题描述**：
设计提及 DeadLetterEvent 实体包含 eventPayload、failReason、failTime、state、retryCount、maxRetryCount 等字段，但：
- 未指定 state 字段的类型（String vs enum）和值域（FAILED / COMPENSATED / 其他？）
- 未指定 retryCount 的默认值和 maxRetryCount 的默认值（低层提及 maxRetryCount 默认 3）
- 未指定 eventPayload 的存储形式（TEXT / LONGTEXT / JSON？）
- 未指定 failTime 的类型（LocalDateTime / Timestamp？）

与同文档中 ConfigChangeLog、AuditRecord、DosageStandard 等实体的详细字段表相比，DeadLetterEvent 的字段规范深度不一致。

**改进建议**：将 DeadLetterEvent 字段定义扩展为完整的字段表（参照 §9.4 ConfigChangeLog 的格式），包含字段名、类型、约束、默认值。或将其降级为"设计细节待实体类定义时确定"并注明。

---

### 问题 7：[一般] AiResult 新增工厂方法设计与 Phase 5 的 "AiResult 不变" 假设存在潜在冲突

**所在位置**：§2.3（line 315-316）、§7 设计决策（line 1021）

**问题描述**：
§2.3 和 §7 提出为 AiResult 新增两个重载工厂方法：
- `failure(String errorCode, T partialData)`
- `degraded(String fallbackReason, T partialData)`

但 Phase 5 包 G OOD（`a_v22_copy_from_v21.md`，line 96-97）显式声明：
- "AiService.java — 不变"
- "AiResult.java — 不变"

Phase 2/3 与 Phase 5 若由不同团队并行开发，Phase 5 团队可能基于"不变"假设使用 AiResult，在 Phase 2/3 新增方法后将面临编译/行为冲突。虽然新增静态方法在 Java 层面向下兼容，但 Phase 5 的 CapabilityExecutor 模板方法中若构建了 AiResult 实例，factory 方法签名差异可能导致二义性或行为不一致。

**改进建议**：
- 在 §10 时序依赖说明中增加"本设计新增 AiResult 重载工厂方法，可能与 Phase 5 包 G 的 `AiResult.java 不变` 假设冲突"的风险标注
- 建议将 AiResult 的扩展放在 ai-api 模块的独立演进基线中，与 Phase 5 团队沟通标记好兼容性承诺

---

## 总结

本产出质量较高，经过多轮修订已覆盖大量边界场景。上述 7 个问题中，4 个严重问题集中在**实体定义完整性**（主键缺失、DrugFacade 未定义）和**设计声明与执行机制的一致性**（截断标记、覆盖优先级保障），剩余问题为外部依赖管理和跨阶段兼容性。这些问题在内部审议的 10 项问题中未被覆盖，属于需求响应充分度和整体完整性的维度。
