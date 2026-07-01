# 设计审查报告（v8 r3）

## 审查结果
REJECTED

## 发现

- **[一般]** `RegistrationEventListener` 的构造器依赖列表仅包含 `TriageRecordRepository`，但其 `@Recover` 方法的行为契约要求"将失败事件写入 DeadLetterEvent（eventPayload=JSON, failReason=e.getMessage()）"。写入 `DeadLetterEvent` 必须通过 `DeadLetterEventRepository` 完成，该依赖未在类型定义中列出。缺少此依赖将导致 `@Recover` 方法无法编译或无法按契约行为运行。

- **[一般]** `RegistrationEventListener` 的 `handleRegistrationEvent` 行为契约描述为"调用 selectDepartment 写入 finalDepartmentId/finalDepartmentName"，但类型定义中仅有 `TriageRecordRepository` 依赖，未声明 `TriageService` 依赖。若此处指 `TriageService.selectDepartment()` 则缺少 `TriageService`；若指直接通过 Repository 更新实体，则行为描述与常见实现路径不一致。

- **[轻微]** `TriageResponse.ruleVersionMismatch` 声明为 `boolean` 原始类型，但任务描述标注为"(可选)"（暗示可为 null），使用 `Boolean` 包装类型更符合可选语义。

## 修改要求

1. `RegistrationEventListener` 补充 `DeadLetterEventRepository` 依赖，确保 `@Recover` 方法能将失败事件持久化为 `DeadLetterEvent`。
2. 明确 `RegistrationEventListener.handleRegistrationEvent` 的实现路径：若通过 `TriageService.selectDepartment()` 执行，则补充 `TriageService` 依赖；若直接通过 `TriageRecordRepository` 更新实体，则将行为契约修改为"通过 Repository 更新 TriageRecord 的 finalDepartmentId/finalDepartmentName"。
3. 将 `TriageResponse.ruleVersionMismatch` 的类型从 `boolean` 改为 `Boolean` 以匹配任务中"(可选)"语义。
