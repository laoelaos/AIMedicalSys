根据以下审查结果，迭代上一轮的产出，形成新版的文件，从而更好地满足用户需求。

## 当前审查结果

### 问题 1（严重）：§3.1 DialogueSessionManager 详细设计与 §1.1/§6.1 Store 抽象层强制约束矛盾
§1.1 和 §6.1 明确将 Store 抽象层定义为"设计强制项"，要求三项存储必须通过 Store 接口（SessionStore、SuggestionStore、DraftContextStore）间接访问。然而 §3.1 DialogueSessionManager 仍表述为"内部维护 ConcurrentHashMap"且包含"interface 抽象收益不抵实现复杂度"的论证。此矛盾导致两层次问题：(a) Store 抽象已从"建议"升级为"强制"后该论证已失效但未删除；(b) 详细设计仍直接操作 ConcurrentHashMap 而非 SessionStore 接口，实现者按此编码将违背架构约束，导致 Phase 5 迁移时必须重构 DialogueSessionManager 而不仅仅是替换 Store 实现。
- 改进建议：将内部存储引用替换为 SessionStore 接口（注入 ConcurrentHashMapStore 实现），删除或改写否定 interface 抽象价值的论证，同步补充 Store 接口及实现类的包路径。

### 问题 2（严重）：异步 AI 建议流程缺少 AiResult → AiSuggestionResult 映射逻辑定义
§6.3 定义了 check-dose 的异步 AI 调度，§3.4/§4.4 定义了 AiSuggestionResult（含 PENDING/COMPLETED/FAILED 状态），但 AiService.prescriptionAssist() 返回的 `CompletableFuture<AiResult<...>>` 与 AiSuggestionResult 之间的映射逻辑未定义。具体缺失包括：(a) AiResult.success=true → COMPLETED 时 data 如何映射到 suggestion；(b) AiResult.success=false → FAILED 时 errorCode/fallbackReason 如何映射到 failReason；(c) AiResult.degraded=true 归入 COMPLETED 还是 FAILED；(d) partialData 何时写入及格式定义。
- 改进建议：新增明确的 AiResult → AiSuggestionResult 映射规则表，覆盖 COMPLETED/FAILED/DEGRADED 三路径，同步补充 partialData 写入时机和 JSON 格式定义。

### 问题 3（严重）：产出未参考需求中列明的 Phase 23 已有 OOD 草案
需求文档明确列出了"本阶段已有 OOD 草案：`Harness\redeliberations\202606281422_phase23_ood\a_v19_copy_from_v18.md`"作为输入参考。当前产出全文中未引用或提及该草案，无法判断设计继承关系和有意偏离的理由。
- 改进建议：在 §1 概述中增加对 Phase 23 已有 OOD 草案的追迹说明，或在 §7 设计决策表中标注与草案的一致/差异关系。

### 问题 4（一般）：§1.3 DosageCheckRequest.prescriptionId "必填"约束与 §3.4 后端自动生成 fallback 路径矛盾
§1.3 将 prescriptionId 描述为"必填"，但 §3.4 "prescriptionId 分配时机"段定义了后端按需生成 fallback 路径——若 prescriptionId 为空时后端自动生成。如果 API 契约层面为必填，API 验证层将在请求到达 Service 前拒绝空值，后端生成逻辑永不触发。
- 改进建议：将约束改为"主路径必填，空值时由后端自动生成并回写"，或拆分为两个子场景明确定义，并在 §3.4 中明确 API 验证层应跳过 @NotNull 校验。

### 问题 5（一般）：所有 API 端点缺少结构化契约定义
当前端点契约均以自然语言描述，缺少结构化 JSON Schema/OpenAPI 规格。具体缺失包括：(a) 请求 JSON 顶层结构和嵌套层级未以 JSON 示例明确；(b) HTTP 错误响应体格式未定义；(c) 端点 HTTP 状态码集合未完整枚举；(d) 认证头/Content-Type 等说明缺失。
- 改进建议：在 §4 后或新增章节集中给出各端点的完整请求/响应 JSON 示例，明确定义错误响应包装格式，明确前端/QA 获取完整 API 契约的方式。

### 问题 6（一般）：AiSuggestionResult TTL 与 PrescriptionDraftContext TTL 一致性已修复，但 DedupTaskScheduler 去重判定仍可能因 TTL 差异导致逻辑残差
去重逻辑依赖前端 consumed 标记和 TTL 过期清理。当 AiSuggestionResult 在 TTL 过期后被清理但前端尚未消费（consumed=false），新的 check-dose 调用创建新 task——但前端仍可能持有旧 taskId 并发起查询，导致 RX_ASSIST_SUGGESTION_NOT_FOUND。设计中未定义 TTL 过期后的前端重试/刷新策略。
- 改进建议：定义 TTL 过期与 consumed 标记的协调策略——前端查询 TTL 过期时返回特定错误码触发重试，或延长 TTL 以 consumed 标记为唯一释放条件。

