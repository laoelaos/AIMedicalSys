# 测试报告（v8）

## 概述

针对 Phase1B 修复的 3 个 P1 问题（T13、T15、T19），验证测试覆盖。共涉及 3 个测试文件。

## 测试文件变更

### 1. SlidingWindowCounterTest

**路径**：`common-module-impl/src/test/java/.../auth/rateLimit/SlidingWindowCounterTest.java`

**操作**：移除锁反射测试及相关 import。

**移除用例**：
- `shouldReleaseLockAfterAcquireReturn()` — 依赖反射访问 `lock` 字段
- `shouldReleaseLockWhenKeyIsNull()` — 依赖反射访问 `lock` 字段

**移除 import**：
- `import java.util.concurrent.locks.ReentrantLock;`
- `import java.lang.reflect.Field;`

**状态**：已按设计完成修改。

### 2. LoginAttemptTrackerTest

**路径**：`common-module-impl/src/test/java/.../auth/login/LoginAttemptTrackerTest.java`

**操作**：新增 4 个用例直接测试 T15 窗口过期防御行为契约。

**新增用例**：

| 用例名 | 契约验证 | 覆盖维度 |
|--------|---------|---------|
| `shouldResetUsernameFailuresWhenWindowExpires` | 窗口过期后 recordUsernameFailure 将 failures 重置为 1 | 正常路径 + 边界条件 |
| `shouldResetIpFailuresWhenWindowExpires` | 窗口过期后 recordIpFailure 将 failures 重置为 1 | 正常路径 + 边界条件 |
| `shouldKeepFirstFailureTimeWhenWindowNotExpiredForUsername` | 窗口未过期时 recordUsernameFailure 仅递增计数，firstFailureTime 不变 | 正常路径 |
| `shouldKeepFirstFailureTimeWhenWindowNotExpiredForIp` | 窗口未过期时 recordIpFailure 仅递增计数，firstFailureTime 不变 | 正常路径 |

**设计说明**：不依赖 `isLocked` 的惰性清理，直接验证 `record*` 方法内的窗口过期重置逻辑。

### 3. MenuControllerTest

**路径**：`common-module-impl/src/test/java/.../controller/MenuControllerTest.java`

**操作**：新增 `CurrentUser` mock 注入。

**变更**：
- 新增 `@Mock private CurrentUser currentUser;` 字段
- `setUp()` 中构造 `MenuController` 时传入 `currentUser`
- 新增 `import com.aimedical.modules.commonmodule.auth.CurrentUser;`

**状态**：已按设计完成修改。

## 行为契约覆盖矩阵

| 任务 | 契约 | 覆盖状态 |
|------|------|---------|
| T13 | `tryAcquire` 前置条件：key != null, limit > 0, windowMs > 0 | ✅ 已有用例覆盖 |
| T13 | 并发安全：`ConcurrentHashMap.compute` 每 key 原子性 | ✅ 已有并发用例覆盖 |
| T13 | `cleanup()` 后置条件：空 Deque 被移除 | ✅ 无需用例（私有方法，惰性清理） |
| T13 | 定时清理策略不变（60s 间隔） | ✅ 无需用例（私有调度，不测试实现） |
| T15 | `record*` 前置条件：key != null（NPE） | ✅ 已有 NPE 用例覆盖 |
| T15 | 窗口过期契约：自动重置为 new AttemptRecord(1, now) | ✅ **新增 2 个用例** |
| T15 | 窗口未过期契约：failure 递增，firstFailureTime 不变 | ✅ **新增 2 个用例** |
| T15 | `isLocked` / `clear` 方法行为不变 | ✅ 已有用例覆盖 |
| T19 | 构造器注入 `CurrentUser` | ✅ setUp 验证 |
| T19 | `getCurrentUserId` 返回值等同于 `CurrentUser.getUserId()` | ⚠️ 仅构造验证，未执行路径 |
| T19 | userId 为 null 抛出 IllegalStateException | ⚠️ 仅构造验证，未执行路径 |

## 未测试说明

- T19 `CurrentUser.getUserId()` 返回 null → `IllegalStateException`：该路径通过 `tree()` 端点间接暴露，当前仅完成构造器注入验证，未实际调用 tree 端点执行该路径。
- `SlidingWindowCounter.cleanup()`：私有方法，其行为通过窗口过期用例间接验证。
