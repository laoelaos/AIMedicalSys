# 详细设计（v26）

## 概述

本设计包含两个独立修复任务：C14（DeadLetterCompensationService 补偿前检查 retryCount >= maxRetryCount 迁移至 EXPIRED）和 E05（RegistrationEventListener.recover() 使用完整 RegistrationEvent 对象序列化替代手工 3 字段 HashMap）。两者不共享状态或依赖关系，可独立编码。

## 文件规划

| 文件路径 | 操作 | 职责 |
|---------|------|------|
| `AIMedical/backend/modules/consultation/src/main/java/com/aimedical/modules/consultation/service/DeadLetterCompensationService.java` | 修改 | C14：增加 retryCount >= maxRetryCount 检查，失败后迁移至 EXPIRED |
| `AIMedical/backend/modules/consultation/src/main/java/com/aimedical/modules/consultation/event/RegistrationEventListener.java` | 修改 | E05：替换手工 HashMap 为完整 RegistrationEvent 序列化 |
| `AIMedical/backend/modules/consultation/src/test/java/com/aimedical/modules/consultation/DeadLetterCompensationServiceTest.java` | 修改 | 新增 shouldExpireWhenRetryCountExceedsMax 测试 |
| `AIMedical/backend/modules/consultation/src/test/java/com/aimedical/modules/consultation/RegistrationEventListenerTest.java` | 修改 | 验证 recover() 后 eventPayload 包含全部 7 字段 |

## 类型定义

### C14 — DeadLetterCompensationService 修改

**文件路径**：`.../service/DeadLetterCompensationService.java`
**操作**：修改 `compensateDeadLetters()` 方法
**修改内容**：

方法 `compensateDeadLetters()` 控制流调整：

1. **补偿前检查**（try 块入口前）：对每个 event，先判断 `event.getRetryCount() >= event.getMaxRetryCount()`：
   - 已达上限 → `event.setState("EXPIRED")` → `deadLetterEventRepository.save(event)` → `continue`
   - 未达上限 → 进入 try 块执行补偿逻辑

2. **补偿成功**（try 块内现有位置）：`event.setState("COMPENSATED")` → `deadLetterEventRepository.save(event)`（不变）

3. **补偿失败**（catch 块）：
   - `event.setRetryCount(event.getRetryCount() + 1)`（递增，已有）
   - 判断 `event.getRetryCount() >= event.getMaxRetryCount()`（注意：此时 retryCount 已递增）
     - 已达上限 → `event.setState("EXPIRED")` → `deadLetterEventRepository.save(event)` → 继续下一次循环（不执行额外操作）
     - 未达上限 → 保持 `event.setState("FAILED")`（当前已是 FAILED）→ `deadLetterEventRepository.save(event)`
   - 注意：catch 块内 save 后无需主动 continue，循环自然进行下一轮

**行为契约**：
- 前置：`findByCompensableEvents("FAILED")` 查询条件 `retryCount < maxRetryCount` 是常规路径过滤，服务层显式检查是并发/边界安全网
- 后置：retryCount >= maxRetryCount 的事件 state 一定为 EXPIRED，不再被查询返回
- 防御场景：并发修改导致 retryCount 在查询后和补偿前被其他线程递增 → 服务层检查保证不会对已超限事件执行补偿

**依赖关系**：
- 依赖 `DeadLetterEvent.getRetryCount()` / `getMaxRetryCount()` / `setState(String)`（实体已有）
- 依赖 `deadLetterEventRepository.save(DeadLetterEvent)`（Repository 已有）
- 依赖 `ObjectMapper` 反序列化（不变）
- 依赖 `TriageService.selectDepartment(sessionId, departmentId, departmentName)`（不变）

### E05 — RegistrationEventListener.recover() 修改

**文件路径**：`.../event/RegistrationEventListener.java`
**操作**：修改 `recover(Exception e, RegistrationEvent event)` 方法
**修改内容**：

1. 移除手工构建 3 字段 HashMap 的代码（payload.put("sessionId"...)、payload.put("departmentId"...)、payload.put("departmentName"...)、`objectMapper.writeValueAsString(payload)` 调用）

2. 替换为直接序列化完整 `RegistrationEvent` 对象：
   - `String jsonPayload = objectMapper.writeValueAsString(event)`（序列化全部 7 个字段：registrationId、patientId、sessionId、departmentId、departmentName、doctorId、eventTime）

3. 保留 catch `JsonProcessingException` 的降级路径：
   - 若 `writeValueAsString(event)` 抛出 `JsonProcessingException`，回退到手工构造含至少 sessionId 的 fallback
   - 降级 payload 格式：`"{\"sessionId\":\"" + event.getSessionId() + "\"}"`（现有逻辑）
   - 注：`RegistrationEvent` 有默认无参构造 + 公共 getter，Jackson 可正常序列化

4. 方法签名、其余字段赋值（failReason、failTime、state、retryCount、maxRetryCount）、save 调用均不变

**行为契约**：
- 前置：`event` 参数为完整 `RegistrationEvent` 对象（含全部 7 字段）
- 后置：`deadLetter.getEventPayload()` 包含 7 字段 JSON（正常路径）或至少 sessionId（降级路径）
- 异常处理：仅 `JsonProcessingException` 触发降级，其他异常传播（被 Spring @Recover 机制捕获）

**依赖关系**：
- 依赖 `RegistrationEvent` 的所有 getter（Jackson 序列化自动使用）
- 依赖 `ObjectMapper.writeValueAsString(Object)`（已有）
- 依赖 `DeadLetterEvent` 构造函数和 setter（不变）

## 错误处理

### C14 catch 块错误处理
- 补偿抛异常时递增 retryCount，然后判断上限：已达 → EXPIRED，未达 → 保持 FAILED
- 与现有 catch 风格一致（`catch (Exception e)`）

### E05 序列化错误处理
- `JsonProcessingException` 降级到当前手工 fallback
- 降级路径与当前行为一致，确保至少 sessionId 不丢失

## 行为契约

### C14 compensateDeadLetters 完整流程
```
for each event in findByCompensableEvents("FAILED"):
    if retryCount >= maxRetryCount:
        state = EXPIRED, save, continue
    try:
        deserialize payload from eventPayload
        triageService.selectDepartment(...)
        state = COMPENSATED, save
    catch (Exception e):
        retryCount++
        if retryCount >= maxRetryCount:
            state = EXPIRED, save   // 防御：即使查询条件过滤，这里作为安全网
        else:
            state remains FAILED, save
```

### E05 recover 序列化流程
```
build DeadLetterEvent
try:
    jsonPayload = objectMapper.writeValueAsString(event)  // 全 7 字段
catch (JsonProcessingException ex):
    jsonPayload = fallback("{\"sessionId\":\"...\"}")     // 至少 sessionId
deadLetter.setEventPayload(jsonPayload)
...rest unchanged...
```

## 依赖关系

- `com.aimedical.modules.consultation.entity.DeadLetterEvent` — 已有
- `com.aimedical.modules.consultation.repository.DeadLetterEventRepository` — 已有
- `com.aimedical.modules.consultation.service.TriageService` — 已有
- `com.fasterxml.jackson.databind.ObjectMapper` — 已有
- `com.fasterxml.jackson.core.JsonProcessingException` — 已有
- `com.aimedical.modules.commonmodule.event.RegistrationEvent` — 已有（全 7 字段 getter）
