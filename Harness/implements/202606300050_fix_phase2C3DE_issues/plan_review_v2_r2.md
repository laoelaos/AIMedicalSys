# 计划审查报告（v2 r2）

## 审查结果
REJECTED

## 发现

### **[一般] 遗漏 TriageServiceImplTest.shouldNotOverrideFinalDepartmentWhenOverwriteIsFalse 测试的处理**

该测试（`TriageServiceImplTest.java:309`）验证 overwrite=false 时 finalDepartmentId 不被覆盖的行为。R2 将 selectDepartment 接口改为 3 参并始终覆盖写入后，该测试：
- 移除 `false` 参数后编译通过但运行失败：`assertEquals("existing-dept", record.getFinalDepartmentId())` 将断言失败（实际被覆盖为 "dept-01"）
- 需要将其移除或重写为验证始终覆盖行为

task_v2.md 第 8 项仅说"移除 true/false 参数"，未明确说明该测试必须被移除，导致实现者可能遗漏。

### **[一般] 遗漏 DeadLetterCompensationServiceTest.shouldCallSelectDepartmentWithOverwriteFalse 测试的处理**

该测试（`DeadLetterCompensationServiceTest.java:54`）检查 `assertFalse(triageService.lastOverwrite)`。selectDepartment 改为 3 参后：
- `lastOverwrite` 字段在 StubTriageService 中不再被赋值，`assertFalse(false)` 永远通过，测试沦为无意义的空断言
- 需要移除该测试或改写为验证补偿场景的正确语义

task_v2.md 对 DeadLetterCompensationServiceTest 的处理仅描述为"更新模拟实现和调用点为 3 参"，未提及此测试需要移除。

## 修改要求

1. 在 task_v2.md 第 8 项 TriageServiceImplTest 条目中，明确要求移除 `shouldNotOverrideFinalDepartmentWhenOverwriteIsFalse` 测试（或重写为验证始终覆盖行为）
2. 在 task_v2.md 第 8 项 DeadLetterCompensationServiceTest 条目中，明确要求移除 `shouldCallSelectDepartmentWithOverwriteFalse` 测试（或重写为验证补偿语义）
