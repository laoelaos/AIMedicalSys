# 质量审查报告 — Phase 0 OOD 设计方案 v4

## 审查范围

本报告审查的是第 4 轮迭代产出的 OOD 设计文档 `a_v4_design_v1.md`。前 3 轮内部审议已覆盖技术可行性、API 可编译性、依赖关系等维度，本审查侧重**需求响应充分度、全局一致性与落地完整性**，避免重复验证内部审议已确认的维度。

---

## 发现的问题

### 问题 1：`@EnableJpaAuditing` 未声明，`createdAt`/`updatedAt` 自动填充将静默失效

- **所在位置**：3.2 节「数据实体基类」BaseEntity 字段定义
- **严重程度**：严重
- **问题描述**：BaseEntity 的 `createdAt` 和 `updatedAt` 字段标注了 `@CreatedDate` / `@LastModifiedDate`，并依赖 `@EntityListeners(AuditingEntityListener.class)`，但这些注解生效的前提是**必须存在 `@EnableJpaAuditing` 配置**（通常放在 `@Configuration` 或 `@SpringBootApplication` 上）。当前设计未在任何位置提及此配置。开发者照此实现后，时间戳字段将始终为 `null`，且不会在编译期或启动期报错——这是一个静默失效缺陷。前 3 轮审议均未覆盖此问题。
- **改进建议**：在 3.2 节末尾补充 `@EnableJpaAuditing` 的配置位置声明（推荐放在 common 模块的 `config` 包的 `JpaConfig` 类中），可配套给出伪代码骨架。

---

### 问题 2：SecurityConfig 认证策略与 Phase 0 「骨架可运行」目标冲突

- **所在位置**：4.5 节「权限校验契约」SecurityConfig 设计骨架
- **严重程度**：严重
- **问题描述**：SecurityConfig 骨架描述为「`/api/ping` 为 `permitAll`，其余接口要求认证」。但在 Phase 0 中，三端前端需要展示占位首页并调用后端 API，而 Phase 0 **没有登录页面、没有认证 Controller、没有 token 签发机制**。如果骨架真的实施「其余接口要求认证」，则前端无法调用任何业务 API（含 AI Mock 接口）。文档中括号内的补充「可在 Phase 0 用 permitAll 临时放通」与「其余接口要求认证」的主描述矛盾——开发者不知道该以哪个为准。这不是内部审议未发现的「误解」，而是一个设计方案层面的**二选一未决**问题。
- **改进建议**：明确 Phase 0 的 SecurityConfig 策略——推荐**Phase 0 统一使用 `permitAll` 放通所有接口**，仅在代码中添加注释标记未来需要认证的位置；或者提供一个显式的 `@Profile("!phase0")` 或条件化配置来区隔 Phase 0 与后续阶段的认证策略。

---

### 问题 3：FallbackAiService 在 `ai.mock.enabled=false` 时注入失败

- **所在位置**：3.4 节「Bean 装配策略」及 FallbackAiService 描述
- **严重程度**：一般
- **问题描述**：FallbackAiService 设计为始终注册为 Bean，内部通过 `@Resource(name = "mockAiService")` 按名称注入底层实现。当 `ai.mock.enabled=false`（生产环境），MockAiService 不会被注册，导致 `@Resource(name = "mockAiService")` 抛出 `NoSuchBeanDefinitionException`——应用无法启动。v4 修订说明仅解决了 `@Primary` 自引用循环依赖问题，但**未解决按名称硬编码 "mockAiService" 在生产环境无 Bean 可用的问题**。这是 Spring Bean 装配方案的逻辑缺口。
- **改进建议**：推荐两种方案之一——（A）FallbackAiService 内部使用 `@Autowired` + `@Lazy` + `ObjectProvider<AiService>` 延迟获取可用实现，不依赖硬编码 Bean 名称；（B）在装配策略表格中补充「`ai.mock.enabled=false` 时 FallbackAiService 按名称注入真实实现的 beanName」的说明，明确区分两套注入配置。建议选择方案 A，更简洁且适用于所有环境。

---

### 问题 4：`PageRequest.page` 起始值未约定，存在后续模块间 off-by-one 风险

- **所在位置**：3.1 节「PageRequest / PageResponse<T>」字段描述
- **严重程度**：一般
- **问题描述**：PageRequest 的 `page` 字段描述中出现了「（从 0 或 1 开始）」的歧义表述。Spring Data JPA 的 `Pageable` 默认使用 **0-based** 索引，而前端分页组件（如 Element Plus 的 Pagination）默认使用 **1-based** 索引。如果不在设计层面冻结约定，Phase 1+ 各业务模块可能各自采用不同约定，导致分页混乱。前 3 轮审议未涉及此问题。
- **改进建议**：删除「从 0 或 1 开始」，明确约定为 0-based（与 Spring Data JPA 默认一致），并在 PageRequest 的 `page` 字段上添加 Javadoc 注释示例。同时在前端 `packages/shared/api/` 中补充分页参数适配逻辑说明（如前端 1-based → 后端 0-based 转换）。

---

### 问题 5：`Boolean` 类型的 `deleted` 字段存在空指针风险

