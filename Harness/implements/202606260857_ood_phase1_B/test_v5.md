# 测试报告（v5）

## 概述

依据 `detail_v5.md` 详细设计规格（v5 r2）与 `code_v5.md` 实现报告，对 `LoginAttemptTracker` 的 21 个单元测试用例执行回归验证。测试类位于 `AIMedical/backend/modules/common-module/common-module-impl/src/test/java/com/aimedical/modules/commonmodule/auth/login/LoginAttemptTrackerTest.java`，覆盖未知 key、阈值触发、过期惰性清除、状态隔离、null key 错误路径与并发安全 6 个维度。

## 修订说明（v5 r2）

| 审查意见 | 修改措施 |
|---------|---------|
| [一般] 1. 缺失 `clearIp(null)` NPE 测试用例（详细设计错误处理表覆盖 6 个公开方法的 null key 行为，测试列表仅覆盖 5 个） | 接受。新增用例 `shouldThrowNpeWhenClearIpWithNull`（用例 19），参照用例 18 模板实现：`assertThrows(NullPointerException.class, () -> tracker.clearIp(null))`。原并发用例 19/20 顺延为 20/21。同步更新本测试报告的用例明细表与关键行为验证节。 |
| [轻微] 2. 并发测试 `latch.await()` 无超时，存在线程挂起风险 | 接受。引入 `import java.util.concurrent.TimeUnit`，将两处 `latch.await()` 改为 `assertTrue(latch.await(10, TimeUnit.SECONDS))`，避免生产缺陷导致测试线程无限阻塞。 |
| [轻微] 3. 用例 12 步骤 (d) 冗余（断言不依赖 firstFailureTime 重置即可通过） | 不采纳。审查意见明确"可保留（无负面影响）"，且当前实现已在步骤 (d) 之后追加 `readFirstFailureTime` 反射断言（用例 13 同等机制内联于用例 12），精确验证"过期后下次失败重新记录"语义。删除会损失用例自洽性。 |
| [轻微] 4. 并发测试未覆盖 read-write 混合场景（compute 闭包"读+判定+清除"原子性） | 不采纳。属于范围扩展而非缺陷修复；当前用例 20/21 已精确验证 `compute` 闭包串行化的写写原子性，混合读写场景在 6 公开方法行为契约外延，需另起设计任务补充。 |

## 执行环境

| 项 | 值 |
|------|------|
| 项目根 | `C:/Develop/Software/AIMedicalSys` |
| Maven | Apache Maven 3.9.15 |
| JDK | OpenJDK 21.0.11+10-LTS（Microsoft） |
| 编译目标 | Java 17 (`maven.compiler.source/target=17`) |
| Surefire | 3.1.2 |
| 测试框架 | JUnit 5（junit-jupiter） |
| 模块 | `common-module-impl` |
| 测试类 | `com.aimedical.modules.commonmodule.auth.login.LoginAttemptTrackerTest` |
| 命令 | `mvn test -pl modules/common-module/common-module-impl -Dtest=LoginAttemptTrackerTest -DfailIfNoTests=false` |

## 执行结果

**总览**：`Tests run: 21, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.697 s` — **全部通过**。

`BUILD SUCCESS`。

## 用例明细

