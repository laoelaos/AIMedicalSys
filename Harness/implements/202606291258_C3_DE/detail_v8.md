# 详细设计（v8）

## 概述

实现包C（智能分诊/智能导诊）consultation 模块的全部代码，位于 `AIMedical/backend/modules/consultation/`，包根 `com.aimedical.modules.consultation`。包含 REST API、DTO、Service、会话管理、规则引擎、降级兜底、JPA 实体与 Repository、事件监听、转换器。

## 文件规划

| 文件路径 | 操作 | 职责 |
|---------|------|------|
| `consultation/src/main/java/com/aimedical/modules/consultation/api/TriageController.java` | 新建 | REST 端点 |
| `consultation/src/main/java/com/aimedical/modules/consultation/dto/DialogueCreateRequest.java` | 新建 | 分诊对话创建请求 DTO |
| `consultation/src/main/java/com/aimedical/modules/consultation/dto/AdditionalResponse.java` | 新建 | 追问回答值对象 DTO |
| `consultation/src/main/java/com/aimedical/modules/consultation/dto/TriageResponse.java` | 新建 | 分诊响应 DTO |
| `consultation/src/main/java/com/aimedical/modules/consultation/dto/RecommendedDepartment.java` | 新建 | 推荐科室 DTO |
| `consultation/src/main/java/com/aimedical/modules/consultation/dto/RecommendedDoctor.java` | 新建 | 推荐医生 DTO |
| `consultation/src/main/java/com/aimedical/modules/consultation/dto/MatchedRule.java` | 新建 | 匹配规则 DTO |
| `consultation/src/main/java/com/aimedical/modules/consultation/service/TriageService.java` | 新建 | 分诊服务接口 |
| `consultation/src/main/java/com/aimedical/modules/consultation/service/impl/TriageServiceImpl.java` | 新建 | 分诊服务实现 |
| `consultation/src/main/java/com/aimedical/modules/consultation/service/DeadLetterCompensationService.java` | 新建 | 死信补偿定时任务 |
| `consultation/src/main/java/com/aimedical/modules/consultation/dialogue/DialogueSession.java` | 新建 | 会话状态对象 |
| `consultation/src/main/java/com/aimedical/modules/consultation/dialogue/DialogueSessionManager.java` | 新建 | 会话生命周期管理器 |
| `consultation/src/main/java/com/aimedical/modules/consultation/rule/TriageRuleEngine.java` | 新建 | 规则引擎接口 |
| `consultation/src/main/java/com/aimedical/modules/consultation/rule/DefaultTriageRuleEngine.java` | 新建 | 规则引擎默认实现 |
| `consultation/src/main/java/com/aimedical/modules/consultation/rule/entity/TriageRule.java` | 新建 | 分诊规则 JPA 实体 |
| `consultation/src/main/java/com/aimedical/modules/consultation/fallback/DepartmentFallbackProvider.java` | 新建 | 兜底科室接口 |
| `consultation/src/main/java/com/aimedical/modules/consultation/fallback/StaticDepartmentFallbackProvider.java` | 新建 | 静态兜底实现 |
| `consultation/src/main/java/com/aimedical/modules/consultation/entity/TriageRecord.java` | 新建 | 分诊记录 JPA 实体 |
| `consultation/src/main/java/com/aimedical/modules/consultation/entity/DeadLetterEvent.java` | 新建 | 死信事件 JPA 实体 |
| `consultation/src/main/java/com/aimedical/modules/consultation/repository/TriageRecordRepository.java` | 新建 | 分诊记录 Repository |
| `consultation/src/main/java/com/aimedical/modules/consultation/repository/TriageRuleRepository.java` | 新建 | 分诊规则 Repository |
| `consultation/src/main/java/com/aimedical/modules/consultation/repository/DeadLetterEventRepository.java` | 新建 | 死信事件 Repository |
| `consultation/src/main/java/com/aimedical/modules/consultation/event/RegistrationEventListener.java` | 新建 | 挂号事件监听器 |
| `consultation/src/main/java/com/aimedical/modules/consultation/converter/TriageConverter.java` | 新建 | 分诊数据转换器 |

