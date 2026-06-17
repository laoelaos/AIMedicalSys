# 任务指令（v9）

## 动作
NEW

## 任务描述
修复 FallbackAiService ERROR 日志触发时机，使其与 OOD §3.4 "启动期输出 ERROR 日志、运行期输出 WARN 日志"对齐。

涉及 2 个文件：

1. **FallbackAiService.java** — 3 项修改：
   - 构造器末尾（`this.strategies = strategies;` 之后）增加 ERROR 日志：当 `this.delegates.isEmpty()` 时输出 `log.error("No available AiService delegate")`
   - 移除 `private final AtomicBoolean firstEmptyDelegateCall = new AtomicBoolean(true);` 字段（不再需要 once-only 模式）
   - `handleEmptyDelegates()` 方法简化为始终输出 WARN 日志（不再区分首次/后续调用）

2. **FallbackAiServiceTest.java** — 重构 `shouldLogErrorOnFirstCallThenWarnOnSubsequent()` 为两个独立测试：
   - `shouldLogErrorOnConstruction()`：构造时输出 1 条 ERROR
   - `shouldLogWarnOnSubsequentCalls()`：首次及后续调用仅输出 WARN

## 选择理由
T10 为全部 11 项 issue 中最后剩余任务。ai-impl 模块独立，与已完成任务无依赖关系。修复后所有真实存在的代码缺陷（T1-T4、T6-T7、T9-T11）全部对齐 OOD 规范。

## 任务上下文
### 文件路径
- 源文件：`AIMedical/backend/modules/ai/ai-impl/src/main/java/com/aimedical/modules/ai/impl/fallback/FallbackAiService.java`
- 测试文件：`AIMedical/backend/modules/ai/ai-impl/src/test/java/com/aimedical/modules/ai/impl/fallback/FallbackAiServiceTest.java`

### OOD §3.4 要求
"启动期输出 ERROR 日志、运行期输出 WARN 日志"——启动期泛指构造器阶段（Spring 单线程 Bean 创建）。

### 当前代码行为
- **构造器**（第 52-58 行）：仅完成 delegates 过滤和 strategies 赋值，不做空检测
- **handleEmptyDelegates()**（第 60-67 行）：首次调用时 `firstEmptyDelegateCall.getAndSet(false)` 为 true → 输出 ERROR；后续调用 → 输出 WARN
- 当前 ERROR 在"首次调用期"而非"启动期"触发，偏离 OOD §3.4

### 当前测试行为
- `shouldLogErrorOnFirstCallThenWarnOnSubsequent()`（第 117-142 行）：
  - 第 119-121 行：构造前已附加 ListAppender（ListAppender 捕获构造器日志）
  - 第 124 行：构造 FallbackAiService（List.of() 空 delegates）→ 当前不输出日志
  - 第 126 行：首次 triage 调用 → 当前输出 1 条 ERROR（期望变为构造器输出 ERROR + 首次调用输出 WARN）
  - 第 127 行：`assertEquals(1, appender.list.size())` — 期望构造器输出后同样是 1
  - 第 132 行：`appender.list.clear()`
  - 第 134 行：第二次 triage 调用 → 输出 1 条 WARN
  - 第 135 行：`assertEquals(1, appender.list.size())` — WARN
  - 第 130 行：`// ⚠️ 此断言依赖 K3 已知偏差（首次调用触发 ERROR 而非启动期触发），K3 修复后需同步更新` — 遗留待办注释

## 已有代码上下文
### FallbackAiService.java 当前相关代码段
```java
// 第 50 行 — 即将移除
private final AtomicBoolean firstEmptyDelegateCall = new AtomicBoolean(true);

// 第 52-58 行 — 构造器，需在末尾增加 ERROR 日志
public FallbackAiService(List<AiService> aiServiceList,
                         List<DegradationStrategy> strategies) {
    this.delegates = aiServiceList.stream()
            .filter(s -> !(s instanceof FallbackAiService))
            .collect(Collectors.toList());
    this.strategies = strategies;
    // 在此处增加：if (this.delegates.isEmpty()) { log.error("No available AiService delegate"); }
}

// 第 60-67 行 — handleEmptyDelegates，需简化为始终 WARN
private <T> CompletableFuture<AiResult<T>> handleEmptyDelegates() {
    if (firstEmptyDelegateCall.getAndSet(false)) {
        log.error("No available AiService delegate");  // 移除
    } else {
        log.warn("No available AiService delegate");   // 保留
    }
    return CompletableFuture.completedFuture(AiResult.degraded("No available AiService delegate"));
}
// 简化后：
// private <T> CompletableFuture<AiResult<T>> handleEmptyDelegates() {
//     log.warn("No available AiService delegate");
//     return CompletableFuture.completedFuture(AiResult.degraded("No available AiService delegate"));
// }
```

### FallbackAiServiceTest.java 当前相关代码段
```java
// 第 50 行 import — 可移除（不再使用 AtomicBoolean）
import java.util.concurrent.atomic.AtomicBoolean;

// 第 117-142 行 — 需重构为两个独立测试
@Test
void shouldLogErrorOnFirstCallThenWarnOnSubsequent() {
    Logger logger = (Logger) LoggerFactory.getLogger(FallbackAiService.class);
    ListAppender<ILoggingEvent> appender = new ListAppender<>();
    appender.start();
    logger.addAppender(appender);

    try {
        FallbackAiService service = new FallbackAiService(List.of(), List.of());

        service.triage(new TriageRequest()).join();
        assertEquals(1, appender.list.size());           // 修复后：构造器产生 1 条 ERROR（断言保持 1）
        assertEquals(Level.ERROR, appender.list.get(0).getLevel());
        assertEquals("No available AiService delegate", appender.list.get(0).getFormattedMessage());
        // ⚠️ 待办注释 — 修复后移除

        appender.list.clear();

        service.triage(new TriageRequest()).join();
        assertEquals(1, appender.list.size());
        assertEquals(Level.WARN, appender.list.get(0).getLevel());
        assertEquals("No available AiService delegate", appender.list.get(0).getFormattedMessage());
    } finally {
        appender.stop();
        logger.detachAppender(appender);
    }
}
```
