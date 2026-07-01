根据以下审查结果，迭代上一轮的产出，形成新版的文件，从而更好地满足用户需求。

## 当前审查结果

### 严重问题

1. **[严重] 合并症-药品禁忌检查遗漏**：需求文档 §3.4.2 检查项 #2 明确要求"是否与患者的合并症冲突"，设计文本 §3.2 AllergyCheckRule 仅实现过敏史冲突检查，合并症-药品禁忌检查无对应规则实现，也无独立规则类覆盖。
   - 所在位置：§3.2 LocalRuleEngine 实现范围表 + AllergyCheckRule 描述
   - 改进建议：(a) 新增 ContraindicationCheckRule（或扩展 AllergyCheckRule 为 AllergyAndContraindicationCheckRule），补充合并症-药品禁忌检查子项：遍历处方药品列表，对每个药品查询其禁忌症列表，与 patientInfo.comorbidities 做交集比对。命中时产出 WARN 或 BLOCK 级别 LocalRuleResult（severity 判定逻辑可参考 AllergyCheckRule 的分级策略）。(b) 需同步更新 §3.2 实现范围表、§2.1 目录结构和 §7 设计决策，补充禁忌症数据来源实体（如新增 DrugContraindicationMapping JPA @Entity 或复用现有 DrugAllergyMapping 的扩展结构）。

2. **[严重] AiResult 超时降级路径歧义**：AiResult 源码仅含 success/data/errorCode/degraded/fallbackReason 五个字段，failure() 和 degraded() 工厂方法均将 data 设为 null。设计文本 §3.3 声明"AI 超时时 AiResult.data 携带部分生成结果（AiResult 新增 partialData 字段或使用现有 data 字段承载部分结果）"，此描述具有歧义——既提到"新增 partialData 字段"又提到"使用现有 data 字段"，未做明确选择。更关键的是，需求文档 §3.4.3 明确超时场景需同时携带 errorCode（MR_GEN_AI_TIMEOUT）和部分数据（partial_content），当前 AiResult 的 failure() 和 degraded() 工厂方法均将 data 设为 null，无法同时传递错误码和部分数据。§7 仅建议为 degraded() 增加重载，遗漏了 failure() 路径——超时场景是 failure+errorCode+partialData 的组合。
   - 所在位置：§3.3 MedicalRecordService "非流式超时降级路径"；§7 设计决策 "AiResult 超时降级模式"
   - 改进建议：明确选择使用现有 AiResult.data 字段承载部分结果，并删除"新增 partialData"歧义描述。需覆盖两条路径：(a) 超时场景（failure+errorCode+partialData）：AiResult.failure() 新增重载 `failure(String errorCode, T partialData)` 或由 AI 实现直接构造 `AiResult(success=false, partialData, errorCode, degraded=false, fallbackReason=null)`；(b) 降级场景（success=false + degraded=true + data 含部分结果 + fallbackReason）：AiResult.degraded() 新增重载 `degraded(String fallbackReason, T partialData)`。在 §3.3 和 §7 中明确此选择并删除"新增 partialData 字段"描述。

3. **[严重] TriageRecord 缺推荐医生快照字段**：需求文档 §5.1 定义"分诊记录"实体核心字段含"推荐医生"，设计文本 §3.1 TriageRecord 包含 aiRecommendedDepartments 和 ruleMatchedDepartments，但缺少 recommendedDoctors 快照字段。推荐医生列表是 TriageResponse 的核心输出之一，TriageRecord 未持久化推荐医生数据将导致分诊结果的可观测性和事后追溯不完整。
   - 所在位置：§3.1 TriageRecord 实体字段列表
   - 改进建议：在 TriageRecord 中增加 recommendedDoctors（JSON TEXT）字段，存储 TriageResponse.doctors 列表快照（含 doctorId / doctorName / departmentId / availableSlotCount / score）。

### 一般问题

