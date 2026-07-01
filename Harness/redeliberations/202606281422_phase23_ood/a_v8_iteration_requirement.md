根据以下审查结果，迭代上一轮的产出，形成新版的文件，从而更好地满足用户需求。

## 当前审查结果

### 严重问题

1. **ai-api 层 DTO 与业务层 DTO 之间存在严重的字段级契约缺口，设计未定义二者间的映射/转换机制**（§1.2 AiService接口描述 + §3.1/3.2/3.3/3.4 各Service协作描述 + §2.2 "与 AI 模块的协作关系"）
   - ai-api 层 TriageResponse 仅有 recommendedDepartments（每项仅 departmentName）和 reason 两个字段，缺少 recommendedDoctors/matchedRules/sessionId/confidence/needFollowUp/followUpQuestion/degraded/fallbackHint 等业务层关键字段；PrescriptionCheckRequest/Response、PrescriptionAssistRequest/Response、MedicalRecordGenRequest/Response 均为空壳类
   - 需定义：(a) ai-api 层 DTO 扩展策略——补充完整字段定义与需求文档对齐；(b) 业务层 DTO 与 ai-api 层 DTO 的转换规则和转换责任归属（建议由各模块 Converter 类负责）；(c) ai-api 层 DTO 完整字段列表确保与 §1.3 和 §3.x 字段引用一致

2. **PrescriptionAssistRequest（ai-api 层）与业务层 PrescriptionAssistRequest（assist DTO）同名且职责不同，设计未区分**（§3.4 + §2.2 + AiService 签名）
   - ai-api 层与业务层同名类在实现时容易混淆，字段差异和命名空间关系未说明
   - 需明确区分二者字段差异和命名空间关系；补充 ai-api 层 DTO 完整字段定义或复用策略；PrescriptionCheckRequest/Response 同理

3. **allergy_details 扩展容器完全未纳入设计，与需求文档过敏信息扩展性方案脱节**（§3.2 AuditRequest.patientInfo + §3.4 PrescriptionAssistRequest.patientInfo + §3.2 AllergyCheckRule）
   - 需求文档 §3 预见 allergy_details 过渡方案（含 allergen/reaction_type/severity/occurred_at），当前设计仅有 allergyHistory(string)
   - 需在 patientInfo 中增加 allergyDetails 可选字段（List<AllergyDetail>）；AllergyCheckRule 存在 allergyDetails 时优先按结构化过敏信息做精确匹配（区分严重程度影响 AuditRiskLevel 判定），缺失时回退文本匹配；§7 补充过敏信息扩展性设计决策

4. **RX_ASSIST_AI_NO_RECOMMENDATION 错误码已定义但消费场景未描述，/assist 主端点 AI 返回无可推荐药品时的行为不明确**（§3.4 + §4.4 + §5.1）
   - 需在 §4.4 /assist 主端点流程补充"AI 返回无可推荐药品"场景处理路径：明确返回空 prescriptionDraft + 本地校验结果 + 该错误码标记，或作为业务异常返回 4xx 响应；说明此错误码与 check-dose 异步建议 FAILED 状态的关联关系

5. **多轮分诊场景下 TriageServiceImpl 如何将 DialogueSession 的上下文传递给 AiService.triage() 未定义**（§3.1 + §4.1 + AiService.triage() 签名）
   - ai-api 层 TriageRequest 仅有 chiefComplaint 一个字段，多轮场景需将完整对话上下文组装传入
   - 需补充调用组装说明：ai-api 层 TriageRequest 扩展 additionalResponses 字段与需求文档对齐；明确"全量拼接"策略——每次调用将完整上下文传入 AiService

6. **推荐医生的推荐机制和数据来源未定义，RecommendedDoctor 的 score 计算和 availableSlotCount 获取路径未说明**（§3.1 TriageResponse.doctors + §2.2 依赖规则 + §4.1）
   - 需明确推荐医生列表生成机制——AI 返回推荐医生（则需扩展 ai-api 层 TriageResponse）或后端根据 AI 推荐科室从医生排班数据查询；若需跨模块查询，在 §2.2 补充 cross-module 数据获取机制（建议 common-module-api 中定义 DoctorFacade 接口）；明确 availableSlotCount 数据来源和实时性

### 一般问题

