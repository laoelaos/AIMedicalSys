# 任务指令（v5）

## 动作
NEW

## 任务描述
在 `FallbackAiServiceTest.java` 中新增 `shouldLogErrorOnFirstCallThenWarnOnSubsequent` 测试方法，使用 `ListAppender<ILoggingEvent>` 验证 `handleEmptyDelegates` 的日志输出：首次调用 → `log.error("No available AiService delegate")`，后续调用 → `log.warn("No available AiService delegate")`。通过 public 方法路径（`triage()`）触发。

预期文件：`AIMedical/backend/ai-impl/src/test/java/com/aimedical/modules/ai/impl/fallback/FallbackAiServiceTest.java`

## 选择理由
问题8（GlobalExceptionHandler）完成后，问题9 是最自然的下一任务——同为后端 Java 测试增强，同样使用 ListAppender 模式验证日志级别，测试文件属于同一代码域。问题9 是纯测试变更（不修改生产代码），无部署风险，可独立执行。

## 任务上下文
- `FallbackAiService.java:60-67` 的 `handleEmptyDelegates()` 方法使用 `AtomicBoolean firstEmptyDelegateCall` 在首次调用时输出 `log.error(...)`，后续调用输出 `log.warn(...)`
- 当前 `shouldReturnFallbackResultWhenNoDelegateAvailable()` 仅验证返回值的 degraded/success/fallbackReason，未验证日志输出
- 已知偏差 K3 记录：ERROR 日志触发时机为首次调用而非设计要求的启动期。本测试直接验证当前代码行为（首次调用 → ERROR），在断言旁标注 K3 依赖风险

## 已有代码上下文
### FallbackAiService.java (ai-impl)
```java
private static final Logger log = LoggerFactory.getLogger(FallbackAiService.class);
private final AtomicBoolean firstEmptyDelegateCall = new AtomicBoolean(true);

private <T> CompletableFuture<AiResult<T>> handleEmptyDelegates() {
    if (firstEmptyDelegateCall.getAndSet(false)) {
        log.error("No available AiService delegate");
    } else {
        log.warn("No available AiService delegate");
    }
    return CompletableFuture.completedFuture(AiResult.degraded("No available AiService delegate"));
}
```

### 现有测试文件 FallbackAiServiceTest.java
```java
class FallbackAiServiceTest {
    // 已有 5 个测试方法，使用 JUnit 5 + Mockito
    // 无 @SpringBootTest，纯单元测试

    @Test
    void shouldReturnFallbackResultWhenNoDelegateAvailable() {
        FallbackAiService fallback = new FallbackAiService(List.of(), List.of());
        AiResult<TriageResponse> result = fallback.triage(new TriageRequest()).join();
        assertFalse(result.isSuccess());
        assertTrue(result.isDegraded());
        assertEquals("No available AiService delegate", result.getFallbackReason());
    }
}
```

### R4 已知的 ListAppender 问题
R4 实现时发现 logback-classic 1.4.14 的 `Logger.detachAndStopAppender(Appender<ILoggingEvent>)` 编译器解析失败（泛型不匹配），实际采用 `appender.stop(); logger.detachAppender(appender);` 达到相同清理效果。

### 新增测试方法骨架参考（来自 R4 验证通过的实现）
```java
Logger logger = (Logger) LoggerFactory.getLogger(FallbackAiService.class);
ListAppender<ILoggingEvent> appender = new ListAppender<>();
appender.start();
logger.addAppender(appender);
try {
    // 执行测试...
    assertEquals(1, appender.list.size());
    assertEquals(Level.ERROR, appender.list.get(0).getLevel());
    assertEquals("No available AiService delegate", appender.list.get(0).getFormattedMessage());
} finally {
    appender.stop();
    logger.detachAppender(appender);
}
```
