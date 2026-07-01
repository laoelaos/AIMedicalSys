# 详细设计（v1）

## 概述

修复 correctedChiefComplaint 完整数据链路：AI 返回→session 回写（隐式路径，C03）→TriageRecord 持久化（C19）→进程崩溃恢复（C02）→下一次 AI 调用透传（A04）。同步修复 catch 静默问题（C18）。范围限缩为 TriageRecord 加字段、Repository 加查询方法、Converter 透传/回写、catch→WARN。

## 文件规划

| 文件路径 | 操作 | 职责 |
|---------|------|------|
| `ai-api/.../dto/triage/TriageRequest.java` | 修改 | 增加 `correctedChiefComplaint` 字段，供 A04 透传 |
| `consultation/.../entity/TriageRecord.java` | 修改 | 增加 `correctedChiefComplaint` 字段（C01） |
| `consultation/.../repository/TriageRecordRepository.java` | 修改 | 增加 `findTopBySessionIdOrderByTriageTimeDesc`（C02） |
| `consultation/.../converter/TriageConverter.java` | 修改 | toAiTriageRequest 透传 cc（A04），toTriageResponse 回写 cc（C03） |
| `consultation/.../service/impl/TriageServiceImpl.java` | 修改 | saveTriageRecord 写入 cc（C19），catch→WARN（C18），triage 成功路径回写 cc（C03） |
| `consultation/.../dialogue/DialogueSessionManager.java` | 修改 | restoreSession 从 TriageRecord 恢复 cc |
| `consultation/.../dialogue/DialogueSession.java` | 确认（无需修改） | correctedChiefComplaint 字段已存在 |

## 类型定义

### TriageRequest（ai-api）
**形态**：class（DTO）
**包路径**：`com.aimedical.modules.ai.api.dto.triage`
**变更**：新增字段 `correctedChiefComplaint`
**新增字段签名**：`private String correctedChiefComplaint;`
**新增方法**：`getCorrectedChiefComplaint()` → `String` / `setCorrectedChiefComplaint(String)`

### TriageRecord
**形态**：JPA @Entity
**包路径**：`com.aimedical.modules.consultation.entity`
**变更**：新增字段 `correctedChiefComplaint`
**新增字段签名**：
```java
@Column(columnDefinition = "TEXT")
private String correctedChiefComplaint;
```
**新增方法**：`getCorrectedChiefComplaint()` → `String` / `setCorrectedChiefComplaint(String)`
**类型关系**：无新关系

### TriageRecordRepository
**形态**：interface（JPA Repository）
**包路径**：`com.aimedical.modules.consultation.repository`
**变更**：新增查询方法
**新增方法签名**：
```java
Optional<TriageRecord> findTopBySessionIdOrderByTriageTimeDesc(String sessionId);
```
**职责**：按 sessionId 查找最近一次分诊记录（triageTime 降序），用于进程崩溃后从数据库恢复 correctedChiefComplaint 快照

### TriageConverter
**形态**：class（Spring @Component）
**包路径**：`com.aimedical.modules.consultation.converter`
**变更**：两个方法修改

#### toAiTriageRequest
**原签名**：`toAiTriageRequest(DialogueCreateRequest, DialogueSession)` → `TriageRequest`
**修改**：在返回前增加一行：
```java
aiRequest.setCorrectedChiefComplaint(session.getCorrectedChiefComplaint());
```
**行为**：显式透传 session 中的 correctedChiefComplaint 到 AI 请求（A04）。仅在 session 中 cc 非空时携带，避免每次请求携带冗余 null 字段。

#### toTriageResponse
**原签名**：`toTriageResponse(AiResult<TriageResponse>, List<RecommendedDoctor>)` → `TriageResponse`
**修改**：第三个参数 `DialogueSession session`，在方法末尾增加：
```java
if (aiData != null && aiData.getCorrectedChiefComplaint() != null) {
    session.setCorrectedChiefComplaint(aiData.getCorrectedChiefComplaint());
}
```
**行为**：AI 返回 correctedChiefComplaint 非空时回写到 DialogueSession（C03 隐式路径）。

### TriageServiceImpl
**形态**：class（Spring @Service）
**包路径**：`com.aimedical.modules.consultation.service.impl`
**变更**：三处修改

#### (1) triage() — 成功路径 session 回写
**位置**：AI 返回成功分支（line 98-108），`aiData` 获取后
**修改**：在 `aiData` 获取后增加：
```java
if (aiData.getCorrectedChiefComplaint() != null) {
    session.setCorrectedChiefComplaint(aiData.getCorrectedChiefComplaint());
}
```
**注意**：此修改与 toTriageResponse 的 C03 回写路径重叠，为双重保障（triage 方法内直接回写确保即时性，Converter 内回写确保重构兼容性）。设计允许任意一处先执行，幂等写入。

#### (2) saveTriageRecord() — 写入 correctedChiefComplaint（C19）
**位置**：`record.setTriageTime(...)` 之后，`record.setRuleVersion(...)` 之前
**新增**：
```java
record.setCorrectedChiefComplaint(session.getCorrectedChiefComplaint());
```

