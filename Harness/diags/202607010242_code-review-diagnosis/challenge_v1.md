# 诊断质询报告（v1）

## 质询结果

CHALLENGED

## 逐维度审查

### 1. 证据充分性

**[通过]** C01–C08、C15、C20、C21、E02、C07 等已修复项的代码验证准确，引用行号与实际代码一致。

**[通过]** C06 部分修复（DoctorFacade 无超时）的诊断证据充分，`application.yml` 中 `consultation.doctor-facade.timeout: 2` 配置已存在但 TriageServiceImpl 未注入使用，代码事实与诊断一致。

**[通过]** S04 部分修复（restoreSession 缺 synchronized）的诊断证据充分，DialogueSessionManager.java:50 确认 restoreSession 无 synchronized 标注。

**[问题-严重]** P03/S02：诊断称 `AiSuggestionResult 未实现 SuggestionStoreEntry 接口，ClassCastException 被 catch 后跳过，实际也无法清理`。但代码验证：`AiSuggestionResult.java:8` 已声明 `implements SuggestionStoreEntry`，且完整实现了 `getStatusName()`、`isConsumed()`、`getTimestamp()` 三个接口方法。**AiSuggestionResult 的 `instanceof SuggestionStoreEntry` 检查会成功，不会抛出 ClassCastException。** 诊断对 SuggestionCleanupTask 失效原因的判定基于错误的事实前提。SuggestionCleanupTask 的真实失效原因需重新调查——可能是 `isExpiredAndConsumed` 条件过于严格（要求 COMPLETED/FAILED + isConsumed + TTL 过期三者同时满足），或 `getTimestamp()` 返回 null（`createTime` 为 null 时 `toInstant` 可能异常），而非类型不匹配。

**[问题-一般]** A06：诊断称 `FallbackAiService 总是选择 delegates.get(0)，无重试链`。但实际代码 `FallbackAiService.java:66-80` 的 `selectDelegate` 方法遍历所有 delegates，对每个 delegate 检查所有 strategy 的 `shouldDegrade`，若不应降级则返回该 delegate。由于 `NoOpDegradationStrategy` 始终返回 false，`selectDelegate` 确实返回第一个 delegate，但这是 `NoOpDegradationStrategy` 的问题，不是 `FallbackAiService` 本身的逻辑问题。诊断将"降级策略空实现"与"无重试链"混淆——即使 `DegradationStrategy` 有实现，`selectDelegate` 也只会返回**一个** delegate，不存在重试链。诊断声称"无重试链"是对 FallbackAiService 设计意图的误判，真正的问题是 `applyStrategies`（后调用降级检查）使用空 DegradationContext，以及无有意义的 DegradationStrategy 实现。

**[问题-一般]** T20：诊断称 `FallbackAiService.java:290-301` 的 `applyStrategies` 使用空 `DegradationContext`。代码验证正确（第294行 `new DegradationContext()` 无 serviceName/operationName）。但诊断未注意到 `selectDelegate`（第66-80行）在调用前已正确设置了 `DegradationContext` 的 serviceName/operationName（如第87-89行）。`applyStrategies` 仅在 `thenApply` 中对已返回的结果做后处理，其空 DegradationContext 影响的是"结果是否被标记为降级"，而非"delegate 选择"。诊断未区分这两个不同位置的 DegradationContext 使用场景，导致对影响范围的判定不精确。

**[问题-轻微]** A01：诊断称 `PrescriptionAuditServiceImpl 使用 AiResult.degraded() 静态方法`。但实际代码 `PrescriptionAuditServiceImpl.java:96-105` 中 AI 调用失败时 `aiResult` 被设为 `null`，而非使用 `AiResult.degraded()`。诊断对 PrescriptionAuditServiceImpl 的 AI 失败路径描述不准确，但此描述不影响核心结论（AiResultFactory 引用问题），属于细节错误。

### 2. 逻辑完整性