4. **[一般] DoctorFacade 包名不匹配**：DoctorFacade 放置于 `common-module-api/.../auth/` 包下，但 auth 包的语义定位是"用户认证相关门面"。DoctorFacade 是跨模块医生排班/可用性查询门面，与"认证"包名不匹配。
   - 所在位置：§2.1 目录结构；§1.3 核心抽象一览 DoctorFacade 条目
   - 改进建议：在 common-module-api 下为 DoctorFacade 创建独立子包（如 `commonmodule/doctor/DoctorFacade.java`），与 auth 包解耦。

5. **[一般] matched_rules 子字段设计决策缺失**：需求文档 §3.4.1 matched_rules 字段后无子结构定义（需求侧缺口），设计侧主动定义了 ruleId/ruleName/score 三个子字段，但未在 §7 设计决策中说明这是设计侧的主动选择（而非被动对齐），也未论证子字段选择理由（为何含 score 而不含 confidence/reason 等）。
   - 所在位置：§1.3 核心抽象一览 MatchedRule DTO；§10.1 ai-api 层 MatchedRuleItem；§7 设计决策表
   - 改进建议：在 §7 设计决策中增加 matched_rules 子字段的设计决策条目，说明：(a) 需求文档 §3.4.1 matched_rules 子结构未定义（需求侧缺口）；(b) 设计侧主动定义 ruleId / ruleName / score 三个子字段，其中 score 统一承载匹配评分；(c) 若后续需求补充 confidence / reason 子字段，可通过 DTO 扩展兼容。

6. **[一般] AuditAlert.severity 字段类型和值域未定义**：AuditAlert 列出 alertCode / alertMessage / severity 三个字段名，但 severity 字段的类型和值域未定义。severity 应采用 AuditRiskLevel（PASS/WARN/BLOCK）还是独立枚举（AlertSeverity: INFO/WARNING/CRITICAL）不明确。alerts.severity 是单条提示的严重程度，与 AuditRiskLevel（处方整体风险等级）是不同维度，不应复用同一枚举。
   - 所在位置：§1.3 AuditAlert DTO；§3.2 AuditResponse；§10.2 ai-api 层 AlertItem
   - 改进建议：在 §1.3 / §3.2 AuditAlert 定义中补充 severity 字段的类型和值域。推荐方案：severity 类型为 String 或独立枚举（如 AlertSeverity: INFO / WARNING / CRITICAL），与 AuditRiskLevel 是不同维度。需同步更新 §10.2 ai-api 层 AlertItem.severity 定义。

7. **[一般] DosageAlert 无错误码字段**：DosageThresholdService 描述"四级均未命中，降级返回 WARN 级 DosageAlert 并携带 RX_ASSIST_DOSE_STANDARD_NOT_FOUND 错误码"，但 DosageAlert 类定义仅含 alertLevel/alertMessage/drugCode/当前剂量/建议值，无承载错误码的字段。DosageCheckResponse 也只有 alerts + taskId 两个字段。
   - 所在位置：§3.4 DosageAlert 类定义；§3.4 DosageCheckResponse；§4.4 check-dose 流程
   - 改进建议：在 DosageAlert 或 DosageCheckResponse 中增加 errorCode（String，可选）字段，统一承载各类剂量校验相关错误码。在 §4.4 check-dose 流程中明确 RX_ASSIST_DOSE_STANDARD_NOT_FOUND 的传递路径。

8. **[一般] 处方版本校验"一致"的比较语义未定义**：§4.2 WARN 级强制提交时需校验"当前处方与 AuditRecord.originalPrescription 是否一致"，但未定义"一致"的比较语义——JSON 全文二进制比较 vs 业务字段结构化比较。JSON 文本级差异（字段顺序、null 与缺失字段、数值精度）可能产生误判。
   - 所在位置：§4.2 处方提交端点"处方版本校验"逻辑
   - 改进建议：补充"一致"的比较语义定义：按业务字段做结构化比较（drugId + dose + frequency + duration + route 五字段组合比对），忽略 JSON 文本级的格式差异。药品增删、剂量变化等业务实质变更判为"不一致"；仅 JSON 序列化格式差异判为"一致"。

