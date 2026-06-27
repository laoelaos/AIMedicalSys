# 详细设计（v8）

## 概述

修复 Phase1B 报告中的 3 个 P1 问题：T13（SlidingWindowCounter 锁粒度调整）、T15（LoginAttemptTracker record* 窗口过期防御）、T19（MenuController 注入 CurrentUser）。修改 3 个独立源文件及对应的测试文件，无交叉依赖。

## 文件规划

| 文件路径 | 操作 | 职责 |
|---------|------|------|
| `modules/common-module/common-module-impl/src/main/java/.../auth/rateLimit/SlidingWindowCounter.java` | MODIFY | T13：移除全局锁，依赖 ConcurrentHashMap.compute 原子性 |
| `modules/common-module/common-module-impl/src/test/java/.../auth/rateLimit/SlidingWindowCounterTest.java` | MODIFY | 移除 lock 反射测试用例，清理 ReentrantLock import |
| `modules/common-module/common-module-impl/src/main/java/.../auth/login/LoginAttemptTracker.java` | MODIFY | T15：record* 方法内增加窗口过期检查与重置 |
| `modules/common-module/common-module-impl/src/main/java/.../controller/MenuController.java` | MODIFY | T19：构造器注入 CurrentUser，替换 SecurityContextHolder |
| `modules/common-module/common-module-impl/src/test/java/.../auth/login/LoginAttemptTrackerTest.java` | MODIFY | 新增 T15 窗口过期防御的 4 个行为契约用例 |
| `modules/common-module/common-module-impl/src/test/java/.../controller/MenuControllerTest.java` | MODIFY | 新增 CurrentUser mock 注入 |

## 类型定义

### T13: SlidingWindowCounter

**形态**：class（不变）
**包路径**：`com.aimedical.modules.commonmodule.auth.rateLimit`
**职责**：基于滑动窗口的速率限制，每 key 独立计数（不变）

**字段变更**：
| 字段 | 原 | 新 |
|------|----|----|
| `windows` | `ConcurrentHashMap<String, Deque<Long>>` | 不变 |
| `lock` | `ReentrantLock lock` | **移除** |
| `cleanupExecutor` | `ScheduledExecutorService` | 不变 |

**构造方式**：不变（`new SlidingWindowCounter()`），移除 `this.lock = new ReentrantLock()`

**公开接口**：
- `boolean tryAcquire(String key, int limit, long windowMs)` — 移除 `lock.lock()`/`lock.unlock()` 包裹，直接依赖 `windows.compute(key, ...)` 的原子性；`result[0]` 布尔数组保持以备 compute 闭包内赋值；方法签名不变
- `private void cleanup()` — 改为 `windows.forEach((k, v) -> { if (v.isEmpty()) windows.remove(k, v); })`，不再使用全局锁

**import 变更**：
- 移除 `import java.util.concurrent.locks.ReentrantLock;`

### T15: LoginAttemptTracker record* 窗口过期防御

**形态**：class（不变）
**包路径**：`com.aimedical.modules.commonmodule.auth.login`
**职责**：记录登录失败次数并判定锁定状态（不变）

**recordUsernameFailure(String username)** — compute 闭包变更：
```
prev == null → new AttemptRecord(1, now)
prev != null && (now - prev.firstFailureTime()) >= usernameLockDurationMs → new AttemptRecord(1, now)   // 窗口过期，重置
prev != null && (now - prev.firstFailureTime()) < usernameLockDurationMs → new AttemptRecord(prev.failures() + 1, prev.firstFailureTime())  // 窗口内，累加
```

**recordIpFailure(String ip)** — 同理，时长阈值用 `ipLockDurationMs`

**isUsernameLocked / isIpLocked** — 保持不变（已有惰性清除）

### T19: MenuController 注入 CurrentUser

**形态**：class（不变）
**包路径**：`com.aimedical.modules.commonmodule.controller`
**职责**：菜单 REST API 控制器（不变）

**构造器变更**：
- 原：`public MenuController(MenuService menuService)`
- 新：`public MenuController(MenuService menuService, CurrentUser currentUser)`
- 新增字段：`private final CurrentUser currentUser;`