**[通过]** 大部分问题的因果链从现象到根因完整，逻辑自洽。

**[问题-严重]** P03/S02：由于 SuggestionCleanupTask 的失效原因被错误归因为"类型不匹配"，诊断的逻辑链断裂：(1) 诊断声称 AiSuggestionResult 不实现 SuggestionStoreEntry → ClassCastException → 跳过清理；(2) 实际 AiSuggestionResult 实现了该接口 → 不会 ClassCastException。诊断未验证此关键前提，导致 SuggestionCleanupTask 的真正失效原因未被定位。需要重新调查为何 SuggestionCleanupTask 实际未清理成功——可能的原因包括：`isExpiredAndConsumed` 条件要求 `status=COMPLETED/FAILED && consumed=true && TTL过期`，而 `scheduleSuggestionAsync` 在 FAILED 时未设置 consumed，或 `getTimestamp()` 返回 null 导致 `plusSeconds` NPE 被 catch 吞掉。

**[问题-一般]** P03/S02 + T32 合并诊断：诊断将 P03/S02 和 T32 合并为同一根因（recordWrite 无调用方），但 T32 的本质是 DraftContextCleanupTask 的清理失效，P03/S02 的 SuggestionCleanupTask 是另一个独立的清理任务。两者失效原因不同（DraftContextCleanupTask 因 writeTimestamps 为空而失效，SuggestionCleanupTask 因其他原因失效），合并诊断导致修复建议可能遗漏 SuggestionCleanupTask 的独立修复。

**[问题-轻微]** T28：诊断对 `hasNewAlerts` 逻辑矛盾的分析深入，但结论"hasNewAlerts 逻辑仍需重新设计"过于笼统。实际上，submit 方法第152-163行的逻辑是：hasCriticalAlerts=true 时直接返回 BLOCK，不会执行到第182行的 hasNewAlerts 检查。因此 hasNewAlerts 在当前代码路径中确实不可达（hasCriticalAlerts=true 时提前返回，hasCriticalAlerts=false 时 snapshotCriticalAlerts 为空），诊断未明确指出 hasNewAlerts 是**死代码**而非逻辑矛盾。

### 3. 覆盖完备性

**[通过]** 任务描述（todo.md）中的 P0/P1/P2 所有问题项在诊断报告中均有覆盖。

**[问题-一般]** P03/S02：SuggestionCleanupTask 的实际失效原因未被正确诊断。诊断声称类型不匹配导致 ClassCastException，但 AiSuggestionResult 已实现 SuggestionStoreEntry 接口。这属于对核心问题的覆盖不完整——只覆盖了 DraftContextCleanupTask 的失效原因，SuggestionCleanupTask 的失效原因仍为未知。

**[问题-轻微]** NoOpDegradationStrategy 的存在未被诊断提及。`NoOpDegradationStrategy.java` 使用 `@ConditionalOnMissingBean(DegradationStrategy.class)` 注册，意味着当没有其他 DegradationStrategy Bean 时，它会被自动注册且始终返回 false。这使得 A06 的诊断结论"DegradationStrategy 无实现类"不完全准确——存在一个实现，但它是空操作。此细节对根因定位影响较小，但遗漏了 `@ConditionalOnMissingBean` 导致永远无法注册有意义策略的 Spring Bean 加载顺序问题。

**[问题-轻微]** DraftContextStoreImpl 缺少 `compute` 和 `createIfNotExists` 方法（T50），诊断在 P1 部分提及但仅说"与 S07 合并处理"，未给出 DraftContextStoreImpl 的具体缺失方法诊断。DraftContextStoreImpl.java 确认仅有 get/put/remove/containsKey/keySet 五个方法，无 compute/createIfNotExists，但诊断未验证 DedupTaskScheduler 是否实际使用了 DraftContextStore（实际使用的是 SuggestionStore），T50 的根因未充分验证。

## 质询要点

