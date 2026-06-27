# 计划审查报告（v5 r2）

## 审查结果
APPROVED

## 审查依据

本次为独立审查，不参考上一轮（plan_review_v5_r1）的结论。审查对象：v5 r1 修订后的 `plan.md`（仅含 R5 NEW Stage 2 基础 — LoginAttemptTracker）和 `task_v5.md`。

## v5 r1 修订的修复验证

`task_v5.md` 末尾修订说明声称已修复三项反馈。独立验证：

| 修订项 | 修复状态 | 验证依据 |
|--------|---------|---------|
| 收敛过期重置测试设计为方案 A | 已修复 | `task_v5.md:22-23` 明确 package-private 测试构造器 `LoginAttemptTracker(int, long, int, long)`；`task_v5.md:61-65` 明确选用方案 A 并列出 B/C/D 的不选择理由 |
| AttemptRecord 形态明确为 Java 21 record | 已修复 | `task_v5.md:18` 明确 `private static record AttemptRecord(int failures, long firstFailureTime)`；`plan.md:50` 一致描述 |
| 新增未知 key 的 isLocked 用例 | 已修复 | `task_v5.md:45-46` 新增 `shouldReturnFalseForUnknownUsername` / `shouldReturnFalseForUnknownIp`；`task_v5.md:35` 同步明确 `isXxxLocked(unknownKey)` 用 `map.get(key)` 读取的契约 |

修订完整、无遗漏、无新引入的矛盾。

## 新发现

### [轻微] 1. `isXxxLocked` 实现描述的两种模式未明确合并

`task_v5.md:33` 描述"`isXxxLocked` 在闭包内同时完成'读取 + 过期检查 + 惰性清除（过期时返回 null 等同 remove）'"——要求使用 `compute`。

`task_v5.md:35` 又描述"`isXxxLocked(unknownKey)` 通过 `map.get(key)` 直接读取（不在 compute 闭包内触发 entry 创建）"——要求使用 `get`。

`compute` 和 `get` 不能在同一方法体中同时作为主要读取路径，否则无法同时满足"闭包内清除"和"不创建 entry"两个要求。唯一能同时满足两者的实现是"先 `get` 检查存在性、不存在直接返回 false；存在则进入 `compute` 闭包检查过期并惰性清除"的混合模式，但任务文档未明确给出该模式。

有 Java 经验的实施者可由两段描述推出正确实现，但当前文档存在局部模糊性。建议在"内部类型 / 行为契约"小节补一句：`isXxxLocked` 内部先 `get` 短路未知 key，命中已知 entry 才进入 `compute` 闭包完成过期检查与惰性清除。

### [轻微] 2. `shouldUnlockAfterLockDurationExpiry` 测试语义不完整

`task_v5.md:56` 描述："通过 package-private 测试构造器创建短窗口（如 100ms），等待窗口到期后 `isXxxLocked` 返回 false，验证惰性清除生效"。

仅按此描述的字面执行（如仅调用 1 次 `recordUsernameFailure`），`failures=1` 远小于 `USERNAME_THRESHOLD=5`，无论是否过期 `isUsernameLocked` 永远返回 false，测试失去验证意义。

该用例的本意应是"从锁定到未锁定"的转换验证。建议在测试说明中补充关键步骤：先调用阈值次数的 `recordUsernameFailure` 触发锁定（`isUsernameLocked` 返回 true），`Thread.sleep` 等待窗口到期，再调用 `isUsernameLocked` 验证返回 false（验证惰性清除生效）。

### [轻微] 3. 并发测试断言方式未明确访问路径

`task_v5.md:68` 描述："最终断言通过 `recordUsernameFailure` 总调用次数 = ConcurrentHashMap 中 entry 的 failures 值"。

`failures` 是 `private static record AttemptRecord` 的字段，外部测试类无法直接读取。可能方案：
- (a) 测试类与生产类同包，直接通过 package-private 访问 `attempts` 字段并 `computeIfPresent` 间接读取 failures
- (b) 增加 package-private 辅助方法（如 `int currentFailures(String key)` 仅供测试）
- (c) 间接断言：调用 `threadCount` 次 `recordUsernameFailure` 后断言 `isUsernameLocked(username) == true`（仅在 `failures >= threshold` 时成立，但要求 `threadCount >= threshold`）

任务文档未指定具体访问路径。建议在测试策略小节明确选择其一（方案 (c) 最不依赖生产类 API 扩展）。

## 总体评估

v5 r1 修订完整解决了上一轮反馈的 3 项问题，未引入新的严重或一般缺陷。本轮新发现的 3 项均为文档描述的局部模糊，不影响计划的正确性和可实施性：

- 不会改变生产类的最终 API 形状
- 不会破坏 v5 r1 已明确的 Java 21 record 形态
- 不会影响 package-private 测试构造器方案 A 的可行性
- 实施者在编码阶段可独立解决（具备 Java 并发编程经验即可推断正确实现）

按通过/驳回标准（无严重、无一般 → APPROVED），本次审查判定为通过。

## 修改要求

无（APPROVED 不需要修改要求）。上述 3 项 [轻微] 发现供实施阶段参考，不构成驳回依据。