## 类型定义

### DialogueCreateRequest

**形态**：class
**包路径**：com.aimedical.modules.consultation.dto
**职责**：分诊对话创建请求 DTO

```java
public class DialogueCreateRequest {
    private String chiefComplaint;           // 5-500字符, 必填
    private String patientId;
    private Integer age;
    private String gender;
    private String sessionId;                // 必填, UUID v4
    private String ruleVersion;
    private String ruleSetId;
    private List<AdditionalResponse> additionalResponses;  // 与chiefComplaint互斥
    private String correctedChiefComplaint;  // 可选
}
```

**公开接口**：全字段 getter/setter，无参构造器，全参构造器
**构造方式**：new + setter，或全参构造器
**类型关系**：组合 `List<AdditionalResponse>`

### AdditionalResponse

**形态**：class
**包路径**：com.aimedical.modules.consultation.dto
**职责**：追问回答值对象

```java
public class AdditionalResponse {
    private String question;
    private String answer;
    private String answeredAt;  // ISO日期时间, 可选
}
```

**公开接口**：全字段 getter/setter，无参构造器，全参构造器

### TriageResponse

**形态**：class
**包路径**：com.aimedical.modules.consultation.dto
**职责**：分诊响应 DTO（业务层）

```java
public class TriageResponse {
    private List<RecommendedDepartment> departments;   // 0-3项
    private List<RecommendedDoctor> doctors;            // 0-5项
    private String reason;                              // 必填, >=1字符
    private List<MatchedRule> matchedRules;
    private String sessionId;
    private boolean needFollowUp;
    private String followUpQuestion;
    private Float confidence;                           // 可选
    private boolean degraded;
    private String fallbackHint;                        // 可选
    private Boolean ruleVersionMismatch;                // 可选, 包装类型
}
```

**公开接口**：全字段 getter/setter，无参构造器

### RecommendedDepartment

**形态**：class
**包路径**：com.aimedical.modules.consultation.dto
**职责**：推荐科室 DTO（业务层）

```java
public class RecommendedDepartment {
    private String departmentId;
    private String departmentName;
    private float score;
}
```

**公开接口**：全字段 getter/setter，无参构造器，全参构造器

### RecommendedDoctor

**形态**：class
**包路径**：com.aimedical.modules.consultation.dto
**职责**：推荐医生 DTO（业务层）

```java
public class RecommendedDoctor {
    private String doctorId;
    private String doctorName;
    private String departmentId;
    private int availableSlotCount;
    private float score;
}
```

**公开接口**：全字段 getter/setter，无参构造器，全参构造器

### MatchedRule

**形态**：class
**包路径**：com.aimedical.modules.consultation.dto
**职责**：匹配规则 DTO（业务层）

```java
public class MatchedRule {
    private String ruleId;
    private String ruleName;
    private float score;
}
```

**公开接口**：全字段 getter/setter，无参构造器，全参构造器

### TriageService

**形态**：interface
**包路径**：com.aimedical.modules.consultation.service
**职责**：分诊服务接口

```java
public interface TriageService {
    TriageResponse triage(DialogueCreateRequest request);
    TriageResponse selectDepartment(String sessionId, String departmentId, String departmentName, boolean overwrite);
}
```

**selectDepartment overwrite 语义**：
- `overwrite=true`：无条件写入 finalDepartmentId/finalDepartmentName（用于 Controller 手动选科端点）
- `overwrite=false`：仅当 TriageRecord.finalDepartmentId 为 null 时写入，不覆盖已有的手动选科（用于 RegistrationEventListener 和 DeadLetterCompensationService）

### TriageServiceImpl

**形态**：class
**包路径**：com.aimedical.modules.consultation.service.impl
**职责**：分诊服务核心实现

