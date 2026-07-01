# 详细设计（v2）

## 概述

修复 selectDepartment 接口签名（4 参→3 参）、引入业务级错误码 TRIAGE_SESSION_NOT_FOUND、Controller 与 EventListener 同步对齐、@Retryable 异常范围限制。范围限缩为 TriageService 接口及其实现、Controller、RegistrationEventListener、DeadLetterCompensationService 四个组件联动修改。

## 文件规划

| 文件路径 | 操作 | 职责 |
|---------|------|------|
| `consultation/service/TriageService.java` | 修改 | selectDepartment 签名改为 3 参，移除 overwrite |
| `consultation/service/impl/TriageServiceImpl.java` | 修改 | 始终覆盖写入 finalDepartmentId/Name，改用 TriageErrorCode.TRIAGE_SESSION_NOT_FOUND |
| `consultation/exception/TriageErrorCode.java` | **新建** | TRIAGE_SESSION_NOT_FOUND 错误码枚举 |
| `consultation/api/TriageController.java` | 修改 | selectDepartment 调用改为 3 参 |
| `consultation/event/RegistrationEventListener.java` | 修改 | 注入 TriageService，前置检查后委托 selectDepartment；@Retryable 范围限缩 |
| `consultation/service/DeadLetterCompensationService.java` | 修改 | selectDepartment 调用改为 3 参 |
| `consultation/test/.../TriageControllerTest.java` | 修改 | StubTriageService.selectDepartment 签名改为 3 参 |
| `consultation/test/.../TriageServiceImplTest.java` | 修改 | 测试调用改为 3 参；移除 shouldNotOverrideFinalDepartmentWhenOverwriteIsFalse |
| `consultation/test/.../DeadLetterCompensationServiceTest.java` | 修改 | StubTriageService.selectDepartment 签名改为 3 参；移除 shouldCallSelectDepartmentWithOverwriteFalse |
| `consultation/repository/TriageRecordRepository.java` | 确认（无需修改） | —

## 类型定义

### TriageService（接口）
**形态**：interface
**包路径**：`com.aimedical.modules.consultation.service`
**变更**：selectDepartment 方法签名

**当前签名**：
```java
TriageResponse selectDepartment(String sessionId, String departmentId, String departmentName, boolean overwrite);
```

**改为**：
```java
TriageResponse selectDepartment(String sessionId, String departmentId, String departmentName);
```

**职责**：根据会话 ID 和科室信息，始终覆盖写入 finalDepartmentId/finalDepartmentName

---

### TriageServiceImpl
**形态**：class（Spring @Service）
**包路径**：`com.aimedical.modules.consultation.service.impl`
**变更**：selectDepartment 实现重写 + 错误码替换

**selectDepartment 实现**：
```java
@Override
public com.aimedical.modules.consultation.dto.TriageResponse selectDepartment(
        String sessionId, String departmentId, String departmentName) {
    TriageRecord record = triageRecordRepository.findBySessionId(sessionId)
            .orElseThrow(() -> new BusinessException(TriageErrorCode.TRIAGE_SESSION_NOT_FOUND,
                    "TriageRecord not found for sessionId: " + sessionId));

    record.setFinalDepartmentId(departmentId);
    record.setFinalDepartmentName(departmentName);
    triageRecordRepository.save(record);

    return toTriageResponse(record);
}
```

**import 变更**：
- 移除 `import com.aimedical.common.exception.GlobalErrorCode;`
- 新增 `import com.aimedical.modules.consultation.exception.TriageErrorCode;`

---

### TriageErrorCode（新建枚举）
**形态**：enum implements `com.aimedical.common.exception.ErrorCode`
**包路径**：`com.aimedical.modules.consultation.exception`
**职责**：consultation 模块业务错误码

```java
package com.aimedical.modules.consultation.exception;

import com.aimedical.common.exception.ErrorCode;

public enum TriageErrorCode implements ErrorCode {
    TRIAGE_SESSION_NOT_FOUND("TRIAGE_SESSION_NOT_FOUND", "分诊会话不存在");

    private final String code;
    private final String message;

    TriageErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override
    public String getCode() { return code; }

    @Override
    public String getMessage() { return message; }
}
```

**公开接口**：`getCode()` → `String` / `getMessage()` → `String`
**构造方式**：枚举常量直接引用
**类型关系**：实现 `com.aimedical.common.exception.ErrorCode`

---

