# 测试审查报告（v5 r1）

## 审查结果
APPROVED

## 发现
- **[轻微]** `test_v5.md` 文档计数偏差 — SessionStore 正常路径实际 11 个测试方法，报告标注为 10 个；实际测试总数 27 个，报告标注 26 个。不影响测试有效性，文档需同步修正。
- **[轻微]** `shouldHandleConcurrentPutsAndGets:189-193` — 子线程中使用 `assertNotNull`，JUnit 在子线程中抛出的 AssertionError 不会传播到主测试线程，若内层断言失败将静默丢失。建议改用 `AtomicBoolean` 收集断言结果或使用并发测试工具类（如 `ConcurrentUnit`）。当前因 put/get 在同一线程且 ConcurrentHashMap 保证 happens-before，实际不会触发该路径，不影响测试正确性。

## 修改要求（仅 REJECTED 时）
（无）
