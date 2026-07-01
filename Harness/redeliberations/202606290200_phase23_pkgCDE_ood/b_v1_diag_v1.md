# 质量审查报告 — Phase 2/3 包CDE OOD 设计（第 1 轮）

审查日期：2026-06-29  
审查范围：响应需求、事实正确性、逻辑一致性、深度完整性、可落地性  

---

## P1 [严重] AiResult 重载工厂方法存在性描述与代码事实不符

**问题描述**：  
§2.3（line 286-287）和 §3.3（line 538）使用"提供...超时降级重载"、"通过新增重载 AiResult.failure(String errorCode, T partialData) 实现"等肯定式表述，描述 AiResult 存在 `failure(String errorCode, T partialData)` 和 `degraded(String fallbackReason, T partialData)` 两个带 partialData 参数的重载工厂方法。但实际代码（AIMedical/backend/modules/ai/ai-api/src/main/java/com/aimedical/modules/ai/api/AiResult.java:22-32）仅包含 `success(T data)`、`failure(String errorCode)`、`degraded(String fallbackReason)` 三个静态工厂方法，且 data 在 failure/degraded 中固定为 null，不存在带 partialData 的重载。§10 虽说明 ai-api DTO 需扩展，但 AiResult 的重载方法缺失未在 §10 中体现，且多处用"提供"等已完成语义描述本属于待实现的内容。

**所在位置**：§2.3（AiService 接口定义段，第 3 段首句）；§3.3 MedicalRecordService 非流式超时降级路径说明  
**严重程度**：严重 — 编码实现者直接参考此处会认为方法已存在，产生编译错误  
**改进建议**：  
1. 将 §2.3 和 §3.3 中所有肯定式"提供"改为"需新增"或"待实现"  
2. 在 §10 ai-api 扩展规格中增加 AiResult 重载方法作为显式扩展条目  
3. 明确标注这两个重载方法实现与 ai-api DTO 字段扩展同为业务模块开发的前置依赖

---

## P2 [严重] §2.3 AiService 方法参数描述的 ai-api DTO 字段与当前实现状态脱节

**问题描述**：  
§2.3 中 triage()、prescriptionCheck()、generateMedicalRecord()、prescriptionAssist() 四个方法的参数描述直接列出了完整字段集（例如 TriageRequest 含 chiefComplaint、additionalResponses、patientId、sessionId、ruleVersion、ruleSetId 六个字段），但实际 ai-api TriageRequest（AIMedical/backend/modules/ai/ai-api/src/main/java/com/aimedical/modules/ai/api/dto/triage/TriageRequest.java）仅含 chiefComplaint 一个字段。§10 虽说明这些 DTO 需扩展，但 §2.3 未加任何前提标注。整篇文档在 §10 之前有 8 次引用这些 DTO 的完整字段，均使用"含"字表述而非"需扩展为"。阅读者若不逐条核对 §10，无法判断哪些字段已存在、哪些需新增。

**所在位置**：§2.3（line 288-314，四个方法的参数/返回描述）  
**严重程度**：严重 — 编码阶段按 §2.3 理解会直接引用不存在的字段  
**改进建议**：  
在 §2.3 每个方法描述后统一加注"（字段定义现状及扩展计划见 §10）"，或将 §10 提前至 §2 之前作为前提条件说明

---

## P3 [严重] §3.4 DosageThresholdService 匹配优先级层级数与 §8.4 不一致

**问题描述**：  
§3.4（line 577）说"四级均未命中……降级返回"，并列出"精确匹配→年龄范围匹配→体重范围匹配→无分级默认阈值→标准不存在"共 4 级（含标准不存在为 5 种结果）。§8.4（line 1087-1094）定义为六级匹配优先级（v16 修订补充了 Level 2"同时范围匹配"）。§3.4 的"四级"表述是在 v16 修订前写的，修订后未同步更新。且 §3.4 的"精确匹配→年龄范围匹配→体重范围匹配→无分级默认阈值"共 4 级，描述中缺失 Level 2"同时范围匹配"。

**所在位置**：§3.4 DosageThresholdService（line 577）  
**严重程度**：严重 — 实现者按 §3.4 编码会遗漏"同时范围匹配"这一优先级  
**改进建议**：  
将 §3.4 的匹配优先级序列同步更新为六级（与 §8.4 完全一致），"四级均未命中"改为"六级均未命中"或直接引用 §8.4

---

## P4 [重要] §4.1 多轮上下文拼合流程中 correctedChiefComplaint 替换环节缺失

