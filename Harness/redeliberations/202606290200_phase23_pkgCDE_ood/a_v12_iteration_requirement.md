根据以下审查结果，迭代上一轮的产出，形成新版的文件，从而更好地满足用户需求。

## 当前审查结果

### D1 [严重] 处方提交端点缺少正式 DTO 定义和完整 API 契约
- 位置：§4.2（line 864）、§4.6（lines 1154-1166）
- 问题：(a) POST /api/prescription/submit 的请求/响应 DTO 无正式定义；(b) §4.6 缺少非 forceSubmit 场景和正常提交场景的完整 JSON 示例；(c) "forceSubmit=false 且无最新审核结果"时的"常规审核流程"具体路径未定义；(d) "常规提交流程"在无审核结果或 PASS 时的 DTO 层面定义缺失
- 改进建议：补充 SubmitRequest/SubmitResponse DTO 定义（类名、包路径、字段表、约束）；补充正常提交和 forceSubmit=false+WARN 场景的完整 JSON 示例；明确"常规审核流程"是否复用 PrescriptionAuditService.audit()；定义 auditRecordId 必填/可选语义边界

### D2 [严重] visitIdFallback reconciled 任务仅有概念引用，无实现定义
- 位置：§3.3 RecordGenerateRequest（line 638）
- 问题：reconciled 任务在全文无定义——无调度时机、无触发条件、无实现机制、无负责模块，属于"欠债"式设计
- 改进建议：二选一——(A) 在 §6.1 定义 reconciled 定期任务（每 30 分钟），扫描 visitIdFallback=true 记录，通过 encounterId 反查 visit 模块修复；(B) 标注"Phase 2/3 仅设标记，reconciled 推迟至 Phase 4/5"

### D3 [严重] DialogueSession 事务一致性策略未覆盖 QA 历史等状态丢失
- 位置：§3.1（line 426）
- 问题：仅评估了 aiFailCount 丢失的影响，未覆盖 (a) QA 历史列表丢失导致上下文拼接不完整；(b) correctedChiefComplaint 丢失导致主诉修正信息丢失；(c) 对话轮次计数器丢失导致截断策略失准
- 改进建议：扩展事务一致性策略覆盖 DialogueSession 全部可变状态；可选路径包括承认可接受数据丢失范围并显式声明、TriageRecord 存储关键上下文快照、或调整写入顺序

### D4 [一般] AiResultFactory 类未在目录结构中注册
- 位置：§1.1c（line 37）、§10（line 1575）
- 问题：(a) 未出现在 §2.1 目录结构；(b) 无包路径声明；(c) 无具体方法签名定义
- 改进建议：在 §2.1 ai-api 模块补充 AiResultFactory.java 条目；在 §2.3 给出工厂方法签名（至少包含 failure()、degraded() 重载）

### I1 [一般] 缺少领域事件统一目录
- 位置：全文散布
- 问题：10+ 种领域事件无集中目录，新开发者需全文检索了解事件驱动全貌
- 改进建议：在 §2.2 或 §9.3 后新增领域事件目录表格（事件名称、定义位置、发布模块、消费模块、触发条件、事务边界、Phase 范围）

### I2 [一般] 错误码→HTTP 状态码映射缺少集中声明
- 位置：§5.1（line 1300-1309）
- 问题：仅在 §4.x 散落标注部分映射关系，GlobalExceptionHandler 实现者需全文搜集
- 改进建议：在 §5.1 错误码表增加一列 HTTP 状态码，或新增映射表

### I3 [一般] 六级匹配优先级 Level 2 边界规则的实现复杂度已升高
- 位置：§8.4（line 1500）
- 问题：多字段 null/非 null 组合产生 2^4 = 16 种状态，缺少测试用例级别的行为规格
- 改进建议：补充伪代码或决策表，列出关键路径和预期降级优先级；标注需覆盖的边界测试用例

### I4 [轻微] DosageStandard 数据库索引未定义
- 位置：§8.4（line 1486-1495）
- 问题：缺少 (drugCode, routeOfAdministration) 复合索引，大规模数据下可能全表扫描
- 改进建议：补充索引说明，建议建立复合索引加速匹配查询

