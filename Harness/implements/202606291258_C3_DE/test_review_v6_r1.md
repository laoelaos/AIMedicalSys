# 测试审查报告（v6 r1）

## 审查结果
REJECTED

## 发现

- **[一般]** `common-module-api/src/test/java/com/aimedical/modules/commonmodule/doctor/DoctorFacadeTest.java:32` — `shouldAcceptDepartmentIdAsNull` 测试 null 入参，但设计契约明确前置条件为 "departmentId 不为 null"。门面接口层测试不应覆盖前置条件之外的未定义行为，该用例无契约依据。

- **[一般]** `common-module-api/src/test/java/com/aimedical/modules/commonmodule/drug/DrugFacadeTest.java:25` — `shouldAcceptNullDrugCode` 测试 null 入参，但设计契约明确前置条件为 "drugCode 不为 null"。同理，该测试无契约依据。

- **[一般]** `common-module-api/src/test/java/com/aimedical/modules/commonmodule/visit/VisitFacadeTest.java:22` — `shouldAcceptNullEncounterId` 测试 null 入参，但设计契约明确前置条件为 "encounterId 不为 null"。同理，该测试无契约依据。

## 修改要求

### DoctorFacadeTest.java
- **位置**：第 31-36 行 `shouldAcceptDepartmentIdAsNull` 方法
- **问题**：测试违背前置条件，测试 null 入参不属于该层契约
- **修正方向**：删除该用例

### DrugFacadeTest.java
- **位置**：第 24-28 行 `shouldAcceptNullDrugCode` 方法
- **问题**：测试违背前置条件
- **修正方向**：删除该用例

### VisitFacadeTest.java
- **位置**：第 22-26 行 `shouldAcceptNullEncounterId` 方法
- **问题**：测试违背前置条件
- **修正方向**：删除该用例