9. **[一般] allergyHistory/allergyDetails 数据来源语义不一致**：AuditRequest.patientInfo 和 PrescriptionAssistRequest.patientInfo 均包含 allergyHistory（string）和 allergyDetails（List\<AllergyDetail\>），但数据来源语义与需求文档 §3.1.6 存在偏差。需求文档 §3.1.6 规定三层行为：(1) allergy_history 由后端拼接；(2) allergy_details 默认缺省；(3) 前端在健康档案编辑界面将结构化数据存入 allergy_details 扩展容器。设计文本需补充来源优先级说明。
   - 所在位置：§3.2 AuditRequest.patientInfo；§3.4 PrescriptionAssistRequest.patientInfo；§10.2 / §10.4 ai-api 层 DTO
   - 改进建议：统一 allergyHistory 和 allergyDetails 的数据来源语义：(a) allergyHistory 由后端从健康档案实体拼接（与 §3.1.6 第1层一致）；(b) allergyDetails 按 §3.1.6 过渡方案第3层——允许前端存入，但后端 Service 层优先从健康档案实体自动提取 allergyDetails 为 single source of truth，前端传入值仅作为 fallback/离线场景覆盖；(c) 在 §3.2 和 §3.4 补充来源优先级说明。

10. **[一般] 辅助开方过敏告警与处方审核过敏检查关系不明确**：§3.4 /assist 主端点"本地即时校验"包含过敏冲突检查，与 §3.2 AllergyCheckRule 存在逻辑重叠。设计文本未说明二者是冗余双重校验还是各有侧重。辅助开方 allergyWarnings.severity 含 HIGH 级别，与处方审核 AuditRiskLevel.BLOCK 的映射关系未定义。
    - 所在位置：§3.4 PrescriptionAssistService "本地即时校验"；§3.2 AllergyCheckRule
    - 改进建议：在 §3.4 或 §7 补充"辅助开方过敏告警与处方审核过敏检查的关系"说明：明确辅助开方的 allergyWarnings 为即时提示性质（面向医生编辑期间的实时反馈），处方审核的 AllergyCheckRule 为提交时的正式审核判定，二者独立执行不互斥；辅助开方 allergyWarnings.severity=HIGH 不直接等价于处方审核 AuditRiskLevel=BLOCK。

11. **[一般] RegistrationEvent 跨模块事件契约未定义**：TriageRecord 写入时机为"返回响应前同步写入"，finalDepartmentId 通过"挂号模块发布的事件（RegistrationEvent）"补充写入，但 RegistrationEvent 事件契约（字段、发布端、消费端）未被定义。
    - 所在位置：§3.1 TriageService "TriageRecord 写入时机"；§4.1 持久化说明
    - 改进建议：(a) 定义 RegistrationEvent 事件契约（字段至少含 registrationId / patientId / departmentId / doctorId / eventTime），事件类定义在 common-module-api 中；(b) 注册事件发布端为 registration 模块，消费端为 consultation 模块，事件在 application 模块聚合后跨模块传播；(c) 在 §2.2 或 §6 补充跨模块事件传递机制说明。

12. **[一般] §5.1 错误码表 AI/非 AI 分类命名规则不明确**：§5.1 错误码表混合含 `_AI_` 中段和不含 `_AI_` 中段的错误码（如 TRIAGE_AI_TIMEOUT vs TRIAGE_SESSION_EXPIRED），编码实现者无法从表中判断哪些错误码应遵循 AI 命名约定。
    - 所在位置：§5.1 错误码表
    - 改进建议：在 §5.1 增加命名规则说明：AI 相关错误码遵循 `<前缀>_AI_<类型>` 命名约定（如 TRIAGE_AI_TIMEOUT），非 AI 业务逻辑错误码使用 `<前缀>_<类型>` 命名（如 TRIAGE_SESSION_EXPIRED、RX_ASSIST_DOSE_STANDARD_NOT_FOUND），二者的区分规则和适用范围需在表中以注释或分类方式标注。

