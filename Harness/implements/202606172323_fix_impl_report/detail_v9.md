# 详细设计（v9）

## 概述

修复 FallbackAiService ERROR 日志触发时机，使其与 OOD §3.4 "启动期输出 ERROR 日志、运行期输出 WARN 日志"对齐。涉及生产代码 1 文件、测试代码 1 文件。

## 文件规划

| 文件路径 | 操作 | 职责 |
|---------|------|------|
| `AIMedical/backend/modules/ai/ai-impl/src/main/java/com/aimedical/modules/ai/impl/fallback/FallbackAiService.java` | 修改 | 构造器末尾增加 ERROR 日志；移除 `firstEmptyDelegateCall` 字段；`handleEmptyDelegates()` 始终 WARN |
| `AIMedical/backend/modules/ai/ai-impl/src/test/java/com/aimedical/modules/ai/impl/fallback/FallbackAiServiceTest.java` | 修改 | 重构 `shouldLogErrorOnFirstCallThenWarnOnSubsequent()` 为两个独立测试方法；移除 `⚠️` 待办注释 |

## 类型定义

### FallbackAiService（修改）

**形态**：class
**包路径**：`com.aimedical.modules.ai.impl.fallback`
**职责**：AiService 代理 + 降级策略组合器，日志行为与 OOD §3.4 对齐

**修改后字段**（移除 1 项）：

| 字段 | 修改前状态 | 修改后状态 |
|------|-----------|-----------|
| `private final List<AiService> delegates` | 不变 | 不变 |
| `private final List<DegradationStrategy> strategies` | 不变 | 不变 |
| `private final AtomicBoolean firstEmptyDelegateCall = new AtomicBoolean(true)` | 存在 | **移除** |

**构造器**（修改后）：

```java
public FallbackAiService(List<AiService> aiServiceList,
                         List<DegradationStrategy> strategies) {
    this.delegates = aiServiceList.stream()
            .filter(s -> !(s instanceof FallbackAiService))
            .collect(Collectors.toList());
    this.strategies = strategies;
    if (this.delegates.isEmpty()) {
        log.error("No available AiService delegate");
    }
}
```

**变更**：`this.strategies = strategies;` 之后增加空检测和 ERROR 日志。

**handleEmptyDelegates 方法**（简化后）：

```java
private <T> CompletableFuture<AiResult<T>> handleEmptyDelegates() {
    log.warn("No available AiService delegate");
    return CompletableFuture.completedFuture(AiResult.degraded("No available AiService delegate"));
}
```

**变更**：移除 `firstEmptyDelegateCall.getAndSet(false)` 判断分支，始终输出 WARN。

**import 变更**：
- 移除 `import java.util.concurrent.atomic.AtomicBoolean;`

**公开接口**：不变（全部 `AiService` 接口方法 + `handleEmptyDelegates` + `applyStrategies`）

### FallbackAiServiceTest（修改）

**形态**：class（JUnit5）
**包路径**：`com.aimedical.modules.ai.impl.fallback`
**职责**：验证 FallbackAiService 行为，含日志级别对齐

**测试重构**：将 `shouldLogErrorOnFirstCallThenWarnOnSubsequent()` 拆分为：

1. **`shouldLogErrorOnConstruction()`**
   - 构造前附加 ListAppender
   - 以 `List.of()` 空列表构造 `FallbackAiService`
   - 断言 `appender.list` 大小为 1
   - 断言条目级别为 `Level.ERROR`
   - 断言消息为 `"No available AiService delegate"`
   - 清理 appender

2. **`shouldLogWarnOnSubsequentCalls()`**
   - 构造前附加 ListAppender
   - 以 `List.of()` 空列表构造 `FallbackAiService`
   - 调用 `service.triage(new TriageRequest()).join()`（首次调用）
   - 断言 `appender.list` 大小为 2（构造器 1 条 ERROR + 本次 1 条 WARN）
   - 断言最新条目级别为 `Level.WARN`
   - 断言最新条目消息为 `"No available AiService delegate"`
   - `appender.list.clear()`
   - 再次调用 `service.triage(new TriageRequest()).join()`（后续调用）
   - 断言 `appender.list` 大小为 1
   - 断言条目级别为 `Level.WARN`
   - 清理 appender

**import 变更**：
- 移除 `import java.util.concurrent.atomic.AtomicBoolean;`（若存在）
- 无需新增 import（`Level`、`Logger`、`ListAppender`、`ILoggingEvent` 等已在现有 import 中）

**其他测试方法**：不变（6 个现有测试方法保持原样）

## 错误处理

- `FallbackAiService` 日志行为变更不改变功能路径，`AiResult.degraded()` 返回路径不受影响
- 测试断言失败抛出 `AssertionError`，JUnit5 框架自动捕获

## 行为契约

### 日志行为契约

| 前置条件 | 操作 | 预期日志 |
|---------|------|---------|
| `delegates` 为空集合 | 构造 `FallbackAiService(List.of(), ...)` | 1 条 `ERROR "No available AiService delegate"` |
| `delegates` 为非空集合 | 构造 `FallbackAiService(List.of(mock), ...)` | 无日志 |
| 已构造且 `delegates` 为空 | 调用任意 `AiService` 方法（如 `triage()`） | 1 条 `WARN "No available AiService delegate"` |
| 已构造且 `delegates` 为空 | 多次调用任意 `AiService` 方法 | 每次 1 条 `WARN`，不再输出 ERROR |

### 功能契约（不变）

- `delegates.isEmpty()` → 返回 `AiResult.degraded("No available AiService delegate")`
- `delegates` 过滤规则：排除 `FallbackAiService` 自身实例
- `applyStrategies` 行为不变

## 依赖关系

### 已有依赖（不变）

- `ai-api` — 提供 `AiService`、`AiResult`、DTO 等接口和类型
- `slf4j-api` — 提供日志门面
- `logback-classic`（test）— 提供 `Logger`、`ListAppender` 等测试设施

### 移除的依赖

- `java.util.concurrent.atomic.AtomicBoolean` — 不再使用，需从 import 移除
