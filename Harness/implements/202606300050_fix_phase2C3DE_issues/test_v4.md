# 测试报告（v4）

## 概述

对 `TriageServiceImpl.saveTriageRecord` 中 `aiResult.isDegraded()` → `response.isDegraded()` 替换的回归与验证测试。

## 测试文件

`modules/consultation/src/test/java/com/aimedical/modules/consultation/TriageServiceImplTest.java`

## 测试验证清单

### 行为契约 1：saveTriageRecord degraded 赋值

| 用例 | 场景 | 状态 |
|------|------|------|
| `shouldSaveRuleMatchedDepartmentsWhenDegraded` | 降级路径：AiResult.failure() → response.degraded=true → record.degraded=true | ✅ 已有，通过 |
| `shouldSaveAiRecommendedDepartmentsWhenNotDegraded` | 成功路径：AiResult.success() → response.degraded=false → record.degraded=false | ✅ 新增 `assertFalse(record.getDegraded())` |

### 行为契约 2：saveTriageRecord 科室路由

| 用例 | 场景 | 状态 |
|------|------|------|
| `shouldSaveRuleMatchedDepartmentsWhenDegraded` | degraded=true → departments 存入 ruleMatchedDepartments | ✅ 已有，通过 |
| `shouldSaveAiRecommendedDepartmentsWhenNotDegraded` | degraded=false → departments 存入 aiRecommendedDepartments | ✅ 已有，通过 |

### 边界条件

| 用例 | 场景 | 状态 |
|------|------|------|
| `shouldNotSetDepartmentFieldsWhenFinalDepartmentsJsonIsNull` | 降级+空科室 → finalDepartmentsJson=null → 任一科室字段均不设置 | ✅ 新增 |

## 覆盖说明

- 正常路径：AiResult.success → response.degraded=false → record.degraded=false, departments → aiRecommendedDepartments
- 降级路径：AiResult.failure → response.degraded=true → record.degraded=true, departments → ruleMatchedDepartments
- 边界条件：departments 为空导致 finalDepartmentsJson=null → 不设置任何科室字段
- 状态交互：已有 `shouldResetAiFailCountOnSuccessfulTriage` 验证降级恢复后状态重置