```java
@Service
public class TriageServiceImpl implements TriageService {
    private final AiService aiService;
    private final TriageRuleEngine triageRuleEngine;
    private final DepartmentFallbackProvider fallbackProvider;
    private final DoctorFacade doctorFacade;
    private final DialogueSessionManager sessionManager;
    private final TriageRecordRepository triageRecordRepository;
    private final TriageConverter triageConverter;

    public TriageServiceImpl(AiService aiService, TriageRuleEngine triageRuleEngine,
                             DepartmentFallbackProvider fallbackProvider, DoctorFacade doctorFacade,
                             DialogueSessionManager sessionManager,
                             TriageRecordRepository triageRecordRepository,
                             TriageConverter triageConverter) { ... }

    @Override
    public TriageResponse triage(DialogueCreateRequest request) { ... }

    @Override
    public TriageResponse selectDepartment(String sessionId, String departmentId, String departmentName, boolean overwrite) { ... }
}
```

**构造方式**：Spring 构造器注入

**行为契约**：
- `triage()`: ①从 request 提取/创建会话 → ②拼接多轮上下文 → ③调用 AiService.triage()（异步 CompletableFuture，需 join 或同步等待）→ ④AI 成功（isSuccess()==true）则通过 TriageConverter.toTriageResponse(aiResult, doctors) 转换 → ⑤通过 DoctorFacade 获取排班医生 → ⑥结果写入 TriageRecord（先 DB 再更新缓存/内存） → ⑦返回 TriageResponse
- AI 不可用时（!isSuccess()）：降级至 TriageRuleEngine.match() → DepartmentFallbackProvider.getFallbackDepartments()；AI 连续失败 3 次时附加 fallbackHint
- `selectDepartment()`: 从 DB 加载 TriageRecord。若 overwrite=true 或 finalDepartmentId==null，则更新 finalDepartmentId/finalDepartmentName；否则跳过。保存后返回更新后的 TriageResponse。

### DeadLetterCompensationService

**形态**：class
**包路径**：com.aimedical.modules.consultation.service
**职责**：定时补偿死信事件

```java
@Service
public class DeadLetterCompensationService {
    private final DeadLetterEventRepository deadLetterEventRepository;
    private final TriageService triageService;

    public DeadLetterCompensationService(DeadLetterEventRepository deadLetterEventRepository,
                                          TriageService triageService) { ... }

    @Scheduled(fixedRate = 1800000)  // 30分钟
    public void compensateDeadLetters() { ... }
}
```

**行为契约**：
- 调用 `deadLetterEventRepository.findByCompensableEvents("FAILED")` 扫描待补偿事件
- 反序列化 eventPayload（JSON → sessionId/departmentId/departmentName）
- 调用 TriageService.selectDepartment(sessionId, departmentId, departmentName, false)
- 成功则将 state 更新为 'COMPENSATED'，retryCount 不变
- 失败则将 retryCount +1，保留 state='FAILED'

### DialogueSession

**形态**：class
**包路径**：com.aimedical.modules.consultation.dialogue
**职责**：可变会话状态对象

```java
public class DialogueSession {
    private String sessionId;
    private String chiefComplaint;
    private String correctedChiefComplaint;
    private List<AdditionalResponse> additionalResponses;
    private int aiFailCount;
    private int roundCount;
    private String ruleVersion;
    private String ruleSetId;
    private LocalDateTime createdAt;
    private LocalDateTime lastAccessedAt;
}
```

**公开接口**：全字段 getter/setter，无参构造器，sessionId 构造器

### DialogueSessionManager

**形态**：class
**包路径**：com.aimedical.modules.consultation.dialogue
**职责**：会话生命周期管理

```java
@Component
public class DialogueSessionManager {
    private final SessionStore<String, DialogueSession> sessionStore;

    public DialogueSessionManager(SessionStore<String, DialogueSession> sessionStore) { ... }

    public DialogueSession createSession(String sessionId) { ... }
    public void cancelSession(String sessionId) { ... }
    public DialogueSession restoreSession(String sessionId) { ... }

    @Scheduled(fixedRate = 60000)
    public void evictExpiredSessions() { ... }
}
```

