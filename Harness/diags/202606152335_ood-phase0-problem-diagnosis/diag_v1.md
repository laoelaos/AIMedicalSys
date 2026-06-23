# 诊断报告：Docs/04_ood_phase0.md 矛盾与错误分析

## 1. 概述

对 Phase 0 OOD 设计方案（Docs/04_ood_phase0.md）进行逐项审查，对照需求文档（Docs/01_requirement.md）和路线图（Docs/03_roadmap.md），识别出以下会阻塞或严重影响后续编码的问题。

---

## 2. 阻塞级问题（未修复则编码阶段无法正常推进）

### 2.1 中间层聚合 POM 无法作为独立构建入口

**位置**：§2.2，第 224–268 行

**现象**：OOD 声明中间层聚合 POM（`modules/common-module/pom.xml`、`modules/ai/pom.xml`）是 "mvn -pl modules/common-module -am 的独立构建入口"。但根 POM（backend/pom.xml）的 `<modules>` 列表（第 143–154 行）中直接列出的是叶子模块 `modules/common-module/common-module-api`、`modules/common-module/common-module-impl`、`modules/ai/ai-api`、`modules/ai/ai-impl`，并未包含 `modules/common-module` 或 `modules/ai` 聚合模块。

**根因**：Maven 的 `-pl` 参数只能选用 reactor 中已声明的模块。聚合 POM 未被根 POM 的 `<modules>` 收录，Maven 无法识别其为可构建目标。

**影响**：执行 `mvn -pl modules/common-module -am` 从项目根目录（backend/）构建时，Maven 会报错 "Could not find the selected project in the reactor"。开发者尝试按文档描述独立构建公共模块会失败，直接阻塞本地开发和 CI 分阶段构建。

### 2.2 健康检查端点 `/api/ping` 无归属模块

**位置**：§4.1 第 680–691 行 vs §8.1 第 861–867 行

**现象**：§4.1 定义了 `GET /api/ping` 端点作为 Phase 0 验收标准，但 §8.1 要求所有 Controller 基路径统一为 `/api/{module}`。`/api/ping` 不属于任何业务模块，文档未指定它在哪个模块的 Controller 中实现，也未说明是否在 application 模块中创建专用 HealthController。

**影响**：编码人员无法确定 ping 端点的代码归属，不同开发者可能自行选择不同模块实现，导致验收时端点路径不一致或重复实现。

### 2.3 `common-module-api` 和 `ai-api` 的 "零外部依赖" 与 transitive 依赖冲突

**位置**：§2.2 第 272–307 行

**现象**：文档多处描述 `common-module-api` 和 `ai-api` 为 "零外部依赖，仅依赖 common"。但实际上 **common 模块以 compile scope（非 optional）依赖 spring-boot-starter-web 和 spring-boot-starter-data-jpa**，因此 `common-module-api` 和 `ai-api` 通过 transitive 依赖链无条件继承完整的 Spring Web（含 Tomcat 嵌入式容器）和 JPA/Hibernate 依赖栈。

**影响**：
- "零外部依赖"的声明具有误导性，开发者可能误认为可以安全地给 API 模块添加轻量级依赖
- 纯接口 + DTO 模块的类路径中包含不必要的重型依赖，虽不引发运行时错误，但违背设计的模块隔离意图
- 后续如果有纯前端或非 Spring 环境需要独立编译 ai-api DTO 时，会因依赖解析失败受阻

### 2.4 `UserType` 枚举未定义归属模块

**位置**：§3.3 第 536 行，§8.4 第 1072 行

**现象**：`User` 实体定义在 `common-module-impl` 中，其 `userType` 字段为枚举类型区分患者/医生/管理员。同时 `common-module-api` 中的 `UserDTO` 也需携带 `userType` 字段。如果 `UserType` 枚举定义在 `common-module-impl` 中，则 `common-module-api` 的 `UserDTO` 无法引用它（会引入对 impl 模块的依赖，违反架构规则）。如果定义在 `common` 中，虽可访问但文档未说明。

**影响**：编码时若 `UserType` 枚举定义位置不当，会导致 `UserDTO` 编译失败或架构违规。此问题必须在编码前明确。

---

## 3. 严重级问题（虽不阻塞启动，但会引发运行时错误或大量返工）

### 3.1 `PermissionService` 注入在 Phase 0 会导致 Bean 创建失败

