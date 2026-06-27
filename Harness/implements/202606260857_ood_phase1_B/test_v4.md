# 测试报告（v4）

## 测试文件清单

| 文件 | 路径 | 用例数 |
|------|------|--------|
| InMemoryRateLimitGuardTest | `modules/common-module/common-module-impl/src/test/java/com/aimedical/modules/commonmodule/auth/rateLimit/InMemoryRateLimitGuardTest.java` | 8 |
| SlidingWindowCounterTest | `modules/common-module/common-module-impl/src/test/java/com/aimedical/modules/commonmodule/auth/rateLimit/SlidingWindowCounterTest.java` | 11 |

## 测试结果

```
Tests run: 19, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

### InMemoryRateLimitGuardTest（8 用例）

| 测试方法 | 覆盖维度 | 验证点 |
|---------|---------|--------|
| shouldThrowNpeWhenKeyIsNull | 错误路径 | key 为 null 时抛出 NPE |
| shouldMaintainIndependentKeys | 状态交互 | 不同 key 的限流相互独立 |
| shouldAllowUpToLimit | 正常路径 | 5 次以内请求均返回 true |
| shouldRejectWhenExceedLimit | 限流触发 | 第 6 次请求返回 false |
| shouldAllowAfterWindowExpiry | 窗口重置 | 等待 >10 秒后恢复 true |
| shouldHandleConcurrentRequests | 并发安全 | 多线程并发，总放行数 ≤ limit |
| shouldUseDefaultLimitAndWindow | 默认参数 | 无参 tryAcquire 使用 5/10s 默认值 |
| shouldRespectCustomLimitAndWindow | 自定义参数 | 全参构造 + 自定义 limit/window |

### SlidingWindowCounterTest（11 用例）

| 测试方法 | 覆盖维度 | 验证点 |
|---------|---------|--------|
| shouldAcquireWithinLimit | 正常路径 | 单 key 在 limit 内返回 true |
| shouldRejectWhenExceedLimit | 限流触发 | 达到上限后返回 false |
| shouldReturnFalseWhenLimitIsZero | 边界条件 | limit = 0 返回 false |
| shouldReturnFalseWhenLimitIsNegative | 边界条件 | limit = -1 返回 false |
| shouldReturnFalseWhenWindowMsIsZero | 边界条件 | windowMs = 0 返回 false |
| shouldReturnFalseWhenWindowMsIsNegative | 边界条件 | windowMs = -1 返回 false |
| shouldThrowNpeWhenKeyIsNull | 错误路径 | key 为 null 抛出 NPE |
| shouldMaintainIndependentCountersForDifferentKeys | 状态交互 | 不同 key 计数器独立 |
| shouldAllowNewRequestAfterWindowExpiry | 窗口重置 | 超时后新请求返回 true |
| shouldHandleConcurrentRequestsForSameKey | 并发安全 | 同 key 并发总放行数 ≤ limit |
| shouldHandleConcurrentRequestsForDifferentKeys | 并发安全 | 不同 key 并发互不干扰 |

## 覆盖维度统计

| 维度 | 用例数 |
|------|--------|
| 正常路径 | 2 |
| 边界条件 | 4 |
| 错误路径 | 2 |
| 限流触发/窗口重置 | 4 |
| 状态交互 | 3 |
| 并发安全 | 3 |
| 默认/自定义参数 | 2 |

## 设计偏差说明

更新 `InMemoryRateLimitGuardTest` 新增 2 个用例（null key 错误路径、独立 key 状态交互），并新增 `SlidingWindowCounterTest`（11 用例）覆盖 SlidingWindowCounter 的边界条件、错误路径、状态交互和并发安全。
