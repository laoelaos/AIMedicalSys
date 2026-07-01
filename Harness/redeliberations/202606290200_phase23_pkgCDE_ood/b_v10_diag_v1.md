# 质量审查报告 — 第 10 轮

审查对象：Phase 2/3 包C/D-AI1/D-AI2/E OOD 设计方案（v10，自 v9 复制）
审查视角：需求响应充分度、事实/逻辑正确性、深度与完整性（侧重落地编码指导）
排除范围：组件A内部审议已覆盖的技术可行性等维度

---

## 维度一：需求响应充分度

### R1 [一般] 需求文档引用的 Phase 5 包G OOD 参考文档未充分分析

**位置**：§1.1, §1.1b，全文
**问题**：需求文档明确列出 Phase 5 包G 初步 OOD（`Harness\redeliberations\202606271627_phase5_pkgG_ood\a_v22_copy_from_v21.md`）作为参考文档。但产出仅在 §10 简略提及 AiResult 重载工厂方法与包G "AiResult.java 不变"的潜在冲突，未系统分析包G 的架构约束（如 AiService 接口形态、底座模块划分、事件框架选型）对本设计"底座直接落地 + Phase 5 迁移"目标的影响。缺少对目标基线的分析，迁移目标的可达性缺少上游约束验证。
**改进建议**：新增一节（或扩展 §1.1b）系统分析包G OOD 中与本设计相关的架构决策点，识别兼容性风险和迁移前置条件。至少需确认：(a) AiService 接口签名是否可变、(b) 底座的分层架构是否与本设计假设一致、(c) common-module-api 中 Store 接口的 package 路径是否与包G 规划一致。

### R2 [轻微] "关键字段缺失提示补全"中"补全"语义缺显式解释

**位置**：§3.3 MissingFieldDetector、§7 设计决策表
**问题**：需求文档"关键字段缺失提示补全"的"补全"存在两种合理理解：(1) 自动补全缺失字段、(2) 提示用户补全。产出选择了方案(2)（差集比对检测模式，不修改 AI 产出），但未在任何设计决策条目中显式论证此选择。若后续审查或接手团队误理解为自动补全，将产生实现分歧。
**改进建议**：在 §7 设计决策表中新增一条，说明选择"差集比对检测模式"而非"自动补全"的理由（如"避免自动补全引入错误信息，保留医生判断权"），与现有 MissingFieldDetector 决策条目相邻。

---

## 维度二：事实错误或逻辑矛盾

### L1 [一般] MedicalRecordField.TREATMENT_ADVICE 与需求文档 3.4.3 输出字段名 treatment_plan 不匹配

**位置**：§3.3 MedicalRecordField 映射表（lines 601-609）
**问题**：映射表标题为"与需求文档 3.4.3 输出契约字段名的映射关系"，但 enum 值 TREATMENT_ADVICE 与需求文档字段名 treatment_plan 不一致。虽然语义等效（均为"治疗意见"），但映射表作为字段级契约对齐的 source of truth 出现命名偏移，Converter 实现者需额外记忆"TREATMENT_ADVICE = treatment_plan"的等价关系，增加出错概率。
**改进建议**：将 MedicalRecordField 枚举值改为 TREATMENT_PLAN，与需求文档字段名严格对齐。若保留 TREATMENT_ADVICE，需在映射表右侧增加标注列"设计侧枚举值"。

### L2 [一般] DrugInteractionPair 实体在 Phase 2/3 范围内无消费者

**位置**：§2.1 prescription/rule/entity/DrugInteractionPair.java；§3.2 DrugInteractionRule 描述（line 554）
**问题**：DrugInteractionRule 明确标注为 Phase 2/3"不启用"的预留骨架，但 DrugInteractionPair 作为 JPA @Entity 已在目录结构中定义为正式实体（含 Repository）。建表后无 CRUD 操作、无规则消费，产生数据库空表。即使预留 Phase 4 使用，当前版本包含空实体定义会引发无谓的 DDL 执行、测试覆盖挑战和代码审查困惑。
**改进建议**：二选一：(a) 将 DrugInteractionPair 实体标注为"Phase 4 预留，当前版本不建表"，在 @Entity 上增加 `@Table(schema = ...)` 条件建表控制，或排除在 ddl-auto 之外；(b) 从 Phase 2/3 目录结构中移除，移至 §10 扩展规划中以注释形式预留。

### L3 [轻微] §8.4 六级匹配优先级 Level 2 缺少"ageRange 或 weightRange 部分为 null"的边界规则

**位置**：§8.4 六级匹配优先级（lines 1482-1489）
**问题**：Level 2 要求"ageRange 与 weightRange 均非 null"且年龄体重同时匹配。若一条 DosageStandard 记录的 ageRange 非 null 但 weightRange 部分为 null（如 weightRangeStart 有值、weightRangeEnd 为 null），既无法匹配 Level 2（weightRange 不完整），也不满足 Level 3（weightRange 非 both null），从而直接降级至 Level 5"无分级默认阈值"。此场景在管理员维护不完整数据时可能发生。
**改进建议**：补充"部分范围字段为 null 时的匹配规则"——或定义为"该维度不参与过滤"（即 ageRange 非 null 且匹配 + weightRange 不完整则按 age 匹配通过），或定义为"不匹配"并降级至下一级。需明确一个实现可执行的规则语义。

