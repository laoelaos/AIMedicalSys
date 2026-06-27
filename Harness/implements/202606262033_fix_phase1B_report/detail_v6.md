# 详细设计（v6）

## 概述

修复 GlobalExceptionHandler 中两项 P1 缺陷：T3（ACCOUNT_LOCKED 消息模板未插值，客户端收到模板原文）和 T25（RATE_LIMITED 和 ACCOUNT_LOCKED 应返回 HTTP 429）。涉及 2 个源文件 + 1 个测试文件修改。

## 文件规划

| 文件路径 | 操作 | 职责 |
|---------|------|------|
| `common/src/main/java/com/aimedical/common/config/GlobalExceptionHandler.java` | 修改 | handleBusinessException 中新增 formatMessage 插值调用；resolveHttpStatus 中新增 RATE_LIMITED/ACCOUNT_LOCKED → 429 映射 |
| `common/src/test/java/com/aimedical/common/config/GlobalExceptionHandlerTest.java` | 修改 | 新增 shouldInterpolateAccountLockedMessage、shouldReturn429ForRateLimited；可选增强 shouldHandleBusinessExceptionWith400 回归验证 |
| `common-module-impl/.../service/impl/AuthServiceImpl.java` | **修改** | ACCOUNT_LOCKED 的两个构造调用中 args 从完整短语修正为仅占位符替换值 |

### AuthServiceImpl 修正原因

`AuthServiceImpl.java:95` 和 `:99` 调用 `new BusinessException(ACCOUNT_LOCKED, "请30分钟后重试")` / `"请15分钟后重试"`，将完整插值后短语传入 args。

枚举 `ACCOUNT_LOCKED.message = "账户已锁定，请{锁定时间}后重试"` 的占位符 `{锁定时间}` 期望仅接收时间值（如 `"30分钟"`、`"15分钟"`），而非已完成替换的完整短语。

若以当前完整短语作为 args，`formatMessage` 的 `replaceFirst("\\{[^}]+\\}", "请30分钟后重试")` 会产出 `"账户已锁定，请请30分钟后重试后重试"`（双倍"请"和"后重试"）。

修正：两处 args 分别改为 `"30分钟"` 和 `"15分钟"`。

## 类型定义

### 1. GlobalExceptionHandler.formatMessage（新增私有方法）

**形态**：private method（新增）
**包路径**：`com.aimedical.common.config.GlobalExceptionHandler`

**方法签名**：
```java
private String formatMessage(String template, Object[] args)
```

**职责**：将消息模板与 BusinessException 的 args 参数进行插值。优先使用 `MessageFormat.format`，对 `{锁定时间}` 等命名占位符降级为 `String.replaceFirst` 顺序替换。

**行为规则**：
- args 为 null 或空时直接返回 template
- 调用 `MessageFormat.format(template, args)`
  - 若成功（模板使用 `{0}` `{1}` 等编号占位符），返回格式化结果
- 若 `MessageFormat.format` 抛出 `IllegalArgumentException`（模板含 `{名称}` 类命名占位符），降级：
  - 对 args 中每个元素，调用 `template.replaceFirst("\\{[^}]+\\}", String.valueOf(arg))`
  - 按 args 顺序依次替换第一个未匹配的 `{...}` 占位符
- 任何其他异常均捕获，返回原始 template

**前置条件**：无（方法内全异常安全）
**后置条件**：返回值保证非 null（template 非 null 前提下）

### 2. GlobalExceptionHandler.handleBusinessException（修改）

**修改前**（L26-27）：
```java
return ResponseEntity.status(status)
        .body(Result.fail(errorCode));
```

**修改后**：
```java
String message = formatMessage(errorCode.getMessage(), e.getArgs());
return ResponseEntity.status(status)
        .body(Result.fail(errorCode.getCode(), message));
```

- `Result.fail(errorCode)` → `Result.fail(errorCode.getCode(), message)`：传入插值后的 message 而非通过枚举 getMessage() 间接获取
- `formatMessage` 对无 args 的 BusinessException 返回原始 template（无行为变化）

