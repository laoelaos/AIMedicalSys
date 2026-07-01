# 详细设计（v19）

## 概述
修复 phase2 C3 DE 实现报告中的 6 个问题（C07/A02/A10/A05/A06/A01），涉及 AI 超时注入、配置补齐、MockAiService 重构、降级框架完善和 AiResultFactory 迁移。

## 文件规划
| 文件路径 | 操作 | 职责 |
|---------|------|------|
| `consultation/.../TriageServiceImpl.java` | 修改 | 注入 aiTimeout + future.get 超时 + AiResultFactory 替换 |
| `prescription/audit/.../PrescriptionAuditServiceImpl.java` | 修改 | 注入 aiTimeout + future.get 超时 |
| `prescription/assist/.../PrescriptionAssistServiceImpl.java` | 修改 | 注入 aiTimeout + future.get 超时 |
| `ai/ai-impl/.../mock/MockAiService.java` | 修改 | @Profile 切换 + 3 种策略模式 |
| `ai/ai-impl/.../mock/MockAdminController.java` | 新建 | 运行时切换 mock 策略端点 |
| `ai/ai-api/.../degradation/DegradationContext.java` | 修改 | 增加 serviceName/operationName 字段 |
| `ai/ai-impl/.../fallback/FallbackAiService.java` | 修改 | selectDelegate 方法 + 13 个方法替换 |
| `application/src/main/resources/application.yml` | 修改 | 追加 AI 超时/mock/consultation 配置 |
| `application/src/test/resources/application.yml` | 修改 | 追加相同配置键 |
| `consultation/.../TriageServiceImplTest.java` | 修改 | 构造参数追加 aiTimeout |
| `prescription/audit/.../PrescriptionAuditServiceImplTest.java` | 修改 | 构造参数追加 aiTimeout（2 处） |
| `prescription/assist/.../PrescriptionAssistServiceImplTest.java` | 修改 | 构造参数追加 aiTimeout |
| `ai/ai-impl/.../mock/MockAiServiceTest.java` | 修改 | @Profile 验证 + 3 策略覆盖 |
| `ai/ai-impl/.../mock/MockAdminControllerTest.java` | 新建 | GET/POST 端点测试 |
| `ai/ai-impl/.../fallback/FallbackAiServiceTest.java` | 修改 | 多 delegate + 降级跳过 + 全降级回退 |
| `ai/ai-api/.../degradation/DegradationStrategyTest.java` | 修改 | serviceName/operationName 决策验证 |

## 类型定义

### TriageServiceImpl — 字段变更
**形态**：字段追加
**包路径**：`com.aimedical.modules.consultation.service.impl`
**变更**：
- 新增字段 `private final long aiTimeout;`
- 构造器参数追加 `@Value("${ai.timeout.triage:8}") long aiTimeout`
- L55-61 构造器参数签名末尾追加 `long aiTimeout`
- L96 `future.get()` → `future.get(aiTimeout, TimeUnit.SECONDS)`
- 新增 import `java.util.concurrent.TimeUnit`

### PrescriptionAuditServiceImpl — 字段变更
**形态**：字段追加
**包路径**：`com.aimedical.modules.prescription.service.audit.impl`
**变更**：
- 新增字段 `private final long aiTimeout;`
- 构造器参数追加 `@Value("${ai.timeout.prescription-audit:6}") long aiTimeout`
- L73-80 构造器参数签名末尾追加 `long aiTimeout`
- L100 `future.get()` → `future.get(aiTimeout, TimeUnit.SECONDS)`
- 新增 import `java.util.concurrent.TimeUnit`
- 新增 import `java.util.concurrent.TimeoutException` 以处理超时异常

### PrescriptionAssistServiceImpl — 字段变更
**形态**：字段追加
**包路径**：`com.aimedical.modules.prescription.service.assist.impl`
**变更**：
- 新增字段 `private final long aiTimeout;`
- 构造器参数追加 `@Value("${ai.timeout.prescription-assist:8}") long aiTimeout`
- L57-66 构造器参数签名末尾追加 `long aiTimeout`
- L91 `aiService.prescriptionAssist(aiRequest).get()` → `aiService.prescriptionAssist(aiRequest).get(aiTimeout, TimeUnit.SECONDS)`
- 新增 import `java.util.concurrent.TimeUnit`
- 新增 import `java.util.concurrent.TimeoutException` 以处理超时异常

