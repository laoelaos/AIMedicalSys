# 质量审查报告 — v14（对照 v13 复制版）

审查日期：2026-06-29  
审查轮次：第 1 轮  
审查维度：需求响应充分度、事实正确性、逻辑一致性、深度和完整性

---

## 问题清单

### 1. sessionId 生成责任归属矛盾（严重）

- **所在位置**：§1.3 核心抽象一览 DialogueSessionManager 条目（"统一生成 sessionId（UUID v4）"）与 §3.1 DialogueSessionManager 协作描述（"首轮请求时前端生成 sessionId 传入"）
- **问题描述**：§1.3 声明 DialogueSessionManager"统一生成 sessionId"，但 §3.1 同一职责描述中同时出现"统一由 DialogueSessionManager 生成"和"首轮请求时前端生成 sessionId 传入"。两种表述指向相反的生成责任归属——若前端生成并传入 sessionId，则服务端不再承担"生成"职责，仅做校验和复现。当前设计实际语义为"前端按 UUID v4 规范生成并传入"或"后端按 UUID v4 规范生成并返回"二选一，但文档未在该决策点上给出统一陈述，编码阶段开发者在解读职责时将面临歧义。
- **证据**：§1.3 L53"DialogueSessionManager | class | ...统一生成 sessionId（UUID v4）"；§3.1 L345"sessionId 生成：统一由 DialogueSessionManager 生成，采用 UUID v4 格式……首轮请求时前端生成 sessionId 传入"；§3.1 L387"sessionId，必填——首轮请求由前端生成 UUID v4 传入"
- **改进建议**：统一表述为以下任一路径：(a) 后端生成——DialogueSessionManager 在 findOrCreate() 中发现 session 不存在时生成 UUID v4 并返回给前端，DialogueCreateRequest.sessionId 改为返回体字段而非请求体字段（需同时调整与需求文档 session_id 必填语义的对齐方式）；(b) 前端生成——删除 §1.3 和 §3.1 中"DialogueSessionManager 统一生成"的描述，明确责任为"DialogueSessionManager 接受前端传入的 UUID v4 并验证格式，首次使用时创建会话"。路径(b)与现有实现更一致。

### 2. Phase 5 迁移"代码无须修改"断言与设计决策矛盾（严重）

- **所在位置**：§1.1 设计目标第 1 条（"业务模块代码无须修改"）与 §7 设计决策"底座落地与 Phase 5 迁移兼容性"（L977）
- **问题描述**：§1.1 断言"若 Phase 5 保持 AiService 接口签名和 DTO 字段结构不变，业务模块代码无须修改"，但 §7 同一设计决策明确指出"若 Phase 5 需扩展 DTO 字段，业务模块的 Converter 需同步更新"。这是无条件的"无须修改"与有条件的"需要更新"之间的直接矛盾。§1.1 的表述给读者（尤其是项目管理者）造成 Phase 5 迁移零成本的错觉，实际上 AiService 只要发生任何 DTO 字段变更（这种变更在 Phase 5 几乎不可避免），Converter 层就需要调整。
- **证据**：§1.1 L9"若 Phase 5 保持 AiService 接口签名和 DTO 字段结构不变，业务模块代码无须修改"；§7 L977"若 Phase 5 需扩展 DTO 字段，业务模块的 Converter 需同步更新"
- **改进建议**：将 §1.1 中的断言修订为有条件表述，如"业务模块的核心 Service 和 Controller 代码在 AiService 接口签名不变的情况下无须修改；Converter 层需随 ai-api DTO 字段变更同步更新"。

### 3. BLOCK 阻断处方的 AuditRecord isLatest 管理缺失（严重）

- **所在位置**：§3.2 AuditRecord 实体描述（L415–L430）及 §4.2 同一处方多次审核处理（L696–L698）
- **问题描述**：AuditRecord 的 isLatest 管理机制明确基于 prescriptionOrderId 分组的语义（L430"同一 prescriptionOrderId 下已有的 isLatest=true 的记录更新为 isLatest=false"）。但 BLOCK 阻断的处方不会有 prescriptionOrderId（L416"BLOCK 阻断的审核记录此字段可为空"）。当 BLOCK 处方修改后重新审核时，新的 AuditRecord 无法通过 prescriptionOrderId 找到上一条记录并清除 isLatest 标记（因 prescriptionOrderId 为空），导致多条 BLOCK 记录同时 isLatest=true，后续查询"最新审核结果"返回多条记录，审核逻辑无法确定使用哪一条做阻断判定。设计未定义 BLOCK 场景下的 isLatest 管理替代方案。
- **证据**：L416"prescriptionOrderId：……BLOCK 阻断的审核记录此字段可为空"；L430"每次新审核写入 AuditRecord 时，先将同一 prescriptionOrderId 下已有的 isLatest=true 的记录更新为 isLatest=false"
- **改进建议**：补充 BLOCK 处方的 isLatest 管理策略。推荐方案：在 prescriptionOrderId 为空时，按 prescriptionId 分组执行相同的 isLatest 清理逻辑——以 prescriptionId 为键，将之前 isLatest=true 的记录置为 false，再插入新记录 isLatest=true。将此逻辑抽象为"按业务主键分组"模式：prescriptionOrderId 不为空时按 prescriptionOrderId，为空时按 prescriptionId。

