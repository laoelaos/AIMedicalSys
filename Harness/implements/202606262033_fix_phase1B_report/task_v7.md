# 任务指令（v7）

## 动作
RETRY

## 任务描述
GlobalExceptionHandler.java handleBusinessException() 补全缺失的 log.warn 日志行。

修改文件：`common/src/main/java/com/aimedical/common/config/GlobalExceptionHandler.java`

在 `handleBusinessException` 方法中，构造 `ResponseEntity` 之前添加：
```java
log.warn("Business exception: code={}, message={}", errorCode.getCode(), e.getMessage());
```

## 选择理由
R6 中 formatMessage、resolveHttpStatus、AuthServiceImpl args 修正、GlobalExceptionHandlerTest 修改均已正确通过编译和测试。唯一失败的是 `shouldInterpolateAccountLockedMessage_logsOriginalTemplate`：期望 log appender 捕获 1 条日志，但 handleBusinessException 中无任何 log 调用，实际为 0。仅需补一行代码，无需修改测试文件。

## 任务上下文
### 当前代码（handleBusinessException，缺日志行）
```java
@ExceptionHandler(BusinessException.class)
public ResponseEntity<Result<Void>> handleBusinessException(BusinessException e) {
    ErrorCode errorCode = e.getErrorCode();
    HttpStatus status = resolveHttpStatus(errorCode);
    String message = formatMessage(errorCode.getMessage(), e.getArgs());
    return ResponseEntity.status(status)
            .body(Result.fail(errorCode.getCode(), message));
}
```

### 目标代码（添加日志行后）
```java
@ExceptionHandler(BusinessException.class)
public ResponseEntity<Result<Void>> handleBusinessException(BusinessException e) {
    ErrorCode errorCode = e.getErrorCode();
    HttpStatus status = resolveHttpStatus(errorCode);
    String message = formatMessage(errorCode.getMessage(), e.getArgs());
    log.warn("Business exception: code={}, message={}", errorCode.getCode(), e.getMessage());
    return ResponseEntity.status(status)
            .body(Result.fail(errorCode.getCode(), message));
}
```

### 日志内容说明
- 日志级别：WARN（与 T4/T27/T20 阶段保持一致）
- 记录内容：`e.getMessage()` 返回原始消息模板（如 `"账户已锁定，请{锁定时间}后重试"`），而非插值后的结果
- 目的：便于调试时在日志中看到原始模板占位符，确认消息来源
- 位置：在 formatMessage 调用之后、ResponseEntity 构造之前，确保日志记录的是原始模板（formatMessage 内部不修改 errorCode.getMessage()）

### 受影响文件
| 文件 | 操作 |
|------|------|
| `common/src/main/java/com/aimedical/common/config/GlobalExceptionHandler.java` | 修改：补 1 行 log.warn |
| `common/src/test/java/com/aimedical/common/config/GlobalExceptionHandlerTest.java` | 不变 |

### 验证方式
```bash
mvn test -pl common -am
```
预期：GlobalExceptionHandlerTest 全部 10 个测试通过（含 shouldInterpolateAccountLockedMessage_logsOriginalTemplate）。

## RETRY 说明
**失败原因**：R6 实现中 handleBusinessException 遗漏了 `log.warn(...)` 语句，导致测试 `shouldInterpolateAccountLockedMessage_logsOriginalTemplate` 期望 `appender.list.size()` 为 1，实际为 0。

**修正方向**：在返回值之前添加一行 `log.warn("Business exception: code={}, message={}", errorCode.getCode(), e.getMessage());`。