**位置**：§3.3 第 531 行，§8.4 第 1072–1083 行

**现象**：§8.4 定义了 `PermissionService` 作为业务模块的门面注入接口，示例如下：
```java
@Service
public class PatientServiceImpl implements PatientService {
    private final PermissionService permissionService;
    // 通过构造器注入
}
```
但 §3.3 和 §8.4 均声明 Phase 0 "仅提供实体与 Repository 骨架，不提供 `PermissionServiceImpl` 等门面实现"、"暂不实现任何跨模块调用"。

**影响**：如果 Phase 0 的业务模块占位 Service 类中构造器注入了 `PermissionService`，Spring 在启动时会因找不到可用 Bean 而抛出 `UnsatisfiedDependencyException`。文档未明确说明 Phase 0 的占位 Service 禁止注入 `PermissionService`，也未建议使用 `@Autowired(required = false)` 或 `@Lazy` 等方案。

### 3.2 `DegradationContext` 引用类型字段的 null 风险缺乏防御

**位置**：§3.4 第 645 行

**现象**：`DegradationContext` 的 `lastFailureTime`（`LocalDateTime`）、`elapsedTime`（`Duration`）、`requestType`（`String`）三个字段在 Phase 0 默认构造时取 `null`。文档仅提示 Phase 2+ 策略实现者需要做 null 防御性检查，但 Phase 2+ 的 `TimeoutDegradationStrategy` 可能直接访问这些字段导致 NPE，而此缺陷在 Phase 0 的 `NoOpDegradationStrategy`（始终返回 false）下不会被发现。

**影响**：Phase 2+ 首次启用真实降级策略时可能在生产环境出现难以追查的 NPE，且缺乏单元测试覆盖此边界。

### 3.3 配置 `ai.mock.enabled=false` 时 Phase 0 仅对 `ai-api` 模块可见的隐患

**位置**：§3.4 第 635 行

**现象**：Phase 0 在 `ai.mock.enabled=false` 时仅 `FallbackAiService` 注册，`List<AiService>` 排除自身后为空，所有 AI 调用直接返回降级结果。但业务模块在编译期依赖的是 `ai-api`（接口层），`ai-impl` 仅由 `application` 引入。如果某业务模块在 Phase 0 占位代码中直接调用 `aiService.triage(...)` 并 `join()` 等待结果，当降级返回 `AiResult(success=false, degraded=true, data=null)` 时，调用方若未对 `data=null` 做防御性检查，可能触发 NPE。

**影响**：文档虽说明了降级行为，但未要求 Phase 0 业务模块代码对 `AiResult.data` 做 null 检查。该问题在 `ai.mock.enabled=true`（默认配置）下不会被触发，一旦切到 `false` 配置就暴露。

### 3.4 权限矩阵 `○¹` 与 `○²` 的数据范围在 OOD 中未映射

**位置**：§3.3（整个权限模型章节）vs 需求文档 §2.6

**现象**：需求文档（01_requirement.md）的 §2.6 功能-角色可访问矩阵中定义了两种受限访问语义 `○¹`（创建者/责任人）和 `○²`（接诊/经手人），并要求后端按业务实体所属关系进行控制。但 OOD 的权限模型设计（§3.3）仅定义了 `Role`/`Post`/`Function` 三级实体结构和 `PermissionService` 接口，未涉及数据级权限（行级/实体级）的过滤策略，也未预留 `@DataFilter` 或类似 AOP 注解的扩展点。

**影响**：Phase 1 实现 `○¹` 和 `○²` 权限控制时，可能发现 OOD 设计的权限模型无法自然承载数据级权限，需要重新设计数据过滤机制。此问题虽不阻塞 Phase 0，但会导致 Phase 1 的 OOD 阶段需要额外的数据权限设计工作。

---

## 4. 中等级问题（虽不影响核心流程，但影响开发效率和一致性）

### 4.1 `common` 依赖 `spring-boot-starter-data-jpa` 为 compile（非 optional）

**位置**：§2.2 第 306 行

**现象**：所有业务模块（含纯接口模块）通过 common 的 transitive 依赖继承 JPA/Hibernate 栈，即使它们不需要 JPA（如 `ai-api` 和 `common-module-api`）。文档的评估理由 "所有业务模块均包含 JPA 实体和 Repository" 不适用于纯 API 模块。

