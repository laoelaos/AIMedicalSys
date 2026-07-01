# 质量审查诊断报告

## 审查概述

- **审查对象**：Phase 2/3 包C/D-AI1/D-AI2/E 架构级 OOD 设计方案（v11）
- **审查视角**：需求响应充分度、事实/逻辑一致性、深度与完整性
- **审查轮次**：首轮
- **审查日期**：2026-06-28

---

## 发现的问题

### 问题1：【事实矛盾】check-dose 端点请求参数缺少 prescriptionId，无法支撑 PrescriptionDraftContext 写入行为

- **问题描述**：§4.4 即时校验子端点的请求参数定义为 `{ drugCode, dosage, unit, routeOfAdministration, patientAge, patientWeight, frequency }`，该参数列表不含 `prescriptionId`。但同一段落的行为描述要求 "CRITICAL 告警写入 PrescriptionDraftContext（key=prescriptionId，全量覆盖）"。由于 PrescriptionDraftContext 以 `prescriptionId` 为 key，请求中无此字段则后端无法确定写入哪个上下文条目。这是文本中的事实矛盾——行为契约与请求参数结构不匹配。
- **所在位置**：§4.4 "辅助开方场景"——"[即时校验子端点]" 段落，请求参数声明及后续指令
- **严重程度**：严重
- **改进建议**：在 check-dose 请求参数中增加 `prescriptionId`（必填，String），使 CRITICAL 告警写入行为可执行。同步更新 §一.3 包E 核心抽象一览中 DosageCheckRequest 的字段说明。若 check-dose 设计目标即为无状态纯校验端点、不维护草稿上下文，则需从行为描述中删除 CRITICAL 写入逻辑，改为由 PrescriptionAssistController 的 /assist 主端点负责写入。

### 问题2：【关键遗漏】AdditionalResponse 业务层 DTO 缺少字段定义

- **问题描述**：§2.1 目录结构列出 `AdditionalResponse.java`，§3.1 DialogueCreateRequest 引用 `List\<AdditionalResponse\>` 但其字段定义在全文未出现。作为多轮追问请求的核心数据结构，缺少字段定义导致前端开发和 Service 层组装均面临随意假设。
- **所在位置**：§3.1 DialogueCreateRequest 字段表——`additionalResponses` 的说明中未展开 AdditionalResponse 的子字段
- **严重程度**：严重
- **改进建议**：在 §3.1 或 §1.3 包C 核心抽象一览中补充 AdditionalResponse DTO 的字段定义。按需求文档 3.4.1 多轮调用约定及 §4.5 ai-api 层 AdditionalResponseItem 的反向参考，建议至少包含：`question`（string，可选——本轮追问的问题文本）、`answer`（string，必填——患者回答内容）、`answeredAt`（string，ISO 时间戳，可选）。与 ai-api 层 AdditionalResponseItem 保持字段映射一致。

### 问题3：【关键遗漏】TriageRule 实体 conditions 字段 JSON 结构未定义，规则引擎实现缺乏核心输入格式

- **问题描述**：§3.1 TriageRule 实体定义了 `conditions` 字段，仅描述为"JSON 文本，存储症状关键词匹配逻辑"。该字段是 TriageRuleEngine.match() 的核心匹配依据，但其 JSON schema 完全未定义（是关键词列表 `["胸痛", "气短"]` 还是条件表达式 `[{"keyword": "胸痛", "weight": 0.8}]` 或其他格式）。实现者无法据此编码规则引擎的匹配逻辑。
- **所在位置**：§3.1 TriageRule（JPA @Entity）——`conditions` 字段
- **严重程度**：一般
- **改进建议**：至少给出 conditions 的 JSON schema 示例，建议格式为 `List\<ConditionItem\>` 其中 ConditionItem 含 `keyword`（string）、`weight`（float，0–1 可选）、`matchType`（enum：EXACT / FUZZY / REGEX，可选）。同时应在 §7 设计决策中补充 conditions 字段结构的设计选择理由，或在 TriageRuleEngine 的职责中说明 conditions 的解析策略。

### 问题4：【逻辑不完整】全量拼接策略在多轮长对话场景下的 token 超限风险未评估

