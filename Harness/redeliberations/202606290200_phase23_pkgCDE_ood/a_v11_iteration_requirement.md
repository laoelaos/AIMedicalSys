根据以下审查结果，迭代上一轮的产出，形成新版的文件，从而更好地满足用户需求。

## 当前审查结果

### R1 [一般] 需求文档引用的 Phase 5 包G OOD 参考文档未充分分析
- **位置**：§1.1, §1.1b，全文
- **问题**：需求文档明确列出 Phase 5 包G 初步 OOD 作为参考文档。但产出仅在 §10 简略提及 AiResult 重载工厂方法与包G "AiResult.java 不变"的潜在冲突，未系统分析包G 的架构约束（如 AiService 接口形态、底座模块划分、事件框架选型）对本设计"底座直接落地 + Phase 5 迁移"目标的影响。
- **改进建议**：新增一节（或扩展 §1.1b）系统分析包G OOD 中与本设计相关的架构决策点，识别兼容性风险和迁移前置条件。至少需确认：(a) AiService 接口签名是否可变、(b) 底座的分层架构是否与本设计假设一致、(c) common-module-api 中 Store 接口的 package 路径是否与包G 规划一致。

### R2 [轻微] "关键字段缺失提示补全"中"补全"语义缺显式解释
- **位置**：§3.3 MissingFieldDetector、§7 设计决策表
- **问题**：需求文档"关键字段缺失提示补全"的"补全"存在两种理解：(1) 自动补全、(2) 提示用户补全。产出选择了方案(2)但未在任何设计决策条目中显式论证。
- **改进建议**：在 §7 设计决策表中新增一条，说明选择"差集比对检测模式"而非"自动补全"的理由（如"避免自动补全引入错误信息，保留医生判断权"）。

### L1 [一般] MedicalRecordField.TREATMENT_ADVICE 与需求文档 3.4.3 输出字段名 treatment_plan 不匹配
- **位置**：§3.3 MedicalRecordField 映射表（lines 601-609）
- **问题**：映射表标题为"与需求文档 3.4.3 输出契约字段名的映射关系"，但 enum 值 TREATMENT_ADVICE 与需求文档字段名 treatment_plan 不一致。
- **改进建议**：将 MedicalRecordField 枚举值改为 TREATMENT_PLAN，与需求文档字段名严格对齐。若保留 TREATMENT_ADVICE，需在映射表右侧增加标注列"设计侧枚举值"。

### L2 [一般] DrugInteractionPair 实体在 Phase 2/3 范围内无消费者
- **位置**：§2.1 prescription/rule/entity/DrugInteractionPair.java；§3.2 DrugInteractionRule 描述（line 554）
- **问题**：DrugInteractionRule 明确标注为 Phase 2/3"不启用"的预留骨架，但 DrugInteractionPair 作为 JPA @Entity 已在目录结构中定义为正式实体（含 Repository），产生数据库空表。
- **改进建议**：二选一：(a) 将 DrugInteractionPair 实体标注为"Phase 4 预留，当前版本不建表"，在 @Entity 上增加 `@Table(schema = ...)` 条件建表控制；(b) 从 Phase 2/3 目录结构中移除，移至 §10 扩展规划中以注释形式预留。

### L3 [轻微] §8.4 六级匹配优先级 Level 2 缺少"ageRange 或 weightRange 部分为 null"的边界规则
- **位置**：§8.4 六级匹配优先级（lines 1482-1489）
- **问题**：Level 2 要求"ageRange 与 weightRange 均非 null"且年龄体重同时匹配。若一条 DosageStandard 记录的 ageRange 非 null 但 weightRange 部分为 null，既无法匹配 Level 2（weightRange 不完整），也不满足 Level 3（weightRange 非 both null），从而直接降级至 Level 5。
- **改进建议**：补充"部分范围字段为 null 时的匹配规则"——或定义为"该维度不参与过滤"（即 ageRange 非 null 且匹配 + weightRange 不完整则按 age 匹配通过），或定义为"不匹配"并降级至下一级。

### D1 [严重] 处方提交端点缺少并发提交防护机制的具体实现位置
- **位置**：§4.2 处方提交端点（lines 853-882）；§5.1 错误码表 RX_AUDIT_CONCURRENT_SUBMIT
- **问题**：RX_AUDIT_CONCURRENT_SUBMIT 错误码已在 §5.1 定义，但全文未指明该错误码的触发时机和具体防护机制。同 prescriptionId 的两次并发提交请求可同时进入步③写入 forceSubmitted 字段，导致双重落单。
- **改进建议**：在 §4.2 步③中补充并发防护手段——至少包含以下之一：(a) 处方状态乐观锁（`@Version`）确保 forceSubmitted 写入的原子性；(b) 数据库层唯一约束确保同 prescriptionId 的单次提交；并说明 RX_AUDIT_CONCURRENT_SUBMIT 在此防护路径中的触发时机和返回行为。

