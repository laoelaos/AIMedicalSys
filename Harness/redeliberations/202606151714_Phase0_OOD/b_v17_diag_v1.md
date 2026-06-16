# 质量审查诊断报告 — Phase 0 OOD 设计（v17）

审查时间：2026-06-15  
审查轮次：第 1 轮  
审查视角：需求响应充分度、整体深度与完整性、实际落地及可编码性

---

## 问题总览

| # | 严重程度 | 类别 | 简述 |
|---|---------|------|------|
| 1 | 一般 | 关键遗漏 | 框架级公共 ErrorCode 常量未定义，GlobalExceptionHandler 等组件引用的错误码无出处 |
| 2 | 一般 | 关键遗漏 | AuthenticationEntryPoint / AccessDeniedHandler 的实现类结构、包路径、代码骨架未定义 |
| 3 | 一般 | 关键遗漏 | GlobalExceptionHandler 缺乏方法级代码骨架，异常到 Result 的映射对开发者不可直接编码 |
| 4 | 轻微 | 完整性 | SecurityConfigPhase0 代码示例缺少 http.cors() 和 http.exceptionHandling() 配置 |

---

## 问题 1：框架级公共 ErrorCode 常量未定义

**问题描述**：产出定义了 `ErrorCode interface` 和各模块 enum 实现的架构模式，但框架自身使用的公共错误码常量（常见于 `GlobalExceptionHandler`、`SecurityConfig`、异常分类表等处）未被枚举定义。

具体表现为以下位置引用了未定义的 ErrorCode 标识符：

- 第 4.2 节「统一响应流程」：`Result.fail(PARAM_INVALID)` 和 `Result.fail(SYSTEM_ERROR)` — `PARAM_INVALID`、`SYSTEM_ERROR` 未定义
- 第 4.5 节「权限校验契约」：`Result.fail(AUTH_UNAUTHORIZED)` 和 `Result.fail(FORBIDDEN)` — 对应错误码未定义
- 第 5.1 节错误分类表第 7 行：`BusinessException(NOT_FOUND)` — `NOT_FOUND` 未定义
- 第 5.1 节错误分类表第 8 行：`DataIntegrityViolationException` 经 `GlobalExceptionHandler` 转换时使用的 ErrorCode 未定义

这些标识符实际指向框架级公共错误码，应归属于 `CommonErrorCode` 枚举（在 `common.exception` 包中 `implements ErrorCode`）。目前产出描述了「按业务域分配错误码段」的模式，但未产出 `COMMON_XXXX` 段的任何具体枚举值，导致上述引用成为无源之水。开发者实现 Phase 0 时将各自猜测命名和 code 字符串，产生不一致的全局错误码体系，背离了统一错误码命名空间的设计目标。

**所在位置**：
- 第 3.1 节 `ErrorCode` 小节（描述 interface 模式但无 common error code 定义）
- 第 4.2 节统一响应流程
- 第 4.5 节权限校验契约
- 第 5.1 节错误分类表

**严重程度**：一般

**改进建议**：在 `common.exception` 包中补充 `CommonErrorCode enum implements ErrorCode` 定义，至少覆盖当前引用过的错误码：

```
COMMON_SYSTEM_ERROR("COMMON_SYSTEM_ERROR", "系统内部错误"),
COMMON_PARAM_INVALID("COMMON_PARAM_INVALID", "请求参数校验失败"),
COMMON_NOT_FOUND("COMMON_NOT_FOUND", "请求的资源不存在"),
COMMON_DATA_INTEGRITY_VIOLATION("COMMON_DATA_INTEGRITY_VIOLATION", "数据完整性冲突"),
AUTH_UNAUTHORIZED("AUTH_UNAUTHORIZED", "未登录或 token 已过期"),
AUTH_FORBIDDEN("AUTH_FORBIDDEN", "无权限访问");
```

同时更新 4.2 节/4.5 节/5.1 节中引用的错误码标识符，使其与 `CommonErrorCode` 枚举值一致（如 `SYSTEM_ERROR` → `COMMON_SYSTEM_ERROR`）。

---

## 问题 2：AuthenticationEntryPoint / AccessDeniedHandler 实现不可编码

**问题描述**：第 4.5 节指出「骨架中保留真实的 AuthenticationEntryPoint、AccessDeniedHandler 和 PasswordEncoder 配置，共享给两个 profile 的 SecurityConfig 使用」，但：

1. 未指定这两个类的**包路径**（应在 `common.config` 还是 `common.security`？）
2. 未给出**类名**建议（如 `JsonAuthenticationEntryPoint` / `JsonAccessDeniedHandler`）
3. 未提供**代码骨架**（如何构造 `Result<T>` 返回？如何设置 HTTP 状态码？）
4. 未说明两者与 `GlobalExceptionHandler` 的 **Result 格式统一性**如何保障（是复用同一套 ErrorCode + Result 构造逻辑，还是各自实现一遍？）