13. **[一般] 边界场景处理缺失**：(1) DialogueSession 超过 TTL 30 分钟后恰好有请求到达的竞态（请求到达瞬间 ScheduledExecutorService 清理该 session）——未说明清理与并发访问的竞态处理；(2) 多轮分诊场景下规则版本快照对应的规则集因管理员操作被删除/禁用后的行为——DialogueSession 持有 ruleVersion + ruleSetId 快照，但 TriageRuleEngine.match() 使用此快照查询时可能查不到对应规则集。
    - 所在位置：§6.1 对话会话并发管理；§3.1 DialogueSession ruleVersion/ruleSetId 快照机制
    - 改进建议：(1) 补充 TTL 清理的竞态处理说明——ConcurrentHashMap.remove() 的原子性保证下若 session 已被清理，DialogueSessionManager.findOrCreate() 返回 TRIAGE_SESSION_EXPIRED 错误（与当前设计一致，但需明确此竞态路径被覆盖）。(2) 补充规则快照失效处理——当 TriageRuleEngine.match() 使用快照版本查询无结果时，降级使用当前最新版本规则集重新匹配（并在 TriageResponse 中标记），避免因规则管理操作导致分诊完全失败。

14. **[一般] §5.1 错误码表遗漏需求文档明确定义的 AI 能力错误码**：RX_ASSIST_AI_NO_RECOMMENDATION、RX_AUDIT_AI_INPUT_INVALID、MR_GEN_AI_INPUT_INVALID、MR_GEN_AI_OUTPUT_INCOMPLETE 未在 §5.1 错误码表中列出。
    - 所在位置：§5.1 错误码表
    - 改进建议：§5.1 错误码表需补齐需求文档 §3.4.x 明确定义的全部 AI 能力错误码，至少包括：RX_ASSIST_AI_NO_RECOMMENDATION、RX_AUDIT_AI_INPUT_INVALID、MR_GEN_AI_INPUT_INVALID、MR_GEN_AI_OUTPUT_INCOMPLETE。

### 轻微问题

15. **[轻微] TriageResponse.degraded=true 时前端行为未说明**：需求文档 §6.3 明确分诊降级路径为"回退到按科室选择医生模式"，但设计文本 TriageResponse 仅返回 degraded=true 标记，未说明前端收到 degraded=true 后应如何调整 UI。
    - 所在位置：§3.1 TriageService 降级链；§1.3 TriageResponse.degraded 字段
    - 改进建议：在 §4.1 降级判定后补充前端行为说明——degraded=true 时前端仍渲染推荐科室列表（规则匹配/兜底结果），同时显示降级提示文案并提供"手动选择科室"入口。

16. **[轻微] "底座直接落地"与 Phase 5 迁移兼容性未显式论证**：设计目标声称"规避 Phase 5 迁移成本"但未充分论证此约束的实际实现方式。Phase 5 OOD 显示 AI 进阶底座在 AiService 接口不变的前提下替换实现——"接口不变"是迁移关键前提，需显式论证。
    - 所在位置：§1.1 设计目标"底座直接落地"
    - 改进建议：在 §1.1 或 §7 设计决策中增加"底座落地与 Phase 5 迁移兼容性"设计决策条目，说明：业务模块仅依赖 ai-api 的 AiService 接口（编译期依赖），Phase 5 迁移时仅需替换 ai-impl 内的 AiService 实现类，业务模块代码无须修改。

## 历史迭代回顾

### 已解决的问题（出现在历史反馈但当前反馈中不再提及的问题）

