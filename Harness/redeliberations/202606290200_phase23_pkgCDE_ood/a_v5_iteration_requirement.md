根据以下审查结果，迭代上一轮的产出，形成新版的文件，从而更好地满足用户需求。

## 当前审查结果

### 问题 1（严重）：RegistrationEvent sessionId 跨模块传播路径在架构层面未闭合

§1.1a 外部依赖表（registration 模块行，"sessionId 填充逻辑"）；§2.2 "跨模块事件传递机制"段（line 293-294）；§2.2 RegistrationEvent 字段描述（sessionId "由 registration 模块从分诊上下文获取并填充"）。consultation 模块与 registration 模块是独立模块（按 §2.2 依赖规则不允许互相依赖），产出未定义 sessionId 从 consultation 模块的 DialogueSession 传递到 registration 模块的具体路径。前端在分诊流程结束后进入挂号流程时，分诊 sessionId 如何到达挂号模块未被覆盖。改进建议：明确定义 sessionId 从分诊流程到挂号流程的跨模块传播机制。可选方案：(a) 前端侧传递——前端在分诊结束后保留 sessionId，进入挂号界面时将 sessionId 作为请求参数传给 registration 模块，registration 模块将其写入 RegistrationEvent；(b) 后端侧查询——consultation 模块暴露轻量级查询接口（如根据 patientId + 最近分诊时间查询 sessionId），供 registration 模块在发事件前调用；(c) 方案 (a) 与 (b) 结合，前端传递为主路径、后端查询为 fallback。选定后同步更新 §1.1a 外部依赖表和 §2.2 事件传递机制描述。

### 问题 2（一般）：VisitFacade 调用失败无降级策略

§3.3 RecordGenerateRequest（line 593-595，"MedicalRecordService 依赖 VisitFacade 接口而非实现"）；§1.1a 外部依赖表（visit 模块行，仅标注"实现缺失时病历存储失败"未规定具体降级行为）；§4.3 病历生成场景行为契约。DoctorFacade 有完整降级保护，但 VisitFacade（encounterId→visitId 转换）作为同样是强制同步跨模块调用路径，未定义任何故障处理行为。改进建议：为 VisitFacade 补充与 DoctorFacade 对称的降级保护设计，包括：(a) 定义超时阈值（默认值，如 2s）；(b) 定义调用失败时的行为：返回 RX_MR_GEN_VISIT_NOT_FOUND 错误码 + 病历内容仍部分返回（保留已生成字段），或将 encounterId 直接作为 visitId 的 fallback 写入；(c) 同步更新 §4.3 行为契约和 §5.1 错误码表（新增 MR_GEN_VISIT_NOT_FOUND）。

### 问题 3（一般）：FieldMissingHint 字段生成规则未定义，无法直接指导"提示补全"实现

需求 3.4.3 要求"关键字段缺失提示补全"，产出中 MissingFieldDetector 仅定义了缺失检测逻辑（差集比对 + 非空非 null 判定），FieldMissingHint 包含 promptMessage 和 suggestedAction 字段但未定义其内容来源和生成规则。改进建议：定义 FieldMissingHint 字段的生成策略，至少明确以下两者之一：(a) 基于 DepartmentTemplateConfig 中每个 MedicalRecordField 的预定义提示模板（promptMessage + suggestedAction 静态文本，由管理员在科室模板中配置）；(b) 或由 AI 在生成病历的同时返回缺失字段的补全建议（需同步扩展 ai-api MedicalRecordGenResponse 字段）。如采用 (a) 为主要策略，需同步在 §3.3 中补充提示内容的加载/缓存机制，并在 §2.1 目录结构中明确 DeptTemplateConfig 是否扩展为包含提示模板字段。

### 问题 4（一般）：部分错误码遗漏于 §5.1 错误码表

产出正文中定义或引用了若干错误码但未出现在 §5.1 错误码表中：`RX_ASSIST_UNIT_MISMATCH`（§8.3 定义了跨组单位比较时的输出规则，但未进表）；`TRIAGE_SESSION_NOT_FOUND`（§3.1 和 §4.1 手动选科端点描述中定义了 selectDepartment 在 TriageRecord 不存在时返回此错误码，但未进表）；`RX_MR_GEN_VISIT_NOT_FOUND`（若采纳问题 2 建议需新增）。改进建议：将上述遗漏错误码补充至 §5.1 错误码表中，归类到对应模块的合适分类行。

### 问题 5（一般）：MedicalRecord.contentJson 并发写更新丢失未处理

§3.3 MedicalRecordRepository 描述（line 567，"病历更新方法支持增量更新语义——读取 contentJson、合并变更字段、写回"）；§3.3 MedicalRecord 实体（line 561-567）。"单列 JSON TEXT + 读取→合并→写回"模式在并发写入时后提交的写入会覆盖前一个变更。改进建议：在 MedicalRecord 实体中增加 `@Version` 乐观锁字段，Repository 更新操作使用版本号校验，写冲突时返回并发错误码（如 MR_GEN_CONCURRENT_MODIFICATION），由前端提示用户刷新后重试。同步在 §3.3 和 §7 设计决策中补充此边界防护。

## 历史迭代回顾

### 已解决的问题
以下问题出现在历史反馈（R1-R3）中，但当前反馈不再提及，视为已解决：
- R1：AllergyWarningSeverity 枚举值与需求文档语义不匹配；DosageThresholdService 匹配优先级层级标注不一致；PrescriptionAssistService 过敏冲突检查实现归属；encounterId→visitId 转换路径；DialogueSession 与 TriageRecord 事务一致性；Phase 5 迁移"代码无须修改"断言矛盾；LocalRuleEngine 规则计数不一致；CRITICAL/BLOCK 阻断竞态防护；PrescriptionAssistResponse errorCode 字段缺失
- R2：TriageRecord/AuditRecord/DeadLetterEvent JPA @Id 主键；手动选科与 RegistrationEvent 覆盖优先级强制执行；全量拼接上下文 AI 感知截断标记；DrugFacade 接口定义缺失；外部依赖章节与时间线；DeadLetterEvent 字段表；AiResult Phase 5 兼容风险标注
- R3：Store 抽象层强制化；check-dose 异步 AI 去重/节流；admin 模块时间线协调；AiResult success=true data=null 契约约束；撤销审核端点定义；prescriptionId 分配时机；MockAiService 行为契约

### 持续存在的问题
以下问题在多轮反馈中反复出现，需重点解决（当前 5 个问题均源自 R4，在第 5 轮中仍未完全修复）：
1. **RegistrationEvent sessionId 传播路径**（R4 问题1 / 当前问题1）——需明确定义跨模块传播机制，选定一个方案并完整闭合架构路径
2. **VisitFacade 降级策略缺失**（R4 问题2 / 当前问题2）——需补充与 DoctorFacade 对称的降级保护
3. **FieldMissingHint 生成规则未定义**（R4 问题3 / 当前问题3）——需明确 promptMessage/suggestedAction 内容来源策略
4. **错误码遗漏于 §5.1**（R4 问题4 / 当前问题4）——需补充遗漏错误码
5. **MedicalRecord.contentJson 并发写丢失**（R4 问题5 / 当前问题5）——需增加乐观锁并发防护

### 新发现的问题
本轮无新发现的问题。

## 上一轮产出路径
C:\Develop\Software\AIMedicalSys\Harness\redeliberations\202606290200_phase23_pkgCDE_ood\a_v4_copy_from_v3.md

## 用户需求
C:\Develop\Software\AIMedicalSys\Harness\redeliberations\202606290200_phase23_pkgCDE_ood\requirement.md
