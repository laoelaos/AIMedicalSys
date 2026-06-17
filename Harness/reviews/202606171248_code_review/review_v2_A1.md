# R2: AI Module (ai-api + ai-impl) Consistency with OOD Design §3.4

审查时间：2026-06-17 12:48 UTC

### 审查范围

- `AIMedical/backend/ai-api/src/main/java/com/aimedical/modules/ai/api/AiService.java`
- `AIMedical/backend/ai-api/src/main/java/com/aimedical/modules/ai/api/AiResult.java`
- `AIMedical/backend/ai-api/src/main/java/com/aimedical/modules/ai/api/degradation/DegradationContext.java`
- `AIMedical/backend/ai-api/src/main/java/com/aimedical/modules/ai/api/degradation/DegradationStrategy.java`
- `AIMedical/backend/ai-impl/src/main/java/com/aimedical/modules/ai/impl/degradation/NoOpDegradationStrategy.java`
- `AIMedical/backend/ai-impl/src/main/java/com/aimedical/modules/ai/impl/mock/MockAiService.java`
- `AIMedical/backend/ai-impl/src/main/java/com/aimedical/modules/ai/impl/fallback/FallbackAiService.java`
- `AIMedical/backend/ai-api/src/main/java/com/aimedical/modules/ai/api/dto/triage/TriageRequest.java`
- `AIMedical/backend/ai-api/src/main/java/com/aimedical/modules/ai/api/dto/triage/TriageResponse.java`
- `AIMedical/backend/ai-api/src/main/java/com/aimedical/modules/ai/api/dto/triage/RecommendedDepartment.java`
- `AIMedical/backend/ai-api/src/main/java/com/aimedical/modules/ai/api/dto/diagnosis/DiagnosisRequest.java`
- `AIMedical/backend/ai-api/src/main/java/com/aimedical/modules/ai/api/dto/diagnosis/DiagnosisResponse.java`
- All 27 DTO files under `ai-api/src/main/java/com/aimedical/modules/ai/api/dto/`

### 发现

#### [轻微] Empty-delegate ERROR log fires at first invocation, not at startup

- **位置**: `FallbackAiService.java:60-67`
- **描述**: §3.4 规定"启动期输出 ERROR 日志、运行期输出 WARN 日志"。当前实现使用 `AtomicBoolean` 判断首次调用以输出 ERROR、后续调用输出 WARN。但首次调用可能是运行期而非启动期，文字描述与实现存在细微偏差。
- **建议**: 若期望启动期即输出 ERROR，可在构造器中执行一次空列表检查并立即输出 ERROR；或确认当前按调用时机区分 ERROR/WARN 的行为可接受后在文档中澄清。

#### [轻微] @phase0-mock-field annotation not yet defined in codebase

- **位置**: `Docs/04_ood_phase0.md:§8.2`
- **描述**: 设计文档 §8.2 引用了 `@phase0-mock-field` 注解约定用于标记 Phase 0 Mock 字段子集，但代码库中不存在该 Java 注解定义（grep 未命中）。当前 DTO 字段虽按约定实现（`TriageRequest.chiefComplaint`、`TriageResponse.recommendedDepartments` + `reason`、`RecommendedDepartment.departmentName`），但缺少注解层面的显式标记。
- **建议**: 如需强制冻结语义，应定义 `@Phase0MockField` 注解；或确认该约定仅为文档级标记、不要求代码标注后在设计文档中明确说明。

### 已确认符合项

#### Part A: AiService interface — 全部通过

- AiService 声明为 `interface`（非 abstract class），符合 §3.4"为何使用 interface" ✓
- 包含 13 个方法，覆盖所有 13 项 AI 能力（triage, diagnosis, prescriptionCheck, generateMedicalRecord, analysisReportForInspection, analysisReportForLabTest, imageAnalysis, knowledgeBaseQuery, recommendExamination, prescriptionAssist, recommendExecutionOrder, schedule, discussionConclusion）✓
- 每个方法返回 `CompletableFuture<AiResult<T>>` ✓
- 方法签名、输入/输出 DTO 类型与 §8.2 表格完全一致 ✓

