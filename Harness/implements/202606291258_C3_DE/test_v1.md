# 测试报告（v1）

## 测试文件

| 文件路径 | 操作 | 说明 |
|---------|------|------|
| `AIMedical/backend/modules/ai/ai-api/src/test/java/com/aimedical/modules/ai/api/AiResultFactoryTest.java` | 新建 | AiResultFactory 静态工厂方法单元测试，8 个用例 |
| `AIMedical/backend/modules/ai/ai-api/src/test/java/com/aimedical/modules/ai/api/dto/triage/TriageDtoTest.java` | 扩展 | 新增 26 个用例覆盖新 DTO 及字段扩展 |

## 测试框架

JUnit 5 (Jupiter)，与项目已有测试框架一致。

## 覆盖维度

| 维度 | 覆盖情况 |
|------|---------|
| 正常路径 | AiResultFactory 四种工厂方法正向验证；所有 DTO getter/setter 设置与读取验证 |
| 边界条件 | partialData 为 null；success(null)；List 字段默认值为 null 验证；boolean 默认值为 false 验证；Float 默认值为 null 验证 |
| 错误路径 | AiResultFactory 的 failure() 系列方法验证失败语义 |
| 状态交互 | AiResult 各字段组合验证（如 degraded + partialData 同时生效） |
| 完整场景 | TriageRequest + TriageResponse 全部新字段联合构建与断言 |

## 测试用例清单

### AiResultFactoryTest (8 个)

1. shouldCreateFailureResultWithPartialData — failure(errorCode, partialData) 正向
2. shouldCreateFailureResultWithNullPartialData — failure(errorCode, null)
3. shouldCreateDegradedResultWithPartialData — degraded(fallbackReason, partialData) 正向
4. shouldCreateDegradedResultWithNullPartialData — degraded(fallbackReason, null)
5. shouldCreateFailureResultWithoutData — failure(errorCode) 无 partialData
6. shouldCreateSuccessResultWithData — success(data) 正向
7. shouldCreateSuccessResultWithNullData — success(null)
8. shouldSupportDifferentGenericTypes — 泛型类型参数验证

### TriageDtoTest (新增 26 个)

#### AdditionalResponseItem
1. shouldCreateAdditionalResponseItemWithDefaultConstructor
2. shouldSetAndGetAdditionalResponseItemFields

#### RecommendedDoctor
3. shouldCreateRecommendedDoctorWithDefaultConstructor
4. shouldSetAndGetRecommendedDoctorFields

#### MatchedRuleItem
5. shouldCreateMatchedRuleItemWithDefaultConstructor
6. shouldSetAndGetMatchedRuleItemFields

#### TriageRequest 扩展字段
7. shouldSetAndGetAdditionalResponsesInTriageRequest
8. shouldDefaultToNullForNewListFieldInTriageRequest
9. shouldSetAndGetPatientId
10. shouldSetAndGetSessionIdInRequest
11. shouldSetAndGetRuleVersion
12. shouldSetAndGetRuleSetId

#### TriageResponse 扩展字段
13. shouldSetAndGetRecommendedDoctors
14. shouldSetAndGetMatchedRules
15. shouldDefaultToFalseForNeedFollowUp
16. shouldSetAndGetNeedFollowUp
17. shouldSetAndGetFollowUpQuestion
18. shouldDefaultToNullForConfidence
19. shouldSetAndGetConfidence
20. shouldDefaultToFalseForDegraded
21. shouldSetAndGetDegradedInResponse
22. shouldSetAndGetSessionIdInResponse
23. shouldSetAndGetCorrectedChiefComplaint

#### RecommendedDepartment 扩展字段
24. shouldSetAndGetDepartmentId
25. shouldSetAndGetScore
26. shouldHaveDefaultPrimitiveValueForScore

#### 完整场景
27. shouldBuildFullTriageResponseWithAllNewFields