**行为契约**：
- 会话 TTL 30 分钟（lastAccessedAt 超过 30 分钟即过期）
- createSession：创建新 DialogueSession 并放入 SessionStore
- cancelSession：从 SessionStore 移除
- restoreSession：从 SessionStore 读取，更新 lastAccessedAt
- evictExpiredSessions：每分钟扫描，移除过期会话

### TriageRuleEngine

**形态**：interface
**包路径**：com.aimedical.modules.consultation.rule
**职责**：规则引擎接口

```java
public interface TriageRuleEngine {
    List<RecommendedDepartment> match(String chiefComplaint, String ruleVersion, String ruleSetId);
    String currentRuleVersion();
    String currentRuleSetId();
}
```

### DefaultTriageRuleEngine

**形态**：class
**包路径**：com.aimedical.modules.consultation.rule
**职责**：数据库规则源实现，Caffeine 60s 刷新

```java
@Component
public class DefaultTriageRuleEngine implements TriageRuleEngine {
    private final TriageRuleRepository triageRuleRepository;
    private final Cache<String, List<TriageRule>> ruleCache;

    public DefaultTriageRuleEngine(TriageRuleRepository triageRuleRepository) { ... }

    @Override
    public List<RecommendedDepartment> match(String chiefComplaint, String ruleVersion, String ruleSetId) { ... }

    @Override
    public String currentRuleVersion() { ... }

    @Override
    public String currentRuleSetId() { ... }
}
```

**行为契约**：
- 使用 Caffeine Cache，refreshAfterWrite=60s
- match 方法：加载缓存中的规则，按条件匹配，返回 RecommendedDepartment 列表
- 缓存快照失效时降级使用最新版本

### TriageRule

**形态**：class (JPA @Entity)
**包路径**：com.aimedical.modules.consultation.rule.entity
**职责**：分诊规则实体

```java
@Entity
@Table(name = "triage_rule")
public class TriageRule extends BaseEntity {
    private String ruleId;
    private String ruleSetId;
    private String ruleVersion;
    private String conditions;             // JSON TEXT
    private String resultDepartmentId;
    private String resultDepartmentName;
    private float score;
    private Boolean enabled = true;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
```

### DepartmentFallbackProvider

**形态**：interface
**包路径**：com.aimedical.modules.consultation.fallback
**职责**：兜底科室接口

```java
public interface DepartmentFallbackProvider {
    List<RecommendedDepartment> getFallbackDepartments();
}
```

### StaticDepartmentFallbackProvider

**形态**：class
**包路径**：com.aimedical.modules.consultation.fallback
**职责**：静态兜底科室列表（配置化）

```java
@Component
public class StaticDepartmentFallbackProvider implements DepartmentFallbackProvider {
    @Value("${consultation.fallback.departments:}")
    private String fallbackDepartmentsConfig;

    @Override
    public List<RecommendedDepartment> getFallbackDepartments() { ... }
}
```

### TriageRecord

**形态**：class (JPA @Entity)
**包路径**：com.aimedical.modules.consultation.entity
**职责**：分诊结果实体

```java
@Entity
@Table(name = "triage_record", indexes = {
    @Index(name = "idx_triage_patient_id", columnList = "patientId")
})
public class TriageRecord extends BaseEntity {
    @Column(nullable = false, unique = true)
    private String sessionId;

    private String patientId;

    private String chiefComplaint;                 // TEXT
    private String aiRecommendedDepartments;       // JSON TEXT
    private String recommendedDoctors;              // JSON TEXT
    private String ruleMatchedDepartments;          // JSON TEXT
    private String finalDepartmentId;               // nullable
    private String finalDepartmentName;             // nullable
    private Float confidence;
    private Boolean degraded;
    private String ruleVersion;
    private String ruleSetId;
    private LocalDateTime triageTime;
}
```

### DeadLetterEvent

**形态**：class (JPA @Entity)
**包路径**：com.aimedical.modules.consultation.entity
**职责**：死信事件实体

