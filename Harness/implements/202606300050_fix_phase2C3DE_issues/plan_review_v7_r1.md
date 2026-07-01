# 计划审查报告（v7 r1）

## 审查结果
REJECTED

## 发现

- **[严重]** 接口返回类型变更导致 2 个测试文件编译失败，但任务文件和计划中均未提及测试文件更新。

  R7 将 `TriageRuleEngine.match()` 的返回类型从 `List<RecommendedDepartment>` 改为 `MatchResult`，这是一个破坏性接口变更。以下测试代码会因此报编译错误：

  1. `DefaultTriageRuleEngineTest.java`（全文件）—— 第 26、37、49、61 行调用 `engine.match(...)` 并将返回值赋给 `List<RecommendedDepartment>` 类型变量，接口变更后返回值变为 `MatchResult`，所有测试断言均需通过 `matchResult.getDepartments()` 调整。

  2. `TriageServiceImplTest.java:755-772` —— `StubTriageRuleEngine` 实现了 `TriageRuleEngine` 接口，其 `match()` 方法签名当前返回 `List<RecommendedDepartment>`，接口变更后编译不通过。

  当前 task_v7.md 的文件清单和 plan.md R7 描述均未提及上述两个测试文件，实现后测试阶段将编译中断，且无预设的测试对齐方案。

- **[一般]** `DefaultTriageRuleEngine` 中 JSON 解析使用 Jackson `ObjectMapper` 实例未明确复用策略。

  每调用一次 `match()` 就 `new ObjectMapper()` 会导致性能浪费和类加载开销。应复用共享实例（如 Spring 注入或静态常量），且需保证线程安全。计划中应明确此约束。

- **[轻微]** `MatchResult` 类所在包 `dto` 语义不精确。

  `MatchResult` 是引擎内部返回值，非 API DTO。放在 `dto` 包虽可工作，但更合理的归属是 `rule` 包（与 `TriageRuleEngine` 同包）或引擎专属子包。虽不影响正确性，值得改进。

- **[轻微]** R7 完成后 `TriageServiceImpl.java:126` 调用点被修改为使用 `MatchResult`，而 R8 计划在同一位置修改参数（从 request 参数改为 session 快照值），形成对同一区域的两次顺序修改，增加合并冲突风险。虽然这是轮次分割的天然代价，但计划可注明此依赖链，降低实现时的意外。

## 修改要求（仅 REJECTED 时）

1. **[严重] 测试文件同步缺失**：
   - **问题**：`TriageRuleEngine.match()` 返回类型从 `List<RecommendedDepartment>` 改为 `MatchResult`，但 `DefaultTriageRuleEngineTest.java` 和 `TriageServiceImplTest.java`（`StubTriageRuleEngine`）的测试代码未在任务文件清单中体现，实现后将编译失败。
   - **期望的修正方向**：在 task_v7.md 的文件清单和 plan.md R7 描述中补充以下测试文件：
     - `consultation/src/test/java/.../DefaultTriageRuleEngineTest.java` — 修改 5 处 `match()` 调用，通过 `matchResult.getDepartments()` 获取返回列表。
     - `consultation/src/test/java/.../TriageServiceImplTest.java` — 修改 `StubTriageRuleEngine.match()` 返回类型为 `MatchResult` 并构造合适返回值。

2. **[一般] ObjectMapper 复用策略未明确**：
   - **问题**：`DefaultTriageRuleEngine.match()` 中若每次调用都构造新 `ObjectMapper` 实例将引入不必要的性能开销。社区最佳实践是复用单例实例（线程安全）。
   - **期望的修正方向**：在 task_v7.md 的 R7 实施要点中补充：ObjectMapper 应定义为 `DefaultTriageRuleEngine` 的 `private static final` 字段或通过构造器注入 Spring 管理的 `ObjectMapper` Bean。
