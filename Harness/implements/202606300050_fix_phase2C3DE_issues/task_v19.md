# 任务指令（v19）

## 动作
NEW

## 任务描述
**R19 — AI超时外化+MockAiService+降级框架+配置+AiResultFactory**

对应问题：C07, A02, A05, A06, A10, A01（共 6 项，P0/P1/P2）

### 1. C07 + A02 — AI 调用 future.get() 超时注入（4 个 Service 中 3 个待修复，MedicalRecordServiceImpl 已修复）
- TriageServiceImpl.java:96 `future.get()` → `future.get(aiTimeout, TimeUnit.SECONDS)`
- 构造器注入 `@Value("${ai.timeout.triage:8}") long aiTimeout`
- PrescriptionAuditServiceImpl.java:100 `future.get()` → `future.get(aiTimeout, TimeUnit.SECONDS)`
- 构造器注入 `@Value("${ai.timeout.prescription-audit:6}") long aiTimeout`
- PrescriptionAssistServiceImpl.java L91（prescriptionAssist 方法内）`future.get()` → `future.get(aiTimeout, TimeUnit.SECONDS)`
- 构造器注入 `@Value("${ai.timeout.prescription-assist:8}") long aiTimeout`
- **测试同步**：以上 3 个 Service 新增 aiTimeout 构造参数后，同步更新 3 个测试文件的 `new` 实例化处：
  - `TriageServiceImplTest.java:81-82` — 9 参构造追加 aiTimeout 参数
  - `PrescriptionAuditServiceImplTest.java:76-77` — 追加 aiTimeout；L402 第 2 处实例化同步
  - `PrescriptionAssistServiceImplTest.java:56-57` — 追加 aiTimeout

### 2. A10 — application.yml 补全配置键
- 在 C:\Develop\Software\AIMedicalSys\AIMedical\backend\application\src\main\resources\application.yml 追加：
  - `ai.timeout.triage=8`
  - `ai.timeout.prescription-audit=6`
  - `ai.timeout.medical-record-generate=12`
  - `ai.timeout.prescription-assist=8`
  - `consultation.doctor-facade.timeout=2`
  - `medical-record.visit-facade.timeout=2`
  - `ai.mock.response-strategy=STATIC`
- 在 application/src/test/resources/application.yml 也同步补全（如存在且未包含）

### 3. A05 — MockAiService 重构
- 修改 `MockAiService.java:40` `@ConditionalOnProperty(name = "ai.mock.enabled", ...)` → `@Profile("mock")`
- 实现 3 种响应策略（通过 `ai.mock.response-strategy` 配置）：
  - `STATIC`（默认）：返回固定占位数据（当前行为）
  - `AI_UNAVAILABLE`：所有方法返回 `AiResult.failure("AI_UNAVAILABLE")`
  - `TIMEOUT`：所有方法抛出超时（`CompletableFuture` 延时超时或模拟异常）
- 新建 `MockAdminController` 在 `com.aimedical.modules.ai.impl.mock` 包：
  - `POST /api/admin/ai/mock/strategy` 接受 `{ "strategy": "STATIC" }` 运行时切换策略
  - `GET /api/admin/ai/mock/strategy` 返回当前策略
- 配置键 `ai.mock.response-strategy` 注入（@Value）
- **测试同步**：
  - `MockAiServiceTest`：验证 `@Profile("mock")` 注解存在；覆盖 3 种策略模式（STATIC/AI_UNAVAILABLE/TIMEOUT）的返回行为
  - 新增 `MockAdminController` 端点基础请求/响应测试（GET/POST /api/admin/ai/mock/strategy）