- **DialogueSession 不可变 vs 可变矛盾**（迭代1-问题1）：v5起已明确为可变 class + DialogueSessionManager 并发控制
- **包E 异步 AI 建议缺少消费路径**（迭代1-问题2）：v4起已定义 GET /assist/suggestion/{taskId} 查询端点
- **对话历史维护责任不明确**（迭代1-问题3）：v5起已明确 DialogueSession 为单一真相来源 + 全量拼接策略
- **DosageCheckRequest 缺少给药途径参数**（迭代1-问题4）：v4起已增加 routeOfAdministration
- **DosageStandard 写权限归属未定义**（迭代1-问题5）：v3起已明确 admin 模块为唯一写入者
- **分诊规则配置变更生效机制未定义**（迭代1-问题6）：v2起已定义 Caffeine 定时缓存刷新 + 事件驱动
- **科室模板 CRUD 和默认兜底缺失**（迭代1-问题7）：v2起已定义 DEFAULT 兜底 + Repository + seed
- **对话会话内存未覆盖重启场景**（迭代1-问题8）：v2起已补充 TRIAGE_SESSION_EXPIRED 错误码
- **新模块依赖声明缺 common-module-api**（迭代1-问题9）：v3起已更正为依赖 common, common-module-api, ai-api
- **剂量标准数据初始化和编码规范缺失**（迭代1-问题10）：v3起已补充种子脚本和药品编码规范
- **BLOCK 风险等级缺后端强制阻断**（迭代2-问题1）：v3起已通过 PrescriptionAuditEnforcer + HTTP 422 实现
- **AuditRecord 缺处方级关联标识**（迭代2-问题2）：v4起已补充 prescriptionOrderId/doctorId/patientId
- **AiSuggestionResult 服务重启后丢失**（迭代2-问题3）：v4起已补充 NOT_FOUND 错误码和 TTL
- **DosageStandard 缺年龄/体重分级支持**（迭代2-问题4）：v4起已内联年龄/体重范围字段
- **病历降级策略"返回空框架"**（迭代2-问题5）：v3起已改为分层保护策略
- **AuditRecord 降级路径落库遗漏**（迭代3-问题1）：v4起已在 §4.2 显式补充 fromFallback=true
- **分诊降级链不完整**（迭代3-问题2）：v4起已改为线性降级链 AI→规则→兜底
- **TriageResponse DTO 字段未定义**（迭代3-问题3）：v5起已补充完整字段定义
- **剂量单位转换规则未定义**（迭代3-问题4）：v4起已定义 DosageUnitGroup 枚举
- **MedicalRecord 字段和病历输出模型缺失**（迭代3-问题5）：v4起已补充 MedicalRecordField 枚举
- **规则/模板配置变更缺审计溯源**（迭代3-问题6）：v4起已补充 ConfigChangeLog 实体
- **AiSuggestionResult 缺 FAILED 状态**（迭代4-问题1）：v5起已新增 FAILED + failReason + 预创建→更新模式
- **TriageRequest DTO 与 DialogueCreateRequest 关系不明确**（迭代4-问题2）：v5起已统一为 DialogueCreateRequest
- **关键 DTO 存在于目录但设计文本无定义**（迭代4-问题3）：v5起已补充全部 DTO 字段定义
- **DosageThresholdService 缺"记录不存在"行为**（迭代4-问题4）：v5起已补充四级匹配 + RX_ASSIST_DOSE_STANDARD_NOT_FOUND
- **DosageAlert 告警级别枚举未定义**（迭代4-问题5）：v5起已定义 DosageAlertLevel: INFO/WARN/CRITICAL
- **DosageCheckRequest 缺 frequency**（迭代4-问题6）：v5起已增加 frequency 字段
- **MedicalRecord 缺 visitId**（迭代4-问题7）：v5起已增加 visitId 必填字段
- **PrescriptionAssistResponse 字段不完整**（迭代4-问题8）：v5起已修正
- **AuditRiskLevel 与需求文档映射缺失**（迭代4-问题9）：v5起已补充映射说明
- **AiSuggestionResult 并发竞争**（迭代5-问题1）：v6起已使用 ConcurrentHashMap.compute() 原子操作
- **包E check-dose 脱节需求文档**（迭代5-问题2）：v6起已定义 POST /assist 主端点
- **TriageResponse 缺推荐医生/理由字段**（迭代5-问题3）：v6起已补充 doctors + reason
- **AuditRequest.patientInfo 字段不完整**（迭代5-问题4）：v6起已显式列出全部字段
- **RecordGenerateRequest.visitId 与 encounter_id 映射**（迭代5-问题5）：v6起已明确映射关系
- **WARN 留痕数据结构未定义**（迭代5-问题6）：v6起已补充 forceSubmitted/forceSubmitTime
- **sessionId 生成策略和职责**（迭代5-问题7）：v7起已明确前端 UUID v4 生成 + 必填
- **session_id 必填/可选矛盾**（迭代5-问题8）：v7起已由前端首轮传入消除
- **CRITICAL/BLOCK 联动触发链路**（迭代5-问题9）：v6起已使用 PrescriptionDraftContext 写入上下文
- **DrugInteractionPair/DrugAllergyMapping 缺失**（迭代5-问题10）：v6起已补充 JPA @Entity 定义
- **分诊降级"AI 无结果"判定不清**（迭代5-问题11）：v6起已明确 success=true 空列表视为有效结果
- **AI 超时配置未体现**（迭代5-问题12）：v6起已在 §5.5 补充
- **AuditResponse 与需求契约不对齐**（迭代5-问题13）：v6起已补充 interactions + suggestions
- **TriageRecord 实体字段缺失**（迭代5-问题14）：v6起已补充完整字段
- **AuditResponse.issues vs alerts 命名不一致**（迭代6-问题1）：v7起已同时保留 alerts + issues 并补充映射
- **DialogueCreateRequest 缺 ruleSetId**（迭代6-问题2）：v7起已增加
- **TriageResponse 缺 matchedRules**（迭代6-问题3）：v7起已增加
- **department 每项字段未定义**（迭代6-问题4）：v7起已新增 RecommendedDepartment DTO
- **病历生成流式设计缺失**（迭代6-问题5）：v7起已标注 stream 待后续 + 补充 stream 字段
- **RecordGenerateRequest 缺 stream**（迭代6-问题6）：v7起已增加
- **RecordGenerateResponse 字段映射不一致**（迭代6-问题7）：v7起已补充映射说明
- **本地规则仅实现 1/4 最小检查项**（迭代6-问题8）：v7起已扩展为 4 条规则
- **PrescriptionDraftContext key 不清**（迭代6-问题9）：v7起已明确为 prescriptionId
- **AI 连续失败 3 次兜底缺失**（迭代6-问题10）：v7起已补充 aiFailCount + fallbackHint
- **AuditRecord 缺审核次序/最新标记**（迭代6-问题11）：v7起已补充 auditSequence + isLatest
- **DosageStandard 与药品基础信息关系**（迭代6-问题12）：v7起已明确通过 drugCode 关联
- **ai-api 层 DTO 字段级契约缺口**（迭代7-问题1）：v8起已新增 §4.5 映射机制 + §10 ai-api DTO 扩展规格
- **PrescriptionAssistRequest 同名冲突**（迭代7-问题2）：v8起已通过不同包名区分
- **allergy_details 扩展容器未纳入**（迭代7-问题3）：v8起已新增 allergyDetails + AllergyDetail DTO
- **RX_ASSIST_AI_NO_RECOMMENDATION 消费场景**（迭代7-问题4）：v8起已补充 AI 返回无可推荐药品场景
- **多轮分诊上下文传递**（迭代7-问题5）：v8起已补充全量拼接策略
- **推荐医生数据来源**（迭代7-问题6）：v8起已补充 DoctorFacade + 排班数据查询
- **TriageRecord 写入时机**（迭代7-问题7）：v8起已明确返回前同步写入 + 事件补充
- **AllergyCheckRule 一律 BLOCK**（迭代7-问题8）：v8起已补充按 allergyDetails 严重程度分级
- **WARN 强制提交前后端交互**（迭代7-问题9）：v8起已补充提交端点 forceSubmit + auditRecordId
- **病历非流式超时 AiResult 降级模式**（迭代7-问题10）：v8起已补充但仍有歧义（见当前问题2）
- **WARN 时序竞态**（迭代7-问题11）：v8起已补充 originalPrescription 版本校验
- **MedicalRecord 字段存储形式**（迭代7-问题12）：v8起已明确单列 JSON TEXT
- **配置变更事件事务边界**（迭代7-问题13）：v8起已补充 @TransactionalEventListener(AFTER_COMMIT)
- **session_id 必填语义**（迭代7-问题14）：v8起已改为前端首轮生成传入
- **DrugInteractionPair/DrugCompositionDict 持久化**（迭代7-问题15）：v8起已移至 entity/ 包
- **TriageRule 匹配模型**（迭代7-问题16）：v8起已补充实体定义 + match() 签名
- **处方提交端点**（迭代7-问题17）：v8起已在 §4.2 补充简要契约
- **ConcurrentHashMap 跨实例部署**（迭代7-问题18）：v8起已补充部署约束 + Phase 5 迁移节点

