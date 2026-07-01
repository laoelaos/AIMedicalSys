
## 迭代第 6 轮

## 迭代第 11 轮

1. **问题描述**：处方提交端点缺少正式 DTO 定义和完整 API 契约，无法指导编码
   - 所在位置：§4.2（line 864）、§4.6（lines 1154-1166）
   - 严重程度：严重
   - 改进建议：补充 SubmitRequest/SubmitResponse DTO 定义、完整 JSON 示例，明确常规审核流程技术路径
2. **问题描述**：visitIdFallback reconciled 任务仅有概念引用，无实现定义
   - 所在位置：§3.3 RecordGenerateRequest（line 638）
   - 严重程度：严重
   - 改进建议：补充 reconciled 任务定义或标注推迟至 Phase 4/5
3. **问题描述**：DialogueSession 事务一致性策略未覆盖 QA 历史等状态丢失
   - 所在位置：§3.1（line 426）
   - 严重程度：严重
   - 改进建议：扩展事务一致性策略覆盖 DialogueSession 全部可变状态
4. **问题描述**：AiResultFactory 类未在目录结构中注册
   - 所在位置：§1.1c（line 37）、§10（line 1575）
   - 严重程度：一般
   - 改进建议：在 §2.1 目录结构和 §2.3 补充 AiResultFactory 条目及方法签名
5. **问题描述**：缺少领域事件统一目录
   - 所在位置：全文散布
   - 严重程度：一般
   - 改进建议：新增领域事件目录表格
6. **问题描述**：错误码→HTTP 状态码映射缺少集中声明
   - 所在位置：§5.1（line 1300-1309）
   - 严重程度：一般
   - 改进建议：在 §5.1 增加 HTTP 状态码列或新增映射表
7. **问题描述**：六级匹配优先级 Level 2 边界规则的实现复杂度已升高
   - 所在位置：§8.4（line 1500）
   - 严重程度：一般
   - 改进建议：补充伪代码或决策表，标注边界测试用例

1. **问题描述**：DrugFacade 作为 prescription 模块同步调用的跨模块门面接口，无超时配置边界和错误处理/降级策略定义
   - 所在位置：§2.2（line 298）；§1.3（line 143）；§3.4 PrescriptionAssistService 职责描述
   - 严重程度：一般
   - 改进建议：对标 DoctorFacade/VisitFacade 补充降级保护——至少包含超时阈值（如 2s）、调用失败后的回退行为（返回空药品信息 + WARN 日志，不阻断主流程），并在 §8.2 同步补充降级声明

2. **问题描述**：§8.4 描述文本"按优先级 1→2→3→4→5 依次尝试"与实际列出的 6 级优先级层级数不符
   - 所在位置：§8.4（line 1432）
   - 严重程度：一般
   - 改进建议：将文本"1→2→3→4→5"修正为"1→2→3→4→5→6"

3. **问题描述**：RX_AUDIT_FORCE_SUBMIT_INVALID 错误码在 §4.2 端点行为描述中使用，但未列入 §5.1 模块级错误码表
   - 所在位置：§4.2（line 832）；§5.1 错误码表（line 1250）
   - 严重程度：一般
   - 改进建议：在 §5.1 审核（非AI）错误码行中补充 RX_AUDIT_FORCE_SUBMIT_INVALID 并标注使用场景说明

4. **问题描述**：AiSuggestionResult TTL 描述仅提及"由 ScheduledExecutorService 定期清理过期条目"，未给出具体清理间隔参数
    - 所在位置：§3.4（line 655）；§6.3（line 1303）；§4.4（line 894）
    - 严重程度：轻微
    - 改进建议：补充清理周期（建议统一为 5 分钟扫描间隔），与 DialogueSessionManager 和 PrescriptionDraftContext 的清理机制描述一致

## 迭代第 7 轮

1. **问题描述**：PrescriptionDraftContext CRITICAL 告警缺少 /assist 端点写入路径，导致医生通过 /assist 获取处方草案后直接提交时 CRITICAL 告警被遗漏
    - 所在位置：§3.4 PrescriptionAssistService 职责描述（line 619–621）、§3.4 PrescriptionDraftContext 覆盖更新行为契约（line 689）、§4.2 处方提交端点步①（line 824）
    - 严重程度：严重
    - 改进建议：在 /assist 主端点本地即时校验执行后，将 alertLevel=CRITICAL 的告警同步写入 PrescriptionDraftContext，写入逻辑与 check-dose 路径一致