**getCurrentUserId() 方法** — 保留 `private Long getCurrentUserId()` 签名，方法体改为：
```java
private Long getCurrentUserId() {
    Long userId = currentUser.getUserId();
    if (userId == null) {
        throw new IllegalStateException("无法从SecurityContext获取用户ID");
    }
    return userId;
}
```

**import 变更**：
- 移除 `import org.springframework.security.core.Authentication;`
- 移除 `import org.springframework.security.core.context.SecurityContextHolder;`
- 新增 `import com.aimedical.modules.commonmodule.auth.CurrentUser;`

## 测试文件变更

### SlidingWindowCounterTest

**移除用例**：
- `shouldReleaseLockAfterAcquireReturn()` — 依赖反射访问 `lock` 字段
- `shouldReleaseLockWhenKeyIsNull()` — 依赖反射访问 `lock` 字段

**移除 import**：
- `import java.util.concurrent.locks.ReentrantLock;`
- `import java.lang.reflect.Field;`

### LoginAttemptTrackerTest

**新增用例**（T15 窗口过期防御行为契约）：

| 用例名 | 契约验证 | 覆盖维度 |
|--------|---------|---------|
| `shouldResetUsernameFailuresWhenWindowExpires` | 窗口过期后 recordUsernameFailure 将 failures 重置为 1 | 正常路径 + 边界条件 |
| `shouldResetIpFailuresWhenWindowExpires` | 窗口过期后 recordIpFailure 将 failures 重置为 1 | 正常路径 + 边界条件 |
| `shouldKeepFirstFailureTimeWhenWindowNotExpiredForUsername` | 窗口未过期时 recordUsernameFailure 仅递增计数，firstFailureTime 不变 | 正常路径 |
| `shouldKeepFirstFailureTimeWhenWindowNotExpiredForIp` | 窗口未过期时 recordIpFailure 仅递增计数，firstFailureTime 不变 | 正常路径 |

### MenuControllerTest

- `setUp()` 中构造 `MenuController` 时新增 `CurrentUser` mock 参数
- 新增 `@Mock private CurrentUser currentUser;` 字段
- 新增 import: `import com.aimedical.modules.commonmodule.auth.CurrentUser;`
- `tree()` 方法不再测试（当前 MenuControllerTest 未覆盖 `/tree` 端点，无需修改）

## 错误处理

| 任务 | 错误场景 | 处理方式 |
|------|---------|---------|
| T13 | key 为 null | 不变（NPE 由 ConcurrentHashMap.compute 抛出） |
| T15 | username/ip 为 null | 不变（NPE 由 ConcurrentHashMap.compute 抛出） |
| T19 | currentUser.getUserId() 返回 null | 抛出 IllegalStateException（与原行为一致） |

## 行为契约

### T13
- `tryAcquire` 前置条件：`key != null`，`limit > 0 && windowMs > 0`（不变）
- 并发安全：`ConcurrentHashMap.compute` 保证每 key 原子性，无全局锁竞争
- `cleanup()` 后置条件：所有空 Deque 被移除，非空 Deque 不变
- 定时清理策略不变（60s 间隔）

### T15
- `recordUsernameFailure` / `recordIpFailure` 前置条件：`key != null`
- 窗口过期契约：若 `now - firstFailureTime >= lockDuration`，自动重置为新窗口（failure=1, firstFailureTime=now）
- 窗口未过期契约：failure 递增，firstFailureTime 不变
- `isLocked` / `clear` 方法行为不变

### T19
- 构造器注入：`CurrentUser` 通过 Spring 容器注入
- `getCurrentUserId` 返回值等同于 `CurrentUser.getUserId()`，但将 null 转为 IllegalStateException
- tree 端点行为不变（通过 getCurrentUserId 间接依赖 CurrentUser）

## 依赖关系

| 依赖 | 说明 |
|------|------|
| `CurrentUser` 接口 (`common-module-api`) | T19 构造器注入 |
| `ConcurrentHashMap` (JDK) | T13 原子性保障 |
| `ScheduledExecutorService` (JDK) | T13 定时清理，不变 |