### 持续存在的问题（在多轮反馈中反复出现的问题，需重点解决）

- **合并症-药品禁忌检查遗漏**：v6 问题8 首次提出"本地规则仅实现 1/4 最小检查项"（未提及合并症），v7 问题3 仅在检查项 #2 描述中涉及过敏检查但未明确合并症子要求，v8 问题3 明确指出需求文档 §3.4.2 检查项 #2 的合并症-药品禁忌检查完全无对应规则实现。**此问题经历了从"检查项不完整"到"合并症检查遗漏"的逐步精化，是连续3轮未解决的核心需求响应问题，必须本轮解决。**

- **AiResult 超时降级路径歧义**：v7 问题10 首次提出"病历非流式超时 AiResult 降级模式"未定义，v8 已补充但描述存在歧义（"新增 partialData 字段"与"使用现有 data 字段"二选一未决），且仅覆盖 degraded() 路径遗漏 failure() 路径。v8 问题4（本迭代问题2）进一步明确需求文档 §3.4.3 超时场景是 failure+errorCode+partialData 组合。**此问题历经2轮未彻底解决，歧义和路径遗漏需本轮一并解决。**

- **TriageRecord 缺推荐医生快照**：v5 问题14 首次提出 TriageRecord 字段缺失，v6 补充了核心统计字段但未包含推荐医生，v8 问题5 明确指出需求文档 §5.1 规定推荐医生为分诊记录核心字段。**此问题经历了从"字段缺失"到"推荐医生字段缺失"的逐步精化，是2轮未解决的需求响应问题，必须本轮解决。**

