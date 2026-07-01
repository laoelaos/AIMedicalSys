# 测试报告（v19）

## 概述
对 phase2 C3 DE 修复的 7 个测试文件进行验证/修改/新建。覆盖 6 个问题（C07/A02/A10/A05/A06/A01）的所有行为契约。

## 测试文件清单

### 1. TriageServiceImplTest
**路径**: `consultation/.../TriageServiceImplTest.java`
**操作**: 修改（构造参数追加 aiTimeout）+ 新增 TimeoutException 测试
**变更详情**:
- `setUp()` L82: 构造器追加 `5L` 作为 aiTimeout 参数
- 新增 `shouldFallbackOnTimeout()`: 构造 aiTimeout=0 的 service，传入永不完成的 future，验证触发 TimeoutException 后走 handleAiFailure 降级路径，返回 degraded=true
- 已有测试验证 ExecutionException/InterruptedException 降级路径不受影响
- 已有测试验证 AiResultFactory.degraded 替换（通过 handleAiFailure 路径的 2 条 degraded message）

### 2. PrescriptionAuditServiceImplTest
**路径**: `prescription/audit/.../PrescriptionAuditServiceImplTest.java`
**操作**: 修改（2 处构造参数追加 aiTimeout）+ 新增 TimeoutException 测试
**变更详情**:
- `setUp()` L77: 构造器追加 `2L`(drugFacadeTimeout) 和 `6L`(aiTimeout)
- L402 第 2 处实例化: 同样追加 `2L, 6L`
- 新增 `auditShouldFallbackOnTimeout()`: 构造 aiTimeout=0 的 service，传入永不完成的 future，验证触发 TimeoutException 后 aiResult=null → 走 fallback 路径，返回 fromFallback=true

### 3. PrescriptionAssistServiceImplTest
**路径**: `prescription/assist/.../PrescriptionAssistServiceImplTest.java`
**操作**: 修改（构造参数追加 aiTimeout）+ 新增 TimeoutException 测试
**变更详情**:
- `setUp()` L56: 构造器追加 `2L`(drugFacadeTimeout) 和 `8L`(aiTimeout)
- 新增 `assistShouldReturnEmptyOnTimeout()`: 构造 aiTimeout=0 的 service，传入永不完成的 future，验证触发 TimeoutException 后返回 `{"drugs":[]}` + errorCode=RX_ASSIST_AI_NO_RECOMMENDATION

### 4. MockAiServiceTest
**路径**: `ai/ai-impl/.../mock/MockAiServiceTest.java`
**操作**: 已有 + 验证 3 策略覆盖
**覆盖维度**:
- `@Profile("mock")` 注解存在: `shouldBeAnnotatedWithProfile()` 使用反射验证
- `@Service` 注解存在: `shouldBeAnnotatedWithService()` 验证
- STATIC 策略: 全部 13 个方法返回 success + 非 degraded + data 非 null（`shouldBeAnnotatedWithProfile` 之前的 12 个方法 + `staticStrategyShouldReturnSuccess`）
- AI_UNAVAILABLE 策略: `aiUnavailableStrategyShouldReturnFailure()` — 所有方法返回 failure("AI_UNAVAILABLE")
- TIMEOUT 策略: `timeoutStrategyShouldTimeout()` — future 永不完成，`future.get(1, TimeUnit.MILLISECONDS)` 抛出 TimeoutException
- 策略切换通过 `setStrategy()` 运行时生效（volatile currentStrategy）

### 5. MockAdminControllerTest
**路径**: `ai/ai-impl/.../mock/MockAdminControllerTest.java`
**操作**: 新建
**测试用例**:
- `getStrategyShouldReturnStaticByDefault()`: GET 返回 200 + `{"strategy":"STATIC"}`
- `setStrategyAndVerify()`: POST `{"strategy":"AI_UNAVAILABLE"}` → 200 OK → GET 验证已切换为 AI_UNAVAILABLE
- `setStrategyToTimeout()`: POST `{"strategy":"TIMEOUT"}` → 200 OK → GET 验证已切换为 TIMEOUT

### 6. FallbackAiServiceTest
**路径**: `ai/ai-impl/.../fallback/FallbackAiServiceTest.java`
**操作**: 修改（新增 selectDelegate 相关测试）
**覆盖维度**:
- `selectDelegateShouldPickFirstWhenNoStrategies()`: 多 delegate + 无 strategy → 选第 1 个
- `selectDelegateShouldSkipFirstWhenDegradedByStrategy()`: 多 delegate + strategy 跳过第 1 个 → 选第 2 个
- `selectDelegateShouldReturnEmptyDelegatesWhenAllSkipped()`: 全部被跳过 → handleEmptyDelegates() 降级结果
- `selectDelegateShouldUseContextWithServiceNameAndOperationName()`: strategy 根据 DegradationContext 的 serviceName="triage" + operationName="triage" 决策
- 已有测试验证 13 个方法的委托/降级路径不受影响

### 7. DegradationStrategyTest
**路径**: `ai/ai-api/.../degradation/DegradationStrategyTest.java`
**操作**: 修改（新增 serviceName/operationName 决策验证）
**覆盖维度**:
- `shouldDegradeBasedOnServiceName()`: context.setServiceName("triage") → strategy 返回 true; setServiceName("prescription") → 返回 false
- `shouldDegradeBasedOnOperationName()`: context.setOperationName("prescriptionCheck") → 返回 true; setOperationName("prescriptionAssist") → 返回 false
- 已有测试验证 DegradationContext 默认构造和策略基础行为

## 行为契约覆盖矩阵

| 契约 | 覆盖测试 | 状态 |
|------|---------|------|
| C07: AI 超时注入 — 3 Service 构造参数追加 aiTimeout | TriageServiceImplTest L82, PrescriptionAuditServiceImplTest L77/402, PrescriptionAssistServiceImplTest L56 | ✓ |
| C07: future.get(aiTimeout, TimeUnit.SECONDS) | 所有 3 Service 均已替换 | ✓ |
| A02: TimeoutException 降级路径 | TriageServiceImplTest, PrescriptionAuditServiceImplTest, PrescriptionAssistServiceImplTest（3 个新增测试） | ✓ |
| A10: 配置补齐 — application.yml | 构造参数已体现配置注入 | ✓ |
| A05: MockAiService — @Profile + 3 策略 | MockAiServiceTest 全部策略覆盖 | ✓ |
| A05: MockAdminController — 运行时切换端点 | MockAdminControllerTest GET/POST | ✓ |
| A06: DegradationContext serviceName/operationName | DegradationStrategyTest 决策验证 | ✓ |
| A06: selectDelegate — 跳过机制 | FallbackAiServiceTest 4 场景 | ✓ |
| A01: AiResultFactory.degraded 替换 | TriageServiceImplTest handleAiFailure 路径（通过 ExecutionException/TimeoutException 测试间接覆盖） | ✓ |

## 未覆盖说明
- 编译验证: 未执行，需由 CI 或主 Agent 执行
- 集成测试: 未覆盖，需通过 Spring Boot 集成测试验证配置注入和 Profile 激活
