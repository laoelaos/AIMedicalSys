# 测试报告（v6）

## 概述

InMemoryTokenBlacklist 单元测试共 **12 用例**，全部通过。

## 测试环境

- **构建工具**：Maven 3
- **测试框架**：JUnit 5 (JUnitPlatformProvider)
- **模块**：`modules/common-module/common-module-impl`
- **被测类**：`com.aimedical.modules.commonmodule.auth.blacklist.InMemoryTokenBlacklist`
- **测试类**：`com.aimedical.modules.commonmodule.auth.blacklist.InMemoryTokenBlacklistTest`

## 测试结果

```
Tests run: 12, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

## 用例明细

| # | 测试方法 | 覆盖维度 | 结果 |
|---|---------|---------|------|
| 1 | `shouldReturnTrueForBlacklistedJti` | 正常路径 | ✅ PASS |
| 2 | `shouldReturnFalseForUnknownJti` | 未知 key | ✅ PASS |
| 3 | `shouldHandleMultipleJtiIndependently` | 状态隔离 | ✅ PASS |
| 4 | `shouldReturnFalseAfterRemoval` | 移除后 | ✅ PASS |
| 5 | `shouldReturnFalseForExpiredEntry` | 过期条目 | ✅ PASS |
| 6 | `shouldThrowNpeWhenAddWithNullJti` | 错误路径 | ✅ PASS |
| 7 | `shouldThrowNpeWhenIsBlacklistedWithNull` | 错误路径 | ✅ PASS |
| 8 | `shouldHandleConcurrentAddSameJti` | 并发安全 | ✅ PASS |
| 9 | `shouldHandleConcurrentAddDifferentJti` | 并发安全 | ✅ PASS |
| 10 | `shouldNotThrowWhenAddingExistingJti` | 幂等性 | ✅ PASS |
| 11 | `shouldCleanupExpiredEntries` | 定时清理 | ✅ PASS |
| 12 | `shouldRetainNonExpiredEntriesAfterCleanup` | 清理保留 | ✅ PASS |

## 覆盖维度总结

- **正常路径**：用例 1/2/3/10
- **过期清理**：用例 4/5/11/12
- **错误路径**：用例 6/7
- **并发安全**：用例 8/9

## 测试代码风格

- 与项目已有测试（InMemoryRateLimitGuardTest / SlidingWindowCounterTest / LoginAttemptTrackerTest）保持一致：
  - package-private class，无 Spring 上下文
  - JUnit 5，无额外注解
  - 使用 `CountDownLatch` + `CyclicBarrier` 并发测试模式
  - 使用反射 (`Field.setAccessible`) 访问 `private final` 字段
  - 测试构造器 (`InMemoryTokenBlacklist(ConcurrentHashMap)`) 注入预填 map 跳过清理线程

## 实现偏差

无偏差。测试与 `detail_v6.md` 完全一致。