### 4. MedicalRecordController 声称支持流式输出与 Phase 2/3 范围矛盾（严重）

- **所在位置**：§1.3 包D-AI2 核心抽象 MedicalRecordController 条目（L94）
- **问题描述**：§1.3 描述 MedicalRecordController"支持非流式与流式两种输出模式"，但 §3.3 和 §4.3 均明确"Phase 2/3 仅实现非流式模式"、"stream=true 时返回 MR_GEN_STREAM_NOT_SUPPORTED 错误码"。§1.3 作为快速参考章节，其表述会误导编码者在 Controller 层设计流式端点，与后续详细设计矛盾。编码者可能为"支持流式"提前搭建 SSE/WebSocket 端点，造成 Phase 2/3 交付范围内的无用工作量。
- **证据**：§1.3 L94"MedicalRecordController | class | 病历生成 REST 端点，接收医生端对话文本，返回结构化病历；支持非流式与流式两种输出模式"；§3.3 L524"Phase 2/3 仅实现非流式模式。stream=true 时返回 MR_GEN_STREAM_NOT_SUPPORTED 错误码"
- **改进建议**：将 §1.3 的 Controller 条目改为"支持非流式输出（Phase 2/3），流式输出预留到 Phase 4 以实现"，与 §3.3/§4.3 的详细范围说明保持一致。

### 5. AiSuggestionResult 并发安全描述混淆对象与容器（严重）

- **所在位置**：§1.3 包E 核心抽象 AiSuggestionResult 条目（L113）及 §3.4 AiSuggestionResult 类描述（L561）
- **问题描述**：文档称 AiSuggestionResult"内部字段更新通过 ConcurrentHashMap.compute() 原子操作保护"，但 AiSuggestionResult 是一个普通值对象类（class，含 taskId、suggestion、status 等字段），其本身并非 ConcurrentHashMap。实际并发安全机制是对存储 AiSuggestionResult 的 ConcurrentHashMap 执行 compute() 操作来原子替换整个结果条目，而非保护 AiSuggestionResult 对象内部字段的更新。此描述混淆了"数据容器（Map）的并发安全"与"值对象内部字段的并发安全"两个不同层次，编码时若按照"对象内部字段加锁保护"的思路实现，会写出在 AiSuggestionResult 内部方法上加 synchronized 的错误代码。正确的实现是直接替换 Map 中的整个 AiSuggestionResult 实例。
- **证据**：§1.3 L113"内部字段更新通过 ConcurrentHashMap.compute() 原子操作保护"；§3.4 L561"并发安全策略：内部字段更新通过 ConcurrentHashMap.compute() 原子操作保护"
- **改进建议**：修改为"AiSuggestionResult 实例由 ConcurrentHashMap 存储，通过 compute() 原子替换整个实例保证并发安全，不通过对象内部锁或 volatile 保护单个字段"。同时在 §3.4 的并发安全策略段落中补充示例伪代码或说明 Map 上的 compute() 操作逻辑。

### 6. RX_AUDIT_BLOCK 错误码已定义但无消费路径（一般）

- **所在位置**：§5.1 错误码表（L862）
- **问题描述**：错误码表定义了 RX_AUDIT_BLOCK 作为"审核（非AI）"类错误码，但全文中没有任何消费该错误码的描述。BLOCK 阻断的实际实现是通过 HTTP 422 + BlockResponse 直接返回，未提及使用此错误码。BlockResponse 的结构也未包含 errorCode 字段。定义但不消费的错误码会在编码阶段引发两种无价值讨论——"这个错误码什么时候用"和"是不是我漏了消费路径"。
- **证据**：§5.1 L862"| 审核（非AI）| RX_AUDIT_ | … RX_AUDIT_BLOCK（BLOCK 阻断）"；L874"BLOCK 阻断响应通过 BlockResponse 封装，使用 HTTP 422 状态码"
- **改进建议**：任选其一——(a) 从错误码表中删除 RX_AUDIT_BLOCK，阻断场景统一使用 BlockResponse 的阻断码（如有需要则定义 BlockResponse.reasonCode）; (b) 保留 RX_AUDIT_BLOCK 并加入 BlockResponse 的字段定义，明确其作为阻断响应唯一标识的消费路径。

### 7. DrugInteractionPairChangeEvent 在 Phase 2/3 范围内无实际意义（一般）

