# 详细设计（v7）

## 概述

实现 Stage 2 安全 Filter 链核心组件 GlobalRateLimitFilter（OncePerRequestFilter），在 JwtAuthenticationFilter 之前对所有非白名单 API 路径实施 IP 级速率限制（100 次/60 秒/IP）。新建 1 个生产类、1 个测试类，位于 `auth/security/` 子包下。

设计目标：
- 全局 IP 限流：覆盖所有 API 路径（白名单除外），白名单直接放行
- 与 InMemoryRateLimitGuard 职责分工明确：全局限流与登录专用限流计数器独立，互不干扰
- 不使用 @Component，由后续 SecurityConfigPhase1 显式注册 Bean
- 不抛出异常，超过阈值直接写入 HTTP 429 响应

不在范围：InMemoryRateLimitGuard 修改、SecurityConfigPhase1 创建、JwtAuthenticationFilter 迁移、RequestHelper 工具类创建。

## 文件规划

| 文件路径（相对 `AIMedical/backend/`） | 操作 | 职责 |
|---------|------|------|
| `modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/auth/security/GlobalRateLimitFilter.java` | 新建 | 全局 IP 限流 OncePerRequestFilter |
| `modules/common-module/common-module-impl/src/test/java/com/aimedical/modules/commonmodule/auth/security/GlobalRateLimitFilterTest.java` | 新建 | GlobalRateLimitFilter 单元测试（9 用例） |

## 类型定义

### GlobalRateLimitFilter

**形态**：public class
**包路径**：`com.aimedical.modules.commonmodule.auth.security`
**职责**：全局 IP 速率限制 OncePerRequestFilter，在 JwtAuthenticationFilter 之前执行，对所有非白名单路径实行 100 次/60 秒/IP 限流。

```java
package com.aimedical.modules.commonmodule.auth.security;

import com.aimedical.common.exception.GlobalErrorCode;
import com.aimedical.common.result.Result;
import com.aimedical.modules.commonmodule.auth.rateLimit.SlidingWindowCounter;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

public class GlobalRateLimitFilter extends OncePerRequestFilter {

    private static final int RATE_LIMIT = 100;
    private static final long RATE_WINDOW_MS = 60_000L;
    private static final List<String> WHITELIST_PATHS = List.of(
        "/api/auth/login",
        "/api/auth/refresh",
        "/actuator/health",
        "/actuator/info"
    );

    private final SlidingWindowCounter counter;
    private final ObjectMapper objectMapper;

    public GlobalRateLimitFilter(SlidingWindowCounter counter) {
        this.counter = counter;
        this.objectMapper = new ObjectMapper();
    }
}
```

**字段说明**：

| 字段 | 类型 | 可见性 | 说明 |
|------|------|--------|------|
| `counter` | `SlidingWindowCounter` | `private final` | 滑动窗口计数器实例，由构造器注入；每个 GlobalRateLimitFilter Bean 拥有独立实例，与 InMemoryRateLimitGuard 的计数器互不干扰 |
| `objectMapper` | `ObjectMapper` | `private final` | Jackson JSON 序列化器，用于将 `Result.fail(...)` 写入响应 body |
| `RATE_LIMIT` | `int` | `private static final` | 限流阈值，100 次 |
| `RATE_WINDOW_MS` | `long` | `private static final` | 限流时间窗口，60,000 ms |
| `WHITELIST_PATHS` | `List<String>` | `private static final` | 白名单路径列表，调用 `request.getRequestURI()` 精确匹配 |

**公开接口**：

| 方法签名 | 返回 | 说明 |
|---------|------|------|
| `public GlobalRateLimitFilter(SlidingWindowCounter counter)` | 构造器 | 注入 SlidingWindowCounter 实例；ObjectMapper 在构造器内新建 |
| `protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)` | void | `@Override` 自 OncePerRequestFilter，Filter 核心逻辑 |
| `protected boolean shouldNotFilter(HttpServletRequest request)` | boolean | `@Override` 自 OncePerRequestFilter（可选），返回 true 跳过 filter |

**构造方式**：由 SecurityConfigPhase1 的 @Bean 方法显式创建：`new GlobalRateLimitFilter(new SlidingWindowCounter())`。
**类型关系**：继承 `org.springframework.web.filter.OncePerRequestFilter`，依赖 `SlidingWindowCounter`。

### doFilterInternal 行为规格

