# 计划审查报告（v25 r1）

## 审查结果
REJECTED

## 发现

### **[一般]** P11 和 P14 两项 P1 问题在计划中完全遗漏

**问题**：实现报告 `Docs/Diagnosis/impl/06_phase2C3DE_report.md` 中定义了以下两项 P1 问题，但既未出现在计划任何轮次中，也未在「排期外说明」中提及：

- **P11** — SpecialPopulationDosageRule 与 DosageLimitRule 使用相同通用 DosageStandard 查询，未区分特殊人群分级，可能产生重复告警
- **P14** — PrescriptionAssistServiceImpl 各失败路径未写入 PrescriptionDraftContext，AI 不可用时 CRITICAL 阻断未持久化

计划 header 声明"修复 phase2 C3 DE 实现报告中的全部 P0—P2 问题"，但 P11 和 P14（均为 P1）完全缺失。五项排期外项（P09、P10、P12、P13、P15）有明确解释，但 P11 和 P14 未被任何形式覆盖或说明理由。

**修正方向**：将 P11 和 P14 纳入实施范围——可以（a）在剩余轮次中补充安排，或（b）在排期外说明中明确记录排除理由和后续安排。

### **[轻微]** Task v25 对 TimeoutException 测试断言问题未给出完整修复方案

**问题**：task_v25.md 第 34 行正确识别了 `asyncSuggestionShouldStoreFailedOnTimeoutException` L711 的 `contains("TimeoutException")` 断言将因 CompletableFuture 包装为 ExecutionException 而失败，并注明"需同步修复"，但未给出具体的断言修正方向（改为验证 ExecutionException 前缀？或验证包含 "timed out" 消息？）。

**修正方向**：补充具体的断言修复方案，例如将 `contains("TimeoutException")` 替换为 `contains("ExecutionException")` 或同时验证 `contains("timed out")`。

## 修改要求（REJECTED）

### 问题 1：P11/P14 遗漏
- **为什么是问题**：计划声称覆盖"全部 P0—P2 问题"，但实际遗漏了两项 P1 问题，导致修复范围不完整
- **期望的修正方向**：补充 P11 和 P14 的实施安排，或在排期外说明中明确记录排除理由及后续轮次安排

### 问题 2：TimeoutException 测试修复不完整
- **为什么是问题**：task_v25 正确识别了问题但未给出完整修复指导，增加实现者理解偏差风险
- **期望的修正方向**：在 task_v25 中补充 `asyncSuggestionShouldStoreFailedOnTimeoutException` 的断言修正方案