### 4. A06 — 降级框架完善
- `DegradationContext`：增加字段以适应降级场景判断（至少包含 `serviceName`/`operationName` 字段 + getter/setter）
- `DegradationStrategy`：接口维持不变（已有 `shouldDegrade(DegradationContext)`）
- `FallbackAiService`：新增私有辅助方法 `selectDelegate(DegradationContext)`，集中封装"遍历 delegates → 对每个 delegate 执行所有 strategy 的 shouldDegrade 预检查 → 跳过被降级的 delegate → 返回第一个通过所有策略检查的 delegate"逻辑
- 将全部 13 个方法（triage/diagnosis/prescriptionCheck/generateMedicalRecord/...）中的 `delegates.get(0)` 替换为 `selectDelegate(context)`
- 所有 delegate 均被降级时返回降级 AiResult（复用 `handleEmptyDelegates` 模式）
- `applyStrategies()` 保持现有实现不变（已是正确的后置策略迭代逻辑，无需修改）
- **测试同步**：
  - `FallbackAiServiceTest`：新增多 delegate 场景——部分 delegate 被降级时 selectDelegate 跳过降级 delegate 选择下一个；所有 delegate 均被降级时返回降级 AiResult；混合降级策略+多 delegate 组合行为
  - `DegradationStrategy` 集成测试：验证基于 DegradationContext.serviceName/operationName 的决策逻辑

### 5. A01 — 业务 Service 改用 AiResultFactory
- **TriageServiceImpl**（L178/L180）：将 2 处 `AiResult.degraded(fallbackReason)` → `AiResultFactory.degraded(fallbackReason, (TriageResponse) null)`（使用 2 参重载，partialData=null，保持 errorCode=null 不变）；补充 import `com.aimedical.modules.ai.api.AiResultFactory`
- **PrescriptionAuditServiceImpl**：确认当前无 `AiResult.success/failure/degraded()` 静态调用，仅确保 import 存在
- **PrescriptionAssistServiceImpl**：确认当前无 `AiResult.success/failure/degraded()` 静态调用，仅确保 import 存在
- **MedicalRecordServiceImpl**：已使用 AiResultFactory（M08 修复），跳过

### 实施顺序建议
A10(配置) → C07+A02(超时注入) → A05(MockAiService) → A06(降级框架) → A01(AiResultFactory替换)

### 涉及文件（预计 14-18 个）
**生产文件：**
- TriageServiceImpl.java
- PrescriptionAuditServiceImpl.java
- PrescriptionAssistServiceImpl.java
- application.yml（main + test）
- MockAiService.java
- MockAdminController.java（新建）
- DegradationContext.java
- FallbackAiService.java

**测试文件（需同步更新）：**
- TriageServiceImplTest.java（构造参数同步）
- PrescriptionAuditServiceImplTest.java（构造参数同步，含第 2 处实例化 L402）
- PrescriptionAssistServiceImplTest.java（构造参数同步）
- FallbackAiServiceTest.java（多 delegate + 降级跳过 + 全降级回退）
- MockAiServiceTest.java（@Profile 注解 + 3 种策略模式）
- MockAdminController 端点测试（新建或内联，GET/POST /api/admin/ai/mock/strategy）
- DegradationStrategyTest.java（基于 serviceName/operationName 集成测试）

## 选择理由
C07(P1) AI 超时；A02(P0)+A10(P2) 超时配置群组；A05(P1) Mock 契约对齐；A06(P1) 降级框架完善；A01(P1) AiResultFactory 使用——A01 是 A07/A09/A11 修复的自然扩展，同为 AiResult 契约对齐。R18 已验证通过，此轮为剩余 PENDING 轮次中的首轮（R19→R23 依次推进）。

## 任务上下文
- C07: TriageServiceImpl.java:96 `future.get()` 无超时参数。OOD §2.3 要求`ai.timeout.triage=8s`
- A02: PrescriptionAuditServiceImpl.java:81 `future.get()` 无超时；PrescriptionAssistServiceImpl.java:78 `future.get()` 无超时
- A10: application.yml 仅含 JWT 配置
- A05: MockAiService 使用 `@ConditionalOnProperty` 而非 `@Profile("mock")`，无三种返回模式，无 MockAdminController
- A06: DegradationContext 空类；FallbackAiService 仅取 delegates.get(0)
- A01: 各 Service 使用 AiResult 的 static 方法而非 AiResultFactory

