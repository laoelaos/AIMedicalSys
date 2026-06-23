# 详细设计（v5）

## 概述
在 `FallbackAiServiceTest` 中新增 `shouldLogErrorOnFirstCallThenWarnOnSubsequent` 测试方法，使用 `ListAppender<ILoggingEvent>` 验证 `handleEmptyDelegates` 的日志输出：首次调用 → `log.error("No available AiService delegate")`，后续调用 → `log.warn("No available AiService delegate")`。通过 public 方法 `triage()` 触发，与生产调用路径一致。

## 文件规划
| 文件路径 | 操作 | 职责 |
|---------|------|------|
| `AIMedical/backend/ai-impl/src/test/java/com/aimedical/modules/ai/impl/fallback/FallbackAiServiceTest.java` | 修改 | 新增 `shouldLogErrorOnFirstCallThenWarnOnSubsequent` 测试方法 |

## 类型定义

### FallbackAiServiceTest（已有类，新增测试方法）
**形态**：class
**包路径**：`com.aimedical.modules.ai.impl.fallback`
**职责**：FallbackAiService 的单元测试

**新增测试方法签名**：
```java
@Test
void shouldLogErrorOnFirstCallThenWarnOnSubsequent()
```

**测试行为**：
1. 通过 `LoggerFactory.getLogger(FallbackAiService.class)` 获取 logback `Logger` 实例
2. 创建 `ListAppender<ILoggingEvent>`，调用 `appender.start()`，通过 `logger.addAppender(appender)` 注入
3. 在 `try-finally` 中确保清理：
   - `try` 块中执行测试断言
   - `finally` 块中执行 `appender.stop(); logger.detachAppender(appender);`（logback-classic 1.4.14 中 `detachAndStopAppender` 泛型不匹配，采用等价的两步替代）
4. 首次调用验证：
   - 构造 `FallbackAiService(List.of(), List.of())`（空 delegate 列表触发 `handleEmptyDelegates`）
   - 调用 `service.triage(new TriageRequest()).join()`
   - 断言 `appender.list.size()` == 1
   - 断言 `appender.list.get(0).getLevel()` == `Level.ERROR`
   - 断言 `appender.list.get(0).getFormattedMessage()` == `"No available AiService delegate"`
5. 后续调用验证：
   - `appender.list.clear()`
   - 再次调用 `service.triage(new TriageRequest()).join()`
   - 断言 `appender.list.size()` == 1
   - 断言 `appender.list.get(0).getLevel()` == `Level.WARN`
   - 断言 `appender.list.get(0).getFormattedMessage()` == `"No available AiService delegate"`
6. 在断言旁标注 K3 已知偏差依赖：`// ⚠️ 此断言依赖 K3 已知偏差（首次调用触发 ERROR 而非启动期触发），K3 修复后需同步更新`

**新增 import**：
```java
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.slf4j.LoggerFactory;
```

## 错误处理
- 无新增错误处理逻辑（纯测试方法）
- `ListAppender` 清理在 `finally` 块中保证执行，避免测试失败时污染后续测试的日志上下文

## 行为契约
- 测试通过 public 方法 `triage()` 触发 `handleEmptyDelegates`，不直接调用 private 方法
- 首次 `triage()` 调用后 `firstEmptyDelegateCall` 从 `true` 切换为 `false`，后续调用进入 `log.warn` 分支
- 当前行为与 OOD §3.4"兜底保护"的设计语义存在已知偏差（K3）：ERROR 日志触发时机为首次调用而非启动期。本测试验证当前代码的实际行为，在断言旁显式标注 K3 依赖风险
- R4 已知清理模式：`finally` 块使用 `appender.stop(); logger.detachAppender(appender);` 而非 `logger.detachAndStopAppender(appender)`（logback-classic 1.4.14 编译兼容性问题）

## 依赖关系
- 依赖的已有类型：`FallbackAiService`、`TriageRequest`、`TriageResponse`、`AiResult`（均为 `ai-impl` 及 `ai-api` 模块已有类型）
- 依赖的测试基础设施：`ListAppender`（由 `spring-boot-starter-test` 传递引入的 `logback-classic` 提供）
- 无新增外部依赖
- 不修改生产代码（纯测试变更）
