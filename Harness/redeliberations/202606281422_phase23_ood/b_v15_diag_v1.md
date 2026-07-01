# 质量审查报告：Phase 2/3 OOD 设计 v15

## 审查概况

- **产出版本**：a_v15_copy_from_v14.md（第 15 轮迭代）
- **审查视角**：需求响应充分度、全局一致性与完整性、编码落地可行性
- **审查维度**：前述 14 轮内部审议已覆盖大量局部细致问题，本审查侧重跨模块/跨章节的整体性问题和边缘场景缺口

---

## 发现的问题

### 问题 1：DosageStandard 五级匹配策略存在未覆盖路径 — 先混合维度后降级路径丢失

- **所在位置**：§8.4 五级匹配优先级
- **严重程度**：严重
- **问题描述**：五级匹配的定义为：Level 1（精确）→ Level 2（仅年龄范围）→ Level 3（仅体重范围）→ Level 4（均 null）→ Level 5（未命中）。当一条 DosageStandard 记录**同时设置了 ageRange 和 weightRange**（如 `ageRangeStart=18, ageRangeEnd=65, weightRangeStart=50, weightRangeEnd=80`），而患者条件不满足 Level 1 的精确匹配要求（`ageRangeStart=ageRangeEnd` 且 `weightRangeStart=weightRangeEnd`）时，该记录既不匹配 Level 2（weightRange 非 null）、也不匹配 Level 3（ageRange 非 null）、也不匹配 Level 4（两个维度均非 null），将被跳过至 Level 5 报"标准不存在"。但此记录本应合理匹配（如患者 30 岁/70kg 显然应在覆盖范围内）。这是一个算法级的假阴性缺陷，会导致临床正确剂量规则被错误跳过。
- **改进建议**：补充 Level 2.5 或重新定义匹配优先级：(a) 当 ageRange 和 weightRange 同时存在时，新增独立优先级（建议插入为 Level 2），要求患者年龄和体重**同时落入**各自区间即视为匹配（非强制精确相等）；(b) 或采用评分制——对多条可能命中的记录分别计算匹配度，选择匹配度最高的记录；(c) 至少需补充文字说明：ageRange 和 weightRange 同时非 null 时应如何处理。

---

### 问题 2：DoctorFacade 跨模块同步调用缺失降级保护

- **所在位置**：§3.1 TriageService 协作描述 + §4.1 推荐医生列表生成
- **严重程度**：严重
- **问题描述**：DoctorFacade.findAvailableDoctorsByDepartment() 在设计中是 TriageServiceImpl 的强制同步调用路径。但以下场景全无定义：(a) DoctorFacade 调用超时的超时阈值和失败处理；(b) Doctor 模块不可用时（如网络故障、服务宕机）TriageServiceImpl 的替代策略；(c) DoctorFacade 抛出异常时，TriageResponse.doctors 是否置空列表返回还是整体降级返回错误。对比同一模块中 RegistrationEvent 消费端拥有完整的 @Retryable + @Recover + DeadLetterEvent + 定时补偿任务四层保护机制，DoctorFacade 作为同步调用链路同样重要的依赖却没有任何降级保护，形成实质性设计不对称——核心依赖无容错路径。
- **改进建议**：在 §3.1 或 §4.1 中补充：(a) DoctorFacade 调用的超时配置（建议默认 2s）；(b) 调用失败/超时时将 TriageResponse.doctors 置空列表 + 在 reason 或独立字段中说明"医生排班信息暂不可用"，仍正常返回其他分诊结果；(c) 补充 DoctorFacade 调用失败的日志级别和可观测性标记（如 TriageResponse.monitoringTags）。

---

### 问题 3：MissingFieldDetector 的 null/空字符串判定语义未定义

- **所在位置**：§3.3 MissingFieldDetector 职责 + §4.3 病历生成场景 + §10.3 MedicalRecordGenResponse
- **严重程度**：一般
- **问题描述**：MissingFieldDetector 定义为"基于科室模板的必填字段列表与 AI 输出实际包含字段进行差集比对"。但以下边界判定未定义：(a) AI 返回空字符串 "" 的字段，应计为"已包含"还是"缺失"？(b) ai-api 层 MedicalRecordGenResponse（§10.3）各字段为 string 类型，JSON 响应中字段缺失 vs 显式 null vs 空字符串，行为是否一致？(c) 差集比对是基于"字段在 Map 的 keySet 中存在"还是"字段值非 null 且非空"？此缺口直接导致不同实现者写出不同逻辑（保守方可能将空字符串判为缺失，开放方可能判为包含），且影响对降级路径"分层保护"策略的判断——若 AI 生成的空字符串不被视为缺失，则模板配置中标记为必填的字段可能漏检。
- **改进建议**：在 §3.3 MissingFieldDetector 或 §4.3 中明确：(a) 判定策略为"基于字段值的非空非 null 存在性"——字段在 AI 响应中不存在、值为 null、值为空字符串三者均视为"缺失"；(b) 补充此策略对"分层保护"降级路径的影响说明——AI 超时时返回的 partial data 中字段值为 null 的均视为缺失。

---

### 问题 4：多轮分诊中 DialogueSession 不支持主诉修正

