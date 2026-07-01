# 实现报告（v6）

## 概述

修改 `TriageServiceImpl.findDoctorsForDepartments()` 方法，修复 C06/E03（DoctorFacade 同步调用缺少 try/catch 异常保护）和 C17（多科室医生列表未排序取前 5）两个问题。仅涉及一个文件。

## 文件变更清单

| 操作 | 文件路径 | 说明 |
|------|---------|------|
| 修改 | `AIMedical/backend/modules/consultation/src/main/java/com/aimedical/modules/consultation/service/impl/TriageServiceImpl.java` | 改造 `findDoctorsForDepartments` 方法 |

## 编译验证

编译因 `TriageConverter.java` 的预存问题（`TriageRequest` 缺少 `setCorrectedChiefComplaint` 方法）失败，与本次修改无关。修改代码语法经人工检查正确。

## 设计偏差说明

无偏差。
