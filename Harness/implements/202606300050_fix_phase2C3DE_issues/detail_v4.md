# 详细设计（v4）

## 概述

修复 `saveTriageRecord` 中 `degraded` 标记赋值和科室路由判断逻辑：将 2 处 `aiResult.isDegraded()` 替换为 `response.isDegraded()`，使记录持久化使用业务层最终降级决策（`response` 对象持有，而非 `aiResult` 原始状态）。仅涉及 1 个文件。

## 文件规划

| 文件路径 | 操作 | 职责 |
|---------|------|------|
| `consultation/service/impl/TriageServiceImpl.java` | 修改 | `saveTriageRecord` 中 2 处 `aiResult.isDegraded()` 条件改为 `response.isDegraded()` |

## 类型定义

### TriageServiceImpl.saveTriageRecord

**形态**：class（Spring `@Service`）
**包路径**：`com.aimedical.modules.consultation.service.impl`

**变更点 1 — degraded 标记赋值（原 line 230-234）**：

```java
// 修改前
if (aiResult != null && aiResult.isDegraded()) {
    record.setDegraded(true);
} else {
    record.setDegraded(false);
}

// 修改后
record.setDegraded(response.isDegraded());
```

**变更点 2 — 科室路由判断（原 line 236-241）**：

```java
// 修改前
if (finalDepartmentsJson != null) {
    if (aiResult != null && aiResult.isDegraded()) {
        record.setRuleMatchedDepartments(finalDepartmentsJson);
    } else {
        record.setAiRecommendedDepartments(finalDepartmentsJson);
    }
}

// 修改后
if (finalDepartmentsJson != null) {
    if (response.isDegraded()) {
        record.setRuleMatchedDepartments(finalDepartmentsJson);
    } else {
        record.setAiRecommendedDepartments(finalDepartmentsJson);
    }
}
```

**说明**：
- `response` 参数类型为 `com.aimedical.modules.consultation.dto.TriageResponse`，其 `isDegraded()` 直接返回 `degraded` 布尔字段（`boolean isDegraded()` 行 86-88），无 null 安全问题
- 降级路径：`fallbackResponse.setDegraded(true)`（`triage()` 行 141），`response.isDegraded()` 返回 `true`
- 成功路径：`triageConverter.toTriageResponse(aiResult, ...)` 基于 `aiResult.isDegraded()` 设置 `response.degraded`，`response.isDegraded()` 与原始 `aiResult.isDegraded()` 一致
- 变更后无需 `aiResult != null` 判空，因此 `response` 始终非 null（由 `triage()` 方法保证传入）

## 错误处理

无变更。错误类型、传播策略与 v3 一致。

## 行为契约

### saveTriageRecord degraded 赋值
- **前置条件**：`response` 参数非 null
- **行为**：`record.setDegraded(response.isDegraded())` — 直接委托给业务层最终决策的 `degraded` 状态
- **后置条件**：`TriageRecord.degraded` 与 `response.degraded` 一致

### saveTriageRecord 科室路由
- **前置条件**：`response` 参数非 null
- **行为**：`response.isDegraded() == true` 时存入 `ruleMatchedDepartments`，否则存入 `aiRecommendedDepartments`
- **后置条件**：科室 JSON 存入正确字段

## 依赖关系

无新增依赖。`com.aimedical.modules.consultation.dto.TriageResponse.isDegraded()` 为现有方法。

## 修订说明（v4 r1）

| 审查意见 | 修改措施 |
|---------|---------|
| [严重] `saveTriageRecord` 中 `aiResult.isDegraded()` 在 `AiResult.failure()` 路径返回 `false`，导致降级记录 `degraded=false` 且科室路由错误 | 两处条件均改为 `response.isDegraded()`，`response` 由业务层保证反映最终降级决策；`degraded` 标记赋值的 if/else 简化为 `record.setDegraded(response.isDegraded())` |
