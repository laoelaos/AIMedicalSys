# 详细设计（v21）

## 概述

修正 `FallbackAiServiceTest.shouldDegradeWhenStrategyTriggers` 测试中断言预期值，对齐实际执行路径返回的 fallbackReason 字符串。

## 文件规划

| 文件路径 | 操作 | 职责 |
|---------|------|------|
| `AIMedical/backend/modules/ai/ai-impl/src/test/java/com/aimedical/modules/ai/impl/fallback/FallbackAiServiceTest.java` | 修改 | 修正 L92 断言预期值 |

## 类型定义

无新增类型。仅涉及测试方法中断言字符串字面量变更。

## 变更内容

**文件**：`AIMedical/backend/modules/ai/ai-impl/src/test/java/com/aimedical/modules/ai/impl/fallback/FallbackAiServiceTest.java`

**位置**：L92（`shouldDegradeWhenStrategyTriggers` 方法内）

**修改前**：
```java
assertEquals("Degraded by strategy", result.getFallbackReason());
```

**修改后**：
```java
assertEquals("No available AiService delegate", result.getFallbackReason());
```

## 执行路径说明

测试设置 1 个 mock `AiService` delegate + 1 个 `DegradationStrategy`（`shouldDegrade` 始终返回 `true`）：
1. `triage()` 调用 `selectDelegate(context)` → 唯一 delegate 被策略标记为 skip → 返回 `null`
2. `delegate == null` → `handleEmptyDelegates()` → 返回 `AiResult.degraded("No available AiService delegate")`
3. 原测试断言 `"Degraded by strategy"` 不正确 —— 该字符串由 `applyStrategies()` 路径产生，但 `selectDelegate()` 的前置跳过检查先于 delegate 调用执行，`applyStrategies()` 从未被触发

## 验证依据

- 同类测试 `selectDelegateShouldReturnEmptyDelegatesWhenAllSkipped`（L487-501）已正确断言 `"No available AiService delegate"`，可作为参照
- 此变更为纯测试断言修正，不涉及生产代码变更

## 错误处理

不涉及。本变更仅修正测试断言字符串，无运行时错误处理逻辑变更。

## 行为契约

- **前置条件**：`shouldDegradeWhenStrategyTriggers` 测试方法存在，L92 当前断言 `"Degraded by strategy"`
- **后置条件**：L92 断言 `"No available AiService delegate"`，与 `handleEmptyDelegates()` 实际返回一致
- **不变量**：测试方法其他断言（L90-91）及测试结构不变；生产代码不变

## 依赖关系

- **无新增依赖**
- **暴露给后续任务**：此变更为独立修复，无后续任务依赖
