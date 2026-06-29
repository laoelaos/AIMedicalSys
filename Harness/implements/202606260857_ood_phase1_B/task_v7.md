# 任务指令（v7）

## 动作
NEW

## 任务描述
新建 `auth/security/GlobalRateLimitFilter.java`（OncePerRequestFilter），实现 OOD 第 12 节 Stage 2 任务 2.2 + OOD 4.1 全局 IP 限流策略。新建配套单元测试 `auth/security/GlobalRateLimitFilterTest.java`。

预期文件路径（相对 `AIMedical/backend/`）：
- `modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/auth/security/GlobalRateLimitFilter.java`
- `modules/common-module/common-module-impl/src/test/java/com/aimedical/modules/commonmodule/auth/security/GlobalRateLimitFilterTest.java`

## 选择理由
- GlobalRateLimitFilter 是 Stage 2 安全 Filter 链的核心组件之一，在所有非白名单 API 路径上实施 IP 级速率限制（100 次/60 秒/IP），在 JwtAuthenticationFilter 之前执行
- 仅依赖已有基础设施：SlidingWindowCounter（R4 ✅）+ GlobalErrorCode.RATE_LIMITED_GLOBAL（R2 ✅），无其他未完成任务依赖
- 实现边界清晰，便于独立测试和验证
- 完成此 Filter 后 SecurityConfigPhase1（任务 2.10）可开始构建完整 Filter 链

## 任务上下文

### 行为规范（OOD 4.1）

**全局 IP 限流（GlobalRateLimitFilter）**：对所有 API 路径（排除白名单）实施 IP 级限流。实现为 OncePerRequestFilter，在 JwtAuthenticationFilter 之前执行。白名单路径直接 `chain.doFilter` 放行；非白名单路径检查滑动窗口计数器（100 次/60 秒/IP），超限返回 429 + ErrorCode.RATE_LIMITED_GLOBAL。

**白名单路径**（直接放行，不限流）：
- `/api/auth/login`
- `/api/auth/refresh`
- `/actuator/health`
- `/actuator/info`

**限流策略**：100 次/60 秒/IP（滑动窗口）。

### 与 InMemoryRateLimitGuard 的职责分工
GlobalRateLimitFilter **不委托** InMemoryRateLimitGuard，而是独立维护自己的滑动窗口计数器（同样基于 `ConcurrentHashMap<String, SlidingWindowCounter>`），与 InMemoryRateLimitGuard（针对登录端点 `/api/auth/login` 的专用限流，阈值 5 次/10 秒）职责分工明确：全局限流覆盖所有 API 路径（白名单除外），登录专用限流仅覆盖登录端点。两者共用滑动窗口算法实现，但**计数器实例独立，互不干扰**——GlobalRateLimitFilter 拥有自己的 `ConcurrentHashMap<String, SlidingWindowCounter>` 实例，InMemoryRateLimitGuard 拥有另一个完全独立的实例，两者不会共享同一个计数器 Map。

### 包路径与类型签名
- `package com.aimedical.modules.commonmodule.auth.security`
- `public class GlobalRateLimitFilter extends OncePerRequestFilter`
- 不使用 `@Component`（由 SecurityConfigPhase1 显式注册 Bean）
- 构造函数：`public GlobalRateLimitFilter(SlidingWindowCounter counter)` — 注入 SlidingWindowCounter 工具类实例

### Filter 行为详细描述
1. **白名单检查**：从 `request.getRequestURI()` 获取路径，匹配白名单列表。若匹配则直接 `filterChain.doFilter(request, response); return;`
2. **IP 提取**：
   - 优先检查 `X-Forwarded-For` header：取第一个逗号分隔的 IP 地址（`header.split(",")[0].trim()`）
   - 若 header 为空，回退到 `request.getRemoteAddr()`
3. **限流检查**：
   - 以 IP 为 key，从 `ConcurrentHashMap<String, SlidingWindowCounter>` 获取或创建 SlidingWindowCounter
   - 调用 `counter.tryAcquire(ipKey, 100, 60_000)` 检查是否超限
   - 返回 false → 超限：设置 response 状态码 429，Content-Type application/json，写入 `Result.fail(GlobalErrorCode.RATE_LIMITED_GLOBAL)` 的 JSON 字符串，不调用 `filterChain.doFilter`
   - 返回 true → 放行：`filterChain.doFilter(request, response)`
4. **不抛出异常**：直接写入 HTTP 响应，不走 Spring Security 的 AuthenticationEntryPoint

### 已有代码上下文

**SlidingWindowCounter**（`auth/rateLimit/SlidingWindowCounter.java`，R4 完成）：
```java
public class SlidingWindowCounter {
    public boolean tryAcquire(String key, int limit, long windowMs) { ... }
    // 内部 ConcurrentHashMap<String, Deque<Long>> windows
}
```