```
输入：HttpServletRequest request, HttpServletResponse response, FilterChain filterChain
输出：void（直接操作 response 或调用 filterChain.doFilter）

算法：
1. 白名单检查：
   String uri = request.getRequestURI()
   if (WHITELIST_PATHS.contains(uri)) → filterChain.doFilter(request, response); return;
2. IP 提取：
    String forwardedHeader = request.getHeader("X-Forwarded-For")
   String ip = (forwardedHeader != null && !forwardedHeader.isEmpty())
       ? forwardedHeader.split(",")[0].trim()
       : request.getRemoteAddr()
3. 限流检查：
   boolean allowed = counter.tryAcquire(ip, RATE_LIMIT, RATE_WINDOW_MS)
   if (allowed) → filterChain.doFilter(request, response); return;
   else → 写入 429 响应
4. 429 响应写入：
   response.setStatus(HttpServletResponse.SC_TOO_MANY_REQUESTS)  // 429
   response.setContentType("application/json")
   String body = objectMapper.writeValueAsString(Result.fail(GlobalErrorCode.RATE_LIMITED_GLOBAL))
   response.getWriter().write(body)
```

### shouldNotFilter 行为规格

不覆写 `shouldNotFilter`（使用 OncePerRequestFilter 默认实现，对任意请求均执行 doFilterInternal），白名单逻辑在 doFilterInternal 中内联处理。这样设计的原因：是否限流需要在 `doFilterInternal` 中有上下文（response 引用）来写入 429，而 `shouldNotFilter` 只有 request 没有 response，不适合处理限流细节。此外，对所有请求先进入 doFilterInternal 再做分发是 Spring Security Filter 链的常见模式，且白名单检查本身极轻量（List.contains）。

## 错误处理

GlobalRateLimitFilter **不抛出异常**（包括业务异常和 ServletException），超过阈值时直接写入 HTTP 429 响应：

| 场景 | 处理方式 |
|------|---------|
| 请求在白名单内 | `filterChain.doFilter(request, response)` 正常放行 |
| 请求未超限 | `filterChain.doFilter(request, response)` 正常放行 |
| 请求超限 | `response.setStatus(429)`, `response.setContentType("application/json")`, 写入 `Result.fail(GlobalErrorCode.RATE_LIMITED_GLOBAL)` JSON |
| `counter.tryAcquire` 参数校验 | SlidingWindowCounter 内部对 limit<=0 或 windowMs<=0 返回 false，不会抛出异常 |
| IP 提取时 request/response 为 null | OncePerRequestFilter 自身不会传入 null；如有异常则由 Servlet 容器处理 |

ErrorCode 引用路径：`com.aimedical.common.exception.GlobalErrorCode.RATE_LIMITED_GLOBAL`。

## 行为契约

### doFilterInternal 整体契约
- **白名单精确匹配**：使用 `List.contains()` 做精确 URI 匹配，不包含通配或前缀匹配
- **IP 提取优先级**：`X-Forwarded-For` header 优先（取其第一个逗号前 IP），无 header 时回退 `getRemoteAddr()`
- **限流粒度**：每个 IP 独立计数（`counter.tryAcquire(ip, 100, 60_000)`，SlidingWindowCounter 内部以 IP 为 key 管理 Deque<Long>）
- **429 响应格式**：Content-Type: application/json，body 为 `Result.fail(RATE_LIMITED_GLOBAL)` 的 JSON 序列化结果
- **非阻塞响应**：在调用 `response.getWriter().write()` 后**不调用** `filterChain.doFilter()`，防止重复放行
- **无异常传播**：限流超限时直接写 response，不走 Spring Security 的 AuthenticationEntryPoint

### 线程安全
- `counter.tryAcquire()` 内部使用 `ConcurrentHashMap.compute()` 保证原子性
- `WHITELIST_PATHS` 不可变 List，线程安全
- `objectMapper.writeValueAsString()` 线程安全（Jackson ObjectMapper 设计为线程安全）
- 无共享可变状态，Filter 本身无额外加锁需求

## 依赖关系

### 新增依赖（生产代码）
- `org.springframework.web.filter.OncePerRequestFilter` — 父类
- `com.aimedical.modules.commonmodule.auth.rateLimit.SlidingWindowCounter` — 滑动窗口计数器
- `com.aimedical.common.exception.GlobalErrorCode` — RATE_LIMITED_GLOBAL 枚举值
- `com.aimedical.common.result.Result` — 统一响应格式
- `com.fasterxml.jackson.databind.ObjectMapper` — JSON 序列化
- `jakarta.servlet.*` — Servlet API
- `java.util.List` — 白名单集合