## 已有代码上下文
- AiResultFactory 已定义完整的 static 工厂方法：success/failure/degraded 各重载
- MedicalRecordServiceImpl 已使用 AiResultFactory 和 @Value 超时注入（R11/R14）
- MockAiService 当前在 ai-impl 模块，13 个方法全部实现
- FallbackAiService 当前使用 delegates.get(0) + applyStrategies
- DegradationStrategy 接口已定义 shouldDegrade(DegradationContext)
- DegradationContext 为空类（仅默认构造器）
- NoOpDegradationStrategy 作为默认降级策略（@ConditionalOnMissingBean）
- TriageServiceImpl 构造器无 timeout 参数，@Value 需新增
- PrescriptionAuditServiceImpl 已有 `drugFacadeTimeout` @Value，需追加 `aiTimeout`
- PrescriptionAssistServiceImpl 已有 `drugFacadeTimeout` @Value，需追加 `aiTimeout`

---

## 修订说明（v19 r1）
| 审查意见 | 修改措施 |
|---------|---------|
| [一般] FallbackAiService.applyStrategies() 描述与实际不匹配：applyStrategies() 已是正确的后置策略迭代逻辑，真正使用 delegates.get(0) 的是 13 个具体方法 | §4(A06) 重写：改为新增私有 selectDelegate(DegradationContext) 辅助方法，在 13 个方法中替换 delegates.get(0)；明确 applyStrategies() 保持现有实现不变 |
| [轻微] PrescriptionAssistServiceImpl future.get() 行号 ~L78 应为 L91 | §1 行号修正为 L91，描述改为 "prescriptionAssist 方法内" |
| [轻微] A01 覆盖的 3 个 Service 中仅 TriageServiceImpl 有实际迁移内容（L178/L180 共 2 处 AiResult.degraded） | §5(A01) 重写：分 Service 标注——TriageServiceImpl 含 2 处实际替换，PrescriptionAuditServiceImpl 和 PrescriptionAssistServiceImpl 注明"确认无迁移内容" |

## 修订说明（v19 r2）
| 审查意见 | 修改措施 |
|---------|---------|
| [严重] 3 个 Service 构造器新增 aiTimeout 参数后测试文件未同步更新（TriageServiceImplTest、PrescriptionAuditServiceImplTest 含第 2 处实例化、PrescriptionAssistServiceImplTest） | §1 补充"测试同步"子项，逐文件列出 aiTimeout 追加位置；涉及文件清单补充 3 个测试文件 |
| [严重] FallbackAiService.selectDelegate 引入后 FallbackAiServiceTest 未覆盖多 delegate+降级跳过+全降级回退场景 | §4(A06) 补充"测试同步"子项：FallbackAiServiceTest 新增多 delegate/降级跳过/全降级回退 3 类场景；涉及文件清单补充 FallbackAiServiceTest.java |
| [严重] MockAiService @Profile 切换后测试断言失效 + 3 种新策略模式未覆盖；MockAdminController 端点无集成测试 | §3(A05) 补充"测试同步"子项：MockAiServiceTest 覆盖 @Profile 注解+3 种策略模式；新增 MockAdminController 端点测试；涉及文件清单补充 |
| [一般] DegradationContext 字段扩展后 DegradationStrategy 测试未覆盖基于 serviceName/operationName 的决策逻辑 | §4(A06) 测试同步补充 DegradationStrategy 集成测试要求；涉及文件清单补充 DegradationStrategyTest.java |
| [一般] A01 AiResult.degraded() → AiResultFactory.degraded() 替换未说明重载选择 | §5(A01) 明确指定使用 AiResultFactory.degraded(fallbackReason, (TriageResponse) null) 2 参重载（partialData=null，errorCode 保持 null） |
| [轻微] 涉及文件清单未包含测试文件，与历史轮次惯例不一致 | 涉及文件清单拆分为"生产文件"和"测试文件"两组，共列出 7 个测试文件 |