**问题描述**：  
§3.1 定义了 correctedChiefComplaint 替换语义——全量拼接时若该字段存在则替换原始 chiefComplaint 为推理上下文起点。但 §4.1（line 650-656）的"AI 调用上下文组装"流程只描述了"首轮映射 chiefComplaint→多轮拼合主诉+历次 QA"，没有展示 correctedChiefComplaint 的替换发生在拼合流程的哪一步。实现者不清楚替换与拼合的顺序（先替换再拼合，还是先拼合再替换首段）。

**所在位置**：§4.1（line 650-656，AI 调用上下文组装段）  
**严重程度**：重要 — 替换时机可能影响拼合结果  
**改进建议**：  
在 §4.1 的拼合流程中增加步骤："(3) 若 DialogueSession.correctedChiefComplaint 存在，以替换后的主诉文本作为拼合上下文起点"

---

## P5 [重要] §4.2 处方版本校验的"结构化比较"的具体哈希输入未定义

**问题描述**：  
§4.2（line 737-741）定义 forceSubmit=false 路径采用"五字段结构化哈希比对"，但未说明哈希的输入构造方式——originalPrescription 是 JSON TEXT 存储的处方快照，其 JSON 序列化格式（字段顺序、空格、null vs 缺失字段）可能导致同一处方的两次 JSON 序列化产生不同哈希。同样泛化问题存在于 forceSubmit=true 的处方版本校验。

**所在位置**：§4.2（forceSubmit=false 段落 line 737；forceSubmit=true 段落 line 738-741）  
**严重程度**：重要 — 哈希策略选择不当会导致误判（同处方因 JSON 格式差异判为"不一致"）  
**改进建议**：  
明确约定规范化的哈希比较方式："按药品编码排序后，对每项药品的 drugId + dose + frequency + duration + route 拼接为规范字符串，五字段按顺序以 `|` 分隔，整体计算 SHA-256。JSON 层级的格式差异（字段顺序、空白、null 与缺失）不参与比较。"

---

## P6 [重要] §2.2 手动选科路径 selectDepartment 的方法调用链路存在歧义

**问题描述**：  
§3.1 定义 TriageService.selectDepartment(sessionId, departmentId, departmentName)"按 sessionId 查找 TriageRecord，若记录存在则更新"。但 TriageRecord 是 JPA @Entity 持久化在数据库的，而 sessionId 同时也是 DialogueSession 的内存标识。文档未明确该方法是从 TriageRecordRepository（数据库）查找记录，还是从 DialogueSessionManager（内存）查找记录。如果实现者采取了先查 DialogueSessionManager 再转为数据库操作的路径，在 session TTL 超时场景（但 TriageRecord 仍存在于数据库）下会错误地返回 TRIAGE_SESSION_NOT_FOUND。§1.3 TriageRecord 条目和 §3.1 的 selectDepartment 描述均未澄清此关键路径。

**所在位置**：§1.3 TriageRecord 条目（line 50-51）；§3.1 TriageService selectDepartment 方法描述（line 347）  
**严重程度**：重要 — 不同的实现选择导致不同的运行时行为，TriageRecord 仍在数据库但内存 session 已超时的场景下前端手动选科会失败  
**改进建议**：  
明确 selectDepartment 直接通过 TriageRecordRepository.findBySessionId() 查询数据库，不依赖内存 Session 状态。在 §1.3 或 §3.1 中添加实现指引说明。

---

## P7 [一般] §1.3 LocalRuleEngine 规则计数跨章节不一致

**问题描述**：  
§1.3 包D-AI1 核心抽象表说 LocalRuleEngine"封装 6 条独立规则"，并在列举中包含 DrugInteractionRule（DDI，Phase 2/3 预留骨架）。但 §3.2（line 452-454，LocalRuleEngine 职责描述）列举了 5 条规则（不含 DrugInteractionRule）。§4.2 降级路径明确说明"5 条规则完整执行"。§1.3 中"6 条"的表述与运行时的"5 条"不一致。

**所在位置**：§1.3 LocalRuleEngine 条目  
**严重程度**：一般 — 细读 §3.2 可理解 DrugInteractionRule 为预留，但 §1.3 易让读者误以为 6 条均参与运行时  
**改进建议**：  
将 §1.3 的"封装 6 条独立规则——DrugInteractionRule（DDI，Phase 2/3 预留骨架）"改为"封装 5 条运行时独立规则 + 1 条预留骨架 DrugInteractionRule（Phase 2/3 不启用）"