| # | 测试方法 | 覆盖维度 | 用时 (s) | 结果 |
|---|---------|---------|---------|------|
| 1 | `shouldReturnFalseForUnknownUsername` | 契约 - 未知 key（username） | 0.000 | PASS |
| 2 | `shouldReturnFalseForUnknownIp` | 契约 - 未知 key（IP） | 0.000 | PASS |
| 3 | `shouldNotLockUsernameBelowThreshold` | 正常路径 - 阈值前 | 0.000 | PASS |
| 4 | `shouldLockUsernameWhenThresholdReached` | 锁定触发 - username | 0.000 | PASS |
| 5 | `shouldRemainLockedAfterThreshold` | 锁定持续 | 0.001 | PASS |
| 6 | `shouldClearUsernameAfterSuccess` | 清除逻辑 - username | 0.001 | PASS |
| 7 | `shouldNotLockIpBelowThreshold` | 正常路径 - 阈值前（IP） | 0.000 | PASS |
| 8 | `shouldLockIpWhenThresholdReached` | 锁定触发 - IP | 0.001 | PASS |
| 9 | `shouldClearIpAfterSuccess` | 清除逻辑 - IP | 0.001 | PASS |
| 10 | `shouldMaintainIndependentUsernames` | 状态交互 - username | 0.000 | PASS |
| 11 | `shouldMaintainIndependentIps` | 状态交互 - IP | 0.001 | PASS |
| 12 | `shouldUnlockAfterLockDurationExpiry` | 过期重置（短窗口） | 0.301 | PASS |
| 13 | `shouldResetFirstFailureTimeAfterExpiry` | 过期后再失败重置 | 0.312 | PASS |
| 14 | `shouldThrowNpeWhenRecordUsernameFailureWithNull` | 错误路径 - NPE | 0.022 | PASS |
| 15 | `shouldThrowNpeWhenRecordIpFailureWithNull` | 错误路径 - NPE | 0.000 | PASS |
| 16 | `shouldThrowNpeWhenIsUsernameLockedWithNull` | 错误路径 - NPE | 0.000 | PASS |
| 17 | `shouldThrowNpeWhenIsIpLockedWithNull` | 错误路径 - NPE | 0.001 | PASS |
| 18 | `shouldThrowNpeWhenClearUsernameWithNull` | 错误路径 - NPE | 0.000 | PASS |
| 19 | `shouldThrowNpeWhenClearIpWithNull` | 错误路径 - NPE（v5 r2 新增） | 0.000 | PASS |
| 20 | `shouldHandleConcurrentRecordUsernameFailure` | 并发安全 - username | 0.008 | PASS |
| 21 | `shouldHandleConcurrentRecordIpFailure` | 并发安全 - IP | 0.003 | PASS |

## 关键行为验证

- **未知 key 行为**（用例 1/2）：`isXxxLocked` 返回 false 且通过反射断言 `readMapSize == 0`，确认未触发 entry 创建。
- **阈值触发**（用例 4/8）：第 5 次 username 失败 / 第 20 次 IP 失败后 `isXxxLocked` 返回 true，验证 `failures >= threshold` 判定。
- **过期惰性清除**（用例 12）：100ms 短窗口下，连续 5 次失败触发锁定 → `Thread.sleep(300)` → 再次查询返回 false（关键路径：failures=5 仍 >= 阈值，必须靠过期分支才能解锁，证明 compute 闭包返回 null 触发了 `ConcurrentHashMap` 自动移除）。
- **过期后首败时间重置**（用例 12/13）：`isUsernameLocked` 在过期分支移除 entry 后，下次 `recordUsernameFailure` 走 `prev == null` 分支创建新 `AttemptRecord(1, now)`，反射断言新 `firstFailureTime > originalFirstFailureTime`。
- **状态隔离**（用例 10/11）：不同 username / IP 互不影响。
- **null key 一致性**（用例 14-19）：6 个公开方法（`recordUsernameFailure` / `recordIpFailure` / `isUsernameLocked` / `isIpLocked` / `clearUsername` / `clearIp`）全部覆盖，验证 `ConcurrentHashMap` 对 null key 一致抛 NPE 的契约。v5 r1 遗漏的 `clearIp(null)` 已在用例 19 补齐。
- **并发安全**（用例 20/21）：10 线程 × 100 次/线程并发 `recordXxxFailure`，主线程 `assertTrue(latch.await(10, TimeUnit.SECONDS))` 等待线程收敛（v5 r2 引入超时保护，防止生产缺陷导致线程挂起），反射读取 `failures()` 精确断言 `== 1000`，验证 `ConcurrentHashMap.compute` 闭包串行化的原子性。

## 编译警告

测试编译阶段产生 1 条 javac 警告：

```
LoginAttemptTrackerTest.java: ʹ����δ�����Ĳ�����
```

`LoginAttemptTrackerTest.java: 使用了未经检查或不安全的操作`

JDK 21 编译器对反射调用 `getMethod("failures")` / `getMethod("firstFailureTime")` 产生的原始类型未检查警告，不影响测试正确性，与 `InMemoryRateLimitGuardTest` 等既有测试的反射写法一致（项目惯例保持）。

## 结论

21 个测试用例全部通过。测试覆盖了 `LoginAttemptTracker` 详细设计规格 v5 r2 中声明的全部行为契约（正常路径、边界条件、错误路径、状态交互、并发安全），其中 v5 r1 审查遗漏的 `clearIp(null)` NPE 路径已在用例 19 补齐，6 个公开方法的 null key 契约实现完整可回归验证；并发测试已引入 10s 超时保护以避免生产缺陷导致的测试挂起。代码与设计一致，可进入下一阶段。