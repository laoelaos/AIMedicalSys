# 详细设计（v10）

## 概述

修复 TriageConverterTest 中因 A07 `AiResult.success(null)` 的 `Objects.requireNonNull(data)` 断言导致的 2 个测试 ERROR。生产代码无须变更。

## 文件规划

| 文件路径 | 操作 | 职责 |
|---------|------|------|
| `AIMedical/backend/modules/consultation/src/test/java/com/aimedical/modules/consultation/TriageConverterTest.java` | 修改 | 2 处 `AiResult.success(null)` → `AiResult.failure("AI_UNAVAILABLE")` |

## 类型定义

（无新增类型）

## 错误处理

- `AiResult.failure("AI_UNAVAILABLE")` 表示 AI 不可用/失败，对应 `success=false, data=null, errorCode="AI_UNAVAILABLE", degraded=false`
- `TriageConverter.toTriageResponse()` 对 failure 结果的现有行为：`aiResult.isDegraded()` 为 false → 不进入降级分支；`aiResult.getData()` 返回 null → 跳过 aiData 处理分支（含 departments、matchedRules 映射）；`session.getCorrectedChiefComplaint()` 保持 null（因 aiData == null，写回条件不满足）
- 两个测试的断言预期不变：`assertNull(session.getCorrectedChiefComplaint())` 和 `assertNull(result.getDepartments())` 在新的 failure 语义下仍然成立

## 行为契约

### 测试 1: `shouldNotWriteBackCorrectedChiefComplaintWhenAiDataIsNull`（L150-155）
- **当前**：`AiResult.success(null)` → NPE
- **修改后**：`AiResult.failure("AI_UNAVAILABLE")` → `toTriageResponse()` 中 `aiData==null` → `session.setCorrectedChiefComplaint()` 不执行 → `session.getCorrectedChiefComplaint()` 返回 null → `assertNull` 通过

### 测试 2: `shouldReturnEmptyDepartmentsForNullAiData`（L180-184）
- **当前**：`AiResult.success(null)` → NPE
- **修改后**：`AiResult.failure("AI_UNAVAILABLE")` → `toTriageResponse()` 中 `aiData==null` → `response.departments` 未设置 → 保持 null → `assertNull` 通过

## 依赖关系

- `AiResult.success()` 契约：要求 data 非 null（`Objects.requireNonNull`），违反时抛 NPE
- `AiResult.failure()` 契约：success=false, data=null, errorCode 为描述性字符串
- `TriageConverter.toTriageResponse()`：failure 结果下 aiData=null，正常走降级路径（不报错）