**GlobalErrorCode**（`common/exception/GlobalErrorCode.java`，R2 完成）：
```java
RATE_LIMITED_GLOBAL("RATE_LIMITED_GLOBAL", "请求过于频繁，请稍后重试")
```

**Result**（`common/result/Result.java`）：
```java
public class Result<T> {
    public static <T> Result<T> fail(ErrorCode errorCode) { ... }
    // 返回 { "code": errorCode.getCode(), "message": errorCode.getMessage(), ... }
}
```

**InMemoryRateLimitGuard** 模式参考（`auth/rateLimit/InMemoryRateLimitGuard.java`）：
```java
public class InMemoryRateLimitGuard implements RateLimitGuard {
    private final SlidingWindowCounter counter;
    // 拥有自己的 counter 实例
}
```

**JwtAuthenticationFilter** 迁移来源（`application/config/JwtAuthenticationFilter.java`）—— 当前仅做参考，该 Filter 也将迁移至 `auth/security/` 包（任务 2.1）。

### Spring 注册方式
GlobalRateLimitFilter 不使用 `@Component` 注解，由后续 SecurityConfigPhase1（任务 2.10）通过 Bean 方法显式创建和注册：
```java
@Bean
public GlobalRateLimitFilter globalRateLimitFilter() {
    return new GlobalRateLimitFilter(new SlidingWindowCounter());
}
```
Filter 注册到 SecurityFilterChain：
```java
http.addFilterBefore(globalRateLimitFilter, JwtAuthenticationFilter.class);
```

### 不在此范围
- InMemoryRateLimitGuard 的修改（已有，独立职责）
- SecurityConfigPhase1 的创建（任务 2.10）
- JwtAuthenticationFilter 的迁移（任务 2.1）
- RequestHelper 工具类创建（IP 提取 Filter 内内联实现即可）

## 测试设计

### GlobalRateLimitFilterTest
**形态**：class（JUnit 5），使用 `MockHttpServletRequest` / `MockHttpServletResponse`，不引入 Spring 上下文
**包路径**：`com.aimedical.modules.commonmodule.auth.security.GlobalRateLimitFilterTest`

主要覆盖场景：

| # | 测试方法 | 覆盖维度 | 验证点 |
|---|---------|---------|--------|
| 1 | `shouldPassRequestWithinLimit` | 正常路径 | 同一 IP 请求次数在阈值内应放行（status 200 / chain.doFilter 被调用） |
| 2 | `shouldBlockRequestWhenLimitExceeded` | 限流触发 | 超过 100 次/60 秒后返回 429 |
| 3 | `shouldPassWhitelistedLoginPath` | 白名单 | `/api/auth/login` 始终放行 |
| 4 | `shouldPassWhitelistedRefreshPath` | 白名单 | `/api/auth/refresh` 始终放行 |
| 5 | `shouldPassWhitelistedActuatorHealthPath` | 白名单 | `/actuator/health` 始终放行 |
| 6 | `shouldPassWhitelistedActuatorInfoPath` | 白名单 | `/actuator/info` 始终放行 |
| 7 | `shouldHandleDifferentIpsIndependently` | IP 隔离 | 不同 IP 的计数器独立 |
| 8 | `shouldReturnRateLimitExceededResponseBody` | 响应体 | 429 响应的 body 包含 `RATE_LIMITED_GLOBAL` 错误码 |
| 9 | `shouldUseXForwardedForHeader` | IP 提取 | `X-Forwarded-For` header 优先于 `getRemoteAddr()` |

### 测试实现策略
- 使用 `MockHttpServletRequest` 设置 URI、header、remoteAddr
- 使用 `MockHttpServletResponse` 捕获响应状态码和内容
- 使用 `Mockito.mock(FilterChain.class)` 验证 `doFilter` 调用次数
- 测试构造器注入 `SlidingWindowCounter` 实例（与 InMemoryRateLimitGuardTest 风格一致）
- 通过多次调用 `doFilterInternal` 模拟超过阈值的场景
- 不需要 Servlet 容器或 Spring 上下文

### 测试代码模板
```java
package com.aimedical.modules.commonmodule.auth.security;

import com.aimedical.common.result.Result;
import com.aimedical.modules.commonmodule.auth.rateLimit.SlidingWindowCounter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GlobalRateLimitFilterTest {

    private final SlidingWindowCounter counter = new SlidingWindowCounter();
    private final GlobalRateLimitFilter filter = new GlobalRateLimitFilter(counter);

    @Test
    void shouldPassRequestWithinLimit() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/auth/me");
        request.setRemoteAddr("192.168.1.1");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        assertEquals(HttpServletResponse.SC_OK, response.getStatus());
        verify(chain).doFilter(request, response);
    }
    // ... 其他测试方法
}
```
