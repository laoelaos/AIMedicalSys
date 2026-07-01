# 任务指令（v2）

## 动作
NEW

## 任务描述
修复 selectDepartment 接口签名（4 参→3 参）、引入业务级错误码 TRIAGE_SESSION_NOT_FOUND、Controller 与 EventListener 同步对齐、@Retryable 异常范围限制。预期涉及以下文件：

| 修改级别 | 文件路径 |
|---------|---------|
| 修改 | `consultation/.../service/TriageService.java` |
| 修改 | `consultation/.../service/impl/TriageServiceImpl.java` |
| 修改 | `consultation/.../api/TriageController.java` |
| 修改 | `consultation/.../event/RegistrationEventListener.java` |
| 修改 | `consultation/.../compensation/DeadLetterCompensationService.java` |
| 新增 | `consultation/.../exception/TriageErrorCode.java` |
| 修改 | `consultation/.../test/.../TriageControllerTest.java` |
| 修改 | `consultation/.../test/.../TriageServiceImplTest.java` |
| 修改 | `consultation/.../test/.../DeadLetterCompensationServiceTest.java` |
| 确认（无需修改） | `consultation/.../repository/TriageRecordRepository.java` |

具体变更要求：

### 1. TriageService 接口 — selectDepartment 签名改为 3 参
- **当前**：`TriageResponse selectDepartment(String sessionId, String departmentId, String departmentName, boolean overwrite)`
- **改为**：`TriageResponse selectDepartment(String sessionId, String departmentId, String departmentName)`
- **理由**：移除 overwrite 参数（C08），Service 内部始终覆盖写入 finalDepartmentId

### 2. TriageServiceImpl — 始终覆盖写入 + 业务错误码
- `selectDepartment()` 实现中始终执行 `record.setFinalDepartmentId(departmentId)` + `record.setFinalDepartmentName(departmentName)` — 无需判断 overwrite（参数已移除）
- TriageRecord 不存在时抛出 `BusinessException(TriageErrorCode.TRIAGE_SESSION_NOT_FOUND, "TriageRecord not found for sessionId: " + sessionId)`（C09）
- **新增枚举**：在 `consultation/exception/` 下创建 `TriageErrorCode implements ErrorCode`，包含：
  - `TRIAGE_SESSION_NOT_FOUND("TRIAGE_SESSION_NOT_FOUND", "分诊会话不存在")`

### 3. TriageController — selectDepartment 调用改为 3 参
- **当前**：`triageService.selectDepartment(sessionId, departmentId, departmentName, true)`
- **改为**：`triageService.selectDepartment(sessionId, departmentId, departmentName)`（C22 自动消除）
- 保持 `@RequestParam` 绑定 3 参数不变

### 4. RegistrationEventListener — 改用 TriageService.selectDepartment + 前置检查
- **当前**：`handleRegistrationEvent` 直接操作 `triageRecordRepository.findBySessionId()` + `record.setFinalDepartmentId()` + `save()`
- **改为**：注入 `TriageService`，调用 `triageService.selectDepartment(sessionId, departmentId, departmentName)` 前先自检查：仅当 `finalDepartmentId == null` 时调用（事件写入仅在手动选科未发生时生效）
- 方法内使用 `findBySessionId` 读取记录判断 `finalDepartmentId`，为空则调用 `selectDepartment`

### 5. RegistrationEventListener — @Retryable 限制异常范围（C15/E01）
- **当前**：`@Retryable(retryFor = Exception.class, maxAttempts = 3, backoff = @Backoff(delay = 2000))`
- **改为**：`@Retryable(retryFor = {DataAccessException.class, TimeoutException.class}, noRetryFor = {IllegalArgumentException.class, NullPointerException.class}, maxAttempts = 3, backoff = @Backoff(delay = 2000))`
- 添加缺失的 `import org.springframework.dao.DataAccessException`
- 添加缺失的 `import java.util.concurrent.TimeoutException`

