# 设计审查报告（v8 r4）

## 审查结果
REJECTED

## 发现

- **[一般]** TriageService.selectDepartment 接口签名与任务描述不一致。任务指定三参数 `(sessionId, departmentId, departmentName)`，设计实际为四参数 `(..., boolean overwrite)`。虽然 overwrite 参数解决了 DeadLetterCompensationService 可能覆盖手动选科的问题（合理改进），但任务文件未同步更新，接口契约存在漂移。

- **[一般]** RegistrationEventListener.handleRegistrationEvent 实现路径与任务描述不一致。任务要求"调用 selectDepartment 写入 finalDepartmentId"，设计采用 TriageRecordRepository 直写（理由：避免 TriageService 循环依赖、语义更清晰）。虽然理由合理，但行为描述与任务存在实质偏差，任务文件未同步更新。

- **[轻微]** DeadLetterEventRepository 方法名与任务描述不一致。任务指定 `findByStateAndRetryCountLessThan`，设计使用 `findByCompensableEvents`（因 Spring Data JPA 派生查询无法实现字段间比较 retryCount < maxRetryCount，改用 @Query 自定义 JPQL）。功能等价，方法名差异不影响正确性。

## 修改要求

1. **（一般）TriageService.selectDepartment 接口**：任务文件需同步更新为四参数签名，或在设计的行为契约中明确标注与任务的差异及理由。

2. **（一般）RegistrationEventListener 实现路径**：任务文件需按设计修正描述（从"调用 selectDepartment"改为"使用 TriageRecordRepository 直写"），或设计改回调用 selectDepartment 以保持一致性。
