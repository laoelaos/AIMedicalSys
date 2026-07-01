# 详细设计（v3）

## 概述

修复 `saveTriageRecord` 的事务边界（C04 P0）、UPDATE 语义而非仅 INSERT（E02 P0）、并发控制（C20 P1/S04 P1）。涉及 4 个文件：`TriageServiceImpl.java`、`TriageRecordRepository.java`、`DialogueSessionManager.java`、`DialogueSession.java`。额外同步修改 `RegistrationEventListener.java`（`findBySessionId` 事务上下文缺失）。

## 文件规划

| 文件路径 | 操作 | 职责 |
|---------|------|------|
| `consultation/repository/TriageRecordRepository.java` | 修改 | `findBySessionId` 增加 `@Lock(PESSIMISTIC_WRITE)` + `@Transactional(Propagation.MANDATORY)` |
| `consultation/service/impl/TriageServiceImpl.java` | 修改 | `saveTriageRecord` 改为 `TransactionTemplate` 编程式事务 + 先查后改/增；`selectDepartment` 添加 `@Transactional` |
| `consultation/dialogue/DialogueSessionManager.java` | 修改 | `createSession` 改为 `synchronized` + `putIfAbsent` 语义 |
| `consultation/dialogue/DialogueSession.java` | 修改 | 所有 getter/setter 添加 `synchronized`；`additionalResponses` 改为 `CopyOnWriteArrayList`；`aiFailCount`/`roundCount` 改为 `AtomicInteger` |
| `consultation/event/RegistrationEventListener.java` | 修改 | `handleRegistrationEvent` 添加 `@Transactional` |

## 类型定义

### TriageRecordRepository

**形态**：interface（Spring Data JPA Repository）
**包路径**：`com.aimedical.modules.consultation.repository`

**变更方法**：

```java
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Lock(LockModeType.PESSIMISTIC_WRITE)
@Transactional(propagation = Propagation.MANDATORY)
Optional<TriageRecord> findBySessionId(String sessionId);
```

**职责**：悲观写锁确保同一 sessionId 的并发写入串行化；`MANDATORY` 传播强制调用方必须处于事务上下文中，防止无事务调用。

**未变更方法**（保持原有无锁、无 `@Transactional` 注解）：
- `findTopByPatientIdOrderByTriageTimeDesc` — 只读查询，不涉及并发写
- `findTopBySessionIdOrderByTriageTimeDesc` — 只读查询
- `findBySessionIdIn` — 批量只读查询

---

### TriageServiceImpl

**形态**：class（Spring `@Service`）
**包路径**：`com.aimedical.modules.consultation.service.impl`

#### 新增依赖

```java
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.annotation.Transactional;
```

#### 构造器变更

```java
// 新增字段
private final TransactionTemplate transactionTemplate;

// 构造器新增参数（注入 PlatformTransactionManager，构造器内创建 TransactionTemplate）
public TriageServiceImpl(/* ... 已有 8 参 ... */,
                          PlatformTransactionManager transactionManager) {
    // ... 已有赋值 ...
    this.transactionTemplate = new TransactionTemplate(transactionManager);
}
```

**说明**：Spring Boot `TransactionAutoConfiguration` 不自动配置 `TransactionTemplate` Bean，但自动配置 `PlatformTransactionManager` Bean。因此注入 `PlatformTransactionManager` 并在构造器内 `new TransactionTemplate(transactionManager)`，此为 Spring 官方推荐做法。

#### saveTriageRecord 方法重写

