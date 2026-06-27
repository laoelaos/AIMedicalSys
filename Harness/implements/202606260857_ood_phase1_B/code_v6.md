# 实现报告（v6）

## 概述

实现了 Stage 2 登出 No-Op 修复的核心基础设施：`TokenBlacklist` 接口、`InMemoryTokenBlacklist` 内存实现及其单元测试。共创建 3 个文件，位于 `auth/blacklist/` 子包下。

## 文件变更清单

| 操作 | 文件路径 | 说明 |
|------|---------|------|
| 新建 | `modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/auth/blacklist/TokenBlacklist.java` | Token 黑名单查询接口 |
| 新建 | `modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/auth/blacklist/InMemoryTokenBlacklist.java` | 内存黑名单实现（Phase 1，仅 Access Token） |
| 新建 | `modules/common-module/common-module-impl/src/test/java/com/aimedical/modules/commonmodule/auth/blacklist/InMemoryTokenBlacklistTest.java` | InMemoryTokenBlacklist 单元测试（12 用例） |

## 编译验证

`mvn compile -q` 和 `mvn test -q` 均通过，所有 12 个测试用例全部绿色。

## 设计偏差说明

无偏差。实现与 `detail_v6.md` 完全一致：

- `TokenBlacklist` 接口：`add(String, long)` / `isBlacklisted(String)`，与设计一致
- `InMemoryTokenBlacklist`：public 构造器初始化 ConcurrentHashMap 并启动 ScheduledExecutorService daemon 线程，package-private 测试构造器接受预填 map 不启动清理线程；`cleanup()` 为 package-private；null key 自然传播 NPE
- 测试 12 用例覆盖正常路径、过期清理、错误路径、并发安全、幂等性、清理保留