### 已有依赖（不变）
- `SlidingWindowCounter` — R4 已完成，`tryAcquire(String, int, long)` 签名稳定
- `GlobalErrorCode.RATE_LIMITED_GLOBAL` — R2 已完成
- `Result.fail(ErrorCode)` — 已有，返回包含 code/message 的 Result

### 测试代码新增依赖
- `org.junit.jupiter.api.Test`
- `org.springframework.mock.web.MockHttpServletRequest`
- `org.springframework.mock.web.MockHttpServletResponse`
- `org.mockito.Mockito` — mock FilterChain
- `jakarta.servlet.FilterChain`
- `jakarta.servlet.http.HttpServletResponse`
- `com.aimedical.modules.commonmodule.auth.rateLimit.SlidingWindowCounter`
- `com.fasterxml.jackson.databind.ObjectMapper` — 反序列化验证 429 body

### 暴露给后续任务的公开接口
- `GlobalRateLimitFilter(SlidingWindowCounter)` — 供 SecurityConfigPhase1（任务 2.10）通过 @Bean 创建
- `doFilterInternal` — 由 OncePerRequestFilter 框架调用，SecurityConfigPhase1 通过 `http.addFilterBefore(globalRateLimitFilter, JwtAuthenticationFilter.class)` 注册到 Filter 链

### 不在此范围
- SecurityConfigPhase1 的 Filter 链组装（任务 2.10）
- JwtAuthenticationFilter 迁移（任务 2.1）
- InMemoryRateLimitGuard 修改（已有，独立职责）
- RequestHelper 工具类（IP 提取 Filter 内内联实现）

## 单元测试设计

### GlobalRateLimitFilterTest

**形态**：class（JUnit 5），无 Spring 上下文，使用 `MockHttpServletRequest` / `MockHttpServletResponse`
**包路径**：`com.aimedical.modules.commonmodule.auth.security.GlobalRateLimitFilterTest`
**注解**：无（package-private class，与 InMemoryRateLimitGuardTest / SlidingWindowCounterTest 风格一致）

**测试夹具**：
```java
class GlobalRateLimitFilterTest {
    private final SlidingWindowCounter counter = new SlidingWindowCounter();
    private final GlobalRateLimitFilter filter = new GlobalRateLimitFilter(counter);
    private final ObjectMapper objectMapper = new ObjectMapper();
}
```

**测试方法清单**（9 用例）：

| # | 测试方法 | 覆盖维度 | 验证点 |
|---|---------|---------|--------|
| 1 | `shouldPassRequestWithinLimit` | 正常路径 | 同一 IP 请求 1 次（<100），status 200，chain.doFilter 被调用 |
| 2 | `shouldBlockRequestWhenLimitExceeded` | 限流触发 | 请求 100 次后第 101 次返回 429 |
| 3 | `shouldPassWhitelistedLoginPath` | 白名单 | `/api/auth/login` 始终放行，即使超过 100 次 |
| 4 | `shouldPassWhitelistedRefreshPath` | 白名单 | `/api/auth/refresh` 始终放行 |
| 5 | `shouldPassWhitelistedActuatorHealthPath` | 白名单 | `/actuator/health` 始终放行 |
| 6 | `shouldPassWhitelistedActuatorInfoPath` | 白名单 | `/actuator/info` 始终放行 |
| 7 | `shouldHandleDifferentIpsIndependently` | IP 隔离 | IP-A 超限后 IP-B 仍可放行 |
| 8 | `shouldReturnRateLimitExceededResponseBody` | 响应体 | 429 body 包含 `RATE_LIMITED_GLOBAL` 错误码 |
| 9 | `shouldUseXForwardedForHeader` | IP 提取 | 设置 `X-Forwarded-For: 10.0.0.1, 10.0.0.2`，限流以 10.0.0.1 为准 |

**测试策略**：

1. **正常路径（用例 1）**：设置 URI `/api/user/me`，remoteAddr `192.168.1.1`，调用 `filter.doFilterInternal(request, response, chain)`，断言 `response.getStatus() == 200`（MockHttpServletResponse 默认 200），`verify(chain).doFilter(request, response)`。

2. **限流触发（用例 2）**：对同一 IP 循环调用 `doFilterInternal` 100 次，第 101 次断言 `response.getStatus() == 429`，`verify(chain, times(100)).doFilter(...)`，`verifyNoMoreInteractions(chain)`。

