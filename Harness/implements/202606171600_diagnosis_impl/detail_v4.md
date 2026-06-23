# 详细设计（v4）

## 概述
在 `GlobalExceptionHandler` 中新增两个 `@ExceptionHandler` 方法处理序列化异常，并在 `GlobalExceptionHandlerTest` 中新增对应单元测试。OOD §5.3 要求的 `HttpMessageNotReadableException` / `HttpMessageNotWritableException` 统一异常处理，消除 Spring 默认错误响应格式对统一响应契约的污染。

## 文件规划
| 文件路径 | 操作 | 职责 |
|---------|------|------|
| `AIMedical/backend/common/src/main/java/com/aimedical/common/config/GlobalExceptionHandler.java` | 修改 | 新增 `handleMessageNotReadable` 和 `handleMessageNotWritable` 方法 |
| `AIMedical/backend/common/src/test/java/com/aimedical/common/config/GlobalExceptionHandlerTest.java` | 修改 | 新增 `shouldHandleMessageNotReadableWith400` 和 `shouldHandleMessageNotWritableWith500` 测试方法 |

## 类型定义

### GlobalExceptionHandler（已有类，新增方法）
**形态**：class（@ControllerAdvice）
**包路径**：`com.aimedical.common.config`
**职责**：全局异常处理

**新增方法签名**：

```java
@ExceptionHandler(HttpMessageNotReadableException.class)
public ResponseEntity<Result<Void>> handleMessageNotReadable(HttpMessageNotReadableException e)

@ExceptionHandler(HttpMessageNotWritableException.class)
public ResponseEntity<Result<Void>> handleMessageNotWritable(HttpMessageNotWritableException e)
```

**方法行为**：
- `handleMessageNotReadable`：`log.warn("Request body malformed", e)` + `ResponseEntity.badRequest().body(Result.fail(GlobalErrorCode.PARAM_INVALID))`
- `handleMessageNotWritable`：`log.error("Response body serialization failed", e)` + `ResponseEntity.status(500).body(Result.fail(GlobalErrorCode.SYSTEM_ERROR))`

**新增 import**：
```java
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
```

**类型关系**：无变化，沿用 @ControllerAdvice + @ExceptionHandler 模式

### GlobalExceptionHandlerTest（已有类，新增测试方法）
**形态**：class
**包路径**：`com.aimedical.common.config`
**职责**：GlobalExceptionHandler 的单元测试

**新增测试方法签名**：

```java
@Test
void shouldHandleMessageNotReadableWith400()

@Test
void shouldHandleMessageNotWritableWith500()
```

**测试行为**：
- `shouldHandleMessageNotReadableWith400`：
  1. 直接创建 `HttpMessageNotReadableException` 实例
  2. 通过 `ListAppender<ILoggingEvent>` 注入 `GlobalExceptionHandler` 的 Logger（`ch.qos.logback.classic.Logger`），在 try-finally 中确保 `detachAndStopAppender` 清理
  3. 调用 `handler.handleMessageNotReadable(ex)`
  4. 验证 `appender.list` 大小为 1，事件等级为 `Level.WARN`，消息为 `"Request body malformed"`
  5. 验证状态码=400、body.getCode()=GlobalErrorCode.PARAM_INVALID.code()、body.getMessage()=GlobalErrorCode.PARAM_INVALID.message()
  6. 验证 `assertNull(body.getData())`

- `shouldHandleMessageNotWritableWith500`：
  1. 直接创建 `HttpMessageNotWritableException` 实例
  2. 通过 `ListAppender<ILoggingEvent>` 注入 `GlobalExceptionHandler` 的 Logger，在 try-finally 中确保 `detachAndStopAppender` 清理
  3. 调用 `handler.handleMessageNotWritable(ex)`
  4. 验证 `appender.list` 大小为 1，事件等级为 `Level.ERROR`，消息为 `"Response body serialization failed"`
  5. 验证状态码=500、body.getCode()=GlobalErrorCode.SYSTEM_ERROR.code()、body.getMessage()=GlobalErrorCode.SYSTEM_ERROR.message()
  6. 验证 `assertNull(body.getData())`

**ListAppender 注入骨架**（在测试方法中使用 try-finally 确保清理）：
```java
Logger logger = (Logger) LoggerFactory.getLogger(GlobalExceptionHandler.class);
ListAppender<ILoggingEvent> appender = new ListAppender<>();
appender.start();
logger.addAppender(appender);
try {
    // 调用 handler 并断言
    assertEquals(1, appender.list.size());
    assertEquals(Level.ERROR, appender.list.get(0).getLevel());
    assertEquals("Response body serialization failed", appender.list.get(0).getFormattedMessage());
} finally {
    logger.detachAndStopAppender(appender);
}
```

**新增 import**（测试文件）：
```java
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.slf4j.LoggerFactory;
```

## 错误处理
- `HttpMessageNotReadableException`：客户端请求体 JSON 格式错误 → HTTP 400，以 `log.warn` 记录（客户端错误，非服务端问题）
- `HttpMessageNotWritableException`：响应体序列化失败 → HTTP 500，以 `log.error` 记录（服务端内部错误）
- 两个异常类位于 `org.springframework.http.converter` 包，由 `spring-boot-starter-web` 传递引入，无需新增依赖

## 行为契约
- `handleMessageNotReadable` 必须在 `@ExceptionHandler(Exception.class)` 之前匹配（Spring 按精确匹配优先），不会触发通配的 `handleException`
- `handleMessageNotWritable` 同理
- 两个新增方法遵循已有 handler 的命名约定（`handle<异常名>` + 语义化测试方法名 `should<行为>With<状态码>`）

## 依赖关系
- 依赖的已有类型：`HttpMessageNotReadableException`、`HttpMessageNotWritableException`（均由 `spring-boot-starter-web` 传递引入）
- 依赖的已有错误码：`GlobalErrorCode.PARAM_INVALID`（与 MethodArgumentNotValidException 共享）、`GlobalErrorCode.SYSTEM_ERROR`（与通用 Exception 共享）
- 依赖的已有返回类型：`Result<Void>`、`ResponseEntity<Result<Void>>`
- 无新增外部依赖

## 修订说明（v4 r1）
| 审查意见 | 修改措施 |
|---------|---------|
| 设计未覆盖任务要求的"日志级别验证" | 在 §GlobalExceptionHandlerTest 测试行为中补充了 ListAppender 注入骨架，明确 `shouldHandleMessageNotReadableWith400` 验证 `Level.WARN`、`shouldHandleMessageNotWritableWith500` 验证 `Level.ERROR`；同步补充了消息内容断言 |
| 测试未包含 `getData()` 断言 | 在两个新增测试方法中均增加第 6 步：`assertNull(body.getData())`，与现有 `shouldHandleBusinessExceptionWith400`、`shouldHandleGenericExceptionWith500` 保持模式一致 |