### TriageController
**形态**：class（Spring @RestController）
**包路径**：`com.aimedical.modules.consultation.api`
**变更**：selectDepartment 调用

**当前**：
```java
TriageResponse response = triageService.selectDepartment(sessionId, departmentId, departmentName, true);
```

**改为**：
```java
TriageResponse response = triageService.selectDepartment(sessionId, departmentId, departmentName);
```

**说明**：`@RequestParam` 绑定 3 参数保持不变，不移除 hardcoded `true`（因参数已不存在）

---

### RegistrationEventListener
**形态**：class（Spring @Component）
**包路径**：`com.aimedical.modules.consultation.event`
**变更**：方法体重构 + @Retryable 范围限缩

**新增依赖**：`TriageService triageService`

**构造器变更**：
```java
public RegistrationEventListener(TriageRecordRepository triageRecordRepository,
                                  DeadLetterEventRepository deadLetterEventRepository,
                                  ObjectMapper objectMapper,
                                  TriageService triageService) {
    this.triageRecordRepository = triageRecordRepository;
    this.deadLetterEventRepository = deadLetterEventRepository;
    this.objectMapper = objectMapper;
    this.triageService = triageService;
}
```

**handleRegistrationEvent 方法体**：
```java
@EventListener
@Retryable(retryFor = {DataAccessException.class, TimeoutException.class},
           noRetryFor = {IllegalArgumentException.class, NullPointerException.class},
           maxAttempts = 3, backoff = @Backoff(delay = 2000))
public void handleRegistrationEvent(RegistrationEvent event) {
    triageRecordRepository.findBySessionId(event.getSessionId()).ifPresent(record -> {
        if (record.getFinalDepartmentId() == null) {
            triageService.selectDepartment(event.getSessionId(), event.getDepartmentId(), event.getDepartmentName());
        }
    });
}
```

**import 变更**：
- 新增 `import com.aimedical.modules.consultation.service.TriageService;`
- 新增 `import org.springframework.dao.DataAccessException;`
- 新增 `import java.util.concurrent.TimeoutException;`

**行为**：自检查仅当 finalDepartmentId == null 时调用 selectDepartment（事件写入仅在手动选科未发生时生效）

---

### DeadLetterCompensationService
**形态**：class（Spring @Service）
**包路径**：`com.aimedical.modules.consultation.service`
**变更**：selectDepartment 调用

**当前**：
```java
triageService.selectDepartment(sessionId, departmentId, departmentName, false);
```

**改为**：
```java
triageService.selectDepartment(sessionId, departmentId, departmentName);
```

**说明**：selectDepartment 新实现始终覆盖写入 finalDepartmentId，对死信补偿场景正确（补偿任务需要始终覆盖以恢复一致性）

---

### TriageControllerTest
**形态**：class（JUnit 5 test）
**包路径**：`com.aimedical.modules.consultation`
**变更**：StubTriageService.selectDepartment 签名

**当前**：
```java
public TriageResponse selectDepartment(String sessionId, String departmentId, String departmentName, boolean overwrite) {
    TriageResponse resp = new TriageResponse();
    resp.setSessionId(sessionId);
    resp.setReason("Department selected");
    return resp;
}
```

**改为**：
```java
public TriageResponse selectDepartment(String sessionId, String departmentId, String departmentName) {
    TriageResponse resp = new TriageResponse();
    resp.setSessionId(sessionId);
    resp.setReason("Department selected");
    return resp;
}
```

**测试方法 `shouldDelegateSelectDepartmentToServiceWithOverwriteTrue`**：测试名可保留（不再有 overwrite 语义，但测试逻辑不变），保持不变

---

### TriageServiceImplTest
**形态**：class（JUnit 5 test）
**包路径**：`com.aimedical.modules.consultation`
**变更**：三处修改

**(1) shouldSelectDepartmentWithOverwriteTrue** — 改为 3 参调用：
```java
com.aimedical.modules.consultation.dto.TriageResponse result = service.selectDepartment(
        "session-001", "dept-01", "内科");
```

**(2) shouldSelectDepartmentWithOverwriteFalseWhenFinalIsNull** — 改为 3 参调用（方法名保持，语义改为始终覆盖测试）：
```java
com.aimedical.modules.consultation.dto.TriageResponse result = service.selectDepartment(
        "session-001", "dept-01", "内科");
```

**(3) shouldNotOverrideFinalDepartmentWhenOverwriteIsFalse（line 308-321）** — **移除**（验证 overwrite=false 不覆盖的行为不再适用）

