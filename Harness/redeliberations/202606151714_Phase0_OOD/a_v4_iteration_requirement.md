根据以下审查结果，迭代上一轮的产出，形成新版的文件，从而更好地满足用户需求。

## 当前审查结果

### 问题 1（严重）：CI 多阶段流水线存在事实错误——`mvn compile` 不安装到本地仓库，后续阶段依赖解析将失败

- **所在位置**: 第10节「CI 占位」
- **问题描述**: 流水线第一阶段执行 `mvn compile -pl common,modules/common-module,modules/ai/ai-api`，第二阶段执行 `mvn compile -pl modules/patient,modules/doctor,modules/admin,modules/ai/ai-impl`。`mvn compile` 不会将编译产物安装到本地 Maven 仓库。第二阶段构建时，patient/doctor/admin 等模块需要解析对 common 和 common-module 的编译期依赖，但第一阶段产物不在仓库中，Maven 无法通过 `-pl` 限定之外的模块解析依赖，第二阶段将直接失败。
- **改进建议**: 方案一：将前三个阶段中的 `mvn compile` 改为 `mvn install -DskipTests`（安装到本地仓库供后续阶段引用）。方案二：在一个 Maven 调用中完成全部模块编译（不拆分阶段），或每个阶段使用 `-am`（also-make）自动包含依赖模块。

### 问题 2（中等）：FallbackAiService 的底层 AiService 实例获取方式未定义，可能导致循环依赖

- **所在位置**: 第3.4节「Bean 装配策略」
- **问题描述**: FallbackAiService 标注 `@Primary` 作为装饰器始终注册为 Bean，内部需要持有底层 AiService 实现（MockAiService 或真实实现）。但设计未说明 FallbackAiService 如何获取该实现。如果 FallbackAiService 使用 `@Autowired private AiService aiService` 注入，由于自身标注了 `@Primary`，Spring 会优先注入自身实例，形成无限递归或循环依赖。
- **改进建议**: 明确 FallbackAiService 内部通过 `@Resource(name = "mockAiService")` 或 `@Qualifier` 按名称注入底层实现；或在设计决策中补充说明 Factory 模式或 DelegatingAiService 方案。

### 问题 3（中等）：`DegradationStrategy.shouldDegrade()` 方法签名缺少上下文参数

- **所在位置**: 第3.4节「降级策略框架」
- **问题描述**: 接口定义 `boolean shouldDegrade()` 无入参。一个降级判定接口如果不接收任何上下文信息（如当前调用耗时、失败次数、请求类型等），实现类无法做出有意义的降级决策。即使 Phase 0 的 Mock 实现返回固定值，接口签名也应预留给未来真实策略使用。
- **改进建议**: 将 `shouldDegrade()` 签名调整为接受调用上下文参数，例如 `boolean shouldDegrade(DegradationContext context)`，其中 DegradationContext 包含调用次数、上次失败时间、请求类型等关键信息；或在接口上注明 Phase 0 暂返回 false、Phase 2+ 再引入参数。

### 问题 4（中等）：8 个嵌套 DTO 类型缺少字段结构定义，无法直接指导编码

- **所在位置**: 第8.2节「AI 能力方法清单—DTO 核心字段定义」
- **问题描述**: 以下类型在 DTO 字段伪代码中被引用但自身未定义：`RecommendedDoctor`（TriageResponse）、`PrescriptionDrug`（PrescriptionCheckRequest）、`PatientInfo`（PrescriptionCheckRequest）、`Finding`（ImageAnalysisResponse）、`RecommendedExam`（ExaminationRecommendResponse）、`TaskItem`（ExecutionOrderRequest/Response）、`ScheduleItem`（ScheduleResponse）。缺乏这些嵌套类型的字段定义，下游编码时各模块可能自行推测字段结构，导致前端和后端、不同模块间的不一致。
- **改进建议**: 在 8.2 节末尾为上述每个嵌套类型补充核心字段伪代码定义，或标注"字段结构由 Phase 1 业务分析时细化，Phase 0 暂使用 `Map<String, Object>` 占位"（但需确保不影响 Result 的序列化统一性）。

### 问题 5（轻微）：业务模块显式依赖规则未列出 ai-api

- **所在位置**: 第2.2节「模块职责与依赖方向」—依赖规则列表
- **问题描述**: patient/doctor/admin 模块的依赖规则只列出 `common` 和 `common-module`，未显式列入 `ai-api`。`ai-api` 的依赖说明中提到"业务模块仅依赖 ai-api"，但两者分散在不同条目中。如果开发者仅阅读 patient 的依赖规则来配置 pom.xml，可能遗漏 `ai-api` 依赖，导致编译失败。虽然结合上下文可理解，但不是自包含的描述。
- **改进建议**: 在 `modules/patient/doctor/admin` 的依赖规则中显式补充 `ai-api`，改为"依赖 `common`、`common-module` 和 `modules/ai/ai-api`"，或注明"详见 ai-api 依赖规则"。

## 历史迭代回顾

### 已解决的问题（出现在历史反馈但当前反馈中不再提及）

- **迭代第1轮全部8个问题**：AI 方法标识命名问题、权限模型实体归属、同步非阻塞表述、BaseEntity 字段定义、MockAiService 注入机制、Security 配置骨架、User-UserDetails 适配、ai-api 编译期强制保障——均在 v2 修订中解决。
- **迭代第2轮全部6个问题**：ai.mock.enabled 默认值、DTO 类型名缺失、配置加载失败事实错误、ui-core 包定义、SecurityConfig 归属矛盾、CI 流水线构建顺序——均在 v3 修订中解决。

### 持续存在的问题（在多轮反馈中反复出现，需重点解决）

- **问题 1（CI 流水线）**：迭代第3轮首次报告 CI 流水线构建顺序问题（已通过四阶段重构解决），本轮发现底层更严重的事实错误——`mvn compile` 不产生本地仓库产物。同一个 CI 节已连续两轮出现问题，说明该部分需要更彻底的审查和测试。
- **问题 2（FallbackAiService 循环依赖）**：迭代第3轮首次报告，持续存在。
- **问题 3（shouldDegrade 缺少上下文）**：迭代第3轮首次报告，持续存在。
- **问题 4（嵌套 DTO 未定义）**：迭代第3轮首次报告，持续存在。

### 新发现的问题

- **问题 5（业务模块依赖规则未列出 ai-api）**：本轮新识别的依赖规则遗漏问题。

## 上一轮产出路径
C:\Develop\Software\AIMedicalSys\Harness\redeliberations\202606151714_Phase0_OOD\a_v3_design_v1.md

## 用户需求
C:\Develop\Software\AIMedicalSys\Harness\redeliberations\202606151714_Phase0_OOD\requirement.md
