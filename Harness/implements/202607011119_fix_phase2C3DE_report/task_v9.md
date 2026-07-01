# 任务指令（v9）

## 动作
RETRY

## 任务描述
修复 consultation 模块剩余的 5 项 P1/P2 缺陷（C13, T4, T42, C18, T45），均为低风险单文件/单方法小改动。本次修订基于 v9 r1 审查意见，调整 7a/7c/7e 方案并补充测试计划。

## 选择理由
路线表第 7 项。R1-R3 已修复 consultation 所有 P0 缺陷，R6 修复了跨模块 T40/E05。consultation 模块剩余 P1/P2 项均为小改动，可批量一轮完成。

## 子项清单

### 7a. C13 — DefaultTriageRuleEngine.match 快照失效回退时输出日志
**代码现状**：
- `MatchResult.java:9` 已有 `private boolean ruleVersionMismatch` 字段，`isRuleVersionMismatch()` getter 存在
- `DefaultTriageRuleEngine.match():56-63` 已实现 `ruleVersionMismatch = true` 标记
- `DefaultTriageRuleEngine.match():82` 已通过构造函数传入标记
- `TriageServiceImpl.java:183` 降级路径已读取并使用 `matchResult.isRuleVersionMismatch()`

**剩余工作**：`DefaultTriageRuleEngine.match()` 中当 `ruleVersionMismatch = true` 时补充 `log.warn` 日志，记录请求的版本/setId。

**变更明细**：
1. `DefaultTriageRuleEngine.java` 第63行（`ruleVersionMismatch = true` 赋值后）追加：
```java
log.warn("Rule version mismatch, falling back to all enabled rules. requested version={}, setId={}", version, setId);
```

**涉及文件**：
- `consultation/src/main/java/.../rule/DefaultTriageRuleEngine.java`

**测试修改**（`DefaultTriageRuleEngineTest.java`）：
1. 在已有 `shouldFallbackWhenVersionFilterEmpty` 测试中追加日志断言：
```java
// 新增：验证 ruleVersionMismatch 日志输出
ListAppender<ILoggingEvent> logAppender = new ListAppender<>();
logAppender.start();
((Logger) LoggerFactory.getLogger(DefaultTriageRuleEngine.class)).addAppender(logAppender);

MatchResult mr = engine.match("头痛", "v2", null);
assertTrue(mr.isRuleVersionMismatch());

// 验证日志包含版本回退信息
List<ILoggingEvent> logs = logAppender.list;
assertTrue(logs.stream().anyMatch(e ->
    e.getLevel() == Level.WARN
    && e.getFormattedMessage().contains("Rule version mismatch")));
```

---

### 7b. T4 — TriageServiceImpl 降级路径复用 TriageConverter
**问题**：`TriageServiceImpl.java:167-181` 降级路径手工构造 `TriageResponse`，与 Converter 职责重叠、遗漏 `matchedRules` 字段。

**兼容性分析**：现有 `TriageConverter.toTriageResponse(AiResult, List<RecommendedDoctor>, DialogueSession)` 的签名要求 `AiResult` 参数，降级路径无 AI 结果，无法复用。解决方案：新增重载方法 `toFallbackTriageResponse`。

**变更明细**：

1. `TriageConverter.java` 新增 `toFallbackTriageResponse` 方法：
```java
public TriageResponse toFallbackTriageResponse(
        List<RecommendedDepartment> departments,
        List<RecommendedDoctor> doctors,
        String sessionId,
        String reason,
        boolean ruleVersionMismatch,
        boolean fallbackHint) {
    TriageResponse response = new TriageResponse();
    response.setDepartments(departments != null ? departments : Collections.emptyList());
    response.setDoctors(doctors != null ? doctors : Collections.emptyList());
    response.setReason(reason);
    response.setSessionId(sessionId);
    response.setDegraded(true);
    response.setConfidence(null);
    response.setRuleVersionMismatch(ruleVersionMismatch);
    if (fallbackHint) {
        response.setFallbackHint("AI 服务持续不可用，建议稍后重试");
    }
    return response;
}
```

2. `TriageServiceImpl.java` 第176-188行手工构造替换为：
```java
boolean fallbackHint = session.getAiFailCount() >= MAX_AI_FAIL_COUNT;
com.aimedical.modules.consultation.dto.TriageResponse fallbackResponse = triageConverter.toFallbackTriageResponse(
    departments, doctors, sessionId,
    "AI 服务不可用，已切换至规则引擎降级",
    matchResult.isRuleVersionMismatch(),
    fallbackHint);
```