### 6. DeadLetterCompensationService — selectDepartment 调用改为 3 参
- **当前**：`triageService.selectDepartment(sessionId, departmentId, departmentName, false)`（4 参，false 表示不覆盖）
- **改为**：`triageService.selectDepartment(sessionId, departmentId, departmentName)`（3 参，移除 false 参数）
- **理由**：接口签名变更后编译兼容；`selectDepartment` 新实现始终覆盖写入 finalDepartmentId，对死信补偿场景正确（补偿任务需要始终覆盖以恢复一致性）
- **注意**：其他死信逻辑修复（状态迁移、完整序列化）保留在 R15

### 7. TriageController — 保持 @RequestParam 不变
- **改为**：`triageService.selectDepartment(sessionId, departmentId, departmentName)` 3 参调用
- **说明**：保持 `@RequestParam` 绑定 3 参数不变，不移除 hardcoded `true`（因参数已不存在）。无需引入 DTO 或切换为 `@RequestBody`
- **参考**：当前已使用 `@RequestParam String sessionId, @RequestParam String departmentId, @RequestParam String departmentName`

### 8. 测试文件同步更新
- TriageControllerTest：更新 `StubTriageService.selectDepartment` 模拟方法签名为 3 参
- TriageServiceImplTest：
  - 更新 `service.selectDepartment(sessionId, departmentId, departmentName)` 调用（移除 true/false 参数）
  - **必须移除** `shouldNotOverrideFinalDepartmentWhenOverwriteIsFalse` 测试方法（`TriageServiceImplTest.java:309`）——该测试验证 overwrite=false 不覆盖的行为，R2 改为始终覆盖后该测试必失败；或将其重写为验证始终覆盖行为
- DeadLetterCompensationServiceTest：
  - 更新模拟实现和调用点为 3 参
  - **必须移除** `shouldCallSelectDepartmentWithOverwriteFalse` 测试方法（`DeadLetterCompensationServiceTest.java:54`）——该测试检查 `lastOverwrite` 字段，3 参后该字段不再被赋值，测试沦为无意义空断言

## 选择理由
R1 已修复 correctedChiefComplaint 数据链路，此轮（R2）处理 selectDepartment 群组（C08 P0/C09 P1/C22 P2/C15 P0）作为 consultation 模块核心接口修复。R1 已添加 `findTopBySessionIdOrderByTriageTimeDesc` 查询方法，R2 不依赖 R1 数据链路修改，可并行实施。所有修改集中于 TriageService/RegistrationEventListener 两个组件，耦合度低，一并通过验证。

## 任务上下文
摘录自 OOD §3.1 和诊断报告：

- **OOD §3.1（line 467/469）**：`TriageService.selectDepartment` 接口为 3 参 `(sessionId, departmentId, departmentName)`，无 overwrite；Service 内部始终覆盖写入 finalDepartmentId；记录不存在时返回 TRIAGE_SESSION_NOT_FOUND。
- **OOD §3.1（line 472）**：RegistrationEventListener 在调用 selectDepartment 前先检查 TriageRecord.finalDepartmentId 是否为空，仅当为空时调用（事件写入仅在手动选科未发生时生效）。
- **OOD §3.1（line 434）**：死信补偿任务 `DeadLetterCompensationService.compensateDeadLetters()` 也调用 `TriageService.selectDepartment(sessionId, departmentId, departmentName)` 3 参版本。
- **C08（P0）**：`TriageService.java:10` 签名为 4 参（含 overwrite），OOD 设计为 3 参。
- **C09（P1）**：`TriageServiceImpl.java:154` 使用 `GlobalErrorCode.NOT_FOUND`，OOD 要求 `TRIAGE_SESSION_NOT_FOUND`。
- **C15/E01（P0）**：`RegistrationEventListener.java:36` `@Retryable(retryFor = Exception.class, ...)` 范围过宽，仅应对 DataAccessException/TimeoutException 重试。
- **C22（P2）**：`TriageController.java:34` 硬编码 `overwrite=true`，随 C08 接口改为 3 参自动消除。

## 已有代码上下文

**当前 `TriageService.java`（接口）**：
```java
TriageResponse selectDepartment(String sessionId, String departmentId, String departmentName, boolean overwrite);
```

