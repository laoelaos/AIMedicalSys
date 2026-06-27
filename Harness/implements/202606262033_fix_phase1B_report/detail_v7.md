# 详细设计（v7）

## 概述

补全 `GlobalExceptionHandler.handleBusinessException()` 中缺失的 `log.warn` 日志行。该行在 v6 设计中已明确指定，但实现时被遗漏，导致测试 `shouldInterpolateAccountLockedMessage_logsOriginalTemplate` 期望捕获 1 条日志但实际为 0。

本轮仅修改 1 个文件共 1 行，不涉及测试文件或其他文件变更。

## 文件规划

| 文件路径 | 操作 | 职责 |
|---------|------|------|
| `common/src/main/java/com/aimedical/common/config/GlobalExceptionHandler.java` | 修改 | `handleBusinessException` 中 `formatMessage` 调用之后、`return ResponseEntity` 之前补 1 行 `log.warn` |

## 类型定义

### 1. GlobalExceptionHandler.handleBusinessException（补日志行）

**当前代码**（缺日志行）：
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

**修改后代码**（L28 与 L29 之间插入日志行）：
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

**日志签名**：
```java
log.warn("Business exception: code={}, message={}", errorCode.getCode(), e.getMessage());
```

**日志行为说明**：
| 属性 | 值 |
|------|-----|
| 日志级别 | `WARN`（与已有 ExceptionHandler 日志风格一致） |
| 记录内容 | `e.getMessage()` 返回**原始消息模板**（如 `"账户已锁定，请{锁定时间}后重试"`），而非 `formatMessage` 插值后的结果 |
| 记录时机 | `formatMessage` 调用之后、`return ResponseEntity` 之前——此时 `e.getMessage()` 尚未被修改，确保日志捕获原始模板 |
| 记录目的 | 便于调试时从日志中确认消息的原始模板占位符，而非仅看到插值后的消息 |

## 错误处理

- `log.warn` 不抛出任何异常，不影响业务流程
- 日志行在 `formatMessage` 之后，即使 `formatMessage` 内部有异常，日志仍会记录原始模板

## 行为契约

### handleBusinessException（整体方法）

- **前置条件**：接收 `BusinessException` 实例
- **方法逻辑顺序**：
  1. 获取 `errorCode`
  2. 解析 HTTP 状态码
  3. 对消息模板插值 → `message`
  4. **记录日志（新增）：`log.warn` 输出 `errorCode.getCode()` 和 `e.getMessage()`**
  5. 构造并返回 `ResponseEntity`
- **后置条件**：
  - 响应体 `Result.code` = `errorCode.getCode()`
  - 响应体 `Result.message` = formatMessage 插值后的消息
  - 日志记录的是原始消息模板，而非插值结果

## 依赖关系

| 依赖 | 说明 |
|------|------|
| `org.slf4j.Logger` | 已有 import（L7），`log` 字段已存在（L22），无需新增 |
| `org.slf4j.LoggerFactory` | 同上 |
| `BusinessException.getCode()`、`getMessage()` | `ErrorCode` 接口已有方法，无需修改 |

**不受影响的文件**：
| 文件 | 原因 |
|------|------|
| `GlobalExceptionHandlerTest.java` | 测试已包含 `shouldInterpolateAccountLockedMessage_logsOriginalTemplate`，无需修改。该测试验证 log appender 捕获到 1 条日志 |
| `AuthServiceImpl.java` | 已在 v6 中正确修正 args，无需改动 |
| 其他所有文件 | 不变 |

## 验证方式

```bash
mvn test -pl common -am
```

预期结果：`GlobalExceptionHandlerTest` 全部 10 个测试通过，特别是 `shouldInterpolateAccountLockedMessage_logsOriginalTemplate` 不再因 log count 为 0 而失败。

## 修订说明（v7 r0 — RETRY）
| 审查意见 | 修改措施 |
|---------|---------|
| [严重] v6 实现中 `handleBusinessException` 遗漏了 `log.warn(...)` 行，导致测试 `shouldInterpolateAccountLockedMessage_logsOriginalTemplate` 期望 `appender.list.size()` 为 1、实际为 0（verify_v6.md:346） | 本轮设计在 `formatMessage` 调用之后、`return ResponseEntity` 之前明确指定插入 `log.warn("Business exception: code={}, message={}", errorCode.getCode(), e.getMessage())`。仅此 1 行变更，无其他修改。 |
