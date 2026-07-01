# 测试报告（v17）

## 文件变更

| 操作 | 文件路径 | 说明 |
|------|---------|------|
| 修改 | `AIMedical/backend/modules/prescription/src/test/java/com/aimedical/modules/prescription/service/audit/impl/PrescriptionAuditServiceImplTest.java` | 修改 `auditShouldHandleAiDataNull` → `auditShouldFallbackWhenAiUnavailable`，修复 `AiResult.success(null)` 与 A07 契约不兼容导致的 NPE |

## 测试方法变更

| 变更项 | 原内容 | 新内容 | 原因 |
|--------|--------|--------|------|
| 方法名 | `auditShouldHandleAiDataNull` | `auditShouldFallbackWhenAiUnavailable` | 语义对齐新的 "AI_UNAVAILABLE" 场景，与同类 fallback 测试命名风格一致 |
| AiResult 构造 | `AiResult.success(null)` | `AiResult.failure("AI_UNAVAILABLE")` | `AiResult.success()` 在 R9 A07 后通过 `Objects.requireNonNull(data)` 拒绝 null |
| `localRuleEngine.check()` stub | 缺失 | `when(localRuleEngine.check(any())).thenReturn(List.of())` | `AiResult.failure()` 的 `success=false` 使 fallback 分支调用 `localRuleEngine.check(request)` |
| `auditConverter.toAuditResponse(aiResult)` stub | 存在 (L458-464) | 删除 | Fallback 路径不调用 `toAuditResponse`，Mockito `STRICT_STUBS` 会拒绝未使用的 stub |

## 行为契约覆盖

| 契约 | 覆盖情况 |
|:----|:--------|
| `AiResult.failure("AI_UNAVAILABLE")` 使测试进入 fallback 路径 | ✅ `localRuleEngine.check(any())` stub 返回 `List.of()` |
| `aggregateRiskLevel(List.of())` 返回 `AuditRiskLevel.PASS` | ✅ 通过 `assertNull(captor.getValue().getAuditIssues())` 间接验证 |
| `persistAuditRecord` 不设置 `auditIssues`（`fromFallback=true` 且 `alerts` 为空） | ✅ `assertNull(captor.getValue().getAuditIssues())` |

## 覆盖维度分析

| 维度 | 说明 | 状态 |
|:----|:----|:----:|
| 正常路径 | `AiResult.failure("AI_UNAVAILABLE")` → fallback → 空 `auditIssues` | ✅ |
| 错误路径 | N/A（本变更不涉及新错误路径） | - |
| 边界条件 | N/A（本变更为单一场景替换） | - |
| 状态交互 | Fallback 路径与 `persistAuditRecord` 的 `fromFallback` 逻辑协同 | ✅ |
