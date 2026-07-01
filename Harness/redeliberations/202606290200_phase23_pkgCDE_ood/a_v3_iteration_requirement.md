根据以下审查结果，迭代上一轮的产出，形成新版的文件，从而更好地满足用户需求。

## 当前审查结果

### 问题 1：[严重] TriageRecord、AuditRecord、DeadLetterEvent 实体未定义 JPA @Id 主键字段

**所在位置**：§3.1 TriageRecord、§3.2 AuditRecord、§2.2 DeadLetterEvent

**问题描述**：三个 JPA @Entity 定义了丰富的业务字段，但均未声明 `@Id` 主键字段。缺少 PK 使实体定义不完整，无法直接编码。

**改进建议**：
- TriageRecord 补充 `recordId`（Long，@GeneratedValue）或显式声明 sessionId 为主键并补充唯一约束
- AuditRecord 补充 `auditId`（Long，@GeneratedValue）；若使用 prescriptionId + auditSequence 复合键需用 @IdClass 或 @EmbeddedId 显式定义
- DeadLetterEvent 补充 `id`（Long，@GeneratedValue），使补偿任务可按 id 检索更新

### 问题 2：[严重] 手动选科与 RegistrationEvent 自动写入的覆盖优先级缺少强制执行机制

**所在位置**：§2.2、§3.1 TriageService.selectDepartment

**问题描述**：设计声明"当两路径均命中时以手动选科值为准（覆盖语义）"，但 selectDepartment 被手动选科端点和 RegistrationEventListener 共同调用，RegistrationEvent 可能迟到（死信补偿 30 分钟），迟到的事件会无声覆盖手动选择。

**改进建议**：
- 方案 A（推荐）：RegistrationEventListener 在调用 selectDepartment 前检查 finalDepartmentId，仅在为空时写入；手动选科端点调用时始终覆盖
- 方案 B：selectDepartment 增加 callerContext 参数（如 enum SELECTOR {MANUAL, EVENT}），MANUAL 始终覆盖，EVENT 仅在 finalDepartmentId 为空时覆盖

### 问题 3：[严重] 全量拼接上下文截断缺少 AI 感知截断标记

**所在位置**：§3.1 TriageService"全量拼接 Token 超限风险评估"

**问题描述**：上下文截断策略（保留首轮主诉 + 最近 N 轮 QA，丢弃中间轮次）截断后传递给 AiService.triage() 的上下文中没有任何标记告知 AI 中间轮次已被丢弃，AI 可能产生错误推理。

**改进建议**：在拼接上下文中显式插入截断标记，例如在首轮主诉和最近 N 轮之间追加 `[NOTE: 部分对话内容因长度限制已省略]`，使 AI 感知到信息缺口。

### 问题 4：[严重] DrugFacade 接口有引用无定义

**所在位置**：§8.2

**问题描述**：文档声明的 DrugFacade 在常见药品信息查询中显式引用，但未出现在 §1.3 核心抽象、§2.1 目录结构、§2.3 依赖关系或跨模块协作图中，接口方法签名完全未定义。

**改进建议**：在 §1.3 核心抽象表中新增 DrugFacade 条目（interface，定义在 common-module-api/drug/ 包下，提供 findByDrugCode(drugCode) 等方法）；在 §2.1 目录结构中补充 DrugFacade 的包路径；在 §2.3 跨模块数据获取机制中说明与 DoctorFacade 一致的模式。

### 问题 5：[严重] Registration 模块和就诊模块作为外部依赖未在设计范围中说明

**所在位置**：§2.2 RegistrationEvent、§3.3 RecordGenerateRequest

**问题描述**：设计依赖两个当前代码库中不存在的模块（registration 模块发布 RegistrationEvent、visit 模块实现 VisitFacade），但 OOD 中未将此项列为设计前提/风险/依赖。

