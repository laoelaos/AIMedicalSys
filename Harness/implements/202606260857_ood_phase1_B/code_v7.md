# 实现报告（v7）

## 概述

实现了 Stage 2 安全 Filter 链核心组件 `GlobalRateLimitFilter`（OncePerRequestFilter）及其单元测试 `GlobalRateLimitFilterTest`。

- GlobalRateLimitFilter：全局 IP 速率限制 Filter，在 JwtAuthenticationFilter 之前对所有非白名单 API 路径实施 100 次/60 秒/IP 限流，超限时写入 HTTP 429 JSON 响应
- GlobalRateLimitFilterTest：9 个 JUnit 5 用例覆盖正常放行、限流触发、4 个白名单路径、IP 隔离、响应体验证、X-Forwarded-For 优先

## 文件变更清单

| 操作 | 文件路径 | 说明 |
|------|---------|------|
| 新建 | `modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/auth/security/GlobalRateLimitFilter.java` | 全局 IP 限流 OncePerRequestFilter |
| 新建 | `modules/common-module/common-module-impl/src/test/java/com/aimedical/modules/commonmodule/auth/security/GlobalRateLimitFilterTest.java` | GlobalRateLimitFilter 单元测试（9 用例） |

## 编译验证

编译通过。全部 9 个测试用例通过。

## 设计偏差说明

| 设计规格 | 偏差原因 | 实际处理 |
|---------|---------|---------|
| `doFilterInternal` 伪代码中使用 `HttpServletResponse.SC_TOO_MANY_REQUESTS` | 当前 Jakarta Servlet API 版本中 `HttpServletResponse` 无 `SC_TOO_MANY_REQUESTS` 常量 | 直接使用整数 `429` |
