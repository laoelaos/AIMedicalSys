# 任务指令（v3）

## 动作
NEW

## 任务描述
修复 saveTriageRecord 的事务边界（C04 P0）、UPDATE 语义而非仅 INSERT（E02 P0）、并发控制（C20 P1/S04 P1）。涉及 4 个文件：TriageServiceImpl.java, TriageRecordRepository.java, DialogueSessionManager.java, DialogueSession.java。

具体变更要求：

### 1. TriageServiceImpl.saveTriageRecord — 编程式事务 + UPDATE 路径（C04/E02）
- 移除 `triageRecordRepository.save(record)` 的直接调用，改用 `TransactionTemplate` 编程式事务包围持久化操作
- 先调用 `triageRecordRepository.findBySessionId(sessionId)` 查询是否存在
  - **存在时**：更新已有记录的字段值后 `save()`（JPA merge 语义）
  - **不存在时**：直接 `save()` 新建
- 注入 `TransactionTemplate transactionTemplate`（可通过构造器注入或 `@Bean`，同模块已有 `TransactionTemplate` 建议使用构造器注入）
- 事务范围仅包围 `findBySessionId` + `save` 操作，不包围 JSON 序列化等前置操作

### 2. TriageRecordRepository — 悲观锁（C20）
- 为 `findBySessionId(String sessionId)` 增加 `@Lock(PESSIMISTIC_WRITE)` 注解
- 同时增加 `@Transactional(propagation = Propagation.MANDATORY)` 确保在事务内调用
- 保持 `findTopBySessionIdOrderByTriageTimeDesc` 和 `findTopByPatientIdOrderByTriageTimeDesc` 不变（不涉及并发写）

### 3. DialogueSessionManager.createSession — putIfAbsent（S04）
- **当前**：`sessionStore.put(sessionId, session)`
- **改为**：`sessionStore.putIfAbsent(sessionId, session)` 或等效并发安全操作
- 若 `putIfAbsent` 返回非空（即已有 session），需处理已存在情况（可记录 WARN 日志或返回已有 session，避免覆盖）

### 4. DialogueSession — 并发安全保护（C20）
- 对 `DialogueSession` 的所有 setter/getter/内部集合字段(`additionalResponses`) 进行并发安全保护
- 方法：将 `additionalResponses` 字段（`List<AdditionalResponse>`）改为 `CopyOnWriteArrayList` 或使用 `synchronized` 方法包装
- 或使用 `Collections.synchronizedList()` + 对原子字段使用 `AtomicInteger`（如 `aiFailCount`, `roundCount`）
- 简单方案：在关键 getter/setter 上添加 `synchronized` 关键字，对集合字段使用 `CopyOnWriteArrayList`

## 选择理由
C04(P0)+E02(P0)+S04(P1)+C20(P1) 四者耦合于同一并发控制群组。依 OOD §3.1 推荐方案：TransactionTemplate 编程式事务 + @Lock(PESSIMISTIC_WRITE) 悲观锁并发控制。R2 已完成 selectDepartment 接口修复，此轮修复核心数据写入链路的事务安全。R1 已添加 correctedChiefComplaint 字段和 findTopBySessionId 查询方法，R3 不依赖 R1 的数据链路变更。

## 任务上下文
- **OOD §3.1（并发控制）**：推荐 `TransactionTemplate` 编程式事务 + 悲观锁防止并发覆盖；`saveTriageRecord` 的持久化操作为写热点，需事务保护
- **C04（P0）**：`saveTriageRecord` 无事务边界，JSON 序列化与 DB 写入未隔离
- **E02（P0）**：对同一 sessionId 多次调用时，`save()` 始终 INSERT 而非 UPDATE，违反 TriageRecord.sessionId 唯一约束（当前 `unique = true`），导致 `DataIntegrityViolationException`
- **S04（P1）**：`DialogueSessionManager.createSession` 使用 `put()` 而非 `putIfAbsent()`，并发下可能覆盖已有 session
- **C20（P1）**：`DialogueSession` 非线程安全，多线程访问共享 POJO 可能导致数据竞争

## 已有代码上下文

### TriageServiceImpl.java (saveTriageRecord, line 184-221)
```java
private void saveTriageRecord(DialogueCreateRequest request, DialogueSession session,
                               List<RecommendedDepartment> departments, List<RecommendedDoctor> doctors,
                               AiResult<TriageResponse> aiResult,
                               com.aimedical.modules.consultation.dto.TriageResponse response) {
    TriageRecord record = new TriageRecord();
    record.setSessionId(request.getSessionId());
    record.setPatientId(request.getPatientId());
    record.setChiefComplaint(request.getChiefComplaint());
    record.setTriageTime(LocalDateTime.now());
    record.setCorrectedChiefComplaint(session.getCorrectedChiefComplaint());
    record.setRuleVersion(request.getRuleVersion());
    record.setRuleSetId(request.getRuleSetId());
    record.setConfidence(response.getConfidence());
    // ... degraded/departments/doctors JSON serialization with try/catch ...
    log.warn("Failed to serialize triage record JSON fields for sessionId: {}", request.getSessionId(), e);
    // ...
    triageRecordRepository.save(record);  // ← always INSERT, no transaction
}
```

**当前 imports**（line 1-32）：已有 `BusinessException`、`TriageErrorCode`、`TriageRecordRepository`、`ObjectMapper`、`LocalDateTime` 等。**缺少** `TransactionTemplate`、`@Transactional`。

### TriageRecordRepository.java
```java
@Repository
public interface TriageRecordRepository extends JpaRepository<TriageRecord, Long> {
    Optional<TriageRecord> findBySessionId(String sessionId);  // ← 需加 @Lock
    Optional<TriageRecord> findTopByPatientIdOrderByTriageTimeDesc(String patientId);
    Optional<TriageRecord> findTopBySessionIdOrderByTriageTimeDesc(String sessionId);
    List<TriageRecord> findBySessionIdIn(List<String> sessionIds);
}
```

**当前 imports**：`JpaRepository`、`Repository`、`List`、`Optional`。**缺省** `@Lock`、`LockModeType`、`@Transactional`、`Propagation`。

### DialogueSessionManager.java (createSession, line 27-31)
```java
public DialogueSession createSession(String sessionId) {
    DialogueSession session = new DialogueSession(sessionId);
    sessionStore.put(sessionId, session);  // ← 应改为 putIfAbsent
    return session;
}
```

### DialogueSession.java（所有字段非线程安全）
```java
public class DialogueSession {
    private String sessionId;
    private String chiefComplaint;
    private String correctedChiefComplaint;
    private List<AdditionalResponse> additionalResponses;  // ← 非线程安全集合
    private int aiFailCount;  // ← 非原子 int
    private int roundCount;   // ← 非原子 int
    private String ruleVersion;
    private String ruleSetId;
    private LocalDateTime createdAt;
    private LocalDateTime lastAccessedAt;
    // ... plain getters/setters, no synchronization
}
```

## 变更文件清单
| 修改级别 | 文件路径 |
|---------|---------|
| 修改 | `consultation/.../service/impl/TriageServiceImpl.java` |
| 修改 | `consultation/.../repository/TriageRecordRepository.java` |
| 修改 | `consultation/.../dialogue/DialogueSessionManager.java` |
| 修改 | `consultation/.../dialogue/DialogueSession.java` |
| 验证（运行测试） | 全量回归确保 517+ 测试通过 |
