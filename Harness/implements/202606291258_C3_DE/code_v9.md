# 实现报告（v9）

## 概述

修复 `TriageServiceImpl.triage()` 中连续 3 次 AI 失败后 `fallbackHint` 不生效的缺陷：在 fallback 块入口处对非 degraded 的 AI 失败结果（`AiResult.failure()`，`isSuccess()=false`, `isDegraded()=false`）显式递增 `aiFailCount`，确保两条失败路径（异常抛出 vs 正常返回 failure）均计入连续失败计数。

## 文件变更清单

| 操作 | 文件路径 | 说明 |
|------|---------|------|
| 修改 | `AIMedical/backend/modules/consultation/src/main/java/com/aimedical/modules/consultation/service/impl/TriageServiceImpl.java` | fallback 块入口添加非 degraded 路径的 `aiFailCount` 递增 |

## 编译验证

`mvn test -pl modules/consultation -am` — 85 个测试全部通过，其中 `TriageServiceImplTest.shouldSetFallbackHintAfterThreeAiFailures` 断言 `result.getFallbackHint()` 等于 `"AI service has been continuously unavailable"`。

## 设计偏差说明

无偏差。