### MockAiService — 策略模式
**形态**：class 重构
**包路径**：`com.aimedical.modules.ai.impl.mock`
**职责**：支持 3 种 mock 响应策略，运行时可通过 MockAdminController 切换
**变更**：
- `@ConditionalOnProperty(name = "ai.mock.enabled", havingValue = "true", matchIfMissing = true)` → `@Profile("mock")`
- 新增 `@Value("${ai.mock.response-strategy:STATIC}")` 字段注入
- 新增内部枚举 `ResponseStrategy { STATIC, AI_UNAVAILABLE, TIMEOUT }`
- 新增字段 `private volatile ResponseStrategy currentStrategy;` — 允许运行时切换
- 新增 `void setStrategy(ResponseStrategy strategy)` — 包级可见供 MockAdminController 调用
- 13 个方法全部改为根据 `currentStrategy` 分支：
  - `STATIC`：返回当前行为（`CompletableFuture.completedFuture(AiResult.success(...))`）
  - `AI_UNAVAILABLE`：返回 `CompletableFuture.completedFuture(AiResult.failure("AI_UNAVAILABLE"))`
  - `TIMEOUT`：返回 `new CompletableFuture<>()` 永不完成（模拟超时），或使用 `CompletableFuture.supplyAsync(() -> { try { Thread.sleep(Long.MAX_VALUE); } catch ... })` 等效方式
- 移除 import `org.springframework.boot.autoconfigure.condition.ConditionalOnProperty`
- 新增 import `org.springframework.context.annotation.Profile`, `org.springframework.beans.factory.annotation.Value`

### MockAdminController — 新建
**形态**：class
**包路径**：`com.aimedical.modules.ai.impl.mock`
**职责**：提供运行时切换 MockAiService 策略的管理端点
**构造方式**：Spring `@RestController` + 构造器注入 `MockAiService`
**公开接口**：
- `POST /api/admin/ai/mock/strategy` — 接受 `MockStrategyRequest` JSON body `{ "strategy": "STATIC" }`，调用 `mockAiService.setStrategy(...)`，返回 200 OK
- `GET /api/admin/ai/mock/strategy` — 返回 `{ "strategy": "STATIC" }` 表示当前策略
**公开方法**：
- `getCurrentStrategy()`: `ResponseEntity<Map<String, String>>`
- `setStrategy(@RequestBody MockStrategyRequest request)`: `ResponseEntity<Void>`
**内部类型**：
- `MockStrategyRequest` — static inner class，字段 `String strategy` + getter/setter

### DegradationContext — 字段扩展
**形态**：class 修改
**包路径**：`com.aimedical.modules.ai.api.degradation`
**变更**：
- 新增字段 `private String serviceName;`
- 新增字段 `private String operationName;`
- 对应 getter/setter: `getServiceName()`, `setServiceName(String)`, `getOperationName()`, `setOperationName(String)`

### FallbackAiService — selectDelegate 引入
**形态**：class 修改
**包路径**：`com.aimedical.modules.ai.impl.fallback`
**变更**：
- 新增私有方法 `private AiService selectDelegate(DegradationContext context)`：
  - 遍历 `this.delegates`
  - 对每个 delegate，遍历全部 `this.strategies` 调用 `shouldDegrade(context)`
  - 任意 strategy 返回 true 则跳过该 delegate
  - 返回第一个通过全部策略检查的 delegate
  - 所有 delegate 均被跳过则返回 `null`
- 13 个方法中替换 `delegates.get(0)` 为：
  ```java
  DegradationContext context = new DegradationContext();
  context.setServiceName("triage"); // 依方法而定
  context.setOperationName("triage");
  AiService delegate = selectDelegate(context);
  if (delegate == null) {
      return handleEmptyDelegates();
  }
  return delegate.methodName(request).thenApply(this::applyStrategies);
  ```
- `applyStrategies()` 保持现有实现不变
- `handleEmptyDelegates()` 保持现有实现不变

## 配置项

### application.yml（main）— 追加内容
```yaml
# AI 超时配置
ai:
  timeout:
    triage: 8
    prescription-audit: 6
    medical-record-generate: 12
    prescription-assist: 8
  mock:
    response-strategy: STATIC

# 外部门面超时
consultation:
  doctor-facade:
    timeout: 2

medical-record:
  visit-facade:
    timeout: 2
```

### application.yml（test）— 追加内容
同 main 配置，追加到文件末尾。

## 错误处理

### 超时异常处理（C07 + A02）
3 个 Service 中 `future.get(aiTimeout, TimeUnit.SECONDS)` 新增 `TimeoutException` catch 分支：
- **TriageServiceImpl**：catch TimeoutException → `aiResult = handleAiFailure(session)`（复用现有降级路径）
- **PrescriptionAuditServiceImpl**：catch TimeoutException → `aiResult = null`（复用现有降级路径）
- **PrescriptionAssistServiceImpl**：catch TimeoutException → `return buildEmptyResponse(request.getPrescriptionId())`（复用现有降级路径）

