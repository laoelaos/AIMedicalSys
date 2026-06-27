# 任务指令（v6）

## 动作
NEW

## 任务描述
修复 T3 和 T25 两项 GlobalExceptionHandler 缺陷，涉及 1 个源文件 + 1 个测试文件：

### T3: ACCOUNT_LOCKED 消息模板未解析，客户端收到模板原文（P1）
- **问题**：`GlobalExceptionHandler.handleBusinessException()` 调用 `Result.fail(errorCode)` 直接返回枚举 `getMessage()` 的模板原文 `"账户已锁定，请{锁定时间}后重试"`，忽略 `BusinessException` 构造时传入的 args 参数（如 `"请30分钟后重试"`）。
- **OOD 10.3 节要求**：实现 `BusinessException args → GlobalExceptionHandler → Result` 的完整插值管线。插值时机在 GlobalExceptionHandler 中，方法签名：`formatMessage(String template, Object[] args)`，优先使用 `MessageFormat.format(template, args)`，对 `{锁定时间}` 等命名占位符降级为 `String.replace`。
- **修改位置**：`GlobalExceptionHandler.java:27` 将 `Result.fail(errorCode)` 改为带 args 插值后的消息。
- **受影响的测试文件**：无现有测试断言该消息内容，但需在 `GlobalExceptionHandlerTest` 中新增测试验证插值行为。

### T25: RATE_LIMITED 和 ACCOUNT_LOCKED 应返回 HTTP 429（P1）
- **问题**：`GlobalExceptionHandler.resolveHttpStatus()` 中 RATE_LIMITED 和 ACCOUNT_LOCKED 均落入默认 400 分支。
- **OOD 10.1 节要求**：RATE_LIMITED 和 ACCOUNT_LOCKED 应返回 HTTP 429（Too Many Requests）。
- **修改位置**：`GlobalExceptionHandler.java:38-52`（约）`resolveHttpStatus()` 方法，在 `default -> 400` 之前增加 RATE_LIMITED 和 ACCOUNT_LOCKED 到 429 的映射。
- **受影响的测试文件**：需在 `GlobalExceptionHandlerTest` 中新增/更新对应状态码断言。

### 影响文件清单
| 操作 | 文件路径 |
|------|---------|
| 修改 | `common/src/main/java/com/aimedical/common/config/GlobalExceptionHandler.java` |
| 修改 | `common/src/test/java/com/aimedical/common/config/GlobalExceptionHandlerTest.java` |

## 选择理由
- T3 和 T25 均为 P1 优先级，且均在 `GlobalExceptionHandler.java` 同一文件中修改，可一次完成
- T3 是批次 4（消息管线修复）的核心——消息插值管线是 T13/T5 依赖的基础设施
- 无前置依赖，可在 R5 完成的基线上安全执行

## 任务上下文
### T3: 消息模板插值管线
- **OOD 10.3 节关键设计（已补全）**：
  - 插值时机：GlobalExceptionHandler.handleBusinessException()
  - 插值方法：`formatMessage(String template, Object[] args)`，优先 `MessageFormat.format`，对 `{锁定时间}` 等命名占位符降级为 `String.replace`，可扩展
  - Result 响应格式：`Result.fail(errorCode.getCode(), interpolatedMessage)`
  - BusinessException 中 args 存储：`new BusinessException(ErrorCode, Object... args)`，通过 `e.getArgs()` 获取

- **当前代码（handleBusinessException）**：
```java
@ExceptionHandler(BusinessException.class)
public ResponseEntity<Result<Void>> handleBusinessException(BusinessException e) {
    ErrorCode errorCode = e.getErrorCode();
    HttpStatus status = resolveHttpStatus(errorCode);
    log.warn("Business exception: code={}, message={}", errorCode.getCode(), e.getMessage());
    return ResponseEntity.status(status)
            .body(Result.fail(errorCode));
}
```

- **修改后目标**：
```java
@ExceptionHandler(BusinessException.class)
public ResponseEntity<Result<Void>> handleBusinessException(BusinessException e) {
    ErrorCode errorCode = e.getErrorCode();
    HttpStatus status = resolveHttpStatus(errorCode);
    log.warn("Business exception: code={}, message={}", errorCode.getCode(), e.getMessage());
    String message = formatMessage(errorCode.getMessage(), e.getArgs());
    return ResponseEntity.status(status)
            .body(Result.fail(errorCode.getCode(), message));
}
```