```java
private void saveTriageRecord(DialogueCreateRequest request, DialogueSession session,
                               List<RecommendedDepartment> departments, List<RecommendedDoctor> doctors,
                               AiResult<TriageResponse> aiResult,
                               com.aimedical.modules.consultation.dto.TriageResponse response) {
    // 前置操作：JSON 序列化（事务外）
    String departmentsJson = null;
    String doctorsJson = null;
    try {
        if (departments != null && !departments.isEmpty()) {
            departmentsJson = objectMapper.writeValueAsString(departments);
        }
        if (doctors != null && !doctors.isEmpty()) {
            doctorsJson = objectMapper.writeValueAsString(doctors);
        }
    } catch (JsonProcessingException e) {
        log.warn("Failed to serialize triage record JSON fields for sessionId: {}", request.getSessionId(), e);
    }

    // 持久化操作：事务内
    String finalDepartmentsJson = departmentsJson;
    String finalDoctorsJson = doctorsJson;
    transactionTemplate.execute(status -> {
        // 先查：是否存在已有记录
        Optional<TriageRecord> existing = triageRecordRepository.findBySessionId(request.getSessionId());

        TriageRecord record;
        if (existing.isPresent()) {
            // UPDATE 路径：更新已有记录的字段
            record = existing.get();
        } else {
            // INSERT 路径：创建新记录
            record = new TriageRecord();
        }

        record.setSessionId(request.getSessionId());
        record.setPatientId(request.getPatientId());
        record.setChiefComplaint(request.getChiefComplaint());
        record.setTriageTime(LocalDateTime.now());
        record.setCorrectedChiefComplaint(session.getCorrectedChiefComplaint());
        record.setRuleVersion(request.getRuleVersion());
        record.setRuleSetId(request.getRuleSetId());
        record.setConfidence(response.getConfidence());

        if (aiResult != null && aiResult.isDegraded()) {
            record.setDegraded(true);
        } else {
            record.setDegraded(false);
        }

        if (finalDepartmentsJson != null) {
            if (aiResult != null && aiResult.isDegraded()) {
                record.setRuleMatchedDepartments(finalDepartmentsJson);
            } else {
                record.setAiRecommendedDepartments(finalDepartmentsJson);
            }
        }
        if (finalDoctorsJson != null) {
            record.setRecommendedDoctors(finalDoctorsJson);
        }

        triageRecordRepository.save(record);
        return null;
    });
}
```

**行为说明**：
- JSON 序列化等前置操作在事务外执行，减少事务持有时间
- `TransactionTemplate.execute()` 包围 `findBySessionId` + 字段赋值 + `save`，三者在一个事务内
- 存在时更新已有记录（JPA merge 语义），不存在时新建（JPA persist 语义）
- 乐观锁/悲观锁：`findBySessionId` 的 `@Lock(PESSIMISTIC_WRITE)` 在事务内生效，防止并发覆盖

#### selectDepartment 方法变更

```java
@Override
@Transactional
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

**变更说明**：新增 `@Transactional` 注解，为 `findBySessionId(MANDATORY)` 及后续 `save` 提供事务上下文。

---

### DialogueSession

**形态**：class（Plain POJO）
**包路径**：`com.aimedical.modules.consultation.dialogue`

**新增 imports**：
```java
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
```

**字段变更**：
```java
private List<AdditionalResponse> additionalResponses = new CopyOnWriteArrayList<>();
private AtomicInteger aiFailCount = new AtomicInteger(0);
private AtomicInteger roundCount = new AtomicInteger(0);
```

**方法变更（synchronized + AtomicInteger + CopyOnWriteArrayList）**：

```java
public synchronized List<AdditionalResponse> getAdditionalResponses() {
    return additionalResponses;
}

public synchronized void setAdditionalResponses(List<AdditionalResponse> additionalResponses) {
    this.additionalResponses = additionalResponses instanceof CopyOnWriteArrayList
            ? (CopyOnWriteArrayList<AdditionalResponse>) additionalResponses
            : new CopyOnWriteArrayList<>(additionalResponses);
}

public int getAiFailCount() {
    return aiFailCount.get();
}

public void setAiFailCount(int aiFailCount) {
    this.aiFailCount.set(aiFailCount);
}

public int getRoundCount() {
    return roundCount.get();
}

