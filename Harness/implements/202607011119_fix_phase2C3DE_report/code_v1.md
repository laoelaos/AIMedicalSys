# 实现报告（v1）

## 概述

修改 `TriageServiceImpl`，为 `doctorFacade.findAvailableDoctorsByDepartment()` 调用注入 `CompletableFuture.supplyAsync().get(timeout, SECONDS)` 超时控制，配置键 `consultation.doctor-facade.timeout`（默认 2s）。超时时捕获异常、记录 WARN 日志、跳过该科室，不阻断分诊主流程。

## 文件变更清单

| 操作 | 文件路径 | 说明 |
|------|---------|------|
| 修改 | `AIMedical/backend/modules/consultation/src/main/java/com/aimedical/modules/consultation/service/impl/TriageServiceImpl.java` | 新增 `doctorFacadeTimeout` 字段和构造参数；`findDoctorsForDepartments` 中包裹 supplyAsync + get 超时调用；catch 块补充 InterruptedException 中断恢复 |

## 编译验证

通过：`mvn compile -pl modules/consultation -am -q` 零错误。

## 设计偏差说明

无偏差。
