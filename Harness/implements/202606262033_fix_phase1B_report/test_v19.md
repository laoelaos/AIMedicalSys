# 测试报告（v19）

## 验证结果

`AuthServiceTest.java` 中 `login_shouldThrowIpLocked()` 和 `login_shouldThrowUsernameLocked()` 已包含 `ex.getArgs()` 断言：

| 测试方法 | 行号 | 断言 | 状态 |
|---------|------|------|------|
| `login_shouldThrowIpLocked()` | L139 | `assertEquals("30分钟", ex.getArgs()[0])` | 已就位 |
| `login_shouldThrowUsernameLocked()` | L159→L160 | `assertEquals("15分钟", ex.getArgs()[0])` | 已就位 |

## 与详细设计一致性

- 断言位置：audit 断言块后、方法结束花括号前 — 一致
- IP 锁定参数 `"30分钟"` — 一致
- 用户名锁定参数 `"15分钟"` — 一致
- 不新增测试方法 — 一致
- 不新增 import — 一致

## 结论

测试已按 detail_v19 要求完成，无偏差。