- **所在位置**：3.2 节「BaseEntity」字段定义 `deleted: Boolean`
- **严重程度**：一般
- **问题描述**：逻辑删除标记使用了包装类型 `Boolean` 而非基本类型 `boolean`。虽然标注了 `@Column(nullable = false)` 约束了数据库列不可为空，但 Java 层面 `Boolean` 仍可被赋值 `null`。若在业务代码中出现 `if (entity.getDeleted())` 的自动拆箱操作，`null` 值将抛出 `NullPointerException`。Phase 0 虽无业务逻辑，但 BaseEntity 作为全量实体的基类，其类型选择错误将被传播到所有后续阶段。
- **改进建议**：将 `deleted` 字段类型从 `Boolean` 改为 `boolean`（基本类型），同时保持 `@Column(nullable = false)` 注解以确保 DDL 正确。建议同步在 3.2 节补充逻辑删除的实现方式说明（如 `@SQLDelete` + `@Where` 注解的具体用法）。

---

### 问题 6：前端 26+ DTO 类型定义与后端同步机制未定义

- **所在位置**：8.2 节「AI 能力方法清单 + DTO 核心字段定义」及 2.4 节「packages/shared/types/」
- **严重程度**：一般
- **问题描述**：设计定义了 26 个 Java DTO 类型（含 8 个嵌套 DTO），并在 2.4 节的前端 `packages/shared/types/` 中要求维护对应的 TypeScript 类型定义。但设计**未定义前后端类型的一致性和同步机制**。Phase 0 中 6 人以上并行协作（4+ 后端模块 + 3 端前端），手动同步 26+ 个 DTO 类型将导致以下风险：（1）后端字段变更后前端未感知，运行时类型不匹配；（2）不同类型的前端应用导入同类型但存在差异，调试困难。前 3 轮审议聚焦于 DTO 字段结构本身的完整性，未涉及跨端一致性保障。
- **改进建议**：推荐以下方案之一——（A）在 8.3 节（API 文档工具集成）中补充 OpenAPI 规范生成配置（如 springdoc-openapi 的 `springdoc.api-docs.path`），并声明前端通过 openapi-generator 自动生成 TypeScript 类型；（B）若 Phase 0 不做代码生成，应在 2.4 节明确声明「Phase 0 各 DTO 的 TypeScript 类型由人工维护并纳入 Code Review，Phase 1+ 引入 openapi-generator」。

---

### 问题 7：`Integration` 模块用途完全未定义

- **所在位置**：2.1 节目录布局 `integration/` 条目及第 10 节 CI 占位
- **严重程度**：轻微
- **问题描述**：目录布局中出现了 `integration/` 条目并标注「集成测试模块（选配）」，但通篇未对它的职责、与业务模块的关系、Phase 0 是否要初始化该模块做任何说明。CI 流水线（第 10 节）也未包含该模块的构建步骤。这意味着开发者无法判断该目录是「需要创建的空模块」还是「仅作提示的注释」，结果上它将成为未使用的遗留目录。
- **改进建议**：（A）若 Phase 0 需要集成测试骨架，在 2.1 节补充 `integration/` 的职责和 Phase 0 的交付物要求（如一个占位测试类、Failsafe 插件配置），并在 CI 第四阶段加入该模块的执行；**或**（B）若 Phase 0 不交付集成测试，从目录布局中删除 `integration/` 条目，避免误导。

---

### 问题 8：设计决策表 API 版本管理条目的策略描述不足以指导编码

- **所在位置**：第 7 节设计决策表「API 版本管理」行
- **严重程度**：轻微
- **问题描述**：需求 4.3 明确要求「API 版本管理策略」。设计决策表的实际决策是「无显式版本（通过模块路径隐含）」，同时在正文中说「如需对外暴露版本，后续引入 URL 路径版本」。但 8.1 节定义的 Controller 路径前缀为 `/api/{module}/...`，并未包含版本号段（如 `/api/v1/`）。这两处信息共同暗示「Phase 0 不做 API 版本管理」，但未以策略文档的形式明确写出——开发者需要自行拼凑上下文才能理解版本管理策略是什么。作为一份自称「可直接指导编码实现」的设计文档，版本管理策略的表述应更加明确，而非分散在决策表和正文中。
- **改进建议**：在 8.1 节开头用一句话总结 API 版本管理策略，例如：「Phase 0~Phase 6 在同一主版本内演进，API 路径不含版本号段；Controller 基路径统一为 `/api/{module}`；如需对外暴露版本（如开放平台场景），Phase 6+ 引入 `/api/v2/` 版本路径。」

---

## 整体质量评价

文档整体质量较高，经过 3 轮内部审议后，技术可行性方面的主要问题已得到纠正。但仍存在以下类型的问题：

1. **静默失效风险**（问题 1、5）：Spring 配置缺失和类型选择不当，不会导致编译失败或启动报错，而是运行时功能异常——这类问题最易在实现阶段漏过。
2. **方案层面二选一未决**（问题 2、3）：同一文档中出现了互相矛盾的描述或逻辑缺口，导致实现者无法做出确定的选择。
3. **跨端协作缺口**（问题 6）：前后端类型同步是 Phase 0 多人并行协作的实操痛点，设计文档未覆盖此维度。
4. **歧义约定**（问题 4、7、8）：一些细节约定未冻结，可能被不同模块采用不同解释。
