# 测试报告（v7）

## 概述

GlobalRateLimitFilter 单元测试共 **9 用例**，全部通过。

## 测试环境

- **构建工具**：Maven 3
- **测试框架**：JUnit 5 (JUnitPlatformProvider)
- **模块**：`modules/common-module/common-module-impl`
- **被测类**：`com.aimedical.modules.commonmodule.auth.security.GlobalRateLimitFilter`
- **测试类**：`com.aimedical.modules.commonmodule.auth.security.GlobalRateLimitFilterTest`

## 测试结果

```
Tests run: 9, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

## 用例明细

| # | 测试方法 | 覆盖维度 | 结果 |
|---|---------|---------|------|
| 1 | `shouldPassRequestWithinLimit` | 正常路径 | ✅ PASS |
| 2 | `shouldBlockRequestWhenLimitExceeded` | 限流触发 | ✅ PASS |
| 3 | `shouldPassWhitelistedLoginPath` | 白名单 | ✅ PASS |
| 4 | `shouldPassWhitelistedRefreshPath` | 白名单 | ✅ PASS |
| 5 | `shouldPassWhitelistedActuatorHealthPath` | 白名单 | ✅ PASS |
| 6 | `shouldPassWhitelistedActuatorInfoPath` | 白名单 | ✅ PASS |
| 7 | `shouldHandleDifferentIpsIndependently` | IP 隔离 | ✅ PASS |
| 8 | `shouldReturnRateLimitExceededResponseBody` | 响应体 | ✅ PASS |
| 9 | `shouldUseXForwardedForHeader` | IP 提取 | ✅ PASS |

## 覆盖维度总结

- **正常路径**：用例 1
- **限流触发**：用例 2
- **白名单**：用例 3/4/5/6
- **IP 隔离**：用例 7
- **响应体**：用例 8
- **IP 提取优先**：用例 9

## 测试代码风格

- 与项目已有测试保持一致：package-private class，无 Spring 上下文，JUnit 5
- 使用 `MockHttpServletRequest` / `MockHttpServletResponse` 模拟 Servlet 请求/响应
- 使用 Mockito mock `FilterChain` 验证调用次数
- 使用 `ObjectMapper.readTree()` 解析 JSON 响应体验证错误码

## 设计偏差

无偏差。测试与 `detail_v7.md` 完全一致，生产代码与 `code_v7.md` 一致。
