# 质量审查报告 — 第 11 轮

审查对象：Phase 2/3 包C/D-AI1/D-AI2/E OOD 设计方案（v11，自 v10 复制）
审查视角：需求响应充分度、整体深度与完整性、可落地性（侧重内部审议未充分覆盖的维度）
排除范围：组件A内部审议已覆盖的类型系统可行性、标准库生态、语言特性可行性等维度

---

## 维度一：需求响应充分度

### R1 [通过] Phase 5 包G OOD 参考文档分析
内部审议第 10 轮已指出此缺口，v11 新增 §1.1c 系统分析了包G 的三个关键兼容性约束（AiService 接口签名可变性、底座分层架构一致性、Store 接口 package 路径），并给出了 AiResultFactory 的兼容方案。本轮确认：需求文档列出的两篇参考文档（包G 初步 OOD、Phase 23 已有草案）均已充分分析。

### R2 [通过] 四个业务包的核心功能覆盖
- 包C：单轮/多轮双对话、规则可配置、Mock 兜底回退科室列表 — 全部覆盖
- 包D-AI1：风险等级差异化阻断（PASS/WARN/BLOCK）、AI 超时回退本地规则（5 条规则链）— 全部覆盖
- 包D-AI2：对话转结构化病历、按科室配置规则、关键字段缺失提示补全 — 全部覆盖
- 包E：剂量阈值告警、与处方审核强耦合同步落地（同一模块内两个子域）— 全部覆盖

**结论**：需求响应充分度合格。

---

## 维度二：整体深度与完整性

### D1 [严重] 处方提交端点仅定义为"简要契约"，缺少结构化 DTO 定义和完整 API 契约

**位置**：§4.2（line 864）处方提交端点注解"Phase 2/3 简要契约定义"；§4.6 结构 API 契约章节（lines 1154-1166）
**问题**：处方提交端点是 BLOCK 阻断链路和 WARN 强制提交的端到端闭环关键（步① CRITICAL 阻断检查 → 步② 审核结果阻断检查 → 步③ forceSubmit 判定），但：  
(a) POST /api/prescription/submit 的请求/响应 DTO 没有正式定义（无类名、无包路径、无字段表），仅以自然语言描述请求参数（prescriptionId, prescriptionItems, forceSubmit, auditRecordId）；  
(b) §4.6 结构化 API 契约中仅有 forceSubmit=true 的 JSON 示例片段，缺少非 forceSubmit 场景和正常提交场景的完整示例；  
(c) 对于"forceSubmit=false 且无最新审核结果"时执行"常规审核流程"的具体路径未定义——是直接调用 PrescriptionAuditService.audit() 再后续判定，还是走另一套简化逻辑？  
(d) "常规提交流程"在无审核结果或 PASS 时做了什么，没有 DTO 层面的定义。
**改进建议**：
  - 为 POST /api/prescription/submit 补充正式 DTO 定义：至少定义 SubmitRequest 和 SubmitResponse（或 SubmitResult）的类名、包路径、完整字段表和约束
  - 在 §4.6 中补充正常提交（无审核结果直接提交）和 forceSubmit=false+WARN 两个场景的完整 JSON 示例
  - 明确"常规审核流程"的技术路径——是复用 PrescriptionAuditService.audit() 还是走简化的内部路径
  - 定义 auditRecordId 为必填/可选的语义边界：forceSubmit=true 时必须提供，forceSubmit=false 时可选

### D2 [严重] visitIdFallback reconciled 任务仅有概念引用，无实现定义

**位置**：§3.3 RecordGenerateRequest（line 638）
**问题**：文档定义了 fallback 写入 visitIdFallback=true 标记，"供 visit 模块后续 reconciled 任务识别并修复"。但：  
(a) 该 reconciled 任务在全文任何地方均无定义——无调度时机、无触发条件、无实现机制、无负责模块；  
(b) 属于"欠债"式设计——当前定义了一个标记，但修复该标记的机制没有定义，意味着这个标记在 Phase 2/3 可能长期存在且永远不会被修复；  
(c) 这可能被实现者忽略，导致 visitId 数据完整性问题（使用 encounterId 作为 visitId 实际上是一个数据类型错误，并非有效降级）。
**改进建议**：至少补充以下之一：
  - option A: 定义该 reconciled 任务——在 §6.1 定时任务集中管理中增加一条定期任务（如每 30 分钟），扫描 visitIdFallback=true 的 MedicalRecord，通过某种机制尝试恢复正确的 visitId（如通过 encounterId 反查 visit 模块），成功则清除标记。需明确触发 visit 模块哪个接口查询
  - option B: 若 Phase 2/3 不实现 reconciled，则明确标注"Phase 2/3 仅设置标记，reconciled 修复推迟至 Phase 4/5 实现"，并在受影响的 MedicalRecord 上标注 visitId 可能为 encounterId fallback 的说明

