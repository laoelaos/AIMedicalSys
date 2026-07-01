# 详细设计（v9）

## 概述

修复 consultation 模块 5 项 P1/P2 缺陷（C13, T4, T42, C18, T45），均为低风险单文件/单方法小改动。不涉及新增类型或 API 变更。

## 文件规划

| 文件路径 | 操作 | 职责 |
|---------|------|------|
| `rule/DefaultTriageRuleEngine.java` | 修改 | 7a: 快照失效回退时追加 warn 日志；7c: 缓存策略 refreshAfterWrite(60s) → expireAfterWrite(30s) |
| `converter/TriageConverter.java` | 修改 | 7b: 新增 `toFallbackTriageResponse` 方法 |
| `service/impl/TriageServiceImpl.java` | 修改 | 7b: 降级路径替换为 Converter 调用；7d: 日志级别 warn→error 并记录原始 JSON；7e: selectDepartment 入口参数校验 |
| `event/RegistrationEventListener.java` | 修改 | 7e: null sessionId 时静默跳过 |

## 类型定义

无新增类型。仅新增方法签名：

### 7b. TriageConverter.toFallbackTriageResponse
**形态**：public 方法
**包路径**：`com.aimedical.modules.consultation.converter.TriageConverter`
**签名**：
```java
public TriageResponse toFallbackTriageResponse(
        List<RecommendedDepartment> departments,
        List<RecommendedDoctor> doctors,
        String sessionId,
        String reason,
        boolean ruleVersionMismatch,
        boolean fallbackHint)
```
**职责**：构造降级场景的 TriageResponse，不依赖 AiResult 参数。
**行为**：
- departments/doctors 为 null 时设为 emptyList
- 固定设置 `degraded = true`、`confidence = null`
- `ruleVersionMismatch` 透传 matchResult 状态
- `fallbackHint = true` 时设置中文提示语
- 不设置 `matchedRules`（降级路径无 AI 规则匹配）

## 错误处理

| 子项 | 变更 | 策略 |
|------|------|------|
| 7a | 无新增错误处理 | 日志仅做观测，无异常抛出 |
| 7b | 无 | Null-safe（null 集合 → emptyList） |
| 7c | 无 | Caffeine expireAfterWrite 同步加载，异常由 CacheLoader 向上传播 |
| 7d | log.warn → log.error | 级别提升 + 记录 serialized JSON 值（含 null 保护） |
| 7e | 新增防御 | null sessionId → log.warn + return（不抛异常）；selectDepartment 入口 `Objects.requireNonNull` |

## 行为契约

### 7a (C13)
- `DefaultTriageRuleEngine.match()` 在 `ruleVersionMismatch = true` 赋值后（第63行后）追加 `log.warn("Rule version mismatch, falling back to all enabled rules. requested version={}, setId={}", version, setId)`
- 不影响 match 方法的返回值和执行流

### 7b (T4)
- 降级路径（`TriageServiceImpl.triage()` 第161-189行）手工构造 TriageResponse 改为调用 `triageConverter.toFallbackTriageResponse`
- 字段逐项映射保持一致，行为无变化
- `matchedRules` 在降级路径为 null（原有行为即未设置）

### 7c (T42)
- 缓存策略：`.refreshAfterWrite(60, TimeUnit.SECONDS)` → `.expireAfterWrite(30, TimeUnit.SECONDS)`
- 变更后缓存 miss 触发同步加载，不一致窗口从 ≤60s 缩短到 ≤30s
- 无自定义 CacheLoader 行为变化

**测试设计**：
- `DefaultTriageRuleEngine` 新增重载构造器 `DefaultTriageRuleEngine(TriageRuleRepository repository, Ticker ticker)`，原构造器委托新构造器传入 `Ticker.systemTicker()`
- 测试方法构建 `MockTicker`（实现 `Ticker` 接口，`read()` 返回可手动推进的纳秒值），初始值为 0
- 使用 `MockTicker` 创建引擎实例，调用 `engine.match("a")` 触发加载；将 `MockTicker` 前进 31 秒（`ticker.advance(31_000_000_000L)`）；再次调用 `engine.match("a")` 验证 repository 被调用两次（第一次初始加载，第二次 ticker 推进后 expire 触发重新加载）
- 断言方式：通过 spy repository 验证 `findByEnabledTrue()` 恰好被调用 2 次

### 7d (C18)
- `saveTriageRecord` 中 `JsonProcessingException` 处理：`log.warn` → `log.error`，消息内容追加 `departments={}`、`doctors={}` 的 JSON 值
- 不影响控制流——仍为 catch 后继续执行

### 7e (T45)
- `RegistrationEventListener.handleRegistrationEvent` 入口：`event.getSessionId() == null` 时 `log.warn + return`，跳过后续处理
- `@Retryable` 配置不变，null 场景不会触发重试（已配置 `noRetryFor = NullPointerException.class`）
- `TriageServiceImpl.selectDepartment` 入口：`Objects.requireNonNull(sessionId, "sessionId must not be null")`
- 不改变现有业务逻辑

## 依赖关系

| 依赖 | 说明 |
|------|------|
| `slf4j.Logger`（已有） | 7a/7d/7e 日志 |
| `ch.qos.logback` ListAppender（测试） | 7a/7d 日志断言 |
| `java.util.Objects`（JDK） | 7e requireNonNull |
| `Caffeine` `.expireAfterWrite`（已有依赖） | 7c 缓存变更 |
| `TriageConverter`（已有注入） | 7b 新增方法，TriageServiceImpl 已有该依赖 |

## 修订说明（v9 r1）
| 审查意见 | 修改措施 |
|---------|---------|
| [一般] 7c 测试未真正验证缓存策略变更 | 替换 `shouldLoadRulesFromRepositoryOnCacheMiss` 测试设计：使用 `Ticker` 重载构造器 + `MockTicker` 推进时间+31s 后验证 `findByEnabledTrue()` 恰好被调用 2 次，确保 expireAfterWrite(30s) 策略可被测试验证 |
| [轻微] 7a 行号表述不一致 | 行为契约中"第62行后"修正为"第63行后"，与任务文件对齐 |