2. **问题描述**：DedupTaskScheduler 去重逻辑由"查询→判断→创建"三步组成，非原子操作，并发请求可绕过去重约束
    - 所在位置：§3.4 "异步 AI 调用去重策略"（line 643）、§6.3 "包E 的异步 AI 建议与去重"（line 1305）
    - 严重程度：严重
    - 改进建议：在 SuggestionStore 接口新增原子性 putIfAbsent/createIfNotExists 方法，将去重判定下沉到存储层

3. **问题描述**：阻断竞态防护中步①的 CRITICAL 验证快照未定义存储位置和格式
    - 所在位置：§4.2 阻断竞态防护手段（line 836）
    - 严重程度：一般
    - 改进建议：引入 SubmitContext 值对象（线程级闭包），在步①执行时快照 CRITICAL 告警列表，步③前比对差异

4. **问题描述**：DeadLetterCompensationService "重新投递"未定义投递目标，存在与 @Retryable 形成无限循环的风险
    - 所在位置：§2.2 DeadLetterCompensationService 补偿描述（line 320）
    - 严重程度：一般
    - 改进建议：明确采用直接调用 TriageService.selectDepartment() 的业务补偿模式，补充状态机迁移规则

5. **问题描述**：AiResult.prescriptionAssist() 超时时 partialData 的语义不明确（部分字段 vs 部分药品）
    - 所在位置：§2.3 AiService.prescriptionAssist() 方法定义（line 350–355）、§3.4 AiResult→AiSuggestionResult 映射表（line 665–676）
    - 严重程度：一般
    - 改进建议：明确 partialData 为"已成功生成的完整药品条目列表"（部分药品而非部分字段）

## 迭代第 8 轮

1. **问题描述**：DeadLetterCompensationService 补偿路径缺少 departmentName，RegistrationEvent 不含 departmentName 字段导致 selectDepartment 调用必然失败
   - 所在位置：§2.2 line 320；§1.3 line 144；§3.1 line 408
   - 严重程度：严重
   - 改进建议：二选一：(a) RegistrationEvent 补充 departmentName 字段；(b) TriageService.selectDepartment 增加仅基于 departmentId 的重载版本

2. **问题描述**：AI 输入校验错误码（_AI_INPUT_INVALID 类）已定义但无生产者，全文中无任何触发位置或校验规则定义
   - 所在位置：§5.1 lines 1261-1268；§2.3 lines 330-357
   - 严重程度：严重
   - 改进建议：二选一：(a) AiService 方法契约中补充输入校验职责归属；(b) 标注为"Phase 4 预留"并从当前错误码表移除

3. **问题描述**：核心业务 Service 接口（TriageService、PrescriptionAuditService、MedicalRecordService、PrescriptionAssistService）仅以自然语言描述，缺少显式方法签名
   - 所在位置：§3.1 lines 382-412；§3.2 lines 480-486；§3.3 lines 596-606；§3.4 lines 620-633
   - 严重程度：严重
   - 改进建议：为四个业务 Service 接口补充显式方法签名，参考 §2.3 AiService 格式

4. **问题描述**：patientId 降级查询"最近分诊记录"缺少排序选择标准，triageTime 相同时无法确定"最近"
   - 所在位置：§1.1a line 22；§2.2 line 304；§3.1 lines 474-476；§2.1 line 172
   - 严重程度：严重
   - 改进建议：(a) 补充 findTopByPatientIdOrderByTriageTimeDesc 查询方法定义；(b) 显式说明排序依据；(c) patientId 补充索引说明

5. **问题描述**：RegistrationEvent 缺少 departmentName 导致正常消费路径下 TriageRecord.finalDepartmentName 无法赋值
   - 所在位置：§1.3 line 144；§2.2 lines 302-304
   - 严重程度：一般
   - 改进建议：RegistrationEvent 补充 departmentName 字段，由 registration 模块在发布前通过 departmentId 查询填充

6. **问题描述**：@Retryable 未定义触发重试的异常类型，不可治愈异常（如 IllegalArgumentException）也会触发重试
   - 所在位置：§2.2 line 306
   - 严重程度：一般
   - 改进建议：补充异常过滤器，仅对 DataAccessException、TimeoutException 等可治愈异常重试

7. **问题描述**：TriageRecord.sessionId 缺少唯一约束定义，并发下 delete+insert 模式可能破坏"一对一"语义
   - 所在位置：§3.1 lines 474-476
   - 严重程度：一般
   - 改进建议：增加 @Column(unique = true) 或 @Table uniqueConstraints，明确 update 模式为推荐实现

