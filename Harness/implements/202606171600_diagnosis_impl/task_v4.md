# 任务指令（v4）

## 动作
NEW

## 任务描述
在 `GlobalExceptionHandler.java` 中补充两个 `@ExceptionHandler` 方法：
1. `HttpMessageNotReadableException` → HTTP 400 + `GlobalErrorCode.PARAM_INVALID` + `log.warn`
2. `HttpMessageNotWritableException` → HTTP 500 + `GlobalErrorCode.SYSTEM_ERROR` + `log.error`

同步在 `GlobalExceptionHandlerTest.java` 中新增单元测试，通过直接调用 handler 方法（传入异常实例）验证响应状态码、错误码、日志级别。

预期文件路径：
- `AIMedical/backend/common/src/main/java/com/aimedical/common/config/GlobalExceptionHandler.java`
- `AIMedical/backend/common/src/test/java/com/aimedical/common/config/GlobalExceptionHandlerTest.java`

## 选择理由
POM 基础设施修复（问题2/5/6/7）完成后，首个代码缺陷修复。问题8 是自包含的后端变更，不依赖任何其他模块或外部服务；属于真实代码缺陷；修复后为 Phase 1+ 引入 POST/PUT Controller 扫清异常处理障碍。

## 任务上下文
摘自诊断报告问题8：
- OOD §5.3 明确要求"HttpMessageNotReadableException / HttpMessageNotWritableException 等序列化异常统一在 GlobalExceptionHandler 中注册 @ExceptionHandler 方法，避免 Spring 默认的错误响应格式污染统一响应契约"
- OOD §5.1 错误分类表规定：HttpMessageNotReadableException（请求体 JSON 格式错误）→ HTTP 400；HttpMessageNotWritableException（响应体序列化失败）→ HTTP 500
- 修复骨架：
  ```java
  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<Result<Void>> handleMessageNotReadable(HttpMessageNotReadableException e) {
      log.warn("Request body malformed", e);
      return ResponseEntity.badRequest()
              .body(Result.fail(GlobalErrorCode.PARAM_INVALID));
  }

  @ExceptionHandler(HttpMessageNotWritableException.class)
  public ResponseEntity<Result<Void>> handleMessageNotWritable(HttpMessageNotWritableException e) {
      log.error("Response body serialization failed", e);
      return ResponseEntity.status(500)
              .body(Result.fail(GlobalErrorCode.SYSTEM_ERROR));
  }
  ```
- `HttpMessageNotReadableException` 使用 `log.warn`（客户端请求格式错误）
- `HttpMessageNotWritableException` 使用 `log.error`（服务端序列化失败）
- 两个类位于 `org.springframework.http.converter` 包，已由 `spring-boot-starter-web` 传递引入，无需新增依赖
- 测试策略（Phase 0）：由于所有 Controller 均为 @GetMapping 且不接收 @RequestBody，无法通过 MockMvc 触发序列化异常。改为单元测试直接调用 handler 方法，传入异常实例验证响应格式

## 已有代码上下文

### GlobalExceptionHandler.java（当前）
```java
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Result<Void>> handleBusinessException(BusinessException e) {
        return ResponseEntity.badRequest().body(Result.fail(e.getErrorCode()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<Void>> handleValidationException(MethodArgumentNotValidException e) {
        return ResponseEntity.badRequest().body(Result.fail(GlobalErrorCode.PARAM_INVALID));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleException(Exception e) {
        log.error("System exception", e);
        return ResponseEntity.status(500).body(Result.fail(GlobalErrorCode.SYSTEM_ERROR));
    }
}
```

### GlobalExceptionHandlerTest.java（当前）
已有 3 个测试方法：`shouldHandleBusinessExceptionWith400`、`shouldHandleValidationExceptionWith400`、`shouldHandleGenericExceptionWith500`。测试风格为直接调用 handler 方法+assertJ 或 JUnit 断言验证 ResponseEntity。新增的两个测试应遵循相同模式。

### 相关类型
- `GlobalErrorCode.PARAM_INVALID` — 已有 ErrorCode 枚举值（与 MethodArgumentNotValidException 共享）
- `GlobalErrorCode.SYSTEM_ERROR` — 已有 ErrorCode 枚举值（与通用 Exception 共享）
- `Result.fail(ErrorCode)` — 已有工厂方法，返回 code/message 从 ErrorCode 提取的 Result 实例
- `ResponseEntity.badRequest()` / `ResponseEntity.status(500)` — Spring MVC 标准 API