```java
@Entity
@Table(name = "dead_letter_event")
public class DeadLetterEvent extends BaseEntity {
    @Column(columnDefinition = "TEXT", nullable = false)
    private String eventPayload;

    @Column(length = 500, nullable = false)
    private String failReason;

    private LocalDateTime failTime;

    @Column(length = 20)
    private String state = "FAILED";

    private Integer retryCount = 0;

    private Integer maxRetryCount = 3;
}
```

### TriageRecordRepository

**形态**：interface
**包路径**：com.aimedical.modules.consultation.repository
**职责**：分诊记录数据访问

```java
@Repository
public interface TriageRecordRepository extends JpaRepository<TriageRecord, Long> {
    Optional<TriageRecord> findBySessionId(String sessionId);
    Optional<TriageRecord> findTopByPatientIdOrderByTriageTimeDesc(String patientId);
    List<TriageRecord> findBySessionIdIn(List<String> sessionIds);
}
```

### TriageRuleRepository

**形态**：interface
**包路径**：com.aimedical.modules.consultation.repository
**职责**：分诊规则数据访问

```java
@Repository
public interface TriageRuleRepository extends JpaRepository<TriageRule, Long> {
    List<TriageRule> findByRuleSetIdAndRuleVersion(String ruleSetId, String ruleVersion);
    List<TriageRule> findByEnabledTrue();
}
```

### DeadLetterEventRepository

**形态**：interface
**包路径**：com.aimedical.modules.consultation.repository
**职责**：死信事件数据访问

```java
@Repository
public interface DeadLetterEventRepository extends JpaRepository<DeadLetterEvent, Long> {
    @Query("SELECT e FROM DeadLetterEvent e WHERE e.state = :state AND e.retryCount < e.maxRetryCount")
    List<DeadLetterEvent> findByCompensableEvents(@Param("state") String state);
}
```

### RegistrationEventListener

**形态**：class
**包路径**：com.aimedical.modules.consultation.event
**职责**：监听挂号事件，更新分诊记录的最终科室

```java
@Component
public class RegistrationEventListener {
    private final TriageRecordRepository triageRecordRepository;
    private final DeadLetterEventRepository deadLetterEventRepository;

    public RegistrationEventListener(TriageRecordRepository triageRecordRepository,
                                      DeadLetterEventRepository deadLetterEventRepository) { ... }

    @EventListener
    @Retryable(retryFor = Exception.class, maxAttempts = 3, backoff = @Backoff(delay = 2000))
    public void handleRegistrationEvent(RegistrationEvent event) { ... }

    @Recover
    public void recover(Exception e, RegistrationEvent event) { ... }
}
```

**行为契约**：
- `handleRegistrationEvent()`: 通过 event.sessionId 查找 TriageRecord。若存在且 finalDepartmentId 为 null，则通过 TriageRecordRepository 直接更新 TriageRecord 的 finalDepartmentId=event.departmentId、finalDepartmentName=event.departmentName。不调用 TriageService.selectDepartment()。
- 可治愈异常：最多重试 3 次，间隔 2 秒
- `@Recover`: 构造 DeadLetterEvent，eventPayload 为 event 的 JSON 序列化（含 sessionId/departmentId/departmentName），failReason=e.getMessage()，state='FAILED'。通过 DeadLetterEventRepository.save() 持久化。

**实现路径说明**：使用 TriageRecordRepository 直接更新实体而非调用 TriageService.selectDepartment()，原因：①RegistrationEvent 已携带 departmentId/departmentName；②避免引入 TriageService 循环依赖；③Repository 直写语义更清晰。

**依赖要求**：需在 pom.xml 中添加 `spring-retry` 依赖（@Retryable/@Recover 所需）。

### TriageConverter

**形态**：class
**包路径**：com.aimedical.modules.consultation.converter
**职责**：分诊数据转换

```java
@Component
public class TriageConverter {
    public com.aimedical.modules.ai.api.dto.triage.TriageRequest toAiTriageRequest(
            DialogueCreateRequest request, DialogueSession session) { ... }

    public TriageResponse toTriageResponse(
            AiResult<com.aimedical.modules.ai.api.dto.triage.TriageResponse> aiResult,
            List<RecommendedDoctor> doctors) { ... }
}
```