---

## 维度三：深度与完整性

### D1 [严重] 处方提交端点缺少并发提交防护机制的具体实现位置

**位置**：§4.2 处方提交端点（lines 853-882）；§5.1 错误码表 RX_AUDIT_CONCURRENT_SUBMIT
**问题**：RX_AUDIT_CONCURRENT_SUBMIT 错误码已在 §5.1 定义，但全文未指明该错误码的触发时机和具体防护机制。同 prescriptionId 的两次并发提交请求在步① CRITICAL 检查、步② 审核阻断检查均通过后，可同时进入步③写入 forceSubmitted 字段，导致双重落单。SubmitContext 仅防护 CRITICAL 竞态，不解决提交本身的并发问题。
**改进建议**：在 §4.2 步③中补充并发防护手段——至少包含以下之一：(a) 处方状态乐观锁（`@Version`）确保 forceSubmitted 写入的原子性；(b) 数据库层唯一约束确保同 prescriptionId 的单次提交；并说明 RX_AUDIT_CONCURRENT_SUBMIT 在此防护路径中的触发时机和返回行为。

### D2 [一般] 非 AI 跨模块门面超时配置未在 §5.5 超时配置表中集中收集

**位置**：§5.5 超时配置表（lines 1313-1322）
**问题**：DoctorFacade 默认 2s（§3.1）、VisitFacade 默认 2s（§3.3）、DrugFacade 默认 2s（§2.2）的超时配置仅散见于各章节正文，未在 §5.5 配置表中集中列出。§5.5 标题虽为"AI 超时配置"，但表中本身已包含非 AI 配置项（triage.max-context-chars），将门面超时一并收集可使运维人员在一处文档获取全部超时配置，避免遗漏。
**改进建议**：在 §5.5 表中新增以下三行：

| 配置键 | 默认值 | 说明 |
|--------|--------|------|
| consultation.doctor-facade.timeout | 2s | DoctorFacade 跨模块调用超时 |
| medical-record.visit-facade.timeout | 2s | VisitFacade 跨模块调用超时 |
| prescription.drug-facade.timeout | 2s | DrugFacade 跨模块调用超时 |

### D3 [一般] 处方版本校验的"结构化比较"未定义全 null/空列表等边界行为

**位置**：§4.2 处方提交端点（line 845）
**问题**：结构化比较定义为 drugId + dose + frequency + duration + route 五字段组合比对。但未定义以下边界行为：(a) originalPrescription 中 prescriptionItems 为 null / 空列表 vs 当前处方为 null / 空列表——是否视为"一致"？(b) 字段级 null 与 0 的等价性——dose=null 与 dose=0 是否视为一致？(c) 字段顺序差异——药品列表顺序不同但内容相同是否视为一致？当前描述"忽略 JSON 文本级格式差异"涵盖(c)但不涵盖(a)(b)。
**改进建议**：补充明确规则——(a) 双方均为 null 或空列表视为一致，一方为空另一方非空视为不一致；(b) null 与 0/空字符串按业务语义等价处理（建议统一要求非空校验在前，结构化比较阶段不应出现 null 字段）。

### D4 [轻微] RecordGenerateRequest 的 encounterId → visitId fallback 路径缺少数据完整性说明

**位置**：§3.3 RecordGenerateRequest（line 628）
**问题**：VisitFacade 降级时将 encounterId 直接作为 visitId fallback 写入 MedicalRecord.visitId，但 visitId 与 encounterId 的格式/长度约束可能不同（如 visitId 可能是数字序列而 encounterId 是带前缀的字符串）。后续 visit 模块通过 visitId 查询时若格式不匹配将查不到记录，导致 MedicalRecord 与 Visit 的数据关联断裂。
**改进建议**：至少补充约束说明——如"fallback 写入后由 visit 模块的定时 reconciled 任务修复"或"fallback 写入时在 MedicalRecord 增加 visitIdFallback=true 标记供后续治理"。

---

## 总体评价

产出在需求响应维度总体充分，四个业务包的核心功能均已被覆盖，底座落地和 Phase 5 迁移策略已较为成熟。主要质量问题集中在：(1) 对 Phase 5 包G 上游参考文档的分析缺失，是本设计完整性的显著缺口；(2) 处方提交端点的并发防护仅有错误码定义而无实现位置，落地时存在隐患；(3) 若干细节（枚举命名对齐、空表实体、配置分散、边界规则缺失）虽不致命，但将增加实现时的摩擦成本和审查成本。建议修复者优先处理 D1（并发防护）和 R1（包G 参考分析），其次处理 L1-L3（事实/逻辑问题），其余为增强性建议。