### D3 [严重] DialogueSession 事务一致性策略未完整覆盖 QA 历史等状态丢失

**位置**：§3.1 事务一致性策略段落（line 426）
**问题**：文档承认了"事务提交成功到内存更新之间进程崩溃会导致 TriageRecord 已持久化但 DialogueSession 未同步更新"，但仅评估了 aiFailCount 的影响（"aiFailCount 丢失不影响业务正确性"）。然而 DialogueSession 还维护了：
(a) QA 历史列表（每轮追问的 question/answer/prompt）— 丢失后多轮上下文拼接不完整，下一个 AI 调用的输入将缺少上一轮的信息；
(b) correctedChiefComplaint — 丢失后主诉修正信息丢失，后续推理使用错误的主诉；
(c) 对话轮次计数器（如果隐含在列表中）— 丢失后截断策略可能失准。
**改进建议**：扩展事务一致性策略覆盖 DialogueSession 的全部可变状态。可选的解决路径：
  - 路径 A: 承认除 aiFailCount 外 QA 历史等状态同样存在丢失风险，但在多轮分诊场景（典型 2-5 轮）下概率低，不做额外防护，显式声明可接受的数据丢失范围（不仅限于 aiFailCount）
  - 路径 B: 对 TriageRecord 也存储关键上下文快照（如"累计字符数"或 QA 历史索引），会话重建时从 TriageRecord 恢复 DialogueSession 的部分状态
  - 路径 C: 简化为"多轮场景下每次 AI 调用前先通过 AiService 调用获取结果，写入 TriageRecord 后再追加 QA 历史到 DialogueSession"，若崩溃则恢复的会话从 TriageRecord 读取上一次完整状态的快照

### D4 [一般] AiResultFactory 类未在目录结构中注册

**位置**：§1.1c（line 37）、§10（line 1575）
**问题**：§1.1c 和 §10 反复提出将 AiResult 重载工厂方法移至独立的 AiResultFactory 静态工厂类以兼容 Phase 5 包G 的"AiResult.java 不变"约束。但该工厂类：  
(a) 未出现在 §2.1 目录结构的 ai-api 模块子树中；  
(b) 无指定的包路径声明；  
(c) 无具体的方法签名定义（只是说"移至工厂类"，但工厂类应该有怎样的静态方法签名？——如 `static <T> AiResult<T> failure(String errorCode, T partialData)` 等）。  
这是一个实现方案的落地缺口——实现者知道要做什么，但不知道放在哪里、签名如何。
**改进建议**：
  - 在 §2.1 ai-api 模块目录下补充 AiResultFactory.java 条目（如 com.aimedical.modules.ai.api.AiResultFactory）
  - 在 §2.3 AiService 接口定义之后或 §1.1c 中，给出 AiResultFactory 的简要方法签名——至少包含 failure()、degraded() 重载的签名和语义

---

## 维度三：可落地性（直接指导编码实现）

### I1 [一般] 缺少领域事件统一目录

**位置**：全文散布
**问题**：文档中定义了 10+ 种领域事件（RegistrationEvent, TemplateConfigChangeEvent, TriageRuleChangeEvent, DrugContraindicationChangeEvent, DrugAllergyMappingChangeEvent, DrugCompositionDictChangeEvent, DosageStandardChangeEvent（预留）, DrugInteractionPairChangeEvent（Phase 4）等），但无一处集中列出所有领域事件及其发布者、消费者、触发条件的完整目录。新开发者或接手团队需要从全文检索才能了解事件驱动的全貌。
**改进建议**：在 §2.2"跨模块事件传递机制"或 §9.3 规则管理接口后新增一节"领域事件目录"，以表格形式列出：
| 事件名称 | 定义位置 | 发布模块 | 消费模块 | 触发条件 | 事务边界 | Phase 范围 |
|---------|---------|---------|---------|---------|---------|-----------|

### I2 [一般] 错误码→HTTP 状态码映射缺少集中声明

**位置**：§5.1 错误码表（line 1300-1309）；§4.x 各处散落
**问题**：文档仅在 §4.x 的行为契约中散落标注了部分错误码对应的 HTTP 状态码（如 TRIAGE_FIELD_COMBINATION_INVALID→400、RX_AUDIT_CONCURRENT_SUBMIT→409、BLOCK→422），但没有在 §5 错误处理策略中建立统一的"错误码→HTTP 状态码"映射表。实现者在开发 GlobalExceptionHandler 时需要从全文搜集映射关系，容易遗漏。
**改进建议**：在 §5.1 错误码表之后增加一列"HTTP 状态码"，或新增一张"错误码→HTTP 状态码映射表"，至少覆盖所有已在行为契约中关联状态码的条目。

### I3 [一般] 六级匹配优先级 Level 2 边界规则的实现复杂度已升高

