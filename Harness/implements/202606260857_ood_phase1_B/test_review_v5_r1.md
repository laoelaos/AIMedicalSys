# 测试审查报告（v5 r1）

## 审查结果
REJECTED

## 发现

- **[一般]** `LoginAttemptTrackerTest.java` — 缺少 `clearIp(null)` NPE 测试用例
  - 详细设计 v5 r2 错误处理表（`detail_v5.md:110-115`）明确列出 6 个公开方法对 null key 一致抛 NPE（`recordUsernameFailure` / `recordIpFailure` / `isUsernameLocked` / `isIpLocked` / `clearUsername` / `clearIp`），但测试列表（用例 14-18）仅覆盖 5 个公开方法，遗漏 `clearIp(null)`。测试报告 `test_v5.md:59` 已显式承认此遗漏。设计 v5 r2 修订说明（`detail_v5.md:331`）原意"覆盖被遗漏的 null 路径"，但实际仅新增 3 个用例（`isUsernameLocked` / `isIpLocked` / `clearUsername`），漏掉 `clearIp`。
  - 期望修正方向：新增测试用例 `shouldThrowNpeWhenClearIpWithNull`，参照用例 18 实现；同步将原用例 19/20 编号顺延为 20/21。

- **[轻微]** `LoginAttemptTrackerTest.java:134-135` — 用例 12 步骤 (d) 冗余（断言与设计 WHY 注释均不真正验证 firstFailureTime 重置语义）
  - 该步骤断言 `recordUsernameFailure(key)` 后 `isUsernameLocked == false`，但因前一步 `isUsernameLocked` 已惰性移除过期 entry（`detail_v5.md:236` 步骤 (c) 路径），新 `recordUsernameFailure` 走 `prev == null` 分支创建 `AttemptRecord(1, now)`，`failures=1 < 5`，断言不依赖 firstFailureTime 是否被重置即可通过。"过期后 firstFailureTime 被重置"语义实际仅由用例 13 的 `assertTrue(newFirstFailureTime > originalFirstFailureTime)` 精确验证。设计步骤 (d) WHY 注释（"failures = 6 >= 5"）与实际机制也不符（实现报告 `code_v5.md:33` 已确认此问题为设计侧描述错误）。
  - 期望修正方向：可保留（无负面影响）；建议在断言前增加 firstFailureTime 反射断言，或直接删除步骤 (d) 以精简测试。

- **[轻微]** `LoginAttemptTrackerTest.java:202, 223` — 并发测试 `latch.await()` 无超时
  - `latch.await()` 无限阻塞；若生产代码因缺陷导致线程挂起（如 compute 闭包死锁），测试将一直挂起而非明确失败。
  - 期望修正方向：改为 `assertTrue(latch.await(10, TimeUnit.SECONDS))` 以提高测试可靠性，并补充 `import java.util.concurrent.TimeUnit`。

- **[轻微]** `LoginAttemptTrackerTest.java:186-225` — 并发测试仅覆盖 write-write 原子性
  - 设计 v5 r2 "并发场景"小节（`detail_v5.md:158-159`）声明"compute 闭包保证'读+判定+清除'的原子性，不会出现读到的 failures 与实际状态不一致"，但用例 19/20 仅覆盖 10 线程并发 `recordXxxFailure`（write-write），未覆盖并发 `isXxxLocked` 与 `recordXxxFailure` 混合场景（即 compute 闭包"读+判定+清除"原子性）。该 gap 源于设计测试列表本身的不足，测试代码严格遵循测试列表。
  - 期望修正方向：可补充混合读写并发测试用例，但属于范围扩展而非缺陷修复。

## 修改要求

### [一般] 1. 新增 `clearIp(null)` NPE 测试用例

- **文件**：`AIMedical/backend/modules/common-module/common-module-impl/src/test/java/com/aimedical/modules/commonmodule/auth/login/LoginAttemptTrackerTest.java`
- **位置**：用例 18（`shouldThrowNpeWhenClearUsernameWithNull`，第 179-183 行）之后
- **问题**：详细设计 v5 r2 错误处理表覆盖 6 个公开方法的 null key 行为（`recordUsernameFailure` / `recordIpFailure` / `isUsernameLocked` / `isIpLocked` / `clearUsername` / `clearIp`），但测试列表仅覆盖 5 个；遗漏 `clearIp(null)`。生产代码 `clearIp`（`LoginAttemptTracker.java:100-102`）直接调用 `ipAttempts.remove(ip)`，与 `clearUsername` 行为镜像，NPE 路径需被显式测试以保证契约可回归验证。
- **为什么是问题**：设计契约完整性要求测试覆盖所有声明的 NPE 路径；遗漏 `clearIp(null)` 将无法在重构时捕获该路径的行为回归（如未来有人为 `clearIp` 添加 null guard 静默吞掉 null 而破坏一致性）。当前测试报告虽承认遗漏，但承认不等于可接受。
- **期望修正方向**：新增测试用例 `shouldThrowNpeWhenClearIpWithNull`，参照用例 18 模板实现：

  ```java
  @Test
  void shouldThrowNpeWhenClearIpWithNull() {
      LoginAttemptTracker tracker = new LoginAttemptTracker();
      assertThrows(NullPointerException.class, () -> tracker.clearIp(null));
  }
  ```

  同步更新原用例 19/20 编号顺延为 20/21（仅在测试报告 `test_v5.md` 的用例明细表中调整编号，测试方法名不变）。