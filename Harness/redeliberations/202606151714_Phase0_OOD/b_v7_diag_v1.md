# 质量审查报告 — v7 设计文档

## 审查范围

- 产出：Phase 0 最小化骨架 OOD 设计方案（a_v7_design_v1.md）
- 审查维度：需求响应充分度、事实错误/逻辑矛盾、深度与完整性
- 视角：从实际落地出发，评估设计是否可直接指导编码实现

---

## 发现问题

### 问题 1：common 模块缺少 `spring-boot-starter-data-jpa` 依赖声明，直接编码将导致编译失败

**问题描述**：common 模块包含 BaseEntity（使用了 `@Id`、`@GeneratedValue`、`@MappedSuperclass`、`@EntityListeners`、`@CreatedDate`、`@LastModifiedDate`、`@SQLDelete`、`@SQLRestriction` 等 JPA/Hibernate 注解）和 JpaConfig（使用了 `@EnableJpaAuditing`），但 2.2 节中 common 模块的依赖描述为"依赖 Spring Boot Starter 基础库及 spring-boot-starter-security（用于 SecurityConfig）"，未包含 `spring-boot-starter-data-jpa`。任何包含 BaseEntity 的模块（common 模块自身、common-module、所有业务模块）均无法通过编译。这是已迭代 7 轮后仍遗留的依赖声明遗漏。

**所在位置**：2.2 节 common 模块依赖描述

**严重程度**：严重

**改进建议**：在 2.2 节 common 模块的依赖说明中明确加入 `spring-boot-starter-data-jpa`，标注用途为"用于 BaseEntity JPA 注解及 JpaConfig 审计配置"；同时检查 common-module 是否需显式声明该依赖（若 User 等实体直接使用 `@Entity`、`@Table` 等注解，则建议也声明或通过在父 POM 的 dependencyManagement 中统一管理）。

---

### 问题 2：真实 AiService 实现与 FallbackAiService 的 Bean 共存机制未定义，Phase 2+ 装配存在设计漏洞

**问题描述**：3.4 节描述真实 AiService 实现（Phase 2+）"标注 `@ConditionalOnMissingBean(AiService.class)` 或相反条件，确保二者不会同时激活"。但 FallbackAiService 作为始终注册的 `@Primary` Bean（装饰器模式），本身是 AiService 接口的实现，因此 `@ConditionalOnMissingBean(AiService.class)` 在 Phase 2+ 永远不会匹配——FallbackAiService 的 Bean 已占用 AiService 类型。即使改变条件策略（如使用 `@ConditionalOnProperty` 的相反值），FallbackAiService 的装饰器逻辑在 Phase 2+ 中同样需要被调用者注入，而 FallbackAiService 标注了 `@Primary` 且实现 AiService，它与真实实现的二义性问题目前在文档中未得到清晰的共存策略定义。

**所在位置**：3.4 节「Bean 装配策略」及「装配条件汇总表」

**严重程度**：一般

**改进建议**：

- 方案 A：取消 FallbackAiService 的 `@Primary`，改由 `AiServiceConfig` 配置类显式构造并暴露一个装饰器 Bean（包装 MockAiService 或真实实现），开发者注入时按名称选择。
- 方案 B：明确 Phase 2+ 时 FallbackAiService 仍为 `@Primary`，真实实现不使用 `@ConditionalOnMissingBean` 而使用 `@ConditionalOnProperty(name = "ai.mock.enabled", havingValue = "false")`，FallbackAiService 通过 `ObjectProvider` 获取真实实现（MockAiService 在 `ai.mock.enabled=false` 时被排除，自然无冲突）。
- 无论采用哪种方案，均需更新「装配条件汇总表」增加 Phase 2+ 的列或说明。

---

### 问题 3：`ScheduleRequest.doctorIds` 字段类型与系统其他 doctor ID 字段类型不一致

**问题描述**：8.2 节 `ScheduleRequest` 的 `doctorIds` 字段类型定义为 `List<String>`，但系统中所有其他 doctor ID 相关字段均为 `Long` 类型，包括 `RecommendedDoctor.doctorId: Long`、`ScheduleItem.doctorId: Long`、`TaskItem.taskId: Long`（同为 ID 字段也使用 Long）。类型不一致会导致编码时产生不必要的类型转换和潜在的运行时 ClassCastException 风险。

**所在位置**：8.2 节 ScheduleRequest DTO 定义

**严重程度**：一般

**改进建议**：将 `ScheduleRequest.doctorIds` 的类型统一为 `List<Long>`，与其他 ID 字段类型保持一致。

---

### 问题 4：`springdoc-openapi` 集成缺少依赖归属声明

**问题描述**：8.3 节要求集成 springdoc-openapi（Swagger 3）并给出了 YAML 配置示例，但全文档未说明 `springdoc-openapi-starter-webmvc-ui`（或其他具体 starter）依赖的归属模块。各业务模块（patient、doctor、admin）的 Controller 需要使用 `@Tag`、`@Operation` 注解，这些注解来自 springdoc-openapi 依赖。若该依赖未被明确声明在任何模块的 POM 中，开发者需要自行判断放置位置，可能导致版本不一致或多处重复声明。

**所在位置**：8.3 节

**严重程度**：一般

**改进建议**：在 8.3 节明确 springdoc-openapi 依赖的归属模块（推荐在父 POM 的 `<dependencyManagement>` 中统一版本声明，各业务模块按需引入），并标注 Phase 0 的 springdoc-openapi 版本号或说明从父 POM 的 Spring Boot BOM 继承。

---

## 整体质量评价

产出整体质量较高，对需求中的 7 个维度均做了充分响应，经过 7 轮迭代后事实错误和逻辑矛盾已大幅收敛。接口契约（Result、PageQuery、ErrorCode、GlobalExceptionHandler）、权限模型（Role-Post-Function三级）、AI 能力契约（13 方法 + 26 DTO）、跨模块调用规范（Facade + Event）、本地开发体验（H2、Vite 代理）均已达到可直接指导编码的详细程度。

上述 4 个问题中，问题 1 为严重级别（编译阻断），问题 2/3/4 为一般级别（设计漏洞或类型不一致），建议修复后发布。