### D2 [一般] 非AI跨模块门面超时配置未在 §5.5 超时配置表中集中收集
- **位置**：§5.5 超时配置表（lines 1313-1322）
- **问题**：DoctorFacade 默认 2s、VisitFacade 默认 2s、DrugFacade 默认 2s 的超时配置仅散见于各章节正文，未在 §5.5 配置表中集中列出。
- **改进建议**：在 §5.5 表中新增三行配置项：consultation.doctor-facade.timeout=2s、medical-record.visit-facade.timeout=2s、prescription.drug-facade.timeout=2s。

### D3 [一般] 处方版本校验的"结构化比较"未定义全 null/空列表等边界行为
- **位置**：§4.2 处方提交端点（line 845）
- **问题**：结构化比较定义为 drugId + dose + frequency + duration + route 五字段组合比对。但未定义：(a) originalPrescription 中 prescriptionItems 为 null/空列表 vs 当前处方为 null/空列表的判定；(b) 字段级 null 与 0 的等价性；(c) 字段顺序差异。
- **改进建议**：补充明确规则——(a) 双方均为 null 或空列表视为一致，一方为空另一方非空视为不一致；(b) null 与 0/空字符串按业务语义等价处理（建议统一要求非空校验在前，结构化比较阶段不应出现 null 字段）。

### D4 [轻微] RecordGenerateRequest 的 encounterId → visitId fallback 路径缺少数据完整性说明
- **位置**：§3.3 RecordGenerateRequest（line 628）
- **问题**：VisitFacade 降级时将 encounterId 直接作为 visitId fallback 写入 MedicalRecord.visitId，但 visitId 与 encounterId 的格式/长度约束可能不同，后续 visit 模块通过 visitId 查询时若格式不匹配将查不到记录。
- **改进建议**：至少补充约束说明——如"fallback 写入后由 visit 模块的定时 reconciled 任务修复"或"fallback 写入时在 MedicalRecord 增加 visitIdFallback=true 标记供后续治理"。

## 历史迭代回顾

分析历史反馈（iteration_history.md）与当前反馈的关系：

- **已解决的问题**（历史反馈中提及、当前反馈中不再提及的）：
  - DrugFacade 超时配置边界和降级策略（第6轮）
  - §8.4 描述文本"1→2→3→4→5"与实际6级不符（第6轮）
  - RX_AUDIT_FORCE_SUBMIT_INVALID 错误码缺失（第6轮）
  - AiSuggestionResult TTL 清理间隔参数（第6轮）
  - PrescriptionDraftContext CRITICAL 告警缺少 /assist 写入路径（第7轮）
  - DedupTaskScheduler 去重原子性（第7轮）
  - 阻断竞态防护 SubmitContext 快照（第7轮）
  - DeadLetterCompensationService 无限循环风险（第7轮）
  - AiResult partialData 语义（第7轮）
  - DeadLetterCompensation 缺少 departmentName（第8轮）
  - AI 输入校验错误码无生产者（第8轮）
  - Service 接口缺少显式方法签名（第8轮）
  - patientId 降级查询排序标准（第8轮）
  - RegistrationEvent 缺少 departmentName（第8轮）
  - @Retryable 异常类型过滤（第8轮）
  - TriageRecord.sessionId 唯一约束（第8轮）
  - 独立 ScheduledExecutorService 管理（第8轮）
  - consumed 标记设置职责矛盾（第9轮）
  - DosageStandard 变更事件通知（第9轮）

- **持续存在的问题**（多轮反馈中反复出现，需重点解决）：
  - D1 [严重] 处方提交端点并发防护 — 第10轮即已识别，本轮D1再次确认，且第7轮已有相关阻断竞态防护问题
  - R1 [一般] Phase 5 包G OOD 参考文档分析 — 第10轮即已识别，本轮R1再次确认
  - L1 [一般] TREATMENT_ADVICE 命名对齐 — 第10轮即已识别，本轮L1再次确认
  - L2 [一般] DrugInteractionPair 空实体 — 第10轮即已识别，本轮L2再次确认
  - D2 [一般] 门面超时配置集中收集 — 第10轮即已识别，本轮D2再次确认
  - D3 [一般] 结构化比较边界规则 — 第10轮即已识别，本轮D3再次确认

- **新发现的问题**（本轮新识别）：
  - R2 [轻微] "补全"语义缺显式解释
  - L3 [轻微] Level 2 年龄/体重部分字段为 null 的边界规则
  - D4 [轻微] encounterId → visitId fallback 数据完整性

## 上一轮产出路径
C:\Develop\Software\AIMedicalSys\Harness\redeliberations\202606290200_phase23_pkgCDE_ood\a_v10_copy_from_v9.md

## 用户需求
C:\Develop\Software\AIMedicalSys\Harness\redeliberations\202606290200_phase23_pkgCDE_ood\requirement.md