### 问题 7（一般）：§5.1 错误码表缺少若干边界场景的错误码
缺少以下错误码：(a) 分诊场景 DoctorFacade 调用超时——TRIAGE_DOCTOR_FACADE_UNAVAILABLE；(b) 病历生成模板加载失败——MR_GEN_TEMPLATE_LOAD_FAILED；(c) AI 异步建议创建失败——RX_ASSIST_AI_SUGGEST_CREATE_FAILED；(d) 处方提交并发冲突——RX_AUDIT_CONCURRENT_SUBMIT。
- 改进建议：补充上述错误码至 §5.1 并关联各章节，或在相关章节显式声明复用哪个现有错误码。

### 问题 8（轻微）：§2.1 目录结构遗漏 DeadLetterEventRepository.java
§2.2 明确定义了 DeadLetterEventRepository 的存在，但 §2.1 consultation 模块的 repository 目录行中未列出 DeadLetterEventRepository.java。
- 改进建议：在 §2.1 consultation/repository/ 目录行中补充 DeadLetterEventRepository.java 条目。

## 历史迭代回顾

### 已解决的问题（出现在历史反馈但当前反馈中不再提及）
- 历史第1轮问题1~10（sessionId 字段缺失、AllergyWarningSeverity 枚举不匹配、DosageThresholdService 优先级标注不一致、过敏冲突检查归属未定义、encounterId→visitId 转换未定义、DialogueSession 状态更新事务一致性、Phase 5 迁移断言矛盾、LocalRuleEngine 规则计数不一致、CRITICAL/BLOCK 竞态、PrescriptionAssistResponse errorCode 缺失）——已修复，本轮不再提及。
- 历史第2轮问题1~7（JPA @Id 字段缺失、手动选科覆盖优先级机制缺失、AI 感知截断标记缺失、DrugFacade 未定义、外部依赖未文档化、DeadLetterEvent JPA 字段定义不完整、AiResult 工厂方法与 Phase 5 潜在冲突）——已修复，本轮不再提及。
- 历史第3轮问题3~7（规则可配置依赖 admin 模块无时间线协调、AiResult.data=null 边界处理、撤销审核端点未定义、prescriptionId 传递路径未闭环、AI Mock 实现行为契约缺失）——已修复，本轮不再提及。
- 历史第4轮问题1~5（sessionId 跨模块传播路径未闭合、VisitFacade 降级缺失、FieldMissingHint 生成规则未定义、遗漏错误码——已在第5轮重新出现为问题7、MedicalRecord.contentJson 并发写更新丢失）——已修复，其中问题4（遗漏错误码）在本轮重新出现为问题7。
- 历史第5轮：所有问题均对应本轮问题1~7，未出现独立已修复项。

### 持续存在的问题（在多轮反馈中反复出现，需重点解决）
- **Store 抽象层矛盾**（历史第3轮问题1 → 历史第5轮问题1 → 本轮问题1）：Store 抽象从"建议"到"强制"已历三轮，但 §3.1 DialogueSessionManager 仍未迁移至 SessionStore 接口。需在本轮彻底修复。
- **异步 AI 结果映射逻辑缺失**（历史第3轮问题2 → 历史第5轮问题2 → 本轮问题2）：从去重/节流策略到具体映射逻辑，问题范围逐渐收敛但核心缺口持续存在。需在本轮明确定义 AiResult → AiSuggestionResult 映射规则表。
- **未引用 Phase 23 OOD 草案**（历史第5轮问题3 → 本轮问题3）：已连续两轮指出但未修复。需在本轮补充追迹说明。
- **prescriptionId 约束矛盾**（历史第3轮问题6 → 历史第5轮问题4 → 本轮问题4）：从"传递路径未闭环"演变为"必填约束与 fallback 互斥"，矛盾持续存在。需在本轮统一约束语义。
- **API 契约形式化不足**（历史第5轮问题5 → 本轮问题5）：第一轮提出，持续未解决。需在本轮补充结构化 JSON 示例或约定获取方式。
- **错误码表不完整**（历史第4轮问题4 → 历史第5轮问题7 → 本轮问题7）：持续三轮未完全闭合。需在本轮补充所有遗漏错误码。
- **TTL 与 consumed 标记时序协调缺口**（历史第3轮问题2 → 历史第5轮问题6 → 本轮问题6）：从去重策略演变为 TTL 过期协调，问题持续存在。需在本轮明确定义协调策略。

### 新发现的问题（本轮新识别）
- **问题8**（轻微）：§2.1 目录结构遗漏 DeadLetterEventRepository.java——本轮首次识别。

## 上一轮产出路径
C:\Develop\Software\AIMedicalSys\Harness\redeliberations\202606290200_phase23_pkgCDE_ood\a_v5_copy_from_v4.md

## 用户需求
C:\Develop\Software\AIMedicalSys\Harness\redeliberations\202606290200_phase23_pkgCDE_ood\requirement.md