**改进建议**：
- 在 §1.1 设计目标或末尾新增"外部依赖与前提条件"章节，显式列出：
  - registration 模块（需提供 RegistrationEvent 发布能力 + sessionId 填充逻辑）
  - visit 模块（需提供 VisitFacade 实现）
- 标注这些依赖的实施时间线要求，纳入设计风险

### 问题 6：[一般] DeadLetterEvent 实体缺少精确的 JPA 字段定义

**所在位置**：§2.2

**问题描述**：DeadLetterEvent 包含 eventPayload、failReason、failTime、state、retryCount、maxRetryCount 等字段，但未指定 state 字段类型和值域、retryCount 默认值、eventPayload 存储形式、failTime 类型等，与同文档中其他实体的详细字段表相比深度不一致。

**改进建议**：将 DeadLetterEvent 字段定义扩展为完整的字段表（参照 §9.4 ConfigChangeLog 的格式），包含字段名、类型、约束、默认值。

### 问题 7：[一般] AiResult 新增工厂方法设计与 Phase 5 的"AiResult 不变"假设存在潜在冲突

**所在位置**：§2.3、§7 设计决策

**问题描述**：§2.3 和 §7 提出为 AiResult 新增重载工厂方法，但 Phase 5 包 G OOD 显式声明 AiResult.java 不变，若并行开发可能面临编译/行为冲突。

**改进建议**：
- 在 §10 时序依赖说明中增加"本设计新增 AiResult 重载工厂方法，可能与 Phase 5 包 G 的 `AiResult.java 不变` 假设冲突"的风险标注
- 建议将 AiResult 的扩展放在 ai-api 模块的独立演进基线中，与 Phase 5 团队沟通标记好兼容性承诺

## 历史迭代回顾

### 已解决的问题（出现在第 1 轮反馈，第 2 轮审查中不再提及）
- **RegistrationEvent 缺少 sessionId 字段**：已在 §2.2 补充 sessionId 可选字段，事件驱动路径已闭合
- **AllergyWarningSeverity 枚举值不匹配**：已在 §3.4 修正为 INFO/WARNING/HIGH
- **DosageThresholdService 匹配优先级不一致**：已在 §3.4 与 §8.4 对齐
- **过敏冲突检查实现归属未定义**：已在 §3.4 明确实现方式
- **encounterId → visitId 转换路径未定义**：已在 §3.3 补充 VisitFacade 转换策略
- **DialogueSession/TriageRecord 事务一致性缺失**：已在 §3.1 补充"先写数据库再更新内存"策略
- **Phase 5 迁移 Service 代码不变断言矛盾**：已在 §1.1 引入 Store 抽象层并调整表述
- **LocalRuleEngine 规则计数不一致**：已在 §1.3 修正为"5 条运行时规则 + 1 条预留骨架"
- **CRITICAL/BLOCK 阻断竞态防护缺失**：已在 §4.2 补充防护策略
- **PrescriptionAssistResponse 缺少 errorCode**：已在 §1.3 补充 errorCode 可选字段

### 持续存在的问题
- 无：第 2 轮审查明确聚焦内部审议未覆盖的维度，未发现持续遗留问题

### 新发现的问题（第 2 轮审查识别的 7 个问题）
- 实体未定义 @Id 主键（问题 1）
- 覆盖优先级强制执行机制缺失（问题 2）
- 上下文截断缺少 AI 感知标记（问题 3）
- DrugFacade 有引用无定义（问题 4）
- 外部依赖未声明（问题 5）
- DeadLetterEvent 字段定义不完整（问题 6）
- AiResult 跨阶段兼容冲突（问题 7）

## 上一轮产出路径
C:\Develop\Software\AIMedicalSys\Harness\redeliberations\202606290200_phase23_pkgCDE_ood\a_v2_copy_from_v1.md

## 用户需求
C:\Develop\Software\AIMedicalSys\Harness\redeliberations\202606290200_phase23_pkgCDE_ood\requirement.md
