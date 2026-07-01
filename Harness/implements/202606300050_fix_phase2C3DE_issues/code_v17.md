# 实现报告（v17）

## 概述
修改 `PrescriptionAuditServiceImplTest.java` 中的 `auditShouldHandleAiDataNull` 方法，解决因 `AiResult.success(null)` 与 A07 契约不兼容导致的 NPE 问题。

## 文件变更清单
| 操作 | 文件路径 | 说明 |
|------|---------|------|
| 修改 | `AIMedical/backend/modules/prescription/src/test/java/com/aimedical/modules/prescription/service/audit/impl/PrescriptionAuditServiceImplTest.java` | 见下方变更 |

### 变更明细
- 方法名 `auditShouldHandleAiDataNull` → `auditShouldFallbackWhenAiUnavailable`
- `AiResult.success(null)` → `AiResult.failure("AI_UNAVAILABLE")`
- 移除 `auditConverter.toAuditResponse(aiResult)` stub（L458-464）
- 新增 `when(localRuleEngine.check(any())).thenReturn(List.of())` stub

## 编译验证
通过：`mvn compile test-compile` 无错误，且 `mvn test -Dtest=PrescriptionAuditServiceImplTest#auditShouldFallbackWhenAiUnavailable` 执行成功（日志输出 `AI service unavailable, switching to local rule engine` 确认 fallback 路径被覆盖）。

## 设计偏差说明
无偏差。