**(4) shouldThrowBusinessExceptionWhenRecordNotFound** — 改为 3 参调用：
```java
assertThrows(Exception.class, () ->
        service.selectDepartment("non-existent", "dept-01", "内科"));
```

---

### DeadLetterCompensationServiceTest
**形态**：class（JUnit 5 test）
**包路径**：`com.aimedical.modules.consultation`
**变更**：StubTriageService + 移除测试方法

**StubTriageService 签名变更**：
```java
private static class StubTriageService implements TriageService {
    boolean selectDepartmentCalled = false;
    boolean throwException = false;

    @Override
    public TriageResponse triage(DialogueCreateRequest request) {
        return new TriageResponse();
    }

    @Override
    public TriageResponse selectDepartment(String sessionId, String departmentId, String departmentName) {
        selectDepartmentCalled = true;
        if (throwException) {
            throw new RuntimeException("Simulated failure");
        }
        return new TriageResponse();
    }
}
```

**移除字段**：`boolean lastOverwrite;`（不再需要）

**测试方法变更**：
- `shouldCallSelectDepartmentWithOverwriteFalse`（line 53-62）— **移除**（检查 `lastOverwrite` 字段的测试，3 参后该字段不再被赋值）
- `shouldCompensateDeadLetterEvents` — 保持不变（逻辑不受影响，验证补偿状态和标记）
- `shouldIncrementRetryCountOnFailure` — 保持不变
- `shouldHandleMultipleEvents` — 保持不变
- `shouldSkipWhenNoEvents` — 保持不变

---

### TriageRecordRepository
**形态**：interface（JPA Repository）
**包路径**：`com.aimedical.modules.consultation.repository`
**确认**：无需修改。`findBySessionId(String)` 和 `findTopBySessionIdOrderByTriageTimeDesc(String)` 已存在，满足 R2 所有使用场景

## 错误处理

| 场景 | 错误类型 | 错误码 | 传播方式 |
|------|---------|--------|---------|
| TriageRecord 不存在（selectDepartment） | BusinessException | TRIAGE_SESSION_NOT_FOUND | 抛出异常，由 GlobalExceptionHandler 处理 |
| RegistrationEventListener 重试 | @Retryable | — | DataAccessException / TimeoutException 触发重试；IllegalArgumentException / NullPointerException 不重试直接 fallback 到 @Recover |
| 死信补偿失败 | catch Exception | — | 记录递增 retryCount，不重新抛出 |

## 行为契约

### selectDepartment 语义变更
- **始终覆盖**：调用后 `TriageRecord.finalDepartmentId` 和 `finalDepartmentName` 无条件设为传入值
- **前置条件**：sessionId 对应的 TriageRecord 必须存在，不存在则抛出 TRIAGE_SESSION_NOT_FOUND
- **后置条件**：记录已保存（triageRecordRepository.save）

### RegistrationEventListener 对齐
- 事件处理仅在 `record.getFinalDepartmentId() == null` 时调用 `selectDepartment`
- `selectDepartment` 内部已完成 set + save，事件处理不再直接操作 record 字段
- 事件处理的 `@Recover` 方法逻辑不变

### 幂等性
- `selectDepartment` 始终覆盖写入，多次调用最终状态一致（幂等）
- RegistrationEventListener 前置检查确保手动选科不会被子事件覆盖

### @Retryable 范围
- 仅重试 `DataAccessException` 和 `TimeoutException` 两类可恢复异常
- `IllegalArgumentException` 和 `NullPointerException` 触发后直接 fallback 到 `@Recover`，不浪费重试

## 依赖关系

| 依赖方向 | 源 | 目标 |
|---------|-------|--------|
| 新增依赖 | RegistrationEventListener | TriageService（构造器注入） |
| 新增依赖 | TriageServiceImpl | TriageErrorCode（import，同模块 exception 包） |
| 移除依赖 | TriageServiceImpl | GlobalErrorCode（原 selectDepartment 使用，不再需要） |

### 外部暴露接口
- `TriageService.selectDepartment` 签名从 4 参变为 3 参，调用方需同步变更

## 修订说明（v2 r1）
本设计为 v2 首版设计，基于 task_v2.md 中已收敛的 8 项变更要求制定。task_v2.md 中的修订说明涉及 R2→R15 编译兼容性、测试同步等已纳入本设计。
