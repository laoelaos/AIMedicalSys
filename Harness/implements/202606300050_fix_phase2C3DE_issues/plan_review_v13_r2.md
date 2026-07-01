# 计划审查报告（v13 r2）

## 审查结果
REJECTED

## 发现

### 1. [严重] 缺失 P0 问题：P01（异步 AI 调度）、A03（AiSuggestionResult 状态映射）、P02/E06（DrugFacade 注入）

**位置**：plan.md 实施路线表（第 8-27 行），全部 18 轮无任何一轮覆盖以下问题。

**问题**：实现报告 `06_phase2C3DE_report.md` 列出了以下 P0 问题，在当前计划（v13）的 R1-R18 中完全未被覆盖：

- **P01**（P0）：`DedupTaskScheduler.schedule()` 创建 PENDING 条目后立即返回，无任何代码触发 `AiService.prescriptionAssist()` 异步调用并回填结果。实现报告明确要求"此修复必须与 A03 同步实施"。
- **A03**（P0）：AiSuggestionResult 5 状态映射表（PENDING → COMPLETED/FAILED + consumed 标记）全部未实现。实现报告明确要求"此修复必须与 P01 同步实施"。
- **P02/E06**（P0）：DrugFacade 在 PrescriptionAuditServiceImpl 和 PrescriptionAssistServiceImpl 均未注入。实现报告要求"在两个 Service 的构造器注入 DrugFacade"。

**为什么是问题**：需求 description（requirement.md）明确要求"修复实现报告中列出的**所有**问题"。3 项 P0 问题（最高优先级）完全未纳入计划，且排期外说明（plan.md 第 31 行）仅解释了 P09/P12/P15 三个 P2 问题，未对这些 P0 遗漏做任何说明。

**修正方向**：
1. 增加覆盖 P01+A03 的轮次（异步 AI 调度完整管线），建议置于 R15（AI 超时/降级框架）之后、R16（TTL/事件）之前，因为 P01/A03 的异步 AI 调度依赖超时配置（R15 A02）和 Store 接口修复（R17 S01/S03）；
2. 增加覆盖 P02/E06 的轮次（DrugFacade 注入），可并行归入 R13 或作为独立轮次；
3. 若确有理由不纳入当前实现计划，须在"排期外说明"中明确阐述理由和后续安排。

### 2. [一般] 缺失 P1 问题未被覆盖且无说明

**位置**：plan.md 全部轮次。

**问题**：以下 P1 问题在计划中既未被覆盖也未被解释排除原因：

| 问题 | 描述 |
|------|------|
| A01 | AiResultFactory 在全部 4 个业务 Service 实现中零引用 |
| P06 | 降级路径 LocalRuleResult 未转换为 AuditAlert |
| P07 | AuditRecord.auditIssues 字段从未写入 |
| P08 | forceSubmit 路径在生成 prescriptionOrderId 后未回写到 AuditRecord |
| P10 | DosageThresholdService.matchByPriority 第一、二重循环逻辑重复 |
| P13 | AllergyCheckRule 结构化匹配未命中时不回退到文本匹配 |
| P16 | AuditRecordRepository 没有 List 版本 findByPrescriptionOrderIdAndIsLatestTrue |

**为什么是问题**：需求描述要求修复"所有问题"。P1 问题严重影响业务逻辑，7 项 P1 遗漏会使验收时发现修复不完整。当前排期外说明仅覆盖 P2。

**修正方向**：
1. 将 A01 纳入 R9（AiResult 契约修复群组）——A01（使用 AiResultFactory）是 A07/A09/A11 修复的自然扩展；
2. 将 P06/P07/P08/P16 纳入 prescription 模块修复群组（R13/R14 范围）；
3. 将 P10（DosageThresholdService）作为 prescription 模块独立修复；
4. 将 P13（AllergyCheckRule）作为 prescription 模块 LocalRuleEngine 完善修复；
5. 或明确在排期外说明中解释以上各项的排除理由和后续安排。

## 修改要求（仅 REJECTED 时）

1. **[严重]** 须补充 P01+A03（异步 AI 调度）和 P02/E06（DrugFacade 注入）的修复轮次，或在排期外说明中解释排除理由。这三项均为 P0，不可忽略。
2. **[一般]** 须补充或解释 7 项 P1 遗漏问题（A01、P06、P07、P08、P10、P13、P16）的处理方案。
