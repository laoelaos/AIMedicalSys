# 质量审查报告 — v17（Phase 2/3 OOD 设计方案）

## 审查概述

- **待审查产出**：`a_v17_copy_from_v16.md`（Phase 2/3 包C/D-AI1/D-AI2/E 架构级 OOD 设计方案，v17）
- **审查范围**：事实正确性、逻辑连贯性、与历史迭代问题（1–16 轮）的解决完整性、与需求文档契约的对齐程度
- **审查方法**：逐节通读 + 交叉验证 16 轮迭代中 100+ 条历史问题的解决状态

## 总体评价

文档经过 16 轮迭代演进，绝大多数历史重大问题（100+ 条）已通过系统性修订解决。结构趋于成熟——核心抽象完整、模块划分清晰、行为契约细致、设计决策可追溯。v17 版本对迭代 16 轮遗留的 6 个严重/一般问题均完成了闭环修复，增量内容的集成质量良好。

但仍存在以下需要纠正的质量问题：

---

## 问题 1（严重）— AiResult 字段定义与设计决策自相矛盾

### 问题描述

**§2.3**（AiService 接口定义段首）明确指出 AiResult 封装结果"含 success/data/errorCode/degraded/fallbackReason/partialData **六字段**"，即声称 AiResult 拥有一个名为 `partialData` 的独立字段。

但 **§7 设计决策 "AiResult 超时降级模式"** 明确决议：**"使用现有 AiResult.data 字段承载部分结果（不新增 partialData 字段）"**，并新增 `AiResult.failure(String errorCode, T partialData)` 和 `AiResult.degraded(String fallbackReason, T partialData)` 重载——将 partialData 作为工厂方法的**入参**，数据写入现有的 `data` 字段。

同一文档中两处表述直接矛盾——编码者无法确定 AiResult 是否包含 `partialData` 字段，可能产生编译期字段引用错误。

### 所在位置

- §2.3（第285行附近）："含 success/data/errorCode/degraded/fallbackReason/partialData 六字段"
- §7 设计决策 "AiResult 超时降级模式"（第1014行附近）："使用现有 AiResult.data 字段承载部分结果（不新增 partialData 字段）"

### 严重程度

严重 — 指向编码的字段定义不一致，将在实现阶段直接导致类型定义分歧（5字段 vs 6字段）。

### 改进建议

二选一统一即可（**推荐路径**）：删除 §2.3 中的 "partialData" 字段引用，改为"含 success/data/errorCode/degraded/fallbackReason 五字段"，同时在重载方法说明处补充"partialData 通过重载工厂方法入参传入，数据写入 data 字段"。或者——若设计层决定保留 partialData 字段，则在 §7 中删除"不新增 partialData 字段"的表述并说明保留理由。

---

## 问题 2（一般）— chiefComplaint 与 additionalResponses 互斥违规的处理未定义

### 问题描述

§1.3、§3.1 和 §4.1 均声明 "chiefComplaint 与 additionalResponses 互斥——首轮仅传 chiefComplaint，后续轮仅传 additionalResponses + sessionId，不同时提供"。§3.1 补充"TriageServiceImpl 在处理时校验正确的字段组合规则"——但**未定义**当违规发生时（同时提供二者或均未提供）后端返回何种错误码或执行何种行为。

前端开发者和后端实现者对"错误的字段组合"缺乏统一的错误处理约定，可能导致前后端对违规行为的处理不一致。

### 所在位置

- §1.3 DialogueCreateRequest 字段说明行
- §3.1 "chiefComplaint 与 additionalResponses 的互斥语义"段落
- §4.1 API 契约行

### 严重程度

一般 — 缺失在 Phase 2/3 中未必频繁触发（前端框架可避免），但属于 API 契约缺口。

### 改进建议

在 §3.1 或 §5.1 中补充校验违规的错误码定义，例如 `TRIAGE_FIELD_COMBINATION_INVALID`，并说明后端在检测到互斥字段冲突时返回 400 + 该错误码。建议在 §4.1 的 API 契约描述中增加一行错误响应说明。

---

## 问题 3（一般）— PrescriptionAssistRequest 未列入 §1.3 包E 核心抽象一览

### 问题描述

PrescriptionAssistRequest（业务层 DTO，/assist 主端点请求体）出现在 §2.1 目录结构（`dto/assist/PrescriptionAssistRequest.java`）和 §3.4 详细定义中，但**未列入 §1.3 包E 核心抽象一览表**。对比包D-AI1 的 AuditRequest 已列入核心抽象表，同样的关键请求 DTO 在不同包之间的收录标准不一致。

此遗漏降低文档前置摘要的完整性和快速查阅效率——读者需从 §2.1 目录发现此 DTO，再跳转到 §3.4 查看定义，而非在 §1.3 中直接定位。

### 所在位置

- §1.3 包E 核心抽象一览表（缺少 PrescriptionAssistRequest 条目）
- §2.1 目录（存在该文件引用）
- §3.4（存在详细字段定义）

### 严重程度

一般 — 不影响设计正确性，影响文档一致性和查阅效率。

### 改进建议

在 §1.3 包E 核心抽象表中新增一条：
```
| PrescriptionAssistRequest | class（DTO） | AI 辅助开方主端点请求值对象，对齐需求文档 3.4.10 输入契约。包含 diagnosis/examResults/patientInfo/existingPrescription/encounterId |
```

---

## 问题 4（轻微）— triage.max-context-chars 配置项未列入 §5.5 配置表

### 问题描述

§3.1 "全量拼接 Token 超限风险评估"段落定义了一个可配置参数 `triage.max-context-chars`（默认 3000 字符），用于控制对话上下文截断阈值。但 **§5.5 AI 超时配置表**中仅列出了 AI 调用超时相关的配置项，未包含此业务参数。

编码者需通过全文搜索才能发现此参数的存在和默认值。

### 所在位置

- §3.1 全量拼接 Token 超限风险评估段落
- §5.5 AI 超时配置表（缺少此行）

### 严重程度

轻微 — 功能不受影响，参数值和用途在 §3.1 中已完整定义，仅在配置管理可见性上有缺失。

### 改进建议

在 §5.5 配置表中追加一行（可作为 "业务参数" 子项，或单独创建一个面向开发者的配置汇总表）：

```
| 分诊上下文截断阈值 | triage.max-context-chars | 3000 | 对话上下文累计字符数阈值，超出后截断中间轮次 |
```

---

## 已确认无显著问题的范围

以下维度经审查确认在 v17 中已达到可交付质量：

- **历史问题闭环完整性**：16 轮迭代报告中的 100+ 条问题均已在修订说明中逐条追踪且内容上确认修复，未见漏修或部分修复
- **需求文档契约对齐**：§3.1–§3.4 的 DTO 字段定义与需求文档 3.4.1/3.4.2/3.4.3/3.4.10 的输入输出契约严格对齐；§4.x 行为契约完整
- **架构一致性**：模块划分（3 个扁平模块）、依赖方向（common + common-module-api + ai-api）、底座落地策略与 Phase 0/1 风格一致
- **设计决策可追溯性**：§7 中 60+ 条设计决策均有选择理由和替代方案说明，形成完整的决策记录

---

## 报告元信息

- **审查轮次**：第 1 轮（首轮审查）
- **审查时间**：2026-06-29
- **审查人**：质量审查 Agent
