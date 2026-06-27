# 测试审查报告（v8 r1）

## 审查结果
REJECTED

## 发现

- **[严重]** `test_v8.md` — 测试报告声称在 `LoginAttemptTrackerTest.java` 中新增 4 个用例测试 T15 窗口过期防御，但：
  1. 详细设计 `detail_v8.md` 的文件规划表未包含 `LoginAttemptTrackerTest.java`，测试文件变更部分也仅列出 SlidingWindowCounterTest 和 MenuControllerTest，未授权对 LoginAttemptTrackerTest 的任何修改。
  2. 实现报告 `code_v8.md` 的文件变更清单同样未包含 `LoginAttemptTrackerTest.java`。
  这表明测试报告描述的内容与设计、实现不一致，测试报告中声称的用例可能并不存在，导致 T15 行为契约的实际覆盖状态不明。

- **[一般]** `test_v8.md` 第 68 行 — T19 `getCurrentUserId` 的覆盖状态标记为 "✅ 间接验证（tree 端点通过 setUp 测试可用性）"，但 setUp 仅完成控制器构造，若未实际调用 tree 端点则 `getCurrentUserId` 路径未被执行。不过鉴于设计明确声明不测试 tree 端点，此问题不影响本轮审批，但仍应纠正覆盖状态描述。

## 修改要求（仅 REJECTED 时）

1. **LoginAttemptTrackerTest 的 4 个用例问题**：若确实已新增用例，需在 `code_v8.md` 中补充说明，并在 `detail_v8.md` 的文件规划中补充 `LoginAttemptTrackerTest` 条目；若实际未新增，需在 `test_v8.md` 中删除相关描述，并将 T15 行为契约的覆盖状态修正为 "❌ 未覆盖"。
2. **T19 覆盖状态描述**：`getCurrentUserId` 的覆盖状态应如实标注为 "⚠️ 仅构造验证，未执行路径"，或按照设计声明标注为 "设计范围内不测试"。