3. **白名单（用例 3-6）**：对每个白名单路径，构造 101 次请求（超限），断言 `response.getStatus() == 200`，`verify(chain, times(101)).doFilter(...)`。

4. **IP 隔离（用例 7）**：IP-A 请求 101 次（超限），IP-B 请求 1 次，断言 IP-A 第 101 次 status=429，IP-B 第 1 次 status=200。

5. **响应体验证（用例 8）**：超限后，通过 `objectMapper.readTree(response.getContentAsString())` 解析 JSON body，断言 `code` 字段等于 `"RATE_LIMITED_GLOBAL"`。

6. **X-Forwarded-For 优先（用例 9）**：设置 `request.addHeader("X-Forwarded-For", "10.0.0.1, 10.0.0.2")` 和 `request.setRemoteAddr("192.168.1.1")`，以 `10.0.0.1` 为 IP key 请求 100 次后超限；验证 101 次返回 429（证明使用的是 `10.0.0.1` 而非 `192.168.1.1`）。

**关键实现细节**：

- 多个 `doFilterInternal` 调用需使用新的 `MockHttpServletResponse`（因为 429 后 response 状态固定为 429，后续调用需重置到新 response），但 request 可复用（同一 IP）
- `MockHttpServletResponse` 的默认 status 为 200（`HttpServletResponse.SC_OK`）而非 0，所以放行断言直接用 `assertEquals(200, response.getStatus())`
- 为模拟超限，使用 `for` 循环执行 100 次 `doFilterInternal` + 第 101 次独立调用，共 101 次调用
- `Content-Type` 断言：`assertEquals("application/json", response.getContentType())`

**测试代码模板**（用例 1 / 用例 2 / 用例 8 / 用例 9 关键模式）：

```java
// 用例 1：正常放行
MockHttpServletRequest request = new MockHttpServletRequest();
request.setRequestURI("/api/user/me");
request.setRemoteAddr("192.168.1.1");
MockHttpServletResponse response = new MockHttpServletResponse();
FilterChain chain = mock(FilterChain.class);

filter.doFilterInternal(request, response, chain);

assertEquals(200, response.getStatus());
verify(chain).doFilter(request, response);
```

```java
// 用例 2：限流触发
MockHttpServletRequest request = new MockHttpServletRequest();
request.setRequestURI("/api/user/me");
request.setRemoteAddr("192.168.1.1");
FilterChain chain = mock(FilterChain.class);

for (int i = 0; i < 100; i++) {
    MockHttpServletResponse resp = new MockHttpServletResponse();
    filter.doFilterInternal(request, resp, chain);
    assertEquals(200, resp.getStatus());
}
MockHttpServletResponse blockedResp = new MockHttpServletResponse();
filter.doFilterInternal(request, blockedResp, chain);
assertEquals(429, blockedResp.getStatus());
verify(chain, times(100)).doFilter(any(), any());
```

```java
// 用例 8：响应体验证
// （超限准备同上，获取 blockedResp）
JsonNode body = objectMapper.readTree(blockedResp.getContentAsString());
assertEquals("RATE_LIMITED_GLOBAL", body.get("code").asText());
```

```java
// 用例 9：X-Forwarded-For 优先
MockHttpServletRequest request = new MockHttpServletRequest();
request.setRequestURI("/api/user/me");
request.addHeader("X-Forwarded-For", "10.0.0.1, 10.0.0.2");
request.setRemoteAddr("192.168.1.1");
// 对 10.0.0.1 请求 100 次 → 第 101 次应返回 429
for (int i = 0; i < 100; i++) {
    filter.doFilterInternal(request, new MockHttpServletResponse(), chain);
}
MockHttpServletResponse blockedResp = new MockHttpServletResponse();
filter.doFilterInternal(request, blockedResp, chain);
assertEquals(429, blockedResp.getStatus());
```

## 修订说明（v7 r1）
| 审查意见 | 修改措施 |
|---------|---------|
| [一般] 算法伪代码中的 HTTP Header 名称错误：`"X-Forwarded-Forged"` 应更正为 `"X-Forwarded-For"` | `detail_v7.md:99` 伪代码中 `request.getHeader("X-Forwarded-Forged")` 已更正为 `request.getHeader("X-Forwarded-For")`，与文档其他位置（第 136 行规格说明、第 227 行测试用例 9）保持一致