- **所在位置**：§3.1 DialogueSession 职责 + 全量拼接策略
- **严重程度**：一般
- **问题描述**：DialogueSession 在首轮创建时固定记录 chiefComplaint，后续仅附加 additionalResponses。全量拼接策略（§3.1）为"保留首轮主诉 + 最近 N 轮 QA"。当多轮对话中患者修正了初始主诉（例如首轮说"胸痛"→第二轮说"不是胸痛，是左臂放射痛"），DialogueSession 无法更新/覆盖 chiefComplaint 字段。全量拼接时首轮主诉保持原样，修正信息仅存在于某一轮 QA 中，导致 AI 调用时收到矛盾的临床上下文。分诊场景虽通常轮次少，但此设计缺口在 3 轮以上场景即可实际触发。
- **改进建议**：(a) 在 DialogueSession 中增加 `correctedChiefComplaint` 可选字段，定义为"最近一轮回复中明确包含对主诉修正内容时，服务端自动检测并更新此字段"；(b) 或在全量拼接策略中明确说明：当修正发生时的行为由 AI 自身处理（需要 AI 理解多轮对话中的修正语义），并在 §7 记录此设计决策。

---

### 问题 5：forceSubmit=false 路径缺少对处方已修改的感知

- **所在位置**：§4.2 处方提交端点行为
- **严重程度**：一般
- **问题描述**：§4.2 定义 forceSubmit=false 时"若已有 WARN 级别最新审核结果，不重复调用 AI 审核，直接返回当前 WARN 审核结果"。但若处方已在 WARN 审核后发生了修改（新增/删除/调整药品），此路径返回的仍是基于旧处方的审核结果，导致医生误以为当前处方仅 WARN 级别风险。forceSubmit=true 路径有 originalPrescription 版本校验防护，但 forceSubmit=false 路径无此校验——流程上医生可能在处方已修改的情况下先看到旧审核结果，然后走 forceSubmit=true 才发现"处方已变更，请重新审核"，形成不良用户体验。
- **改进建议**：在 forceSubmit=false 路径中增加一次轻量级的内容变更检测（比对当前处方与 AuditRecord.originalPrescription 的结构化 MD5/哈希），若发现不一致则提示"处方内容已变更，请重新审核"而非直接返回旧结果。

---

### 问题 6：CRITICAL 告警前端同步更新的竞态窗口

- **所在位置**：§3.4 PrescriptionDraftContext 覆盖更新行为 + §4.2 CRITICAL 阻断合并判定
- **严重程度**：一般
- **问题描述**：PrescriptionDraftContext 的"全量覆盖"语义定义了每次 check-dose 重算后清除旧 CRITICAL 标记并写入新标记。但 CRITICAL 告警在前端展示（由 check-dose 响应返回）与提交时检查（由 PrescriptionDraftContext 后端状态判定）之间存在时序窗口：若两次 check-dose 之间 CRITICAL 告警已被清除但前端仍展示旧告警，或前端未收到新 CRITICAL 告警但后端上下文已有标记，导致前端展示与后端阻断行为不一致。
- **改进建议**：在 §4.4 check-dose 流程中补充 PrescriptionDraftContext 状态变更通知机制——每次 check-dose 响应中携带 `contextCriticalCount` 字段（后端当前 CRITICAL 标记数），前端以此判断是否需要同步刷新状态。或在 §6 并发设计中补充前端同步协议的简要说明。

---

### 问题 7：DosageUnitGroup 换算精度与浮点数比较规则缺失

- **所在位置**：§8.3 剂量单位分组
- **严重程度**：轻微
- **问题描述**：§8.3 的换算系数表覆盖了分组和系数，但未定义：(a) 换算后的比较精度——换算至基准单位后的数值比较采用 Java `BigDecimal.compareTo()` 还是 `equals()`，scale 和 roundingMode 如何设置；(b) 比较 tolerance——临床场景中 500.001mg 与 500mg 的微小差异是否有允许的浮动范围（如 ±0.5%）；(c) 换算溢出——kg→mg 换算后数值可达数千万，是否超出业务数值范围。
- **改进建议**：在 §8.3 补充一段实现指导说明——建议采用 `BigDecimal` + `compareTo()`（忽略 scale），roundingMode 统一为 `HALF_UP`；换算系数常量化为 `BigDecimal` 类型。若认为此细节不属 OOD 范围，可明确标注"实现细节由编码阶段确定"。

---

### 问题 8：ai-api DTO 扩展前置依赖的迭代风险未设缓冲

- **所在位置**：§10 开篇时序依赖说明
- **严重程度**：轻微
- **问题描述**：§10 声明 ai-api 层 DTO 扩展是四个业务模块开发的前置依赖，但注释为"同一团队同一迭代同步开发，不涉及跨团队接口冻结"。此安排未提供：(a) 若 ai-api DTO 扩展延误时业务模块开发的替代方案（如基于接口桩/契约测试先行开发）；（b）ai-api DTO 在迭代中发生破坏性变更时 Converter 的追踪与回归策略。在当前"同一团队并行开发"假设下，一旦发生时序冲突或变更失联，将成为编码阶段的串行阻塞点。
- **改进建议**：在 §10 补充：(a) 建议的并行开发顺序——先完成 ai-api DTO 骨架字段定义（空壳→字段→测试），再并行 Converter→Service 开发；(b) DTO 变更通知机制——在 ai-api DTO 字段变更时通过常规代码审查确保所有 Converter 同步更新。

---

## 整体评价

该设计文档经 14 轮迭代审议后在细节丰富度和问题收敛度上达到较高水平，4 个包的模块划分、核心抽象定义和关键行为契约已基本满足编码启动条件。前述 8 个问题集中在三类缺口：

1. **算法级边缘路径缺失**（问题 1、4、7）——匹配策略/修正策略/换算策略中未覆盖的边界场景，可能在运行时产生假阴性或行为歧义；
2. **跨模块可靠性不对称**（问题 2、6）——同步调用与异步事件的容错等级不一致，需要在下游模块依赖层补充降级语义；
3. **条件分支的时序安全**（问题 3、5）——多路径逻辑中先决条件变更后的路径选择缺乏校验。

建议在进入编码前至少解决问题 1（算法缺陷）和问题 2（容错缺口），其余问题可在编码阶段通过补充实现说明解决。
