# 实现报告（v4）

## 概述
实现了 Stage 2 速率限制基础设施：3 个生产类型（SlidingWindowCounter、RateLimitGuard、InMemoryRateLimitGuard）及 1 个单元测试类，位于 `common-module` 的 `auth.rateLimit` 子包下。

## 文件变更清单
| 操作 | 文件路径 | 说明 |
|------|---------|------|
| 新建 | `modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/auth/rateLimit/SlidingWindowCounter.java` | 滑动窗口计数器工具类 |
| 新建 | `modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/auth/rateLimit/RateLimitGuard.java` | 速率限制策略接口 |
| 新建 | `modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/auth/rateLimit/InMemoryRateLimitGuard.java` | 内存限流实现 |
| 新建 | `modules/common-module/common-module-impl/src/test/java/com/aimedical/modules/commonmodule/auth/rateLimit/InMemoryRateLimitGuardTest.java` | 限流单元测试（6 用例） |

## 编译验证
编译通过，6 个单元测试全部通过。

## 设计偏差说明
无偏差。

## 修订说明（v4 r1）
| 审查意见 | 修改措施 |
|---------|---------|
| `cleanup()` 中使用 `now - 50` 阈值错误移除窗口有效条目，破坏限流正确性 | 移除 `cleanup()` 中 while 循环清理旧时间戳的逻辑，仅保留空 Deque key 的清理：`windows.entrySet().removeIf(entry -> entry.getValue().isEmpty())` — 这与设计规格"清理周期：移除空 Deque 对应的 key"一致，过期条目的清理已由 `tryAcquire` 中的 `compute` 闭包负责 |
