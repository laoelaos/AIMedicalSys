# 计划审查报告（v19 r2）

## 审查结果
REJECTED

## 发现

### [严重] 3 个 Service 构造器新增 aiTimeout 参数后，测试文件未同步更新

- **问题**：R19 C07+A02 要求在 TriageServiceImpl、PrescriptionAuditServiceImpl、PrescriptionAssistServiceImpl 的构造器中新增 `@Value("${ai.timeout.*}") long aiTimeout` 参数。
- **影响**：3 个测试文件直接通过 `new` 实例化这些 Service：
  - `TriageServiceImplTest.java:81-82` — 9 参构造，无 aiTimeout
  - `PrescriptionAuditServiceImplTest.java:76-77` — 含 `2L` 作为 drugFacadeTimeout，无 aiTimeout
  - `PrescriptionAssistServiceImplTest.java:56-57` — 含 `2L` 作为 drugFacadeTimeout，无 aiTimeout
  - `PrescriptionAuditServiceImplTest.java:402` — 第二处直接实例化
- **plan.md R19 和 task_v19.md 均未列出任何测试文件**，涉及文件清单只包含生产文件。这将直接导致编译失败，验证轮次被阻断——与 R11→R12 的历史教训相同。
- **修正方向**：在 plan.md 和 task_v19.md 的涉及文件中补充 3 个测试文件，并在任务描述中明确要求同步更新构造参数。

### [严重] FallbackAiService.selectDelegate 引入后测试未覆盖

- **问题**：R19 A06 要求新增私有 `selectDelegate(DegradationContext)` 辅助方法，在 13 个方法中替换 `delegates.get(0)`。当多个 delegate 且部分被策略降级时，`selectDelegate` 的遍历→预检查→跳过→回退行为是核心逻辑变更。
- **影响**：`FallbackAiServiceTest.java` 中所有 13 组测试目前均假设 `delegates.get(0)` 行为。未提及任何对应测试更新，将导致以下场景无验证覆盖：
  - 多个 delegate，部分被降级 → selectDelegate 跳过降级 delegate 选择下一个
  - 所有 delegate 均被降级 → 返回降级 AiResult
  - 混合降级策略 + 多个 delegate 的组合行为
- **修正方向**：在 plan.md R19 中补充要求更新 `FallbackAiServiceTest`，至少覆盖多 delegate 和全降级场景。

### [严重] MockAiService @Profile 切换后测试断言失效 + 新策略模式未覆盖

- **问题**：R19 A05 要求将 MockAiService 的 `@ConditionalOnProperty` 改为 `@Profile("mock")`，并实现 3 种策略模式（STATIC/AI_UNAVAILABLE/TIMEOUT）。
- **影响**：
  - `MockAiServiceTest.java:32` 断言 `MockAiService.class.getAnnotation(Service.class)`，但不验证 `@Profile("mock")`。现有测试仅调用 `new MockAiService()`（无策略参数），不会触发新策略逻辑。
  - 3 种策略模式（STATIC/AI_UNAVAILABLE/TIMEOUT）完全无测试覆盖，且 `MockAdminController` 端点（POST/GET /api/admin/ai/mock/strategy）无集成测试。
- **修正方向**：在 plan.md R19 中补充要求更新 `MockAiServiceTest`，增加：
  - 验证 `@Profile("mock")` 注解存在
  - 3 种策略模式的返回行为验证
  - MockAdminController 端点的基础请求/响应测试

### [一般] DegradationContext 字段扩展后测试覆盖不足

- **问题**：R19 A06 要求 DegradationContext 增加 `serviceName`/`operationName` 字段。
- **影响**：`DegradationStrategyTest.java:10-32` 和 `NoOpDegradationStrategyTest.java:16` 均使用 `new DegradationContext()` 无参构造，新增字段后虽然有 setter/getter，但现有测试不会验证字段在 `shouldDegrade` 决策中被正确访问——降级框架的核心行为（基于 serviceName/operationName 条件降级）无测试保护。
- **修正方向**：补充要求至少一个集成测试验证 DegradationStrategy 基于 DegradationContext 字段的决策逻辑。

### [一般] A01 AiResult.degraded() → AiResultFactory.degraded() 替换未说明重载选择

- **问题**：task_v19.md §5 要求 TriageServiceImpl L178/L180 将 `AiResult.degraded()` 替换为 `AiResultFactory.degraded()`。当前 `AiResult.degraded(String)` 为单参（仅 fallbackReason），而 `AiResultFactory.degraded` 有 2 个重载：`degraded(fallbackReason, partialData)` 和 `degraded(fallbackReason, errorCode, partialData)`。
- **影响**：未指定使用哪个重载。若选用 2 参版本，`partialData` 传 `null` 则与原行为一致；若选用 3 参版本需额外提供 `errorCode`。实现时存在歧义，可能造成 errorCode 意外丢失。
- **修正方向**：明确指定 `AiResultFactory.degraded(fallbackReason, (TriageResponse) null)`（2 参重载，partialData=null）以确保 errorCode 保持 null 不变。

### [轻微] 涉及文件清单未包含测试文件，与历史轮次惯例不一致

- **问题**：以往轮次（如 R2 含 3 个测试文件、R7 含 2 个测试文件、R9 含 2 个测试文件、R13 含 4 个测试文件）均在涉及文件清单中列出测试文件。R19 涉及文件清单仅列 8 个生产文件，未列任何测试文件。
- **影响**：后续验证环节缺少预期测试变更的参考基线。
- **修正方向**：在涉及文件清单中补充需要更新的测试文件路径。

## 修改要求

1. **3 个 Service 的测试文件**：在 plan.md 和 task_v19.md 涉及文件中补充 TriageServiceImplTest.java、PrescriptionAuditServiceImplTest.java（含第 2 处实例化）、PrescriptionAssistServiceImplTest.java，明确要求同步新增 `aiTimeout` 构造参数。
2. **FallbackAiServiceTest**：补充要求添加多 delegate + 降级策略组合的测试用例，覆盖 selectDelegate 的跳过和回退行为。
3. **MockAiServiceTest + MockAdminController 测试**：补充要求覆盖 @Profile 注解验证、3 种策略模式、MockAdminController 端点。
4. **DegradationStrategy 相关测试**：补充要求覆盖基于 DegradationContext.serviceName/operationName 的决策逻辑。
5. **A01 AiResultFactory 重载选择**：明确指定使用 `AiResultFactory.degraded(fallbackReason, (TriageResponse) null)` 2 参重载。