- **所在位置**：§9.3 规则管理接口（L1078）
- **问题描述**：§9.3 列出了 DrugInteractionPairChangeEvent 作为规则变更事件之一，但 §3.2 已明确说明"DrugInteractionRule（DDI）不在 Phase 2/3 本地规则范围内"，DrugInteractionPair 实体仅作为"预留骨架"存在。为一个 Phase 2/3 不参与运行时校验的实体定义变更事件，浪费事件基础设施资源，且编码者可能误解为 DrugInteractionPair 是在 Phase 2/3 交付范围内的数据实体。
- **证据**：§3.2 L455"DrugInteractionRule（DDI）不在 Phase 2/3 本地规则范围内，DrugInteractionPair 数据实体在目录中预留骨架但不参与 Phase 2/3 运行时校验"；§9.3 L1078"DrugInteractionPairChangeEvent"
- **改进建议**：删除 DrugInteractionPairChangeEvent 的相关引用，标注 DrugInteractionPair 相关的 CRUD 接口和变更事件为 Phase 4 范围。在 §9.3 的实体类型列表中标注"DrugInteractionPair（Phase 4 实现）"。

### 8. AllergyWarningSeverity 枚举值排序与其他同类枚举不一致（一般）

- **所在位置**：§1.3 包E AllergyWarningSeverity 枚举定义（L119）及 §3.4 AllergyWarningSeverity 定义（L588–L589）
- **问题描述**：AllergyWarningSeverity 定义为 CRITICAL / WARNING / INFO（从重到轻），而同类枚举 DosageAlertLevel 定义为 INFO / WARNING / CRITICAL（从轻到重），AlertSeverity 也定义为 INFO / WARNING / CRITICAL。文档自身在 §3.4 明确声明"三者已统一为 INFO/WARNING/CRITICAL 命名约定"，但 AllergyWarningSeverity 并未遵守此约定——其取值虽相同但排列顺序相反。编码时可能出现 ordinal() 比较顺序不一致的 bug。
- **证据**：§1.3 L119"AllergyWarningSeverity | enum | ...CRITICAL/WARNING/INFO"；§3.4 L589"此枚举与 DosageAlertLevel（INFO/WARNING/CRITICAL）和 AlertSeverity（INFO/WARNING/CRITICAL）分属不同告警维度，三者已统一为 INFO/WARNING/CRITICAL 命名约定"
- **改进建议**：将 AllergyWarningSeverity 枚举值顺序调整为 INFO / WARNING / CRITICAL，与另外两个枚举保持一致。同时在枚举注释中说明命名约定已统一。

### 9. forceSubmit=false 时 WARN 级处方的提交流程存在循环重审风险（一般）

- **所在位置**：§4.2 处方提交端点行为（L689）
- **问题描述**：§4.2 定义了 forceSubmit=false 时"执行常规审核流程（若待提交处方无最新审核结果则先调用审核）"。若处方已有 WARN 级审核结果但医生选择 forceSubmit=false，提交端点会重新执行审核流程——若新审核仍为 WARN，则再次进入 WARN 分支，医生再次看到警告弹窗。此循环在极端情况下可能无限重复（处方内容未变时审核结果不会改变）。设计未定义"已存在 WARN 审核结果且处方未变更"时的跳过机制或最大重审次数。
- **证据**：§4.2 L686–L694 处方提交端点行为；L672–L681 WARN 分支
- **改进建议**：补充 forceSubmit=false 且已经存在 WARN 最新审核结果时的处理策略——建议定义为"返回 WARN 结果并提示医生选择强制提交或修改"，避免无意义地重复调用 AI 审核。

### 10. §1.3 DialogueCreateRequest 的 AdditionalResponse 引用缺少字段定义说明（轻微）

- **所在位置**：§1.3 DialogueCreateRequest 条目（L61）
- **问题描述**：§1.3 的 DialogueCreateRequest 描述中直接引用了 AdditionalResponse 类型（"追问回答数组（additionalResponses，可选，List\<AdditionalResponse\>）"），但 AdditionalResponse 在该章节之后的单独条目中才定义（L62）。快速查阅 §1.3 的读者在读到 DialogueCreateRequest 时尚未见到 AdditionalResponse 的定义。此顺序问题影响 §1.3 作为"核心抽象一览"的快速查阅效率。
- **证据**：§1.3 L61 提及 additionalResponses 为 List\<AdditionalResponse\>；L62 才定义 AdditionalResponse
- **改进建议**：将 AdditionalResponse 条目提前至 DialogueCreateRequest 之前，或至少在 DialogueCreateRequest 的描述中增加"(详见下方 AdditionalResponse 条目)"的交叉引用。

---

## 整体评价

v14 文档经过 13 轮迭代修复，在需求响应完整性、异常场景覆盖、边界条件处理方面已达到较高成熟度。以上 10 个问题中，4 个为严重级别（主要涉及内部矛盾和误导性断言），5 个为一般级别（涉及缺失定义或范围标注不一致），1 个为轻微级别（排版顺序）。修复后即可满足直接指导编码实现的要求。

核心结论：**存在可修复的严重问题，但非系统性缺陷**。上述 4 个严重问题均源于表述/归责模糊导致的二义性，而非功能缺失，修复成本较低但若不修复将在编码阶段引发实质性分歧。
