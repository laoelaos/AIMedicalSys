# 测试报告（v6）

## 测试文件

`AIMedical/backend/modules/consultation/src/test/java/com/aimedical/modules/consultation/TriageServiceImplTest.java`

## 变更概要

- 增强 `StubDoctorFacade`：支持按部门配置返回医生列表和模拟异常
- 新增 10 个 `findDoctorsForDepartments()` 行为契约测试用例

## 新增用例清单

| 用例 | 覆盖维度 | 说明 |
|------|---------|------|
| `shouldMapDoctorsFromSingleDepartment` | 正常路径 | 单科室映射，验证 doctorId/name/deptId/slotCount/score(0f) |
| `shouldReturnEmptyWhenDepartmentsIsNull` | 边界条件 | departments 为 null 时返回空列表 |
| `shouldReturnEmptyWhenDepartmentsIsEmpty` | 边界条件 | departments 为空时返回空列表 |
| `shouldLimitToFiveDoctorsAcrossDepartments` | 正常路径 | 6 名医生跨 2 科室，限前 5 |
| `shouldSortDoctorsBySlotCountDescending` | 正常路径 | 验证排序降序 |
| `shouldReturnAllDoctorsWhenTotalIsLessThanFive` | 边界条件 | 2 名医生 < 5，全量返回 |
| `shouldSkipDepartmentOnDoctorFacadeException` | 错误路径 | 科室1抛出异常，科室2正常，仅返回科室2医生；验证 WARN 日志格式及参数 |
| `shouldReturnEmptyWhenAllDepartmentsThrow` | 错误路径 | 全部科室异常，返回空列表；验证 2 条 WARN 日志格式及参数 |
| `shouldKeepScoreAsZeroForAllMappedDoctors` | 不变式 | 所有映射医生 score 均为 0f |
| `shouldReturnDoctorsOnFallbackPath` | 状态交互 | 降级路径下仍正确调用 findDoctorsForDepartments |

## 行为契约覆盖

| 契约类别 | 覆盖情况 |
|---------|---------|
| 前置条件：null/空 departments | ✅ |
| 后置条件：0 ≤ size ≤ 5 | ✅ |
| 后置条件：availableSlotCount 降序 | ✅ |
| 不变式：score == 0f | ✅ |
| 异常保护：catch Exception 跳过并记录 WARN 日志 | ✅ |
| 异常保护：全部异常返回空并记录 WARN 日志 | ✅ |
| 日志格式：固定字符串 `"DoctorFacade call failed for department {} after {}ms: {} {}"` | ✅ |