public void setRoundCount(int roundCount) {
    this.roundCount.set(roundCount);
}
```

**其余 getter/setter 添加 `synchronized`**：
```java
public synchronized String getSessionId() { return sessionId; }
public synchronized void setSessionId(String sessionId) { this.sessionId = sessionId; }
public synchronized String getChiefComplaint() { return chiefComplaint; }
public synchronized void setChiefComplaint(String chiefComplaint) { this.chiefComplaint = chiefComplaint; }
public synchronized String getCorrectedChiefComplaint() { return correctedChiefComplaint; }
public synchronized void setCorrectedChiefComplaint(String correctedChiefComplaint) { this.correctedChiefComplaint = correctedChiefComplaint; }
public synchronized String getRuleVersion() { return ruleVersion; }
public synchronized void setRuleVersion(String ruleVersion) { this.ruleVersion = ruleVersion; }
public synchronized String getRuleSetId() { return ruleSetId; }
public synchronized void setRuleSetId(String ruleSetId) { this.ruleSetId = ruleSetId; }
public synchronized LocalDateTime getCreatedAt() { return createdAt; }
public synchronized void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
public synchronized LocalDateTime getLastAccessedAt() { return lastAccessedAt; }
public synchronized void setLastAccessedAt(LocalDateTime lastAccessedAt) { this.lastAccessedAt = lastAccessedAt; }
```

---

### DialogueSessionManager

**形态**：class（Spring `@Component`）
**包路径**：`com.aimedical.modules.consultation.dialogue`

**增加 imports**：
```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
```

**新增字段**：
```java
private static final Logger log = LoggerFactory.getLogger(DialogueSessionManager.class);
```

**createSession 方法重写**（`synchronized` + `containsKey`，因 `SessionStore.put` 返回 `void`）：

```java
public synchronized DialogueSession createSession(String sessionId) {
    if (sessionStore.containsKey(sessionId)) {
        log.warn("Session already exists for sessionId: {}, returning existing session", sessionId);
        return sessionStore.get(sessionId);
    }
    DialogueSession session = new DialogueSession(sessionId);
    sessionStore.put(sessionId, session);
    return session;
}
```

**关于 `synchronized` 方案的说明**：
- `SessionStore` 接口（`com.aimedical.modules.commonmodule.store.SessionStore`）的 `put(K, V)` 返回 `void`，不遵循 `Map.put` 返回旧值的语义，故无法通过返回值判断是否已存在
- 采用 `synchronized` + `containsKey` 组合实现并发安全的"不存在则写入"语义
- `restoreSession` 方法中也有 `sessionStore.put(sessionId, session)` 调用（line 52），该调用在 `restoreSession` 中始终覆盖，不影响 `createSession` 的并发语义（`restoreSession` 不会覆盖 `createSession` 刚创建的 session，因为它只在缓存未命中时写入）
- 若未来 `SessionStore` 接口扩展 `putIfAbsent(K, V)` 方法，可替换为 lock-free 方案

---

### RegistrationEventListener（同步修改）

**形态**：class（Spring `@Component`）
**包路径**：`com.aimedical.modules.consultation.event`

**变更**：`handleRegistrationEvent` 添加 `@Transactional` 注解

```java
import org.springframework.transaction.annotation.Transactional;

@EventListener
@Transactional
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

**说明**：`@Transactional` 提供事务上下文，使 `findBySessionId(MANDATORY)` 正常工作；`selectDepartment(REQUIRED)` 会加入同一事务。

## 错误处理

| 场景 | 错误类型 | 传播方式 |
|------|---------|---------|
| `findBySessionId` 在事务外调用 | `IllegalTransactionStateException` | Spring 事务管理器抛出，阻止无事务调用 |
| `saveTriageRecord` 事务内 `findBySessionId` 发现多条记录（违反唯一约束） | `IncorrectResultSizeDataAccessException` | Spring Data JPA 抛出，事务回滚 |
| `selectDepartment` 无对应 TriageRecord | `BusinessException(TRIAGE_SESSION_NOT_FOUND)` | 向上传播至 Controller，由 GlobalExceptionHandler 处理 |
| 并发时 PESSIMISTIC_WRITE 锁等待超时 | `PessimisticLockException` | 事务回滚，由调用方决定重试策略 |
| `handleRegistrationEvent` 事务内异常 | `DataAccessException` / `TimeoutException` 触发 @Retryable 重试 | 重试耗尽后进入 @Recover 死信队列 |

## 行为契约

### saveTriageRecord 事务边界
- **前置条件**：`request`、`session`、`departments`、`doctors`、`aiResult`、`response` 参数已就绪
- **事务范围**：仅包围 `findBySessionId` + 字段赋值 + `save`，不包围 JSON 序列化
- **UPDATE 语义**：若 sessionId 已有记录，更新所有 triage 相关字段（不覆盖 `finalDepartmentId`/`finalDepartmentName`，因 `saveTriageRecord` 不操作这两个字段）；若不存在则新建
- **后置条件**：数据库中存在该 sessionId 的 TriageRecord（新建或更新）

### findBySessionId 悲观锁
- `@Lock(PESSIMISTIC_WRITE)` 在事务内获取行级排他锁，同一 sessionId 的并发写入串行化
- `@Transactional(Propagation.MANDATORY)` 确保仅在事务内调用
- **调用路径**：
  - `saveTriageRecord`（TransactionTemplate 事务）→ `findBySessionId` → 正常
  - `selectDepartment`（@Transactional 事务）→ `findBySessionId` → 正常
  - `RegistrationEventListener.handleRegistrationEvent`（@Transactional 事务）→ `findBySessionId` → 正常
- 三条调用路径均已提供事务上下文，无遗漏

### selectDepartment
- `@Transactional` 提供事务上下文
- 前置条件：sessionId 对应 TriageRecord 必须存在
- 始终覆盖 `finalDepartmentId`/`finalDepartmentName`
- 后置条件：记录已保存