7. **TriageRecord 写入时机和触发方未在行为契约中体现，finalDepartmentId 赋值时机不明确**（§3.1 + §4.1）
   - 需在 TriageService 接口职责中明确 TriageRecord 写入步骤和时机（建议返回响应前同步写入，finalDepartmentId 为空/nullable）；说明 finalDepartmentId 补充写入机制——患者挂号后通过事件/回调更新

8. **本地规则聚合逻辑缺少精确的"风险等级判定规则表"，AllergyCheckRule 一律输出 BLOCK 无法区分严重程度**（§3.2 + LocalRuleResult 聚合逻辑 + 各规则逻辑描述）
   - 需明确 LocalRuleResult.severity 类型为 AuditRiskLevel 枚举；为每条规则补充 severity 判定细节——AllergyCheckRule 区分严重过敏(BLOCK)/轻度过敏(WARN)；DosageLimitRule 根据超标程度区分 BLOCK/WARN

9. **WARN 级处方"强制提交并留痕"的前后端交互链路不完整——前端如何通知后端"医生选择强制提交"未定义**（§3.2 + §4.2 + PrescriptionAuditController）
   - 需在 §4.2 WARN 分支补充前端→后端交互链路：建议处方提交端点增加 forceSubmit(bool) 参数；后端校验存在 WARN 级最新 AuditRecord；补充处方提交端点契约或明确设计边界

10. **病历生成非流式超时场景的"部分保留"行为契约缺少对 AiResult 降级模式的定义**（§3.3 + §4.3 + §5.5）
    - 需补充非流式超时降级路径：AI 超时时 AiResult.data 携带部分生成结果；AiResult 增加 partialData 字段或设计 MedicalRecordService 在超时时返回部分字段 + missingFieldHints + MR_GEN_AI_TIMEOUT 标记

11. **WARN 级处方审核与强制提交的时序竞态未防护**（§3.2 + §4.2）
    - 需在 AuditRecord 中显式标注 originalPrescription 为 JSON 文本存储完整处方快照；强制提交路径补充处方版本校验——比较当前处方与 AuditRecord.originalPrescription 是否一致；或要求提交接口携带 auditRecordId 校验最新审核

12. **MedicalRecord 实体缺少 MedicalRecordField 级字段检索/更新机制定义**（§3.3 + MedicalRecordRepository）
    - 需明确病历内容存储形式为单列 JSON TEXT（contentJson + JPA @Convert/Jackson）；补充 MedicalRecordRepository 查询方法列表（findByVisitId、findByPatientId 等）；补充病历更新方法的增量更新语义

13. **配置变更审计日志跨模块事件传递的事务边界未定义——规则变更事件由哪个模块发布**（§9.2/§9.3 + §2.2）
    - 需使用 @TransactionalEventListener(phase=AFTER_COMMIT) 确保事务提交后发布事件；确认 Spring ApplicationEvent 在 application 模块聚合后可跨模块传播；补充事件丢失补偿机制——TemplateConfigManager 定时刷新覆盖

14. **consultation 模块的 AI 分诊首次调用缺少对需求文档 session_id 必填语义的完整对齐方案**（§3.1 DialogueCreateRequest + §7）
    - 建议方案：首轮请求也要求前端生成并传入 sessionId（前端生成 UUID v4），消除"首轮为空"特殊分支，与需求文档 3.4.1 完全对齐；若保留当前设计，补充与需求方的确认记录或待办项

15. **DrugInteractionPair 和 DrugCompositionDict 实体缺少持久化层定义**（§2.1 + §3.2）
    - 需将 DrugCompositionDict 和 DrugAllergyMapping 移至 entity/ 包下作为 JPA Entity；补充核心字段定义（drugCode 主键、ingredients 类型、维护入口）；补充 DrugCompositionDictRepository 和 DrugAllergyMappingRepository；§8.1 补充种子数据脚本路径

16. **分诊规则引擎的规则数据实体和 TriageRule 匹配模型未定义**（§3.1 TriageRuleEngine + §4.1 + §7）
    - 需补充 TriageRule 实体核心字段（ruleId、ruleSetId、ruleVersion、conditions、resultDepartmentId、resultDepartmentName、score）；补充 TriageRuleEngine.match() 方法签名（输入：主诉文本 + ruleVersion + ruleSetId，输出：List<RecommendedDepartment>）；说明规则版本和规则集存储位置

