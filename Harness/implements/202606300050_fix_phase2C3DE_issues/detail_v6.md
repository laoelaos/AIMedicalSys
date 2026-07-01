# 详细设计（v6 r1）

## 概述

修复 `TriageServiceImpl.findDoctorsForDepartments()` 中 C06/E03（DoctorFacade 同步调用缺少 try/catch 异常保护）和 C17（多科室医生列表未排序取前 5）两个问题。仅涉及 `TriageServiceImpl.java` 一个文件的修改。

## 文件规划

| 文件路径 | 操作 | 职责 |
|---------|------|------|
| `AIMedical/backend/modules/consultation/src/main/java/com/aimedical/modules/consultation/service/impl/TriageServiceImpl.java` | 修改 | 修复 `findDoctorsForDepartments` 方法 |

## 类型定义

本任务不涉及新增类型、接口或枚举。所有修改仅在 `TriageServiceImpl.findDoctorsForDepartments()` 方法内部。

## 方法改造设计

### `findDoctorsForDepartments`

**当前签名**（不变）：
```java
private List<RecommendedDoctor> findDoctorsForDepartments(List<RecommendedDepartment> departments)
```

**改造后行为**：

1. 遍历 `departments` 参数中的每个 `RecommendedDepartment`。
2. 对每个科室：
   - 记录调用开始时间戳 `long start = System.currentTimeMillis()`
   - 调用 `doctorFacade.findAvailableDoctorsByDepartment(dept.getDepartmentId())`
   - 成功时：将返回的 `AvailableDoctor` 列表映射为 `RecommendedDoctor`（score 始终为 0f），追加到临时收集列表
   - 异常时：catch `Exception`，记录 WARN 日志，跳过该科室继续下一个科室
3. 所有科室遍历完成后，对收集到的全量 `RecommendedDoctor` 列表排序取前 5：
   - 排序键：`availableSlotCount` 降序
   - 取前 5 个元素
   - 排序不稳定时不影响业务语义（同 slot count 的医生顺序不要求确定）


**WARN 日志格式**（固定字符串，不可更改）：
```
"DoctorFacade call failed for department {} after {}ms: {} {}"
```
参数顺序：departmentId, elapsedMs, exception类名, exception消息。

**timing 计算**：`elapsedMs = System.currentTimeMillis() - start`，以毫秒为单位。

## 错误处理

- 异常捕获范围：`Exception`（所有 checked/unchecked 异常均捕获，包括 RuntimeException、Feign 异常、网络超时等基础设施层异常）
- 异常时不传播：不 throw，不中断外层循环；该科室返回空医生列表
- WARN 日志记录后继续下一个科室
- 不涉及自定义错误类型或错误码

## 行为契约

### 前置条件
- `departments` 参数允许为 null 或空列表：null 或空时直接返回 `Collections.emptyList()`
- `departmentId` 允许为 null：若 `DoctorFacade` 调用传 null 参数，由 `DoctorFacade` 实现侧自行处理（本方法不做 null 校验）

### 后置条件
- 返回值长度：`0 <= size <= 5`（即使有 10 个科室各返回 20 名医生，也仅取前 5）
- 排序保证：返回列表中 `availableSlotCount` 值严格不增（降序）
- 元素唯一性：不保证去重（同一医生可能出现在多个科室，保留所有记录）

### 不变式
- `score` 字段始终为 `0f`（DoctorFacade 不提供评分数据，OOD §4.1 场景约束）

## 依赖关系

| 依赖 | 方向 | 说明 |
|------|------|------|
| `DoctorFacade.findAvailableDoctorsByDepartment(String)` | 调用 | 同步调用，不修改，不封装 |
| `RecommendedDoctor` 构造函数 | 实例化 | 5 参构造：doctorId, doctorName, departmentId, availableSlotCount, score |
| `AvailableDoctor` record accessor | 读取 | `.doctorId()`, `.doctorName()`, `.departmentId()`, `.availableSlotCount()` |
| `org.slf4j.Logger` | 日志 | 已有 `log` 字段，不新增 |
| `Collections.emptyList()` | 异常路径返回值 | 已有 import |
| `java.util.stream.Collectors` | 排序取前 5 | 已有 import |

当前 `TriageServiceImpl.java` 已有全部所需 import：`java.util.ArrayList`、`java.util.Collections`、`java.util.List`、`java.util.stream.Collectors`、`org.slf4j.Logger`。无新增 import 需求。
