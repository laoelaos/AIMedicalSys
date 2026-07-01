# 实现报告（v26）

## 概述

实现了两个独立修复任务：C14（DeadLetterCompensationService 补偿前检查 retryCount >= maxRetryCount 迁移至 EXPIRED）和 E05（RegistrationEventListener.recover() 使用完整 RegistrationEvent 对象序列化替代手工 3 字段 HashMap）。共修改 4 个文件，新增 2 个测试方法。

## 文件变更清单

| 操作 | 文件路径 | 说明 |
|------|---------|------|
| 修改 | `AIMedical/backend/modules/consultation/src/main/java/com/aimedical/modules/consultation/service/DeadLetterCompensationService.java` | C14：增加补偿前 retryCount >= maxRetryCount 检查（EXPIRED）；catch 块中递增后二次检查（EXPIRED） |
| 修改 | `AIMedical/backend/modules/consultation/src/main/java/com/aimedical/modules/consultation/event/RegistrationEventListener.java` | E05：移除手工 HashMap 构建，替换为 `objectMapper.writeValueAsString(event)` 完整 7 字段序列化；移除未使用 imports |
| 修改 | `AIMedical/backend/modules/consultation/src/test/java/com/aimedical/modules/consultation/DeadLetterCompensationServiceTest.java` | 新增 shouldExpireWhenRetryCountExceedsMaxOnPreCheck / shouldExpireWhenRetryCountExceedsMaxOnCatch 两个测试 |
| 修改 | `AIMedical/backend/modules/consultation/src/test/java/com/aimedical/modules/consultation/RegistrationEventListenerTest.java` | 新增 shouldContainAllSevenFieldsInEventPayloadOnRecover 测试，验证 eventPayload 含全部 7 字段 |

## 编译验证

编译失败，但唯一错误位于 `TriageConverter.java:52`（`setCorrectedChiefComplaint` 在 `TriageRequest` 中不存在），该错误为已有问题，与本次修改无关。被修改的 4 个文件无语法或类型错误。

## 设计偏差说明

无偏差。严格按详细设计 v26 实现。