### MockAiService TIMEOUT 策略
TIMEOUT 策略返回永不完成的 `CompletableFuture`，调用方将在 `future.get(timeout, unit)` 处触发 TimeoutException。

### AiResultFactory.degraded 替换（A01）
TriageServiceImpl.handleAiFailure() 中 2 处：
- `AiResult.degraded("AI call failed, attempt " + session.getAiFailCount())` → `AiResultFactory.degraded("AI call failed, attempt " + session.getAiFailCount(), (TriageResponse) null)`
- `AiResult.degraded("AI call failed after " + session.getAiFailCount() + " attempts")` → `AiResultFactory.degraded("AI call failed after " + session.getAiFailCount() + " attempts", (TriageResponse) null)`

2 参重载 `degraded(String fallbackReason, T partialData)`，partialData=null，errorCode 保持 null。

## 行为契约

### 实施顺序
A10(配置) → C07+A02(超时注入) → A05(MockAiService) → A06(降级框架) → A01(AiResultFactory替换)

### 降级框架行为
- `selectDelegate(context)` 每次调用都重新评估策略
- 所有 delegate 均被跳过时返回 `handleEmptyDelegates()` 的降级结果
- `applyStrategies()` 在后置阶段执行，保持现有逻辑不变

### MockAdminController 端点契约
- 切换策略后立即对所有新请求生效（`currentStrategy` 为 volatile 字段）
- 不要求已发出的 inflight 请求受影响

## 依赖关系
- TriageServiceImpl 依赖：AiService, TriageRuleEngine, DepartmentFallbackProvider, DoctorFacade, DialogueSessionManager, TriageRecordRepository, TriageConverter, ObjectMapper, TransactionTemplate + **`@Value("${ai.timeout.triage}")`**
- PrescriptionAuditServiceImpl 依赖：现有依赖 + **`@Value("${ai.timeout.prescription-audit}")`**
- PrescriptionAssistServiceImpl 依赖：现有依赖 + **`@Value("${ai.timeout.prescription-assist}")`**
- MockAiService 依赖：`@Profile("mock")`, `@Value("${ai.mock.response-strategy}")`, MockAdminController 通过 setStrategy 控制
- MockAdminController 依赖：MockAiService
- FallbackAiService 依赖：List\<AiService\>(delegates), List\<DegradationStrategy\>(strategies)
- DegradationContext 依赖：无（POJO）

## 测试同步

### TriageServiceImplTest — 构造参数同步
- `setUp()` 中 `new TriageServiceImpl(...)` — 9 参构造追加第 10 个 `aiTimeout` 参数（值任意，如 `5L`）

### PrescriptionAuditServiceImplTest — 构造参数同步
- `setUp()` L76-77 中 `new PrescriptionAuditServiceImpl(...)` — 追加 `aiTimeout` 参数
- L402 第 2 处实例化 `new PrescriptionAuditServiceImpl(...)` — 同样追加 `aiTimeout` 参数

### PrescriptionAssistServiceImplTest — 构造参数同步
- `setUp()` L56-57 中 `new PrescriptionAssistServiceImpl(...)` — 追加 `aiTimeout` 参数

### MockAiServiceTest — 策略覆盖
- 验证 `@Profile("mock")` 注解存在
- 覆盖 3 种策略：
  - `STATIC`：现有测试断言（所有方法返回 success + 非 degraded + data 非 null）
  - `AI_UNAVAILABLE`：所有方法返回 failure("AI_UNAVAILABLE")
  - `TIMEOUT`：所有方法 timeout（CompletableFuture 永不完成，调用 `future.get(1, TimeUnit.MILLISECONDS)` 预期 TimeoutException）

### MockAdminControllerTest — 端点测试
- GET `/api/admin/ai/mock/strategy` 返回 `{ "strategy": "STATIC" }`
- POST `/api/admin/ai/mock/strategy` with `{ "strategy": "AI_UNAVAILABLE" }` → 200 OK
- GET 验证策略已切换
- POST with `{ "strategy": "TIMEOUT" }` → 200 OK + GET 验证

### FallbackAiServiceTest — 降级跳过场景
- 多 delegate + 无降级策略：选择第 1 个 delegate，正常调用
- 多 delegate + 第 1 个被降级跳过：选择第 2 个 delegate，正常调用
- 多 delegate + 全部被降级跳过：返回 `handleEmptyDelegates()` 降级结果
- 混合策略 + 多 delegate：某些 strategy 对 delegate1 返回 true，对 delegate2 返回 false

### DegradationStrategyTest — serviceName/operationName 决策
- 构造 DegradationContext 并设置 serviceName="triage"，验证 strategy 根据 serviceName 决策
- 构造 DegradationContext 并设置 operationName="prescriptionCheck"，验证 strategy 根据 operationName 决策
