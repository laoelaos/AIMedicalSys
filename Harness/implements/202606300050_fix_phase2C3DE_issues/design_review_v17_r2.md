# 设计审查报告（v17 r2）

## 审查结果
REJECTED

## 发现

- **[严重]** 设计遗漏了移除 `auditConverter.toAuditResponse(aiResult)` stub（原 L458-464）。变更 `AiResult.success(null)` → `AiResult.failure("AI_UNAVAILABLE")` 后，测试将进入 fallback 路径（L108-126），`toAuditResponse()` 在该路径中不被调用。测试类使用 `@ExtendWith(MockitoExtension.class)`（Mockito 5.x 默认 `Strictness.STRICT_STUBS`），未使用的 stub 会触发 `UnnecessaryStubbingException` 导致测试失败。所有现有的 fallback 测试（如 `auditShouldFallbackToLocalRuleEngineWhenAiFails` L120、`auditShouldLogWarnWhenAiReturnsFailure` L165）均未对 `toAuditResponse` 做 stub。该行必须删除。

- **[轻微]** 变更清单中标注 "L453: 方法名"，但实际的方法声明行是 L454（L453 为 `@Test` 注解），行号偏差 1。

## 修改要求

1. **[严重]** 在变更清单中增加一项：删除 `when(auditConverter.toAuditResponse(aiResult)).thenAnswer(...)`（L458-464），理由同现有 fallback 测试——fallback 路径不调用 `toAuditResponse`，且 Mockito strict stubs 会拒绝未使用的 stub。