**日志行保持不变**（仍记录 `e.getMessage()` 即原始模板，便于调试时看到模板原文而非插值结果）：
```java
log.warn("Business exception: code={}, message={}", errorCode.getCode(), e.getMessage());
```

### 3. GlobalExceptionHandler.resolveHttpStatus（修改）

**修改前**（L55-56）：
```java
// 其他业务错误码默认返回400
return HttpStatus.BAD_REQUEST;
```

**修改后**（在 `HttpStatus.BAD_REQUEST` 返回之前新增两条映射）：
```java
if (GlobalErrorCode.RATE_LIMITED.getCode().equals(code) || GlobalErrorCode.ACCOUNT_LOCKED.getCode().equals(code)) {
    return HttpStatus.TOO_MANY_REQUESTS;
}
// 其他业务错误码默认返回400
return HttpStatus.BAD_REQUEST;
```

完整 resolveHttpStatus 方法（修改后）：
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
    if (GlobalErrorCode.RATE_LIMITED.getCode().equals(code) || GlobalErrorCode.ACCOUNT_LOCKED.getCode().equals(code)) {
        return HttpStatus.TOO_MANY_REQUESTS;
    }
    return HttpStatus.BAD_REQUEST;
}
```

### 4. 受影响的枚举常量

| 枚举 | code | message | HTTP 状态码变化 |
|-----|------|---------|---------------|
| `ACCOUNT_LOCKED` | `"ACCOUNT_LOCKED"` | `"账户已锁定，请{锁定时间}后重试"` | 400 → **429** |
| `RATE_LIMITED` | `"RATE_LIMITED"` | `"登录尝试过于频繁，请稍后重试"` | 400 → **429** |

## 错误处理

- `formatMessage` 内部完全异常安全：`MessageFormat.format` 的 `IllegalArgumentException` 被捕获作为降级触发信号，`String.replaceFirst` 不抛出受检异常，外层包裹 try-catch 兜底返回 template
- `handleBusinessException` 不新增 throws 声明

## 行为契约

### handleBusinessException
- **前置条件**：接收 `BusinessException` 实例
- **后置条件**：
  - 返回 `ResponseEntity<Result<Void>>`，HTTP 状态码由 `resolveHttpStatus(errorCode)` 确定
  - 响应体 `Result.code` = `errorCode.getCode()`
  - 响应体 `Result.message` = `formatMessage(errorCode.getMessage(), e.getArgs())`（即插值后的消息）
  - 日志记录原始 template（`e.getMessage()`），不记录插值结果

### formatMessage
- **前置条件**：template 非 null（实际调用方保证 `errorCode.getMessage()` 非 null）
- **后置条件**：
  - args 为 null/空 → 返回 template 原值
  - 模板含 `{0}` `{1}` 编号占位符且 args 匹配 → `MessageFormat.format` 结果
  - 模板含 `{名称}` 命名占位符 → 顺序替换后的结果
  - 任意异常 → 返回 template 原值

### resolveHttpStatus
- **前置条件**：errorCode 非 null（实际调用方保证）
- **后置条件**：
  - `RATE_LIMITED` / `ACCOUNT_LOCKED` → `HttpStatus.TOO_MANY_REQUESTS`（429）
  - 其他映射关系不变（UNAUTHORIZED → 401, FORBIDDEN → 403, NOT_FOUND → 404, PARAM_INVALID → 400, SYSTEM_ERROR → 500, 其余 → 400）

## 测试设计

### 新增测试方法

#### shouldInterpolateAccountLockedMessage()
- **场景**：BusinessException(ACCOUNT_LOCKED, "30分钟") 时 handleBusinessException 返回插值后的 message
- **验证**：
  - HTTP 状态码 = 429（T25 生效后 ACCOUNT_LOCKED 映射）
  - body.code = "ACCOUNT_LOCKED"
  - body.message = "账户已锁定，请30分钟后重试"
  - body.data = null

#### shouldReturn429ForRateLimited()
- **场景**：BusinessException(RATE_LIMITED) 时 resolveHttpStatus 返回 429
- **验证**：
  - HTTP 状态码 = 429
  - body.code = "RATE_LIMITED"

#### shouldHandleBusinessExceptionWith400（可选增强）
- **场景**：现有测试使用 TEST_ERROR（未在 GlobalErrorCode 中特殊匹配，落入 default → 400），args = null
- **验证**：回归确认 formatMessage 对无 args 场景返回 template 原文，行为不变
- **可选增强点**：显式断言 message = "业务异常"（当前已有此断言）

### 测试数据说明

| 测试方法 | BusinessException 构造 | 期望 body.message |
|---------|----------------------|------------------|
| shouldInterpolateAccountLockedMessage | `new BusinessException(ACCOUNT_LOCKED, "30分钟")` | `"账户已锁定，请30分钟后重试"` |
| shouldReturn429ForRateLimited | `new BusinessException(RATE_LIMITED)` | `"登录尝试过于频繁，请稍后重试"`（无占位符，原始模板） |
| shouldHandleBusinessExceptionWith400 | `new BusinessException(TEST_ERROR)` | `"业务异常"`（无占位符，原始模板） |

### 插值行为矩阵（formatMessage 测试覆盖）

| template | args | MessageFormat 结果 | 降级结果 | 最终行为 |
|---------|------|-------------------|---------|---------|
| `"账户已锁定，请{锁定时间}后重试"` | `["30分钟"]` | 抛 IllegalArgumentException | `"账户已锁定，请30分钟后重试"` | 降级替换 |
| `"登录尝试过于频繁，请稍后重试"` | null | N/A（直接返回 template） | N/A | 返回 template |
| `"业务异常"` | null | N/A（直接返回 template） | N/A | 返回 template |
| `"业务异常"` | `[]` | N/A（args 为空直接返回） | N/A | 返回 template |

## 依赖关系

### 新增依赖
- `java.text.MessageFormat`：`formatMessage` 方法使用的 JDK 标准库（无需新增 import，同一包 `java.text.*`）

### 受影响类型
| 类型 | 变更 | 影响范围 |
|------|------|---------|
| `GlobalExceptionHandler` | 新增 `formatMessage` 私有方法；修改 `handleBusinessException` 和 `resolveHttpStatus` | 仅该类内部 |
| `Result` | 已有 `fail(String code, String message)` 签名，无需修改 | 新增调用点 |
| `BusinessException` | 已有 `getArgs()` 方法，无需修改 | 新增调用点 |
| `GlobalErrorCode.ACCOUNT_LOCKED` | 枚举 message 含占位符 `{锁定时间}`，不需修改 | 被 formatMessage 处理 |
| `GlobalErrorCode.RATE_LIMITED` | 无占位符，不需修改 | 新增 HTTP 映射 |
| `AuthServiceImpl` | 两处 ACCOUNT_LOCKED 构造的 args 从完整短语修正为仅替换值 | 调用方修正 |

### 不受影响的文件（确认清单）
| 文件 | 原因 |
|------|------|
| 所有其他 `@ExceptionHandler` 方法（handleValidationException、handleMessageNotReadable 等） | 不调用 formatMessage，也不经过 resolveHttpStatus 的 429 分支 |
| `GlobalErrorCode.java` / `ErrorCode.java` / `BusinessException.java` / `Result.java` | 仅新增调用点，接口不变 |
| 其他模块的 Controller/Service 层 | GlobalExceptionHandler 是全局切面，修改不影响业务逻辑 |

## 修订说明（v6 r1）
| 审查意见 | 修改措施 |
|---------|---------|
| [严重] AuthServiceImpl.java 中 ACCOUNT_LOCKED args 传入了完整短语而非仅占位符替换值，导致 formatMessage 降级替换后消息出现双倍文本（如 "请请30分钟后重试后重试"） | 文件规划新增 AuthServiceImpl.java 修改行；"测试数据说明"和"插值行为矩阵"中 args 从 `"请30分钟后重试"` 修正为 `"30分钟"`，预期结果保持不变；新增"AuthServiceImpl 修正原因"章节详细说明 |