- **问题描述**：§3.1 定义 AI 调用上下文为"全量拼接"策略——每次调用 AiService.triage() 将完整对话历史拼合为 chiefComplaint 字段。随着多轮追问轮次增加，拼合文本线性增长，可能超出底层 AI 模型的最大输入长度（如 4K/8K/32K tokens）。设计文本未对此风险做任何评估或说明应对策略（截断策略、滑动窗口、压缩等）。
- **所在位置**：§3.1 TriageService —— "AI 调用上下文传递策略"段落
- **严重程度**：一般
- **改进建议**：补充 token 超限风险的评估。推荐三种可选路径并选择其一写入设计决策：(a) 截断策略——从最早轮次开始丢弃直至拼接文本低于阈值；(b) 摘要策略——每 N 轮对历史进行 AI 摘要压缩后保留摘要文本；(c) 显式标注为已知风险，标注 Phase 4 或 Phase 5 解决。同时在 DialogueSession 中可考虑增加 `contextTokenCount` 字段辅助监控。

### 问题5：【事实矛盾】错误码 `RX_ASSIST_AI_SUGGESTION_NOT_FOUND` 命名违反自身定义的命名规则

- **问题描述**：§5.1 错误码表将 `RX_ASSIST_AI_SUGGESTION_NOT_FOUND` 归类为"开方辅助（非AI）"类别，但其名称含有 `_AI_` 中段。而 §5.1 开头定义的命名规则明确："凡涉及 AI 推理调用的错误码均保留 `_AI_` 中段，纯业务逻辑错误码不含 `_AI_`"。`SUGGESTION_NOT_FOUND`（建议不存在/TTL 过期）是纯业务逻辑错误（查询时发现条目不存在），不应含 `_AI_` 中段。或在 §5.1 表中将其移至"开方辅助（AI）"行。
- **所在位置**：§5.1 错误码表——"开方辅助（非AI）"行
- **严重程度**：一般
- **改进建议**：二选一：(a) 将命名改为 `RX_ASSIST_SUGGESTION_NOT_FOUND`（去掉 `_AI_`）并修正所有引用处；(b) 将其移至"开方辅助（AI）"行并在命名规则说明中增加例外注释。推荐 (a)，因为"条目不存在"与 AI 推理调用无关。

### 问题6：【关键遗漏】全量降级路径中前端如何区分"AI 完全不可用"与"AI 明确返回空"的场景

- **问题描述**：§4.3 定义了两种降级路径——"超时降级"（保留部分结果 + 错误码）和"完全降级"（AI 完全不可用 → 返回空字段集）。但在完全降级路径中，RecordGenerateResponse 的 `fromFallback=true` 标记仅表明"走降级路径"，无法区分"AI 无结果但有本地规则兜底结果"和"AI 和本地均无结果"的差别。前端收到空字段集 + fromFallback=true 时，无法判断是应该展示补全提示还是展示彻底失败的提示。
- **所在位置**：§4.3 病历生成场景——完全降级路径描述；§3.3 RecordGenerateResponse 字段
- **严重程度**：一般
- **改进建议**：在 RecordGenerateResponse 中增加 `fallbackReason` 可选字段（string），承载降级原因描述（如 "AI 服务不可用"、"AI 超时"、"本地规则无匹配"等）。或在 errorCode 层面定义更细粒度的降级错误码（如 MR_GEN_LOCAL_FALLBACK_EMPTY、MR_GEN_AI_UNAVAILABLE），使前端可差异化展示。此建议同样适用于 TriageResponse 的 degraded 场景——§1.3 TriageResponse 已含 fallbackHint，但病历侧尚无对应字段。

### 问题7：【深度不足】check-dose 端点 `taskId` 的生成和生命周期未定义

- **问题描述**：§4.4 定义 check-dose 端点返回 `taskId`，用于后续通过 `GET /api/prescription/assist/suggestion/{taskId}` 查询异步 AI 建议。但 `taskId` 的生成策略（UUID v4 / 自增？归 AiSuggestionResult 管理吗？）、写入时机（check-dose 时预创建 PENDING 记录，但由谁生成 taskId？）、以及 `taskId` 与 `prescriptionId` 或其他上下文的关联方式未定义。
- **所在位置**：§4.4 即时校验子端点返回说明；§3.4 AiSuggestionResult
- **严重程度**：轻微
- **改进建议**：明确 taskId 由 PrescriptionAssistController 统一生成（UUID v4），在 check-dose 响应前预创建 AiSuggestionResult(taskId, PENDING)。补充 taskId 与 prescriptionId 的关联说明，用于后续查询时可将异步结果回溯到原处方。