- **formatMessage 实现要求**：
  - args 为 null 或空时直接返回 template
  - 尝试用 `MessageFormat.format(template, args)` 处理
  - 若保留 `{锁定时间}` 等命名占位符，使用 `String.replace` 依次替换（替换规则：占位符以 `{` 和 `}` 包裹，按 args 顺序匹配）
  - 插值失败时不抛出异常，返回原始 template

### T25: 429 状态码映射
- **OOD 10.1 节要求**：RATE_LIMITED 和 ACCOUNT_LOCKED → HTTP 429
- **当前 resolveHttpStatus 方法（实际代码）**：
```java
private HttpStatus resolveHttpStatus(ErrorCode errorCode) {
    String code = errorCode.getCode();
    if (GlobalErrorCode.UNAUTHORIZED.getCode().equals(code)) {
        return HttpStatus.UNAUTHORIZED;
    }
    if (GlobalErrorCode.FORBIDDEN.getCode().equals(code)) {
        return HttpStatus.FORBIDDEN;
    }
    if (GlobalErrorCode.NOT_FOUND.getCode().equals(code)) {
        return HttpStatus.NOT_FOUND;
    }
    if (GlobalErrorCode.PARAM_INVALID.getCode().equals(code)) {
        return HttpStatus.BAD_REQUEST;
    }
    if (GlobalErrorCode.SYSTEM_ERROR.getCode().equals(code)) {
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }
    // 其他业务错误码默认返回400
    return HttpStatus.BAD_REQUEST;
}
```
- **修改后**：在 default return `HttpStatus.BAD_REQUEST` 之前增加：
```java
if (GlobalErrorCode.RATE_LIMITED.getCode().equals(code) || GlobalErrorCode.ACCOUNT_LOCKED.getCode().equals(code)) {
    return HttpStatus.TOO_MANY_REQUESTS;
}
```

### 已有测试基础设施
- `GlobalExceptionHandlerTest.java` 已有 5 个测试方法：shouldHandleBusinessExceptionWith400、shouldHandleMessageNotReadableWith400、shouldHandleMessageNotWritableWith500、shouldHandleGenericExceptionWith500、shouldHandleValidationExceptionWith400
- 现有 shouldHandleBusinessExceptionWith400 仅验证状态码 400 和 code 字段，未验证 message 插值

## 新增/修改测试要求
1. **新增 `shouldInterpolateAccountLockedMessage()`**：mock BusinessException with args("请30分钟后重试")，验证响应 message 为 `"账户已锁定，请30分钟后重试"`
2. **新增 `shouldReturn429ForRateLimited()`**：mock BusinessException(RATE_LIMITED)，验证 HTTP 状态码为 429
3. **更新 `shouldHandleBusinessExceptionWith400()`**（可选）：增强现有测试，验证 handleBusinessException 返回正确状态码和 code 字段（回归）

## 修订说明（v6 r1）
| 审查意见 | 修改措施 |
|---------|---------|
| [严重] handleBusinessException 方法签名错误（`Result<Void>` 而非 `ResponseEntity<Result<Void>>`），丢失 HTTP 状态码控制 | "修改后目标" 代码块修正为正确的 `ResponseEntity<Result<Void>>` 签名，保留 `resolveHttpStatus()` 调用结构；"当前代码" 代码块同步修正 |
| [一般] 测试文件路径包名错误（`exception` 而非 `config`） | 影响文件清单中测试文件路径修正为 `common/src/test/java/com/aimedical/common/config/GlobalExceptionHandlerTest.java` |
| [轻微] 现有测试方法名引用不精确（`shouldHandleBusinessException` 应为 `shouldHandleBusinessExceptionWith400`） | 已有测试方法列表修正为 `shouldHandleBusinessExceptionWith400`，第 3 项测试名引用同步修正 |

## 修订说明（v6 r2）
| 审查意见 | 修改措施 |
|---------|---------|
| [严重] resolveHttpStatus() 的"当前代码"示例与实际源代码严重不一致——实际代码使用 if-else 链返回 `HttpStatus` 枚举，而非 switch 表达式返回 `int` | T25 节"当前代码"代码块修正为与实际 if-else + `HttpStatus` 结构一致；"修改后"代码块同步修正为 `if (GlobalErrorCode.RATE_LIMITED.getCode().equals(code) || GlobalErrorCode.ACCOUNT_LOCKED.getCode().equals(code)) { return HttpStatus.TOO_MANY_REQUESTS; }` |
