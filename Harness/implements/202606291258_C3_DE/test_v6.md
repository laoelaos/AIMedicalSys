# 测试报告（v6）

## 测试文件清单

| 被测类型 | 测试文件路径 |
|---------|------------|
| AvailableDoctor | `common-module-api/src/test/java/com/aimedical/modules/commonmodule/doctor/AvailableDoctorTest.java` |
| DoctorFacade | `common-module-api/src/test/java/com/aimedical/modules/commonmodule/doctor/DoctorFacadeTest.java` |
| DrugInfo | `common-module-api/src/test/java/com/aimedical/modules/commonmodule/drug/DrugInfoTest.java` |
| DrugFacade | `common-module-api/src/test/java/com/aimedical/modules/commonmodule/drug/DrugFacadeTest.java` |
| VisitFacade | `common-module-api/src/test/java/com/aimedical/modules/commonmodule/visit/VisitFacadeTest.java` |
| RegistrationEvent | `common-module-api/src/test/java/com/aimedical/modules/commonmodule/event/RegistrationEventTest.java` |

## 覆盖维度

### AvailableDoctorTest（9 用例）
- 正常路径：各字段访问（doctorId、doctorName、departmentId、availableSlotCount）
- 边界条件：availableSlotCount = 0、availableSlotCount 负值
- 错误路径：所有字段为 null
- 状态交互：相等性（equals）、不等性（equals 反例）

### DoctorFacadeTest（2 用例）
- 正常路径：指定科室返回医生列表
- 边界条件：无排班时返回空列表（非 null）

### DrugInfoTest（9 用例）
- 正常路径：各字段访问（drugCode、drugName、specification、dosageForm、manufacturer、packageUnit）
- 错误路径：所有字段为 null
- 状态交互：相等性、不等性

### DrugFacadeTest（2 用例）
- 正常路径：根据药品编码返回药品信息
- 边界条件：无对应药品时返回 null

### VisitFacadeTest（2 用例）
- 正常路径：encounterId → visitId 转换
- 边界条件：无对应就诊记录时返回 null

### RegistrationEventTest（5 用例）
- 正常路径：全参构造器各字段赋值、getter/setter 读写
- 错误路径：无参构造器所有字段为 null
- 边界条件：sessionId 为 null（nullable 字段）
- 状态交互：字段独立变更

## 测试风格约定

- JUnit 5（`org.junit.jupiter.api.Test`），`shouldXxx()` 命名
- 基于行为契约编写，验证公开接口行为
- 门面接口使用匿名 Lambda 实现验证契约签名与返回值类型
- 不依赖外部 Spring 上下文，纯单元测试

## 执行方式

```bash
# 在 common-module-api 目录下执行
mvn test
# 或在整个 common-module 下执行
mvn test -pl common-module-api
```
