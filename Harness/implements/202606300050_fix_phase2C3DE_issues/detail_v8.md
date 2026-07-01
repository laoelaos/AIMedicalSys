# 详细设计（v8）

## 概述
修复 R7 验证失败中 TriageServiceImplTest.java:663 的编译错误：`result.isRuleVersionMismatch()` 在 TriageResponse 中不存在，应改为 `result.getRuleVersionMismatch()`。

## 文件规划
| 文件路径 | 操作 | 职责 |
|---------|------|------|
| `AIMedical/backend/modules/consultation/src/test/java/com/aimedical/modules/consultation/TriageServiceImplTest.java:663` | 修改 | 方法名从 `isRuleVersionMismatch()` 改为 `getRuleVersionMismatch()` |

## 变更说明

### 根因
TriageResponse.ruleVersionMismatch 声明为 `Boolean`（包装类型），Java Bean 规范下包装类型 getter 为 `getRuleVersionMismatch()`。测试行 663 误用 `isRuleVersionMismatch()`（该命名仅适用于 `boolean` 基本类型），导致编译失败。

### 变更内容
```java
// 原（编译错误——TriageResponse 中不存在 isRuleVersionMismatch()）
assertTrue(result.isRuleVersionMismatch());

// 改为
assertTrue(result.getRuleVersionMismatch());
```

## 风险分析
- 极低风险：仅测试文件单行方法名修正，不涉及生产代码
- 无需修改 MatchResult 设计（其 `ruleVersionMismatch` 为 `boolean` 基本类型，`isRuleVersionMismatch()` 正确）

## 修订说明（v8 r1）
| 审查意见 | 修改措施 |
|---------|---------|
| R7 验证失败：TriageServiceImplTest.java:663 — result.isRuleVersionMismatch() 不存在 | TriageResponse.ruleVersionMismatch 为 Boolean 包装类型，getter 应为 getRuleVersionMismatch()。测试行 663 修正方法名。 |