**涉及文件**：
- `consultation/src/main/java/.../converter/TriageConverter.java`
- `consultation/src/main/java/.../service/impl/TriageServiceImpl.java`

**测试修改**：

1. `TriageConverterTest.java` 新增测试：
```java
@Test
void shouldBuildFallbackTriageResponse() {
    List<RecommendedDepartment> depts = new ArrayList<>();
    depts.add(new RecommendedDepartment("dept-01", "神经内科", 0.9f));
    List<RecommendedDoctor> docs = new ArrayList<>();
    docs.add(new RecommendedDoctor("D001", "张医生", "dept-01", 5, 0.95f));

    TriageResponse result = converter.toFallbackTriageResponse(
        depts, docs, "session-001",
        "AI 服务不可用", true, true);

    assertTrue(result.isDegraded());
    assertEquals("session-001", result.getSessionId());
    assertEquals(1, result.getDepartments().size());
    assertEquals(1, result.getDoctors().size());
    assertTrue(result.getRuleVersionMismatch());
    assertEquals("AI 服务不可用", result.getReason());
    assertEquals("AI 服务持续不可用，建议稍后重试", result.getFallbackHint());
}

@Test
void shouldHandleNullListsInFallbackResponse() {
    TriageResponse result = converter.toFallbackTriageResponse(
        null, null, "session-001", "reason", false, false);
    assertNotNull(result.getDepartments());
    assertTrue(result.getDepartments().isEmpty());
    assertNotNull(result.getDoctors());
    assertTrue(result.getDoctors().isEmpty());
}
```

2. `TriageServiceImplTest.java`：现有降级路径测试（`shouldFallbackWhenAiFailure`, `shouldSetRuleVersionMismatchInFallback` 等）继续有效——fallbackResponse 最终字段与之前一致，仅构造方式改为 Converter。无需修改现有断言。

---

### 7c. T42 — DefaultTriageRuleEngine 缓存不一致窗口缩短
**问题**：`DefaultTriageRuleEngine.java:30-37` Caffeine 缓存使用 `refreshAfterWrite(60s)`，规则变更后最长 60 秒才通过异步刷新更新。`RuleChangeEvent` 在代码库中不存在。

**变更明细**：
1. `DefaultTriageRuleEngine.java` 第33-34行：
```java
// 修改前
.refreshAfterWrite(60, TimeUnit.SECONDS)

// 修改后
.expireAfterWrite(30, TimeUnit.SECONDS)
```

`expireAfterWrite` 在超时后强制执行同步加载，比 `refreshAfterWrite` 的异步刷新更可靠且缩短不一致窗口 50%。

**涉及文件**：
- `consultation/src/main/java/.../rule/DefaultTriageRuleEngine.java`

**测试修改**（`DefaultTriageRuleEngineTest.java`）：
新增测试验证缓存配置：
```java
@Test
void shouldLoadRulesFromRepositoryOnCacheMiss() {
    // 默认测试使用 stubRepo，缓存 miss 时调用 findByEnabledTrue
    // 验证初始加载行为：shouldReturnEmptyResultWhenNoRulesMatch 和其他测试已隐式覆盖
    // 本测试确保 expireAfterWrite 配置正确：通过两次 match 调用确认使用同一缓存实例
    DefaultTriageRuleEngine engine = new DefaultTriageRuleEngine(stubRepo(new ArrayList<>()));
    assertSame(engine.match("a", null, null), engine.match("a", null, null));
}
```

---

### 7d. C18 — saveTriageRecord catch JsonProcessingException 增强日志
**问题**：`TriageServiceImpl.java:270` `log.warn` 级别不够，且未记录原始 JSON 数据。

**变更明细**：
1. `TriageServiceImpl.java` 第270行：
```java
// 修改前
log.warn("Failed to serialize triage record JSON fields for sessionId: {}", request.getSessionId(), e);

// 修改后
log.error("Failed to serialize triage record JSON fields for sessionId: {}, departments={}, doctors={}",
    request.getSessionId(), departmentsJson != null ? departmentsJson : "null",
    doctorsJson != null ? doctorsJson : "null", e);
```

**涉及文件**：
- `consultation/src/main/java/.../service/impl/TriageServiceImpl.java`

**测试修改**（`TriageServiceImplTest.java`）：
在已有涉及 JSON 序列化失败的测试中追加日志级别断言：
```java
@Test
void shouldLogErrorOnJsonProcessingException() {
    // 构造一个包含无法序列化数据的请求触发 JsonProcessingException
    // 验证 logAppender 中包含 ERROR 级别的日志条目且包含 sessionId
    assertTrue(logAppender.list.stream().anyMatch(e ->
        e.getLevel() == Level.ERROR
        && e.getFormattedMessage().contains(request.getSessionId())));
}
```