**当前 `TriageServiceImpl.java:150-164`（实现）**：
```java
@Override
public TriageResponse selectDepartment(String sessionId, String departmentId, String departmentName, boolean overwrite) {
    TriageRecord record = triageRecordRepository.findBySessionId(sessionId)
            .orElseThrow(() -> new BusinessException(GlobalErrorCode.NOT_FOUND,
                    "TriageRecord not found for sessionId: " + sessionId));
    if (overwrite || record.getFinalDepartmentId() == null) {
        record.setFinalDepartmentId(departmentId);
        record.setFinalDepartmentName(departmentName);
        triageRecordRepository.save(record);
    }
    return toTriageResponse(record);
}
```

**当前 `TriageController.java:30-36`**：
```java
@PostMapping("/select-department")
public Result<TriageResponse> selectDepartment(@RequestParam String sessionId,
                                                @RequestParam String departmentId,
                                                @RequestParam String departmentName) {
    TriageResponse response = triageService.selectDepartment(sessionId, departmentId, departmentName, true);
    return Result.success(response);
}
```

**当前 `RegistrationEventListener.java:35-45`**：
```java
@EventListener
@Retryable(retryFor = Exception.class, maxAttempts = 3, backoff = @Backoff(delay = 2000))
public void handleRegistrationEvent(RegistrationEvent event) {
    triageRecordRepository.findBySessionId(event.getSessionId()).ifPresent(record -> {
        if (record.getFinalDepartmentId() == null) {
            record.setFinalDepartmentId(event.getDepartmentId());
            record.setFinalDepartmentName(event.getDepartmentName());
            triageRecordRepository.save(record);
        }
    });
}
```

**当前 `TriageRecordRepository.java`（已包含 R1 新增方法）**：
```java
Optional<TriageRecord> findTopBySessionIdOrderByTriageTimeDesc(String sessionId);
Optional<TriageRecord> findBySessionId(String sessionId);
```

**错误码模式参考**（`common/GlobalErrorCode.java`、`medical-record/MedicalRecordErrorCode.java`）：
```java
public enum GlobalErrorCode implements ErrorCode {
    NOT_FOUND("NOT_FOUND", "资源不存在"),
    // ...
}
```
需在 `consultation/exception/` 下创建 `TriageErrorCode implements ErrorCode`，包含 `TRIAGE_SESSION_NOT_FOUND` 错误码。

**死信补偿任务 `DeadLetterCompensationService`** 也调用 `TriageService.selectDepartment`——当前接口为 4 参（`..., false`），R2 同步改为 3 参调用以保证编译通过。其他死信逻辑修复保留在 R15。

## 修订说明（v2 r2）
| 审查意见 | 修改措施 |
|---------|---------|
| TriageServiceImplTest.shouldNotOverrideFinalDepartmentWhenOverwriteIsFalse 编译通过但运行失败，未明确指示处理方式 | 第 8 项 TriageServiceImplTest 条目明确要求移除该测试（或重写为验证始终覆盖行为） |
| DeadLetterCompensationServiceTest.shouldCallSelectDepartmentWithOverwriteFalse 在 3 参后沦为无意义空断言，未明确指示处理方式 | 第 8 项 DeadLetterCompensationServiceTest 条目明确要求移除该测试（或重写为验证补偿语义） |

## 修订说明（v2 r1）
| 审查意见 | 修改措施 |
|---------|---------|
| DeadLetterCompensationService 以 4 参调用 selectDepartment，接口变更后 R2→R15 区间编译失败 | R2 同步修改 DeadLetterCompensationService.java:38 移除 false 参数；文件清单加入该文件；新增第 6 项 |
| 测试桩/测试调用点未同步更新导致测试编译失败 | R2 新增第 8 项：同步更新 TriageControllerTest.StubTriageService、TriageServiceImplTest 调用、DeadLetterCompensationServiceTest 模拟实现 |
| @RequestBody 使用上表述不一致 | 统一为保持 @RequestParam 不变；第 7 项明确表述 |
