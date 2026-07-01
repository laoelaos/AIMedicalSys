# 再审议判定报告（v10）

## 判定结果

RETRY

## 判定理由

诊断报告共识别 8 个问题（1 严重 + 5 一般 + 2 轻微），质询报告以 LOCATED 结果确认了所有问题的有效性，内部循环提前终止（实际 1 轮 < 最大 12 轮）。因严重问题（D1）和多个一般问题（R1、L1、L2、D2、D3）均未得到修复，不符合 PASS 条件，判定 RETRY。

## 需要解决的问题（仅 RETRY 时存在）

- **问题描述**：处方提交端点缺少并发提交防护机制的具体实现位置。RX_AUDIT_CONCURRENT_SUBMIT 错误码已定义但未指明触发时机和防护手段，同 prescriptionId 的并发提交可能双重落单。
- **所在位置**：§4.2 处方提交端点（lines 853-882）；§5.1 错误码表 RX_AUDIT_CONCURRENT_SUBMIT
- **严重程度**：严重
- **改进建议**：在 §4.2 步③中补充并发防护手段（乐观锁 `@Version` 或数据库唯一约束），并说明 RX_AUDIT_CONCURRENT_SUBMIT 的触发时机。

---

- **问题描述**：需求文档引用的 Phase 5 包G OOD 参考文档未充分分析。产出仅简略提及 AiResult 重载工厂方法冲突，未系统分析包G架构约束对本设计"底座直接落地 + Phase 5 迁移"目标的影响。
- **所在位置**：§1.1, §1.1b，全文
- **严重程度**：一般
- **改进建议**：新增一节或扩展 §1.1b，系统分析包G OOD 中与本设计相关的架构决策点（AiService 接口签名可变性、底座分层架构一致性、Store 接口 package 路径一致性），识别兼容性风险和迁移前置条件。

---

- **问题描述**：MedicalRecordField.TREATMENT_ADVICE 与需求文档 3.4.3 输出字段名 treatment_plan 不匹配，映射表作为字段级契约对齐的 source of truth 出现命名偏移。
- **所在位置**：§3.3 MedicalRecordField 映射表（lines 601-609）
- **严重程度**：一般
- **改进建议**：将枚举值改为 TREATMENT_PLAN 与需求文档严格对齐，或增加标注列"设计侧枚举值"。

---

- **问题描述**：DrugInteractionPair 实体在 Phase 2/3 范围内无消费者。DrugInteractionRule 标注为"不启用"预留骨架，但 DrugInteractionPair 作为 JPA @Entity 已在目录结构中定义为正式实体（含 Repository），建表后产生空表。
- **所在位置**：§2.1 prescription/rule/entity/DrugInteractionPair.java；§3.2 DrugInteractionRule 描述（line 554）
- **严重程度**：一般
- **改进建议**：二选一：(a) 标注为"Phase 4 预留，当前版本不建表"并控制 DDL；(b) 从 Phase 2/3 目录移除，移至 §10 扩展规划中以注释形式预留。

---

- **问题描述**：DoctorFacade、VisitFacade、DrugFacade 超时配置（均默认 2s）仅散见于各章节正文，未在 §5.5 配置表中集中收集，运维人员无法在一处获取全部超时配置。
- **所在位置**：§5.5 超时配置表（lines 1313-1322）
- **严重程度**：一般
- **改进建议**：在 §5.5 表中新增三行配置项。

---

- **问题描述**：处方版本校验的结构化比较（drugId + dose + frequency + duration + route）未定义全 null/空列表、null 与 0 等价性、字段顺序差异等边界行为。
- **所在位置**：§4.2 处方提交端点（line 845）
- **严重程度**：一般
- **改进建议**：补充明确规则——双方均为 null 或空列表视为一致，一方为空另一方非空视为不一致；null 与 0/空字符串按业务语义等价处理。