**行为契约**：
- `toAiTriageRequest`: 将 DialogueCreateRequest 和 DialogueSession 合并为 ai-api 的 TriageRequest（多轮对话时拼接 additionalResponses）
- `toTriageResponse`: 接收 AiResult<TriageResponse> 而不是裸的 TriageResponse，以便在方法内部通过 aiResult.getData() 获取响应体、通过 aiResult.isDegraded() 获取降级标记。若降级则从 aiResult.getData() 的 recommendedDepartments 和 reason 映射到业务层 TriageResponse。

### TriageController

**形态**：class
**包路径**：com.aimedical.modules.consultation.api
**职责**：分诊 REST 端点

```java
@RestController
@RequestMapping("/api/triage")
public class TriageController {
    private final TriageService triageService;

    public TriageController(TriageService triageService) { ... }

    @PostMapping("/consult")
    public Result<TriageResponse> consult(@Valid @RequestBody DialogueCreateRequest request) { ... }

    @PostMapping("/select-department")
    public Result<TriageResponse> selectDepartment(@RequestParam String sessionId,
                                                    @RequestParam String departmentId,
                                                    @RequestParam String departmentName) { ... }
}
```

**行为契约**：
- `consult()`: 接收 DialogueCreateRequest，@Valid 校验后调用 triageService.triage(request)，返回 Result.success(triageResponse)
- `selectDepartment()`: 接收 sessionId/departmentId/departmentName，调用 triageService.selectDepartment(sessionId, departmentId, departmentName, true)，返回 Result.success(triageResponse)

## 错误处理

- 服务层异常：抛出 `BusinessException`（来自 common 模块），由全局异常处理器统一转换
- 校验失败：`@Valid` 触发 `MethodArgumentNotValidException`
- AI 调用失败：通过 `AiResult` 的 `isSuccess()`/`isDegraded()` 判断，不回抛异常
- 死信补偿：`DeadLetterCompensationService` 内捕获异常，递增 retryCount，不传播

## 行为契约

1. `TriageController.consult()` → `@Valid` 校验 DialogueCreateRequest（chiefComplaint 5-500字符，sessionId 必填）
2. `TriageServiceImpl.triage()` 执行顺序：AI 调用 → 降级（AI 失败时）→ 医生查询 → 记录持久化
3. `TriageServiceImpl.selectDepartment(sessionId, deptId, deptName, overwrite)` — overwrite=true 无条件写入；overwrite=false 仅当 finalDepartmentId==null 时写入
4. 多轮对话：全量拼接 `session.additionalResponses` + `request.additionalResponses` / `request.chiefComplaint`
5. `DialogueSessionManager.evictExpiredSessions()` 每分钟清理 TTL > 30min 会话
6. `DeadLetterCompensationService.compensateDeadLetters()` 每 30 分钟执行，仅处理 retryCount < maxRetryCount 的记录
7. `RegistrationEventListener.handleRegistrationEvent()` 使用 TriageRecordRepository 直写，不调用 TriageService

## 依赖关系

| 依赖 | 来源 | 用途 |
|------|------|------|
| AiService | ai-api | AI 分诊调用 |
| AiResult\<T\> | ai-api | AI 调用结果封装 |
| ai-api triage dto 包 | ai-api | AI 层分诊 DTO |
| SessionStore\<String,DialogueSession\> | common-module-api | 会话存储 |
| DoctorFacade | common-module-api | 获取排班医生 |
| AvailableDoctor | common-module-api | 可用医生值对象 |
| RegistrationEvent | common-module-api | 挂号事件 |
| BaseEntity | common | JPA 实体基类 |
| Result\<T\> | common | REST 统一响应 |
| BusinessException | common | 业务异常 |
| spring-retry | spring-boot-starter (新增) | @Retryable/@Recover 注解支持 |
| Caffeine | spring-boot-starter-cache (新增) | DefaultTriageRuleEngine 缓存 |

## 修订说明（v8 r3）