17. **处方提交端点不在当前设计范围内，但 BLOCK 阻断和 WARN 留痕的端到端闭环依赖此端点**（§3.2 + §3.4 + §4.2）
    - 需显式标注处方提交端点的设计边界——若不在 Phase 2/3 范围，则在 §4.2 BLOCK/WARN 分支补充"处方提交校验在处方提交端点中实现（待设计）"标注并在 §7 记录待办项；若在本设计范围内，补充简要契约定义

18. **分诊场景对话会话存储 ConcurrentHashMap 的跨实例问题——水平扩展时 session 不共享**（§6.1 + §6.3 + §6.4 + §7）
    - 需在 §7 或 §6 补充部署约束说明——Phase 2/3 假设单实例部署或 sticky session；若需多实例部署须替换为分布式缓存；标注 Phase 5 迁移节点——三项内存存储均需迁移至持久化或分布式缓存

### 质询报告补充项

19. **AuditRecord 撤销审核时 isLatest 字段处理逻辑不完整**（§4.2 WARN 分支"撤销审核"操作）
    - §4.2 提及"处方状态回退至'草稿'且不保存本次审核结果（但已产生的审核记录仍持久化保存）"，但"不保存本次审核结果"与"审核记录仍持久化保存"语义需更精确澄清——撤销后该 AuditRecord 的 isLatest 是否回退为 false？若不回退，后续查询最新审核结果可能返回已撤销的记录

## 历史迭代回顾

### 已解决的问题（出现在历史反馈但当前 v7 反馈中不再提及）

- **DialogueSession 不可变/可变矛盾**（v1#1）：v7 已改为可变 class + DialogueSessionManager 并发控制
- **包E 异步 AI 建议缺少消费路径**（v1#2）：v7 已补充 GET /assist/suggestion/{taskId} 四分支查询端点
- **多轮对话历史维护责任不明确**（v1#3）：v7 已明确服务端 DialogueSession 为单一真相来源
- **DosageCheckRequest 缺少给药途径**（v1#4）：v7 已补充 routeOfAdministration 字段
- **DosageStandard 写权限未定义**（v1#5）：v7 已明确 admin 模块唯一写入者
- **分诊规则配置变更生效机制未定义**（v1#6）：v7 已补充 Caffeine 定时刷新 + 事件驱动
- **科室模板 CRUD 和默认兜底缺失**（v1#7）：v7 已补充 DEFAULT 兜底 + TemplateConfigManager
- **对话会话内存存储未覆盖服务重启**（v1#8）：v7 已补充 TRIAGE_SESSION_EXPIRED 错误码
- **新模块依赖未包含 common-module-api**（v1#9）：v7 已补充依赖声明
- **剂量标准初始化和编码规范缺失**（v1#10）：v7 已补充种子脚本 + 国药准字号 + 单位一致性校验
- **BLOCK 后端强制阻断机制缺失**（v2#1）：v7 已补充 PrescriptionAuditEnforcer + HTTP 422
- **AuditRecord 缺少处方级关联标识**（v2#2）：v7 已补充 prescriptionOrderId/doctorId/patientId
- **AiSuggestionResult ConcurrentHashMap 重启丢失**（v2#3）：v7 已补充四分支查询模式含 NOT_FOUND
- **DosageStandard 年龄/体重分级**（v2#4）：v7 已补充内联分级字段 + 五级匹配优先级
- **病历生成降级策略"返回空"**（v2#5）：v7 已改为分层保护策略
- **AuditRecord 落库在降级路径遗漏**（v3#1）：v7 已明确降级路径也写入 AuditRecord(fromFallback=true)
- **分诊降级链不完整**（v3#2）：v7 已改为线性三级：AI→规则→兜底
- **分诊降级判定边界不清晰**（v5#11）：v7 已明确 success=true+空列表为有效结果
- **TriageResponse DTO 字段未定义**（v3#3）：v7 已补充完整字段定义
- **AiSuggestionResult 缺 FAILED 状态**（v4#1）：v7 已补充 FAILED + failReason + 预创建→更新模式
- **TriageRequest DTO 在目录但文本无定义**（v4#2）：v7 已清理
- **本地规则仅实现 1/4**（v6#8）：v7 已扩展为 4 项最小检查项集完整实现
- **AuditResponse.issues 与需求文档 alerts 不一致**（v6#1）：v7 已改为 alerts 主输出 + AuditIssue 内部消费
- **DialogueCreateRequest 缺少 ruleSetId**（v6#2）：v7 已补充
- **TriageResponse 缺少 matchedRules**（v6#3）：v7 已补充
- **TriageResponse.departments 每项字段不完整**（v6#4）：v7 已补充 RecommendedDepartment DTO
- **流式病历缺少设计**（v6#5）：v7 已补充 stream 字段 + Phase 2/3 仅非流式 + 流式超时预留
- **RecordGenerateRequest 缺少 stream**（v6#6）：v7 已补充
- **RecordGenerateResponse 字段映射不一致**（v6#7）：v7 已补充 MedicalRecordField 映射表 + missingFieldHints 超集说明
- **PrescriptionDraftContext key 和清理时机**（v6#9）：v7 已明确 prescriptionId 为 key + 生命周期管理
- **AI 连续失败 3 次兜底**（v6#10）：v7 已补充 aiFailCount + fallbackHint
- **AuditRecord 缺少 auditSequence/isLatest**（v6#11）：v7 已补充
- **DosageStandard 与药品基础信息实体关系**（v6#12）：v7 已补充独立实体通过 drugCode 关联说明
- **WARN 强制提交留痕数据结构**（v5#6）：v7 已补充 forceSubmitted/forceSubmitTime
- **sessionId 生成策略**（v5#7）：v7 已明确 DialogueSessionManager 统一 UUID v4
- **DosageAlertLevel.CRITICAL 与 BLOCK 联动**（v5#9）：v7 已明确 CRITICAL 写入草稿上下文提交时统一终审
- **DrugInteractionPair/DrugAllergyMapping 缺失**（v5#10）：v7 已补充目录条目（DrugCompositionDict 也已补充）
- **分诊降级判定边界**（v5#11）：v7 已明确
- **AI 超时阈值未体现**（v5#12）：v7 已补充 §5.5 完整配置表
- **AuditResponse 缺少 interactions/suggestions**（v5#13）：v7 已补充
- **TriageRecord 实体字段缺失**（v5#14）：v7 已补充完整统计字段
- **配置变更审计溯源缺失**（v3#6）：v7 已补充 ConfigChangeLog + 事件驱动
- **check-dose 缺少 frequency 字段**（v4#6）：v7 已补充 frequency + dailyMax 日剂量校验
- **MedicalRecord 缺少 visitId**（v4#7）：v7 已补充 visitId(encounterId) 必填
- **AuditRiskLevel 术语映射**（v4#9）：v7 已补充映射说明
- **chiefComplaint 字符数约束**（v7 修订#13）：v7 已补充 5-500
- **chiefComplaint 与 additionalResponses 互斥**（v7 修订#14）：v7 已补充互斥语义
- **单位换算规则未定义**（v3#4）：v7 已补充 DosageUnitGroup 枚举
- **DosageCheckRequest 缺少 frequency**（v4#6）：v7 已补充
- **DosageStandard 记录不存在行为**（v4#4）：v7 已补充第 5 级 WARN + RX_ASSIST_DOSE_STANDARD_NOT_FOUND