8. **问题描述**：多处独立使用 ScheduledExecutorService 管理定时任务，缺乏统一线程池管理和优雅关闭机制
   - 所在位置：§3.4 line 659；§3.4 line 687；§6.1 line 1301；§2.2 line 320
   - 严重程度：一般
   - 改进建议：引入统一 ScheduledTaskRegistry 或 Spring @Scheduled，由框架统一接管线程池生命周期

## 迭代第 9 轮

1. **问题描述**：consumed 标记设置职责在 §3.4 和 §4.4 之间矛盾（后端自动设置 vs 前端显式通知），将导致实现分歧
   - 所在位置：§3.4 AiSuggestionResult 状态图（line 665–666）vs §4.4 异步建议查询端点（line 940–943）
   - 严重程度：一般
   - 改进建议：统一为后端在返回 COMPLETED 状态时自动设 consumed=true，并在 §3.4 中补充文档说明边界行为

2. **问题描述**：DosageStandard 缺少变更事件通知，与同级配置实体的事件驱动刷新机制不一致，未来引入缓存后 admin 变更将无法及时传播
   - 所在位置：§9.3（line 1526）；§8 DosageStandard 定义
   - 严重程度：一般
   - 改进建议：在 §9.3 事件声明表中补充 DosageStandardChangeEvent（可标注 Phase 2/3 预留），或在 §8 中显式声明当前无缓存、变更事件推迟至引入缓存时实现

## 迭代第 10 轮

1. **问题描述**：处方提交端点缺少并发提交防护机制的具体实现位置。RX_AUDIT_CONCURRENT_SUBMIT 错误码已定义但未指明触发时机和防护手段，同 prescriptionId 的并发提交可能双重落单。
   - 所在位置：§4.2 处方提交端点（lines 853-882）；§5.1 错误码表 RX_AUDIT_CONCURRENT_SUBMIT
   - 严重程度：严重
   - 改进建议：在 §4.2 步③中补充并发防护手段（乐观锁 `@Version` 或数据库唯一约束），并说明 RX_AUDIT_CONCURRENT_SUBMIT 的触发时机。

2. **问题描述**：需求文档引用的 Phase 5 包G OOD 参考文档未充分分析。产出仅简略提及 AiResult 重载工厂方法冲突，未系统分析包G架构约束对本设计"底座直接落地 + Phase 5 迁移"目标的影响。
   - 所在位置：§1.1, §1.1b，全文
   - 严重程度：一般
   - 改进建议：新增一节或扩展 §1.1b，系统分析包G OOD 中与本设计相关的架构决策点（AiService 接口签名可变性、底座分层架构一致性、Store 接口 package 路径一致性），识别兼容性风险和迁移前置条件。

3. **问题描述**：MedicalRecordField.TREATMENT_ADVICE 与需求文档 3.4.3 输出字段名 treatment_plan 不匹配，映射表作为字段级契约对齐的 source of truth 出现命名偏移。
   - 所在位置：§3.3 MedicalRecordField 映射表（lines 601-609）
   - 严重程度：一般
   - 改进建议：将枚举值改为 TREATMENT_PLAN 与需求文档严格对齐，或增加标注列"设计侧枚举值"。

4. **问题描述**：DrugInteractionPair 实体在 Phase 2/3 范围内无消费者。DrugInteractionRule 标注为"不启用"预留骨架，但 DrugInteractionPair 作为 JPA @Entity 已在目录结构中定义为正式实体（含 Repository），建表后产生空表。
   - 所在位置：§2.1 prescription/rule/entity/DrugInteractionPair.java；§3.2 DrugInteractionRule 描述（line 554）
   - 严重程度：一般
   - 改进建议：二选一：(a) 标注为"Phase 4 预留，当前版本不建表"并控制 DDL；(b) 从 Phase 2/3 目录移除，移至 §10 扩展规划中以注释形式预留。

5. **问题描述**：DoctorFacade、VisitFacade、DrugFacade 超时配置（均默认 2s）仅散见于各章节正文，未在 §5.5 配置表中集中收集，运维人员无法在一处获取全部超时配置。
   - 所在位置：§5.5 超时配置表（lines 1313-1322）
   - 严重程度：一般
   - 改进建议：在 §5.5 表中新增三行配置项。

6. **问题描述**：处方版本校验的结构化比较（drugId + dose + frequency + duration + route）未定义全 null/空列表、null 与 0 等价性、字段顺序差异等边界行为。
   - 所在位置：§4.2 处方提交端点（line 845）
   - 严重程度：一般
   - 改进建议：补充明确规则——双方均为 null 或空列表视为一致，一方为空另一方非空视为不一致；null 与 0/空字符串按业务语义等价处理。