# 任务指令（v26）

## 动作
NEW

## 任务描述
死信状态迁移+序列化 — C14+E05

### C14 — DeadLetterCompensationService 补偿前检查 retryCount >= maxRetryCount → EXPIRED
**文件**：`modules/consultation/src/main/java/.../service/DeadLetterCompensationService.java`

**要求**：`compensateDeadLetters()` 中，对每个从 `findByCompensableEvents("FAILED")` 获取的事件，在补偿前先判断 `retryCount >= maxRetryCount`：
1. 已达上限 → 直接设 `state=EXPIRED`，调用 `deadLetterEventRepository.save(event)`，跳过补偿（continue）
2. 未达上限 → 执行补偿逻辑（`triageService.selectDepartment(...)`）
3. 补偿成功 → 设 `state=COMPENSATED`，save
4. 补偿失败（catch exception）→ 递增 `retryCount`（当前已是），检查 `retryCount >= maxRetryCount` → 是则设 `state=EXPIRED` 并 save，跳过补偿（continue）；否则保持 `state=FAILED` 并 save

> 当前实现：`DeadLetterCompensationService.java:42-46` catch 后仅递增 retryCount，未检查上限后迁移到 EXPIRED。`findByCompensableEvents` 查询虽已包含 `retryCount < maxRetryCount` 条件，但服务层仍需显式检查作为安全网（防御并发/边界条件）。

### E05 — RegistrationEventListener.recover() 序列化完整 RegistrationEvent
**文件**：`modules/consultation/src/main/java/.../event/RegistrationEventListener.java`

**要求**：`recover()` 方法中，将手工构造的 3 字段 HashMap 替换为完整 RegistrationEvent 对象序列化：
1. 使用 `objectMapper.writeValueAsString(event)` 直接序列化完整 `RegistrationEvent` 对象（包含所有 7 个字段：registrationId, patientId, sessionId, departmentId, departmentName, doctorId, eventTime）
2. 保留 catch `JsonProcessingException` 的降级路径：若序列化失败，回退到当前的手工构造 fallback（至少包含 sessionId）

> 当前实现：`RegistrationEventListener.java:57-64` 手工构建仅含 sessionId/departmentId/departmentName 的 HashMap，缺少 registrationId/patientId/doctorId/eventTime。

## 选择理由
当前已无其他未完成 P0/P1 任务前置。R27（P14）和 R28（P11）已确认延后（详见 plan.md 排期外说明），R26 是本批次最终轮次，完成后标记 ALL_DONE。

## 任务上下文
### 死信补偿流程（DeadLetterCompensationService）
- `compensateDeadLetters()` 通过 `@Scheduled(fixedRate = 1800000)` 每 30 分钟执行
- 查询 FAILED 且 retryCount < maxRetryCount 的死信事件
- 反序列化 eventPayload 获取 sessionId/departmentId/departmentName
- 调用 `triageService.selectDepartment()` 执行补偿
- 成功→COMPENSATED，失败→retryCount+1

### RegistrationEventListener.recover 流程
- `handleRegistrationEvent` 重试耗尽后触发 `@Recover`
- 当前手工构建 3 字段 HashMap → 丢失 4 字段（registrationId/patientId/doctorId/eventTime）

### 状态迁移规则（OOD §3.1）
- `FAILED` → `COMPENSATED`（补偿成功）
- `FAILED` → `EXPIRED`（重试次数耗尽）
- `EXPIRED`/`COMPENSATED` 不再被 `findByCompensableEvents` 查询返回

## 已有代码上下文

### DeadLetterCompensationService.java （当前 48 行）
```java
@Scheduled(fixedRate = 1800000)
public void compensateDeadLetters() {
    List<DeadLetterEvent> events = deadLetterEventRepository.findByCompensableEvents("FAILED");
    for (DeadLetterEvent event : events) {
        try {
            Map<String, String> payload = objectMapper.readValue(
                    event.getEventPayload(), new TypeReference<Map<String, String>>() {});
            String sessionId = payload.get("sessionId");
            String departmentId = payload.get("departmentId");
            String departmentName = payload.get("departmentName");
            triageService.selectDepartment(sessionId, departmentId, departmentName);
            event.setState("COMPENSATED");
            deadLetterEventRepository.save(event);
        } catch (Exception e) {
            event.setRetryCount(event.getRetryCount() + 1);
            deadLetterEventRepository.save(event);
        }
    }
}
```

### RegistrationEventListener.recover()（当前 54-72 行）
```java
@Recover
public void recover(Exception e, RegistrationEvent event) {
    DeadLetterEvent deadLetter = new DeadLetterEvent();
    Map<String, String> payload = new HashMap<>();
    payload.put("sessionId", event.getSessionId());
    payload.put("departmentId", event.getDepartmentId());
    payload.put("departmentName", event.getDepartmentName());
    try {
        deadLetter.setEventPayload(objectMapper.writeValueAsString(payload));
    } catch (JsonProcessingException ex) {
        deadLetter.setEventPayload("{\"sessionId\":\"" + event.getSessionId() + "\"}");
    }
    deadLetter.setFailReason(e.getMessage());
    deadLetter.setFailTime(LocalDateTime.now());
    deadLetter.setState("FAILED");
    deadLetter.setRetryCount(0);
    deadLetter.setMaxRetryCount(3);
    deadLetterEventRepository.save(deadLetter);
}
```

### DeadLetterEventRepository.findByCompensableEvents
```java
@Query("SELECT e FROM DeadLetterEvent e WHERE e.state = :state AND e.retryCount < e.maxRetryCount")
List<DeadLetterEvent> findByCompensableEvents(@Param("state") String state);
```

### DeadLetterEvent 实体
- 字段：eventPayload (TEXT), failReason (varchar(500)), failTime, state (varchar(20), default="FAILED"), retryCount (Integer, default=0), maxRetryCount (Integer, default=3)

### RegistrationEvent 完整字段
- registrationId (Long), patientId (String), sessionId (String), departmentId (String), departmentName (String), doctorId (Long), eventTime (LocalDateTime)

### 已有测试文件
- `DeadLetterCompensationServiceTest.java`：含 4 个测试（补偿成功/失败重试计数/多事件/无事件跳过），使用 StubRepository + StubTriageService
- `RegistrationEventListenerTest.java`：含 4 个测试（事件处理委托/已决科室跳过/无记录跳过/recover 写入死信/失败原因）

## 测试要点
- **DeadLetterCompensationServiceTest**: 新增 `shouldExpireWhenRetryCountExceedsMax`（设 retryCount=3, maxRetryCount=3 → 补偿跳过, state=EXPIRED），确保 `selectDepartment` 未调用
- **RegistrationEventListenerTest**: 验证 recover() 后 eventPayload 包含 registrationId/patientId/doctorId/eventTime 等新增字段（非仅 sessionId/departmentId/departmentName）

---

## 修订说明（v26 r1）
| 审查意见 | 修改措施 |
|---------|---------|
| 范围不一致: R26 描述为"最终轮次"但计划含 R27/R28 | 明确延后 R27/R28 至排期外说明，调整"最终轮次"→"本批次最终轮次" |
| C14 catch 块未迁移 EXPIRED 状态 | 第4项补充达上限后设 state=EXPIRED 并跳过补偿 |
| R26 文件列表含 TriageServiceImpl | 已从 plan.md 中移除 |