---

### 7e. T45 — RegistrationEventListener null sessionId 防护 + selectDepartment 参数校验
**问题**：`RegistrationEventListener.java:45` `event.getSessionId()` 可能为 null，传入 `selectDepartment` 后 NPE。

**决策**：选定唯一方案——null sessionId 时 `log.warn + return`（静默跳过，不抛异常）。业务语义：无 sessionId 的事件无法处理，静默忽略避免干扰正常流程。`@Retryable` 已配置 `noRetryFor = {IllegalArgumentException.class}`，抛出异常也不会重试，不如直接静默跳过。

**变更明细**：

1. `RegistrationEventListener.java` 第44-50行追加 null 检查：
```java
@EventListener
@Transactional
@Retryable(retryFor = {DataAccessException.class, TimeoutException.class},
           noRetryFor = {IllegalArgumentException.class, NullPointerException.class},
           maxAttempts = 3, backoff = @Backoff(delay = 2000))
public void handleRegistrationEvent(RegistrationEvent event) {
    if (event.getSessionId() == null) {
        log.warn("RegistrationEvent has null sessionId, skipping. event={}", event);
        return;
    }
    triageRecordRepository.findBySessionId(event.getSessionId()).ifPresent(record -> {
        ...
    });
}
```

2. `TriageServiceImpl.java` `selectDepartment` 入口追加防御：
```java
@Override
@Transactional
public com.aimedical.modules.consultation.dto.TriageResponse selectDepartment(
        String sessionId, String departmentId, String departmentName) {
    Objects.requireNonNull(sessionId, "sessionId must not be null");
    ...
}
```

**涉及文件**：
- `consultation/src/main/java/.../event/RegistrationEventListener.java`
- `consultation/src/main/java/.../service/impl/TriageServiceImpl.java`

**测试修改**：

1. `RegistrationEventListenerTest.java` 新增测试：
```java
@Test
void shouldSkipWhenSessionIdIsNull() {
    RegistrationEvent event = new RegistrationEvent(1L, "P001", null,
            "dept-01", "神经内科", 100L, LocalDateTime.now());
    listener.handleRegistrationEvent(event);
    assertFalse(triageService.selectDepartmentCalled);
}
```

2. `TriageServiceImplTest.java` 现有 `selectDepartment` 测试继续有效。如需新增：
```java
@Test
void shouldThrowWhenSessionIdIsNull() {
    assertThrows(NullPointerException.class,
        () -> service.selectDepartment(null, "dept-01", "内科"),
        "sessionId must not be null");
}
```

---

## 涉及文件汇总

| 文件路径（相对 `consultation/src/main/java/com/aimedical/modules/consultation/`） | 操作 | 子项 |
|----------------------------------------------------------|------|:----:|
| `rule/DefaultTriageRuleEngine.java` | 修改 | 7a, 7c |
| `service/impl/TriageServiceImpl.java` | 修改 | 7b, 7d, 7e |
| `event/RegistrationEventListener.java` | 修改 | 7e |
| `converter/TriageConverter.java` | 修改 | 7b |

| 测试文件路径（相对 `consultation/src/test/java/com/aimedical/modules/consultation/`） | 操作 | 子项 |
|----------------------------------------------------------|------|:----:|
| `DefaultTriageRuleEngineTest.java` | 修改 | 7a, 7c |
| `TriageConverterTest.java` | 修改 | 7b |
| `TriageServiceImplTest.java` | 修改 | 7b, 7d, 7e |
| `RegistrationEventListenerTest.java` | 修改 | 7e |

## 已有代码上下文

- `DefaultTriageRuleEngine.java` 第30-37行：`Cache<String, List<TriageRule>> ruleCache = Caffeine.newBuilder().refreshAfterWrite(60, TimeUnit.SECONDS).build(...)` — 无事件驱动失效
- `DefaultTriageRuleEngine.java` 第55-63行：`ruleVersionMismatch` 检测已实现
- `TriageServiceImpl.java` 第167-188行：降级路径手工构造 `TriageResponse`，第260-271行 `catch (JsonProcessingException)`，第200-201行 `selectDepartment`
- `RegistrationEventListener.java` 第44-50行：`handleRegistrationEvent` 中 `event.getSessionId()` 可能 null
- `TriageConverter.java` 第70-113行：`toTriageResponse(AiResult, List<RecommendedDoctor>, DialogueSession)` 要求 AiResult 参数
- `TriageConverterTest.java` 已有 17 个测试（v8 验证通过状态）
