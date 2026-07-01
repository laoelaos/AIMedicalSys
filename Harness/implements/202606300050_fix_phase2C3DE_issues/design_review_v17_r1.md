# 设计审查报告（v17 r1）

## 审查结果
REJECTED

## 发现

### [严重] 设计遗漏了 localRuleEngine.check() mock：替换为 AiResult.failure() 后测试将因不同的 NPE 再次失败

**问题**：设计将 `AiResult.success(null)` 替换为 `AiResult.failure("AI_UNAVAILABLE")`。但 `AiResult.failure()` 的 `success=false` 会导致 `PrescriptionAuditServiceImpl.audit()` 进入 fallback 分支（L108-126），调用 `localRuleEngine.check(request)`（L112）。

`auditShouldHandleAiDataNull` 测试中 `localRuleEngine` 是 `@Mock`（test L61），且**未对该 mock 的 `check()` 方法做任何 stub**。Mockito 返回默认值 `null`，继而 `aggregateRiskLevel(null)` 在 L452 遍历 `ruleResults` 时抛出 NPE。

对比同一测试类中已有的 fallback 测试（`auditShouldFallbackToLocalRuleEngineWhenAiFails` L121-136 和 `auditShouldLogWarnWhenAiResultIsNull` L139-159），它们**均显式 stub 了** `localRuleEngine.check(any())`。当前设计遗漏了这一必要步骤。

**期望的修正方向**：在测试中增加 `when(localRuleEngine.check(any())).thenReturn(List.of())` 的 stub，确保 fallback 路径可正常执行。或在设计中说明测试方法的完整变更集（包括 mock 准备）。

### [轻微] 测试方法名 `auditShouldHandleAiDataNull` 与新的 "AI_UNAVAILABLE" 场景语义不一致

测试方法名暗示 "AI 返回 null data" 场景，但 `AiResult.failure("AI_UNAVAILABLE")` 表达的是 "AI 不可用" 状态。虽然两者都验证优雅降级，但方法名与场景不符可能降低可读性。建议同步重命名为 `auditShouldFallbackWhenAiUnavailable`，与同类 fallback 测试的命名风格保持一致。

## 修改要求（仅 REJECTED 时）

1. **[严重]** 问题：`AiResult.failure("AI_UNAVAILABLE")` 会触发 fallback 路径，但 `localRuleEngine.check()` 未 mock，导致 NPE。修正方向：在测试中增加 `when(localRuleEngine.check(any())).thenReturn(List.of())`，确保 fallback 路径可正常执行，且测试结果符合预期（`assertNull(captor.getValue().getAuditIssues())` 在 fallback + 空 LocalRuleResult 时仍应通过）。
2. **[轻微]** 问题：方法名语义偏移。修正方向：重命名测试方法为 `auditShouldFallbackWhenAiUnavailable`（可选），与 `auditShouldFallbackToLocalRuleEngineWhenAiFails` 等现有方法名风格一致。
