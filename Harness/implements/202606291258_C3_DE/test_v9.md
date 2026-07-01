# 测试报告（v9）

## 文件变更

| 操作 | 文件路径 | 说明 |
|------|---------|------|
| 修改 | `AIMedical/backend/modules/consultation/src/test/java/com/aimedical/modules/consultation/TriageServiceImplTest.java` | 新增 5 个测试用例，覆盖 v9 行为契约 delta |

## 新增测试用例

| 测试方法 | 维度 | 覆盖契约 |
|---------|------|---------|
| `shouldNotSetFallbackHintAfterTwoAiFailures` | 边界 | 未达 MAX_AI_FAIL_COUNT(3) 时不设置 fallbackHint |
| `shouldIncrementFailCountOnExecutionException` | 异常路径 | ExecutionException → handleAiFailure 递增 count，fallback 块不重复递增 |
| `shouldRequireThreeExecutionExceptionsForFallbackHint` | 异常路径 | 连续 3 次 ExecutionException → fallbackHint 设置 |
| `shouldIncrementFailCountOnInterruptedException` | 异常路径 | InterruptedException → handleAiFailure 递增 count，fallback 块不重复递增 |
| `shouldNotDoubleCountWhenMixedFailurePaths` | 状态交互 | 异常路径 + 非异常失败路径组合计数正确，不重复计数 |

## 覆盖维度检查

| 维度 | 覆盖情况 |
|------|---------|
| 正常路径 | `shouldPerformTriageWithAiSuccess`（已有） |
| 边界条件 | `shouldNotSetFallbackHintAfterTwoAiFailures`（新增） |
| 错误路径（AiResult.failure） | `shouldFallbackToRuleEngineWhenAiFails`（已有）、`shouldSetFallbackHintAfterThreeAiFailures`（已有） |
| 错误路径（ExecutionException） | `shouldIncrementFailCountOnExecutionException`、`shouldRequireThreeExecutionExceptionsForFallbackHint`（新增） |
| 错误路径（InterruptedException） | `shouldIncrementFailCountOnInterruptedException`（新增） |
| 状态交互 | `shouldResetAiFailCountOnSuccessfulTriage`（已有）、`shouldNotDoubleCountWhenMixedFailurePaths`（新增） |

## 修订说明

无。首轮编写，无审查反馈。
