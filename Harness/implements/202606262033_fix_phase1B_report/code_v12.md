# 实现报告（v12）

## 概述

修改 `SlidingWindowCounter.java` 中 `tryAcquire()` 方法，使用 `lock.lock()` / `lock.unlock()` 包裹 `windows.compute()` 调用，使锁策略与 OOD 4.1 节设计约定一致。

## 文件变更清单

| 操作 | 文件路径 | 说明 |
|------|---------|------|
| 修改 | `AIMedical/backend/modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/auth/rateLimit/SlidingWindowCounter.java` | `tryAcquire()` 内添加 `lock.lock()` / `try-finally-lock.unlock()` 包裹 `windows.compute()` |

## 编译验证

`mvn test -Dtest=SlidingWindowCounterTest` 通过：Tests run: 11, Failures: 0, Errors: 0, Skipped: 0

## 设计偏差说明

无偏差。