### 1. P03/S02 SuggestionCleanupTask 失效原因误判

- **问题**：诊断声称 AiSuggestionResult 未实现 SuggestionStoreEntry 接口，导致 ClassCastException 跳过清理。但 `AiSuggestionResult.java:8` 已声明 `implements SuggestionStoreEntry`，完整实现了三个接口方法。类型不匹配的根因判定与代码事实矛盾。
- **原因**：此错误直接导致 SuggestionCleanupTask 的真正失效原因未被定位。如果 ClassCastException 不会发生，那么 SuggestionCleanupTask 未清理数据必定有其他原因（如 `isExpiredAndConsumed` 条件过于严格、`getTimestamp()` 返回 null 等），诊断未覆盖这些可能性，修复者无法据此修复 SuggestionCleanupTask。
- **建议方向**：(1) 确认 AiSuggestionResult 在 SuggestionCleanupTask.cleanupExpiredSuggestions 中的 `instanceof SuggestionStoreEntry` 检查是否通过（应通过）；(2) 调试 `isExpiredAndConsumed` 方法的实际执行路径——检查 COMPLETED/FAILED 状态的 AiSuggestionResult 是否满足 `isConsumed()=true` 条件；(3) 检查 `getTimestamp()` 是否可能返回 null（createTime 为 null 时）；(4) 重新确定 SuggestionCleanupTask 的真正失效原因。

### 2. A06 FallbackAiService 降级诊断不精确

- **问题**：诊断称"DegradationStrategy 无实现类，FallbackAiService 总是选择 delegates.get(0)，无重试链"。但实际存在 NoOpDegradationStrategy 实现（始终返回 false），且 FallbackAiService 的 selectDelegate 逻辑是"遍历 delegates 找第一个不被降级的"，不是"总是取 delegates.get(0)"。selectDelegate 返回第一个 delegate 是因为 NoOpDegradationStrategy 不降级任何 delegate，而非逻辑本身的问题。
- **原因**：将"降级策略空实现"与"无重试链"混淆。FallbackAiService 的设计是选择策略而非重试链——即使有有意义的 DegradationStrategy，也不会产生重试链。真正的降级问题是：(1) applyStrategies 使用空 DegradationContext 无法做后处理降级；(2) NoOpDegradationStrategy 使 selectDelegate 的降级筛选形同虚设。诊断的不精确可能误导修复方向。
- **建议方向**：(1) 明确区分"delegate 选择阶段"（selectDelegate 使用正确的 DegradationContext）和"结果后处理阶段"（applyStrategies 使用空 DegradationContext）的不同问题；(2) 重新评估 FallbackAiService 是否设计为重试链模型——如果不是，则"无重试链"不是问题，问题在于降级决策逻辑不生效；(3) 注意 NoOpDegradationStrategy 的 `@ConditionalOnMissingBean` 会导致自定义策略注册时 NoOp 不加载，诊断应区分"无实现"和"有 NoOp 实现"两种场景。

### 3. T20 applyStrategies 影响 scope 判定不精确

- **问题**：诊断称 FallbackAiService.applyStrategies 使用空 DegradationContext，但未区分这与 selectDelegate 中的 DegradationContext 是两个独立的使用点。selectDelegate 在各业务方法中正确设置了 serviceName/operationName（如 triage 方法第87-89行），applyStrategies 仅在 thenApply 中使用空 context。
- **原因**：applyStrategies 的空 DegradationContext 影响的是"AI 调用失败后是否将结果标记为降级"，而非"选择哪个 delegate"。诊断未明确区分这两个影响范围，可能导致修复者误认为 selectDelegate 也受空 DegradationContext 影响。
- **建议方向**：明确 applyStrategies 的职责——它是对已获取的 AiResult 做降级标记，而非做 delegate 选择。空 DegradationContext 的影响仅限于降级标记无法基于服务名/操作名做精细化决策，不影响 delegate 选择逻辑。