### 问题8：【逻辑不完整】PrescriptionDraftContext 缺少写入前的实例化保证

- **问题描述**：§4.4 定义 check-dose 行为"CRITICAL 告警写入 PrescriptionDraftContext（key=prescriptionId）"，但 PrescriptionDraftContext 的实例化时机描述为"首次 check-dose 请求时（创建时机）"。若首次 check-dose 即命中 CRITICAL 等级，在写入时 PrescriptionDraftContext 可能尚未实例化（或尚未被注入到当前调用链中）。创建与写入的时序关系未定义。
- **所在位置**：§3.4 PrescriptionDraftContext —— 生命周期管理描述（创建时机：首次 check-dose 请求时）
- **严重程度**：轻微
- **改进建议**：明确 PrescriptionDraftContext 采用惰性初始化策略（第一次写操作前检查 null 后创建）或在 Controller 层首次调用前统一初始化。同时说明其 Spring Bean 作用域（默认 singleton）在此场景下的正确性。

### 问题9：【深度不足】prescriptionId 与 prescriptionOrderId 语义关系未定义

- **问题描述**：文档中同时出现 `prescriptionId`（PrescriptionDraftContext key、submit 端点请求参数）和 `prescriptionOrderId`（AuditRecord 实体字段），但未说明二者关系。难以判断它们是同一事物的不同命名还是不同的标识符。
- **所在位置**：§3.2 AuditRecord(`prescriptionOrderId`)、§3.4 PrescriptionDraftContext(key=`prescriptionId`)、§4.2 处方提交端点请求(`prescriptionId`)
- **严重程度**：轻微
- **改进建议**：在 §3.2 或 §3.4 中明确说明：prescriptionId 与 prescriptionOrderId 是同一实体标识的同义词（设计阶段统一使用 prescriptionId），或明确说明二者为不同含义（如 prescriptionId 为编辑草稿 ID，prescriptionOrderId 为提交落单后的处方单号），并相应统一命名。

### 问题10：【深度不足】TriageService AI 调用失败场景缺少 aiFailCount 的存储持久化说明

- **问题描述**：§3.1 定义 AI 连续 3 次失败触发兜底提示，DialogueSession 中维护 `aiFailCount` 字段。但 DialogueSession 仅存于内存（ConcurrentHashMap + TTL 30 分钟），若 AI 连续失败的间隔跨越了 session 的 TTL（30分钟）或者服务重启，aiFailCount 将会丢失。连续失败判定在这些场景下会重置为 0。设计未说明此行为是否可接受。
- **所在位置**：§3.1 TriageService——"AI 连续失败兜底机制"；§6.1 对话会话并发管理
- **严重程度**：轻微
- **改进建议**：在 §3.1 或 §7 中增加说明：aiFailCount 基于内存会话生命周期，跨 TTL/重启会重置；此设计决策基于"AI 连续失败在短时间内发生"的假设（30 分钟内连续失败才触发兜底），若需跨会话持久化则标记为 Phase 5 扩展点。

---

## 整体质量评价

该设计文档经过 10 轮迭代审议，已覆盖绝大多数技术细节和边界场景，整体质量较高。需求响应充分度良好——四个包的输入/输出契约均与需求文档 §3.4.x 对齐，架构约束（底座落地、强耦合同步、风格一致）均已体现。事实错误方面存在 1 处 check-dose 请求参数与行为描述的矛盾及 1 处错误码命名自洽问题，关键遗漏方面有 DTO 定义缺失和规则引擎 condition 格式未定义。整体深度满足编码指导要求，但部分字段的细粒度定义（AdditionalResponse、conditions JSON schema、prescriptionId 语义）尚需补充以消除实现歧义。

共发现 10 个问题，其中严重级 2 个、一般级 4 个、轻微级 4 个。
