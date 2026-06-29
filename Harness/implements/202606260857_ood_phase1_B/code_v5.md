# 实现报告（v5）

## 概述

实现了 `LoginAttemptTracker` 工具类与对应单元测试 `LoginAttemptTrackerTest`，均位于 `com.aimedical.modules.commonmodule.auth.login` 包下。生产类封装 username / IP 双维度的失败计数与账户锁定状态管理；测试类提供 20 个用例覆盖未知 key、阈值触发、过期惰性清除、状态隔离、null key 错误路径与并发安全。

## 文件变更清单

| 操作 | 文件路径 | 说明 |
|------|---------|------|
| 新建 | `AIMedical/backend/modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/auth/login/LoginAttemptTracker.java` | 实现 LoginAttemptTracker 工具类（含 private static record AttemptRecord） |
| 新建 | `AIMedical/backend/modules/common-module/common-module-impl/src/test/java/com/aimedical/modules/commonmodule/auth/login/LoginAttemptTrackerTest.java` | 实现 20 用例的单元测试，含反射 helper 与并发测试 |

## 编译验证

未执行编译验证（项目无统一编译脚本，需通过 IDE 或 Maven 单独触发 `common-module-impl` 模块编译）。

## 设计偏差说明

无偏差。代码严格遵循详细设计 v5 r2：
- 类型形态（public class）、字段可见性（private final）、内部 record 形态（private static record AttemptRecord）完全一致
- 无参构造器委托到全参构造器并传入 4 个默认常量；package-private 测试构造器签名一致
- `recordXxxFailure` / `isXxxLocked` / `clearXxx` 方法签名与行为契约一致
- 惰性过期逻辑：在 `isXxxLocked` 中先 `map.get` 判 null 避免触发 entry 创建；进入 compute 闭包后基于 `now - firstFailureTime >= lockDurationMs` 判定，过期则闭包返回 null 触发 ConcurrentHashMap 自动移除
- null key 一致抛 NPE 自然传播，未引入显式 null 守卫
- 测试 20 用例（含反射 helper 4 个）全部按设计规格实现

## 修订说明（v5 r2）

| 审查意见 | 修改措施 |
|---------|---------|
| [严重] `LoginAttemptTrackerTest.java:141-152` `shouldResetFirstFailureTimeAfterExpiry` 在 `Thread.sleep(300)` 后直接 `recordUsernameFailure(key)`，未触发惰性过期，导致 entry 仍在且 `firstFailureTime` 不变，断言失败 | 接受。在 `Thread.sleep(300)` 之后、`recordUsernameFailure(key)` 之前插入 `assertFalse(tracker.isUsernameLocked(key));`，触发 `isUsernameLocked` 的 compute 闭包检测到过期并返回 null 移除 entry，使下一次 `recordUsernameFailure` 走 `prev == null` 分支创建新 entry，从而正确验证"过期后 firstFailureTime 被重置"语义 |
| [轻微] 用例 12 步骤 (d) 设计 WHY 解释（"failures = 6 >= 5"）与实际机制（`isUsernameLocked` 惰性清除后第 6 次失败通过 `prev == null` 创建新 entry，`failures = 1`）不符 | 不修改代码。该问题为设计侧描述与实际机制不符（审查意见明确指出"测试代码本身正确，建议设计侧修订"），生产代码严格遵循"首次失败时间戳不变"语义，测试断言亦正确反映过期后重新计数行为；该解释属于 `detail_v5.md` 范畴，超出本任务代码修订职责 |
| [轻微] `LoginAttemptTracker.java:57, 79` 使用 `boolean[1] locked` 数组从 compute 闭包传出结果，风格稍显绕，建议改用 `AtomicBoolean` | 不修改。`SlidingWindowCounter.tryAcquire` 同样使用 `boolean[] result = new boolean[1]` 模式（同模块 `rateLimit` 子包已存在该惯例），为保持项目内 ConcurrentHashMap compute 闭包传出布尔值的代码风格一致性，保留 `boolean[1]` 写法 |