**位置**：§8.4（line 1500）
**问题**：v11 已根据前轮审查补充了 Level 2 的"部分 null 边界规则"，包含多级条件分支（"若 ageRange 非 null 但 weightRange 部分为 null → 降级至 Level 3"、"若 ageRange 完全 null 而 weightRange 部分为 null → 降级至 Level 5"等）。该逻辑在实现时容易产生偏差，特别是当多个字段组合产生 2^4 = 16 种 null/非 null 组合状态时。缺少测试用例级别的行为规格。
**改进建议**：在 §8.4 补充匹配过程的伪代码或决策表（decision table），列出年龄范围/体重范围 null 组合的关键路径和预期降级优先级。建议在 §8 末尾标注"实现时需覆盖的边界测试用例"。

### I4 [轻微] DosageStandard 数据库索引未定义

**位置**：§8.4 DosageStandard 字段定义（line 1486-1495）
**问题**：六级匹配优先级每次 check-dose 调用都需按 drugCode + routeOfAdministration 查询 DosageStandard，再在结果集中应用年龄/体重范围过滤。但 §8 对 DosageStandard 实体的索引没有任何定义。DosageStandard 如果规模较大（数千药品 × 多条标准记录），缺少组合索引（drugCode, routeOfAdministration）可能导致全表扫描。这是一个性能隐患。
**改进建议**：在 §8 补充索引说明——建议对 DosageStandard 的 (drugCode, routeOfAdministration) 建立复合索引，加速匹配查询的过滤阶段。

### I5 [轻微] MockAiService 运行时策略切换的并发安全边界未定义

**位置**：§2.3 MockAiService 实现契约（line 383-388）
**问题**：MockAiService 支持运行时通过端点动态切换 STATIC/AI_UNAVAILABLE/TIMEOUT 三种策略，但未定义：  
(a) 切换时正在执行的 AI 调用使用的是旧策略还是新策略（即策略读取的原子性边界在哪里）；  
(b) 策略存储的键/值结构和刷新机制；  
(c) 多线程场景下策略切换对正在运行的异步 CompletableFuture 调用的影响。
**改进建议**：补充策略切换的可见性保证——如使用 volatile 或 AtomicReference 存储策略枚举，确保切换后后续调用的调用线程立即可见。说明"正在执行的调用不受策略切换影响"（使用调用开始时读取的缓存策略值）。

### I6 [轻微] 统一错误格式与 BlockResponse 的边界未覆盖所有提交端点错误

**位置**：§4.6 HTTP 错误响应包装格式（lines 1261-1286）
**问题**：文档定义：(a) 所有 4xx/5xx（BLOCK 阻断的 422 除外）使用统一错误格式；(b) BLOCK 阻断使用独立的 BlockResponse 格式（422）。但处方提交端点步③中可能返回的错误（如 RX_AUDIT_PRESCRIPTION_MODIFIED 是业务错误而非 BLOCK 阻断、RX_AUDIT_CONCURRENT_SUBMIT 返回 409）——它们是否使用统一错误格式？文档未明确。
**改进建议**：在 §4.6 补充说明——"处方提交端点中步①/步②阻断使用 BlockResponse（422）；步③中 forceSubmit 校验失败（如处方被修改、并发提交检测命中）使用统一错误格式 xxx"。

---

## 总结

| 编号 | 严重程度 | 概要 | 类别 |
|------|---------|------|------|
| D1 | 严重 | 处方提交端点缺少正式 DTO 定义和完整 API 契约 | 深度不足 |
| D2 | 严重 | visitIdFallback reconciled 任务仅概念引用无实现定义 | 完整性缺口 |
| D3 | 严重 | DialogueSession 事务一致性未覆盖 QA 历史等状态的丢失 | 深度不足 |
| D4 | 一般 | AiResultFactory 未在目录结构中注册 | 完整性缺口 |
| I1 | 一般 | 缺少领域事件统一目录 | 可落地性 |
| I2 | 一般 | 错误码→HTTP 状态码映射缺少集中声明 | 可落地性 |
| I3 | 一般 | 六级匹配优先级 Level 2 边界规则的实现复杂度 | 可落地性 |
| I4 | 轻微 | DosageStandard 索引未定义 | 性能隐患 |
| I5 | 轻微 | MockAiService 策略切换并发安全边界未定义 | 可落地性 |
| I6 | 轻微 | 统一错误格式与 BlockResponse 的边界未覆盖所有提交场景 | 可落地性 |

**总体评价**：本设计经过 10 轮内部审议，需求响应充分度已过关，四个业务包的核心功能均被覆盖，底座落地和 Phase 5 迁移策略已较成熟。但仍存在三个严重级别的完整性缺口：处方提交端点的 DTO 定义缺失、visitIdFallback reconciled 的悬空引用、DialogueSession 事务一致性策略的覆盖不足。这三项问题不解决，实现者将无法仅依赖此设计文档独立完成编码。建议优先处理 D1 和 D3（直接影响实现可用性），D2 可通过标注推迟至 Phase 4 降低优先级。
