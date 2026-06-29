# 实现报告（v8）

## 概述

修复 Phase1B 报告中的 3 个 P1 问题：T13（SlidingWindowCounter 锁粒度调整）、T15（LoginAttemptTracker record* 窗口过期防御）、T19（MenuController 注入 CurrentUser）。共修改 3 个源文件及 2 个测试文件。

## 文件变更清单

| 操作 | 文件路径 | 说明 |
|------|---------|------|
| MODIFY | `common-module-impl/src/main/java/.../auth/rateLimit/SlidingWindowCounter.java` | T13：移除 ReentrantLock，依赖 ConcurrentHashMap.compute 原子性 |
| MODIFY | `common-module-impl/src/test/java/.../auth/rateLimit/SlidingWindowCounterTest.java` | 移除 lock 反射测试用例及相关 import |
| MODIFY | `common-module-impl/src/main/java/.../auth/login/LoginAttemptTracker.java` | T15：recordUsernameFailure/recordIpFailure 增加窗口过期重置检查 |
| MODIFY | `common-module-impl/src/main/java/.../controller/MenuController.java` | T19：构造器注入 CurrentUser，替换 SecurityContextHolder 获取 |
| MODIFY | `common-module-impl/src/test/java/.../auth/login/LoginAttemptTrackerTest.java` | 新增 T15 窗口过期防御的 4 个行为契约用例 |
| MODIFY | `common-module-impl/src/test/java/.../controller/MenuControllerTest.java` | 新增 CurrentUser mock 注入及 import |

## 编译验证

mvn compile + mvn test-compile 通过，无错误。

## 设计偏差说明

无偏差，所有实现与详细设计规格保持一致。