---

## P8 [一般] §6.3 @Async 异步调用与 §3.4/§4.4 主端点行为描述的边界不清晰

**问题描述**：  
§6.3（line 967）说"@Async / CompletableFuture.runAsync() 调用 AiService.prescriptionAssist()"，但 §3.4 和 §4.4 的 /assist 主端点流程描述中未区分同步与异步路径——主端点返回 PrescriptionAssistResponse（含 AI 完整的处方草案），如果 AI 调用是异步的，响应体不可能立即包含 AI 结果。§4.4 将 check-dose 子端点定义为异步（返回 taskId），但主端点的同步特性未显式说明。

**所在位置**：§6.3（line 967）；§3.4 PrescriptionAssistService 方法描述；§4.4 /assist 主端点流程  
**严重程度**：一般 — 设计意图是主端点同步等待、check-dose 子端点异步，但 §6.3 的表述可能被理解为所有 AI 调用均异步  
**改进建议**：  
在 §6.3 或 §3.4 明确标注"主端点同步等待 AiService.prescriptionAssist() 结果，check-dose 子端点的异步 AI 建议通过 AiSuggestionResult 查询端点返回"，并确保 §4.4 流程描述同步反映此约定

---

## P9 [一般] §4.2 步①与步②之间的 CRITICAL 竞态未提供实质防护

**问题描述**：  
§4.2 处方提交三步流程中，步①检查 PrescriptionDraftContext 的 CRITICAL 告警，步②检查最新 AuditRecord 的 BLOCK。文档在"阻断合并语义"中识别了"步①判定为无 CRITICAL 后并发写入 CRITICAL"的竞态，但仅描述了响应策略（合并阻断原因），未提供防护手段。CRITICAL 状态在步①检查通过后到步②执行前可能被并发写入，导致步①的"通过"结论失效。

**所在位置**：§4.2 阻断合并语义段（line 744）  
**严重程度**：一般 — Phase 2/3 单实例/同步环境下概率低，但多线程并发场景下存在  
**改进建议**：  
补充说明 CRITICAL 阻断采用"快照比较"——将步①检查时的 CRITICAL 列表快照传入后续步骤；或在步②后、提交前增加二次 CRITICAL 验证

---

## P10 [轻微] §1.2 DosageStandard 迁移至 common 模块但 dao/repository 归属未定义

**问题描述**：  
§1.2 和 §2.2 说 DosageStandard 实体迁移至 common 模块（common/entity/DosageStandard.java），prescription 模块通过"仅查询 Repository"访问。但 common 模块的 entity 包下只应放置实体类，DosageStandardRepository（只读 Repository）放在 prescription 模块（§2.1 已标注），而管理端写入所需的 Repository 或 Service 放在 admin 模块。此处设计合理，但 common 实体在零模块依赖约束下如何被 admin 模块的 Repository 引用（admin 需依赖 common 模块，但 common 模块下无 Repository 类）没有明确说明。实际路径是 admin 模块直接依赖 common 模块的 entity 包，这是常规的 Maven 依赖，无需特别说明。只是"prescription 模块通过仅查询 Repository 访问"中，这个 Repository 的实现是否跨模块没有争议——是的，因为 Repository 在 prescription 模块内部，读取 DosageStandard 实体不需要跨模块。这实际上是 OK 的。

**所在位置**：§1.2（line 40-41）  
**严重程度**：轻微  
**改进建议**：  
无实际风险，标注仅作为提示

---

## 整体评价

该 OOD 设计文档整体上深度充分，对 4 个业务包的模块划分、核心抽象、API 契约、行为流程、并发模型、降级策略和配置管理提供了完整覆盖。文档经历了多轮内部审议（v8~v19），大量初始质量缺陷已被修正，当前版本在大多数维度上可直接指导编码实现。

主要剩余问题集中在：(1) **AiResult 重载工厂方法存在性描述与代码事实不符**（P1），这是最严重的误导，直接导致编译错误；(2) **§2.3 的 ai-api DTO 字段描述与 §10 的"待扩展"状态未作关联标注**（P2），阅读者无法区分哪些字段已存在；(3) **§3.4 匹配优先级层级数与 §8.4 不一致**（P3），这是修订漏同步造成的结构性矛盾；(4) 若干边界行为细节（P4~P6）尚未精确定义，需要在实现前明确。

修复前 3 个问题（P1/P2/P3）后，该文档即可进入编码阶段。
