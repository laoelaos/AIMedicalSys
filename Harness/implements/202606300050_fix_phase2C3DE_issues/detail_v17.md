# 详细设计（v17）

## 概述
修复 `PrescriptionAuditServiceImplTest.auditShouldHandleAiDataNull` 测试中因 `AiResult.success(null)` 与 A07 契约不兼容导致的 NPE 问题。变更测试用例构造 AiResult 的方式，使其符合 `AiResult.success()` 拒绝 null 的契约约束。

## 文件规划
| 文件路径 | 操作 | 职责 |
|---------|------|------|
| `AIMedical/backend/modules/prescription/src/test/java/com/aimedical/modules/prescription/service/audit/impl/PrescriptionAuditServiceImplTest.java` | 修改 | 见下方变更清单 |

### 变更清单

**方法：`auditShouldHandleAiDataNull`（L453-473，将重命名为 `auditShouldFallbackWhenAiUnavailable`）**

| 变更项 | 原内容 | 新内容 | 原因 |
|--------|--------|--------|------|
| L455: AiResult 构造 | `AiResult.success(null)` | `AiResult.failure("AI_UNAVAILABLE")` | `AiResult.success()` 在 R9 A07 后通过 `Objects.requireNonNull(data)` 拒绝 null，改用 `failure()` 表达 AI 不可用语义 |
| 新增: localRuleEngine stub | 缺失 | `when(localRuleEngine.check(any())).thenReturn(List.of())` | `AiResult.failure()` 的 `success=false` 使 `PrescriptionAuditServiceImpl.audit()` 进入 fallback 分支（L106-126），调用 `localRuleEngine.check(request)`（L112）；若不 stub 则 Mockito 返回 null，导致 `aggregateRiskLevel(null)` 在 L451 遍历时 NPE |
| L454: 方法名 | `auditShouldHandleAiDataNull` | `auditShouldFallbackWhenAiUnavailable` | 方法名语义与新的 "AI_UNAVAILABLE" 场景对齐，与 `auditShouldFallbackToLocalRuleEngineWhenAiFails`、`auditShouldLogWarnWhenAiResultIsNull` 等同类 fallback 测试的命名风格保持一致 |
| L458-464: 删除未使用的 stub | `when(auditConverter.toAuditResponse(aiResult)).thenAnswer(...)` | 删除该 stub | `AiResult.failure("AI_UNAVAILABLE")` 使测试进入 fallback 路径，该路径不调用 `toAuditResponse`；测试类使用 Mockito `STRICT_STUBS`，未使用的 stub 会触发 `UnnecessaryStubbingException` |

## 类型定义
无新增类型。

## 错误处理
无变更。

## 行为契约
- `AiResult.failure("AI_UNAVAILABLE")` 使测试进入 fallback 路径，`localRuleEngine.check()` 返回空列表 `List.of()`，`aggregateRiskLevel(List.of())` 返回 `AuditRiskLevel.PASS`。
- 进入 fallback 后，`persistAuditRecord` 不会设置 `auditIssues`（因 `fromFallback=true` 但 `alerts` 为空列表，且 `aiResult.isSuccess()=false`），故 `assertNull(captor.getValue().getAuditIssues())` 仍通过。

## 依赖关系
- 依赖 `AiResult.failure(String errorCode)` 静态工厂方法
- 依赖 `localRuleEngine.check(AuditRequest)` 的 Mockito stub 返回 `List.of()`

## 修订说明（v17 r1）
| 审查意见 | 修改措施 |
|---------|---------|
| [严重] 设计遗漏了 `localRuleEngine.check()` mock：替换为 `AiResult.failure()` 后测试将因不同的 NPE 再次失败 | 在变更清单中增加 `when(localRuleEngine.check(any())).thenReturn(List.of())` stub，确保 fallback 路径可正常执行。并在行为契约中说明 trace 验证。 |
| [轻微] 测试方法名 `auditShouldHandleAiDataNull` 与新的 "AI_UNAVAILABLE" 场景语义不一致 | 接受建议，将方法名改为 `auditShouldFallbackWhenAiUnavailable`，与同类 fallback 测试命名风格保持一致。 |

## 修订说明（v17 r2）
| 审查意见 | 修改措施 |
|---------|---------|
| [严重] 设计遗漏了移除 `auditConverter.toAuditResponse(aiResult)` stub（L458-464）：fallback 路径不调用 `toAuditResponse`，Mockito STRICT_STUBS 会拒绝未使用的 stub | 在变更清单中增加删除该 stub 的条目 |
| [轻微] 变更清单中方法名行号偏差 1（L453 为 `@Test`，L454 才是方法声明） | 修正为 L454 |