#### Part B: AiResult class — 全部通过

- 字段: `success` (boolean), `data` (T), `errorCode` (String), `degraded` (boolean), `fallbackReason` (String) — 与 §3.4 一致 ✓
- 独立于 `Result<T>`，属独立 class ✓
- 包含静态工厂方法 `success()`、`failure()`、`degraded()` ✓

#### Part C: Degradation framework — 全部通过

- `DegradationContext`: 仅含无参构造器、无业务字段，符合 Phase 0 零值设计 ✓
- `DegradationStrategy`: interface，仅有 `boolean shouldDegrade(DegradationContext context)` 方法，无泛型 fallback 方法 ✓
- `NoOpDegradationStrategy`: `shouldDegrade()` 始终返回 false；标注 `@Component` + `@ConditionalOnMissingBean(DegradationStrategy.class)` ✓

#### Part D: MockAiService — 全部通过

- 实现 `AiService` 接口 ✓
- 标注 `@ConditionalOnProperty(name = "ai.mock.enabled", havingValue = "true", matchIfMissing = true)` ✓
- 所有 13 个方法返回 `CompletableFuture.completedFuture(AiResult.success(...))` ✓
- Triage mock 数据遵循 §8.2 约定: `"mock_departmentName"` (字符串 = `"mock_" + 字段名`)、`recommendedDepartments` 含 1 条占位数据（集合 1-2 条）、`"mock_reason"` ✓
- 其余 12 个能力返回空壳 DTO，符合 §8.2"未进入 Mock 演示子集的 DTO 允许保持空壳" ✓

#### Part E: FallbackAiService — 全部通过

- 构造器签名 `public FallbackAiService(List<AiService>, List<DegradationStrategy>)` ✓
- 构造器中通过 `.filter(s -> !(s instanceof FallbackAiService))` 排除自身 ✓
- 委托列表为空时返回 `AiResult.degraded("No available AiService delegate")` 等价于 `AiResult(success=false, degraded=true, data=null)` ✓
- 首次调用 `log.error`、后续调用 `log.warn` ✓
- `applyStrategies` 在非成功/非降级结果上遍历 DegradationStrategy 列表判断是否触发降级 ✓
- 降级触发时构造 `DegradationContext` 实例并传入 `shouldDegrade()` ✓

#### Part F: DTO file structure — 全部通过

- 所有 27 个 DTO 文件存在，覆盖 12 个目录（diagnosis, discussion, examination, execution, image, inspection, kb, labtest, medicalrecord, prescription(assist+check), schedule, triage）✓
- `TriageRequest` 含 `chiefComplaint` 字段 ✓
- `TriageResponse` 含 `recommendedDepartments` & `reason` ✓
- `RecommendedDepartment` 含 `departmentName` ✓
- `DiagnosisRequest` / `DiagnosisResponse` 保留空壳 class ✓

#### Bean 装配策略 — 全部通过

- MockAiService: `@ConditionalOnProperty(name = "ai.mock.enabled", havingValue = "true", matchIfMissing = true)` → 默认激活 ✓
- NoOpDegradationStrategy: `@ConditionalOnMissingBean(DegradationStrategy.class)` → 无其他策略时默认注册 ✓
- FallbackAiService: 始终注册（仅 `@Service`，无条件注解）✓
- 依赖方向: ai-impl → ai-api → common，符合 §2.2 ✓

### 本轮统计

| 严重程度 | 数量 |
|---------|------|
| 严重 | 0 |
| 一般 | 0 |
| 轻微 | 2 |

### 总评

AI Module (ai-api + ai-impl) 的代码实现与 OOD 设计文档 §3.4 高度一致。所有核心抽象（`AiService` interface、`AiResult<T>`、降级策略框架、`MockAiService`、`FallbackAiService`）均按设计准确实现。13 个 AI 能力方法签名、DTO 目录结构、条件装配注解均符合设计规范。仅发现两处轻微可改进项，不影响 Phase 0 骨架运行的验收标准。