开发者拿到设计文档后，无法直接编码实现这两个安全组件，必须自行推断类名、包路径和实现方式，多人并行时极易产生不一致实现。

**所在位置**：第 4.5 节「权限校验契约」

**严重程度**：一般

**改进建议**：补充两个类的设计定义：

1. 类名建议：`JsonAuthenticationEntryPoint implements AuthenticationEntryPoint`、`JsonAccessDeniedHandler implements AccessDeniedHandler`
2. 包路径：`com.aimedical.common.config`（与 SecurityConfig 同包）
3. 代码骨架（至少方法签名和关键构造逻辑）：
   - `commence()` 方法中通过 `response.setStatus(401)` + 构造 `Result.fail(AUTH_UNAUTHORIZED)` 对象写入 response body（Content-Type: application/json）
   - `handle()` 方法中通过 `response.setStatus(403)` + 构造 `Result.fail(AUTH_FORBIDDEN)` 对象写入 response body
4. 与 GlobalExceptionHandler 的复用关系：明确两者是否共享 `Jackson ObjectMapper` 实例

---

## 问题 3：GlobalExceptionHandler 缺乏方法级代码骨架

**问题描述**：尽管第 3.1 节和第 5 节详细描述了 GlobalExceptionHandler 的职责和要处理的异常类型，但未给出任何方法签名或代码骨架。第 5.1 节错误分类表列出了 11 种异常场景：

- `MethodArgumentNotValidException`、`HttpMessageNotReadableException`、`HttpMessageNotWritableException`、`BusinessException`、`AuthenticationException`、`AccessDeniedException`、`DataIntegrityViolationException`、通用系统异常、AI 调用异常（非异常）等

但每种异常对应的 `@ExceptionHandler` 方法签名、处理逻辑、`Result` 构造方式均未展示。第 5.3 节提到「HttpMessageNotReadableException / HttpMessageNotWritableException 等序列化异常统一在 GlobalExceptionHandler 中注册 @ExceptionHandler 方法」，但无方法实现。

需求明确要求「可直接指导编码实现」，GlobalExceptionHandler 是所有 Controller 异常的统一出口，其实现细节的缺失降低了产出的可编码性。

**所在位置**：第 3.1 节「GlobalExceptionHandler」、第 5.1 节、第 5.3 节

**严重程度**：一般

**改进建议**：在第 3.1 节或第 5 节补充 GlobalExceptionHandler 的代码骨架，至少包含以下方法签名和处理逻辑：

```
@ControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException ex) { ... }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleValidationException(MethodArgumentNotValidException ex) { ... }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Result<Void> handleMessageNotReadable(HttpMessageNotReadableException ex) { ... }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public Result<Void> handleDataIntegrityViolation(DataIntegrityViolationException ex) { ... }

    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception ex) { ... }
}
```

方法内部至少说明 `Result.fail(errorCode)` 的构造方式和日志记录策略。

---

## 问题 4：SecurityConfigPhase0 代码示例缺少 CORS 和异常处理配置

**问题描述**：第 4.5 节的 SecurityConfigPhase0 代码示例仅包含 `authorizeHttpRequests` 配置块。正文提到「复用 AuthenticationEntryPoint、AccessDeniedHandler、CORS 等基础配置」和「定义 CorsConfigurationSource Bean」，但代码示例中未展示以下关键配置：

1. `http.cors()` — 启用 CORS 配置（`CorsConfigurationSource` Bean 已定义但未在 filterChain 中引用）
2. `http.exceptionHandling()` — 注入 `AuthenticationEntryPoint` 和 `AccessDeniedHandler`
3. `http.sessionManagement()` — Session 策略（虽然可默认，但作为骨架应显式说明）

开发者参照代码示例编写 SecurityConfig 时，将得到一个没有 CORS 和异常处理的安全配置，跨域请求将被拦截，认证/授权异常将使用 Spring 默认的错误页面而非 `Result<T>` 格式。

**所在位置**：第 4.5 节 SecurityConfigPhase0 代码示例

**严重程度**：轻微

**改进建议**：在 filterChain Bean 的方法体中补充以下配置：

```
http
    .cors(withDefaults())
    .exceptionHandling(exceptions -> exceptions
        .authenticationEntryPoint(authenticationEntryPoint)
        .accessDeniedHandler(accessDeniedHandler)
    )
    .authorizeHttpRequests(auth -> auth
        .requestMatchers("/api/ping").permitAll()
        .anyRequest().permitAll()
    );
```

并在代码示例旁或类上方注明 `CorsConfigurationSource` Bean 的定义位置（如同一类中的 `@Bean` 方法或独立的 `CorsConfig` 类）。

---

## 整体评价

产出经过 17 轮迭代，在模块划分、接口契约框架、权限模型、AI 能力抽象、依赖管理和 CI 等方面已达到较高的完整性和一致性。上述四个问题均属于可编码性方面的剩余缺口，而非方向性或架构性错误。修复上述问题后，产出可直接作为 Phase 0 编码实现的完整设计指引。