- **DoctorFacade 包名不匹配**：v8 问题1 首次提出，当前第2轮出现。

- **AuditAlert.severity 类型未定义**：v8 问题6 首次提出，当前第2轮出现。

- §5.1 **错误码表** 多轮持续存在问题：v6起已补部分，但 v8 反馈仍有两类问题——(a) AI/非AI分类命名规则不明确（v8问题14），(b) 需求文档明确定义的 AI 错误码遗漏（v8问题16）。**错误码表完整性问题需本轮彻底解决。**

### 新发现的问题（本轮新识别的问题）

- 问题4（DoctorFacade 包名不匹配）：v8 首次识别
- 问题5（matched_rules 设计决策缺失）：v8 从 v7 问题2 质询后重新定位
- 问题6（AuditAlert.severity 类型未定义）：v8 首次识别
- 问题7（DosageAlert 无错误码字段）：v8 首次识别
- 问题8（处方版本校验比较有歧义）：v8 首次识别
- 问题9（allergyHistory/allergyDetails 数据来源语义不一致）：v8 合并 v7 问题9/16 后重新定位
- 问题10（辅助开方过敏告警与处方审核过敏检查关系不明确）：v8 首次识别
- 问题11（RegistrationEvent 事件契约未定义）：v8 首次识别
- 问题12（错误码 AI/非 AI 分类命名规则）：v8 从 v7 问题14 质询后重新定位
- 问题13（TTL 清理竞态 + 规则快照失效）：v8 首次识别
- 问题14（错误码表遗漏 AI 能力错误码）：v8 首次识别
- 问题15（degraded=true 前端行为）：v8 首次识别
- 问题16（Phase 5 迁移兼容性未论证）：v8 首次识别

## 上一轮产出路径
C:\Develop\Software\AIMedicalSys\Harness\redeliberations\202606281422_phase23_ood\a_v8_design_v1.md

## 用户需求
C:\Develop\Software\AIMedicalSys\Harness\redeliberations\202606281422_phase23_ood\requirement.md