### I5 [轻微] MockAiService 运行时策略切换的并发安全边界未定义
- 位置：§2.3（line 383-388）
- 问题：未定义切换时正在执行的 AI 调用使用旧策略还是新策略、策略存储结构、多线程影响
- 改进建议：补充 volatile 或 AtomicReference 策略存储，声明"正在执行调用不受策略切换影响"

### I6 [轻微] 统一错误格式与 BlockResponse 的边界未覆盖所有提交端点错误
- 位置：§4.6（lines 1261-1286）
- 问题：处方提交端点步③的 RX_AUDIT_PRESCRIPTION_MODIFIED、RX_AUDIT_CONCURRENT_SUBMIT 等错误未明确使用何种格式
- 改进建议：补充说明——步①/步②阻断使用 BlockResponse（422），步③ forceSubmit 校验失败使用统一错误格式

## 历史迭代回顾

### 已解决的问题
以下问题出现在历史反馈中，当前反馈不再提及，视为已解决：
- Phase 5 包G 参考文档分析（第10轮 R1，已通过新增 §1.1c 解决）
- MedicalRecordField.TREATMENT_ADVICE 命名与需求文档不匹配（第10轮）
- DrugInteractionPair 实体无消费者（第10轮）
- DrugFacade 超时配置和降级策略（第6轮）
- 描述文本"1→2→3→4→5"与六级优先级不符（第6轮）
- RX_AUDIT_FORCE_SUBMIT_INVALID 错误码缺失（第6轮）
- AiSuggestionResult TTL 清理周期（第6轮）
- PrescriptionDraftContext CRITICAL 告警遗漏 /assist 路径（第7轮）
- DedupTaskScheduler 并发去重原子性（第7轮）
- 阻断竞态防护快照存储位置（第7轮）
- DeadLetterCompensationService 重试循环风险（第7轮）
- AiResult.prescriptionAssist() partialData 语义（第7轮）
- DeadLetterCompensation departmentName 缺失（第8轮）
- AI 输入校验错误码无生产者（第8轮）
- 核心业务 Service 接口缺少显式方法签名（第8轮）
- patientId 降级查询排序标准（第8轮）
- RegistrationEvent departmentName 缺失（第8轮）
- @Retryable 异常过滤器（第8轮）
- TriageRecord.sessionId 唯一约束（第8轮）
- ScheduledExecutorService 统一管理（第8轮）
- consumed 标记设置职责矛盾（第9轮）
- DosageStandard 变更事件通知（第9轮）
- 处方提交端点并发防护机制（第10轮）
- MedicalRecordField 命名对齐（第10轮）
- DrugInteractionPair 预留标注（第10轮）
- 门面超时配置集中收集（第10轮）
- 处方版本校验边界规则（第10轮）

### 持续存在的问题
以下问题在多轮反馈中反复出现，需重点解决：
- **D1/第6轮→第11轮**：处方提交端点缺少正式 DTO 定义和完整 API 契约 —— 第6轮即提出，至今未完全解决
- **D2/第6轮→第11轮**：visitIdFallback reconciled 任务定义缺失 —— 第6轮即提出，至今未解决
- **D3/第6轮→第11轮**：DialogueSession 事务一致性策略覆盖不足 —— 第6轮即提出，至今未完全解决
- **D4/第6轮→第11轮**：AiResultFactory 类未在目录结构注册 —— 第6轮即提出，至今未解决
- **I1/第6轮→第11轮**：缺少领域事件统一目录 —— 第6轮即提出，至今未解决
- **I2/第6轮→第11轮**：错误码→HTTP 状态码映射缺少集中声明 —— 第6轮即提出，至今未解决
- **I3/第6轮→第11轮**：六级匹配优先级 Level 2 边界规则实现复杂度 —— 第6轮即提出，至今未完全解决

### 新发现的问题
本轮新增的问题：
- I4：DosageStandard 数据库索引未定义
- I5：MockAiService 策略切换并发安全边界未定义
- I6：统一错误格式与 BlockResponse 边界未覆盖所有提交场景

## 上一轮产出路径
C:\Develop\Software\AIMedicalSys\Harness\redeliberations\202606290200_phase23_pkgCDE_ood\a_v11_copy_from_v10.md

## 用户需求
C:\Develop\Software\AIMedicalSys\Harness\redeliberations\202606290200_phase23_pkgCDE_ood\requirement.md