**影响**：增大了纯接口模块的类路径体积，但无运行时错误。

### 4.2 `common` 模块的 `common.config` 包中包含 `SecurityConfigPhase0`

**位置**：§4.5 第 752–768 行

**现象**：Phase 0 的 `SecurityConfigPhase0` 归属 `common.config`（在 common 模块中）。但 common 模块的职责是 "共享基础模块（无业务逻辑）"，SecurityConfig（即使是 Phase 0 的 permitAll 版本）本质上是一种安全策略配置。与 `JpaConfig`（基础设施配置）不同，安全配置随阶段变化，放在 common 中意味着每次需要调整安全策略时都要修改 common 模块。

**影响**：common 模块作为零业务逻辑的基础模块，本应极少变更。将随阶段演进的安全配置放在这里增加了 common 的变更频率，偏离了其设计定位。Phase 1 的 SecurityConfig 在 application 模块，而 Phase 0 的却在 common，配置分散不利于管理。

### 4.3 springdoc-openapi 的 Phase 0 集成与路线图表述不一致

**位置**：§8.3 第 1038–1057 行 vs 路线图 §0.4

**现象**：OOD 将 springdoc-openapi 配置作为 Phase 0 的内置内容，并在父 POM 中声明版本统一管理。但路线图 §0.4 将其列为 "推荐补齐（可在后续阶段首期补齐）"，即非 Phase 0 强制性内容。

**影响**：如果 Phase 0 的实际工期紧张，按路线图可以延后 springdoc-openapi 集成，但 OOD 已将其编入父 POM，需要额外工作移除依赖。

---

## 5. 低等级问题（建议性）

### 5.1 包命名规范中 `commonmodule` 连写与目录 `common-module` 连字符不完全一致

**位置**：§2.3 第 329–345 行

**现象**：目录使用 `common-module`（连字符），Java 包名使用 `commonmodule`（无连字符）。虽然符合 Java 包命名规范（不允许连字符），但未在文档中明确说明此映射关系，新手可能混淆。

### 5.2 CI 分阶段构建未包含 `common-module-api` 依赖中的 `common` 前置构建

**位置**：§10 第 1211 行

**现象**：CI 第一阶段构建 `common,modules/common-module/common-module-api,modules/ai/ai-api`，注意 `common-module-api` 本身的 POM 中应当依赖 `common`。由于第一阶段已包含 `common`，第二阶段中 `common-module-impl` 才能正确解析 `common`。

这里实际没有问题（common 在第一阶段已经构建），但 CI 分阶段的注释中未明确描述模块间的依赖解析顺序。

### 5.3 需求文档 3.4.4 中 `patient_info.comorbidities` 字段在 AiService DTO 中未定义

**位置**：OOD §8.2 第 940–946 行

**现象**：需求文档 3.4.4 的 `DiagnosisRequest` 应当包含 `patientInfo.comorbidities` 字段（合并症列表），但 OOD 的 `DiagnosisRequest` DTO 定义中未包含此字段。同理检查 3.4.9/3.4.10/3.4.13 对应的 DTO。

**影响**：后续阶段对接真实 AI 服务时可能发现输入 DTO 缺少关键字段，需要在 Phase 1+ 补全。

---

## 6. 综合影响评估

| 严重程度 | 数量 | 对编码的直接影响 |
|---------|------|----------------|
| 阻塞 | 4 | 编码启动前必须修复，否则无法推进 |
| 严重 | 4 | 需制定规避方案或提前修复，否则运行时暴露 |
| 中等 | 3 | 建议在 Sprint 计划中安排修复 |
| 低 | 3 | 文档改进，可在编码过程中同步修正 |

## 7. 根因汇总

- **OOD 与 Maven 构建机制脱节**：聚合 POM 的独立构建入口声明未经 Maven 构建验证
- **模块归属定义模糊**：`/api/ping` 端点、`UserType` 枚举缺少归属模块声明
- **依赖声明与实际制约不一致**："零外部依赖"与 common 的 transitive 重型依赖矛盾
- **Phase 0 边界条件考虑不足**：`PermissionService` 注入、`AiResult.data` null 检查等 Phase 0 不实现但在骨架代码中预设的场景
- **数据权限未纳入权限模型**：OOD 仅覆盖功能级权限（Function），未涉及数据级权限过滤