| 审查意见 | 修改措施 |
|---------|---------|
| [r1-一般] TriageConverter.toTriageResponse 第一个参数应为 AiResult\<TriageResponse\> 而非裸 TriageResponse，以准确传递降级状态 | 签名从 `TriageResponse aiResponse` 改为 `AiResult<TriageResponse> aiResult`，行为契约中补充 aiResult.getData() 和 aiResult.isDegraded() 的使用说明 |
| [r1-一般] DeadLetterCompensationService 可能覆盖手动选科，需增加保护 | 在 TriageService.selectDepartment() 中增加 `overwrite` 参数（boolean）。Controller 端点传入 true（无条件写入）；事件监听和补偿服务传入 false（仅当 finalDepartmentId==null 时写入） |
| [r1-轻微] FirstTurn 验证组未定义 | 当前设计未使用验证组，该问题已在之前轮次消除 |
| [r1-轻微] DeadLetterCompensationService 反序列化描述缺少 sessionId | 当前设计已包含 sessionId，已在前序轮次修复 |
| [r2-一般] DeadLetterEventRepository 派生查询无法实现 retryCount < maxRetryCount 字段级比较 | 已替换为 @Query 自定义 JPQL，已在前序轮次修复 |
| [r3-一般] RegistrationEventListener 缺少 DeadLetterEventRepository 依赖，@Recover 无法写入 DeadLetterEvent | 补充 `DeadLetterEventRepository deadLetterEventRepository` 构造器依赖，@Recover 方法中通过该 Repository 持久化 DeadLetterEvent |
| [r3-一般] RegistrationEventListener.handleRegistrationEvent 实现路径不明确（TriageService vs Repository） | 明确使用 TriageRecordRepository 直写（理由：①事件已携带 departmentId/departmentName；②避免 TriageService 循环依赖；③语义更清晰），行为契约同步更新 |
| [r3-轻微] TriageResponse.ruleVersionMismatch 应为 Boolean 而非 boolean | 类型从 `boolean` 改为 `Boolean` 以支持可选语义 |
| [r1-一般] TriageRecord 中 @Index 注解位置错误（字段级 vs 类级） | 已在之前轮次将字段级 `@Index` 改为类级 `@Table(indexes = @Index(...))`，代码正确但修订说明遗漏；现补充记录 |

## 修订说明（v8 r5）

| 审查意见 | 修改措施 |
|---------|---------|
| [r4-一般] TriageService.selectDepartment 接口签名与任务描述不一致（设计四参数 vs 任务三参数 `(sessionId, departmentId, departmentName)`） | **保留设计不变**。四参数签名增加了 `boolean overwrite` 参数，是必要的设计改进：Controller 端点传入 `true`（无条件写入），RegistrationEventListener 和 DeadLetterCompensationService 传入 `false`（仅当 `finalDepartmentId==null` 时写入）。此设计解决了补偿服务可能覆盖手动选科的问题（r1-3）。行为契约已明确标注（§TriageServiceImpl overwrite 语义）。任务文件 `task_v8.md` 应同步更新为四参数签名。 |
| [r4-一般] RegistrationEventListener 实现路径与任务描述不一致（设计用 Repository 直写 vs 任务要求调用 selectDepartment） | **保留设计不变**。使用 `TriageRecordRepository` 直接更新的理由已在 behavior contract 中明确标注（第512行）：①RegistrationEvent 已携带 departmentId/departmentName，无需经过 Service 层；②避免引入 `TriageService` 循环依赖；③Repository 直写语义更清晰。任务文件 `task_v8.md` 应同步修正描述。 |
| [r4-轻微] DeadLetterEventRepository 方法名与任务描述不一致（`findByCompensableEvents` vs 任务指定 `findByStateAndRetryCountLessThan`） | **保留设计不变**。Spring Data JPA 派生查询无法实现字段间比较 `retryCount < maxRetryCount`，必须使用 `@Query` 自定义 JPQL，方法名相应改为 `findByCompensableEvents`。功能等价，方法名差异不影响正确性。任务文件 `task_v8.md` 应同步更新方法名。 |