#### (3) saveTriageRecord() — catch JsonProcessingException 改为 WARN（C18）
**位置**：line 212-213
**原代码**：
```java
} catch (JsonProcessingException e) {
    // ignore serialization errors for optional JSON fields
}
```
**修改为**：
```java
} catch (JsonProcessingException e) {
    log.warn("Failed to serialize triage record JSON fields for sessionId: {}", request.getSessionId(), e);
}
```
**前置条件**：类级别增加 `private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(TriageServiceImpl.class);`

### DialogueSessionManager
**形态**：class（Spring @Component）
**包路径**：`com.aimedical.modules.consultation.dialogue`
**变更**：restoreSession 增加 DB 恢复路径

**构造器变更**：
```java
// 新增依赖
private final TriageRecordRepository triageRecordRepository;

public DialogueSessionManager(SessionStore<String, DialogueSession> sessionStore,
                               TriageRecordRepository triageRecordRepository) {
    this.sessionStore = sessionStore;
    this.triageRecordRepository = triageRecordRepository;
}
```

**restoreSession 修改**：
```java
public DialogueSession restoreSession(String sessionId) {
    DialogueSession session = sessionStore.get(sessionId);
    if (session != null) {
        session.setLastAccessedAt(LocalDateTime.now());
        return session;
    }
    // 进程崩溃恢复：从 TriageRecord 重建 session 快照
    Optional<TriageRecord> latestRecord = triageRecordRepository
            .findTopBySessionIdOrderByTriageTimeDesc(sessionId);
    if (latestRecord.isPresent()) {
        TriageRecord record = latestRecord.get();
        session = new DialogueSession(sessionId);
        session.setCorrectedChiefComplaint(record.getCorrectedChiefComplaint());
        session.setChiefComplaint(record.getChiefComplaint());
        // 快照字段：ruleVersion / ruleSetId 从记录恢复，确保降级路径使用快照
        session.setRuleVersion(record.getRuleVersion());
        session.setRuleSetId(record.getRuleSetId());
        sessionStore.put(sessionId, session);
    }
    return session;
}
```

**行为契约**：
- session 在内存中存在 → 更新 lastAccessedAt 后直接返回（不变）
- session 在内存中不存在 + TriageRecord 存在 → 重建 session 并从记录恢复 correctedChiefComplaint（最新分诊记录的 correctedChiefComplaint）
- session 在内存中不存在 + TriageRecord 不存在 → 返回 null，调用方走 createSession（不变）

## 错误处理

本次修改不引入新错误码。各路径错误处理：
- `saveTriageRecord` 中 `JsonProcessingException` 从静默改为 WARN 日志（C18）
- `restoreSession` 中 DB 查询异常由 Spring Data JPA 运行时异常传播，事务边界外调用方处理

## 行为契约

### correctedChiefComplaint 数据流向

```
前端请求（DialogueCreateRequest.correctedChiefComplaint）
  → triage() 写入 session（已存在 line 73）
  → toAiTriageRequest() 透传到 ai-api TriageRequest（A04）
  → AI 返回 TriageResponse.correctedChiefComplaint
  → triage() 成功路径回写 session（C03）
  → toTriageResponse() 回写 session（C03，双保险）
  → saveTriageRecord() 写入 TriageRecord.correctedChiefComplaint（C19）
  → restoreSession() 从 TriageRecord 恢复（C02，进程崩溃后）
  → 下一次 toAiTriageRequest() 透传（A04 闭环）
```

### 幂等性
- session.setCorrectedChiefComplaint() 可在 triage() 和 toTriageResponse() 两处调用，先执行者写入，后执行者幂等覆盖相同值。
- TriageRecord.setCorrectedChiefComplaint() 每次 save 覆盖写入，保存最新值。
- json 序列化失败时不影响其他字段写入（catch→WARN 不抛异常）。

### 前置条件
- DialogueSession.correctedChiefComplaint 字段已存在（确认）。
- ai-api TriageResponse.correctedChiefComplaint 字段已存在（确认）。
- ai-api TriageRequest.correctedChiefComplaint 字段需新增（本设计执行）。

## 依赖关系

| 依赖方向 | 源 | 目标 |
|---------|-------|--------|
| 新增依赖 | DialogueSessionManager | TriageRecordRepository（同模块，构造器注入） |
| 新增字段 | ai-api TriageRequest | String correctedChiefComplaint（纯数据字段，无外部依赖） |
| 新增调用 | TriageServiceImpl.saveTriageRecord | session.getCorrectedChiefComplaint()（已存在的 session 方法） |
| 新增调用 | TriageConverter.toTriageResponse | session.setCorrectedChiefComplaint()（已存在的 session 方法） |

### 外部暴露接口

本次修改不增加新的外部暴露接口。TriageRecordRepository.findTopBySessionIdOrderByTriageTimeDesc 为同模块内部使用。