### RegistrationEventListener 事务对齐
- `@Transactional` 使整个事件处理在一个事务内执行
- `findBySessionId(MANDATORY)` + `selectDepartment(REQUIRED)` 均在事务内
- `@Retryable` 与 `@Transactional` 共存：事务回滚后重试，新重试启动新事务

### DialogueSession 并发安全
- **集合字段**：`additionalResponses` 初始化为 `CopyOnWriteArrayList`，读操作无锁、写操作复制
- **计数器字段**：`aiFailCount`/`roundCount` 使用 `AtomicInteger`，无锁原子操作
- **其余字段**：getter/setter 添加 `synchronized`，保证可见性和互斥
- **特别说明**：`triage()` 方法中 `session.getAdditionalResponses() == null` 的死代码检查（line 76）在 `additionalResponses` 改为声明时初始化后变为死代码，但保留无运行时影响，编码阶段可考虑清理

### createSession 并发安全
- 因 `SessionStore.put(K, V)` 返回 `void`，无法用返回值判断是否存在
- 采用 `synchronized` + `containsKey` 组合实现并发安全的"不存在则写入"语义
- 已存在时返回已有 session 而非覆盖，记录 WARN 日志

## 依赖关系

| 依赖方向 | 源 | 目标 |
|---------|-------|--------|
| 新增注入 | TriageServiceImpl | PlatformTransactionManager（Spring Boot 自动配置 Bean），构造器内创建 TransactionTemplate |
| 新增注解（无新依赖） | TriageServiceImpl | @Transactional |
| 新增注解（无新依赖） | TriageRecordRepository | @Lock、@Transactional(Propagation.MANDATORY) |
| 新增依赖 | RegistrationEventListener | @Transactional |
| 新增依赖 | DialogueSession | CopyOnWriteArrayList、AtomicInteger |

### 未引入的外部依赖
- 无需额外的 Maven/Gradle 包：`PlatformTransactionManager` + `TransactionTemplate` 来自 `spring-tx`（已有），`@Lock`/`LockModeType` 来自 `spring-data-jpa` + `jakarta.persistence`（已有），`CopyOnWriteArrayList`/`AtomicInteger` 来自 JDK 标准库（已有）

## 修订说明（v3 r1）
| 审查意见 | 修改措施 |
|---------|---------|
| [严重] RegistrationEventListener 事务缺失 — `handleRegistrationEvent` 调用 `findBySessionId(MANDATORY)` 无事务上下文 | 采纳方案 A：为 `handleRegistrationEvent` 添加 `@Transactional` 注解，提供事务上下文；同时在行为契约中更新调用路径说明，明确三条调用路径均已覆盖 |
| [严重] RegistrationEventListener 事务缺失 — 设计断言"仅两条路径"实际存在第三条 | 行为契约章节更新 `findBySessionId` 悲观锁调用路径列表，将 `RegistrationEventListener` 路径纳入，并验证三条路径均已有事务上下文 |
| [一般] TransactionTemplate 注入方式偏离任务要求 — 设计使用 `PlatformTransactionManager` + `new TransactionTemplate()` | 改为直接通过构造器注入 `TransactionTemplate`（Spring Boot 自动配置的 Bean），移除 `PlatformTransactionManager` 相关设计 |
| [轻微] `synchronized` 粗粒度锁影响并发性能 | 保留 `createSession` 当前设计（无 `putIfAbsent` 方法的 `SessionStore` 下，无法避免 `synchronized` 或等效锁）。确认不影响功能正确性，编码阶段可加注 TODO 说明后续若 `SessionStore` 接口扩展 `putIfAbsent` 可优化。不加 `synchronized` 覆盖是因为当前任务范围已明确 |
| [轻微] `triage()` 中 `session.getAdditionalResponses() == null` 死代码 | 行为契约中注明死代码存在但无运行时影响。此代码不在本次变更范围内，不做修改 |

## 修订说明（v3 r2）
| 审查意见 | 修改措施 |
|---------|---------|
| [严重] `SessionStore.put` 返回 `void`，`createSession` 代码无法编译 | 改为 `synchronized` + `containsKey` 组合方案：先 `containsKey` 检查，已存在则返回已有 session 并记录 WARN，不存在则 `new` + `put`。`restoreSession` 中的 `put` 调用不受影响（覆盖语义，非并发场景）。移除原依赖 `put` 返回值的错误代码 |
| [严重] `TransactionTemplate` 非 Spring Boot 自动配置 Bean，构造器注入启动失败 | 回退至正确做法：注入 `PlatformTransactionManager`（Spring Boot 自动配置），在构造器内 `new TransactionTemplate(transactionManager)`。更新依赖关系表、外部依赖说明及构造器签名 |