### 持续存在的问题（在多轮反馈中反复出现，需重点解决）

1. **ai-api 层 DTO 与业务层 DTO 契约缺口**（v7#1，新发现但属于长期积累的架构层面的系统性缺口）——当前最关键技术阻塞项，直接影响编码实现可行性，本轮必须彻底解决
2. **session_id 必填/可选语义矛盾**（v5#8 → v7#14，持续 3 轮）——尽管 v7 补充了映射说明但需求文档契约差异未消除，需本轮做出最终决策
3. **处方提交端点设计边界**（v7#17，与 v7#9 WARN 强制提交交互链路相关联）——BLOCK 阻断和 WARN 留痕闭环依赖此端点，需本轮明确边界
4. **WARN 级强制提交前后端交互链路**（v7#9，与 v7#11 时序竞态和 v7#17 处方提交端点形成递进关系）——三者共同构成 WARN→强制提交→处方落单的端到端闭环，需本轮一并解决

### 新发现的问题

1-18 均为本轮（第 7 轮审查/第 8 轮迭代）新发现的或从新角度识别的问题，其中问题 1-6 为严重、7-18 为一般。问题 19（AuditRecord 撤销审核 isLatest 处理）由质询报告补充发现。

## 上一轮产出路径
C:\Develop\Software\AIMedicalSys\Harness\redeliberations\202606281422_phase23_ood\a_v7_design_v1.md

## 用户需求
C:\Develop\Software\AIMedicalSys\Harness\redeliberations\202606281422_phase23_ood\requirement.md
