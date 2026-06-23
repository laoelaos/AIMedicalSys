根据以下审查结果，迭代上一轮的产出，形成新版的文件，从而更好地满足用户需求。

## 当前审查结果

### 问题 1（严重）：common 模块缺少 `spring-boot-starter-data-jpa` 依赖声明，直接编码将导致编译失败

2.2 节 common 模块的依赖描述为"依赖 Spring Boot Starter 基础库及 spring-boot-starter-security（用于 SecurityConfig）"，未包含 `spring-boot-starter-data-jpa`。但 common 模块包含 BaseEntity（使用了 `@Id`、`@GeneratedValue`、`@MappedSuperclass`、`@EntityListeners`、`@CreatedDate`、`@LastModifiedDate`、`@SQLDelete`、`@SQLRestriction` 等 JPA/Hibernate 注解）和 JpaConfig（使用了 `@EnableJpaAuditing`），因此任何包含 BaseEntity 的模块均无法通过编译。

**改进建议**：在 2.2 节 common 模块的依赖说明中明确加入 `spring-boot-starter-data-jpa`，标注用途为"用于 BaseEntity JPA 注解及 JpaConfig 审计配置"；同时检查 common-module 是否需显式声明该依赖（若 User 等实体直接使用 `@Entity`、`@Table` 等注解，则建议也声明或通过在父 POM 的 dependencyManagement 中统一管理）。

### 问题 2（一般）：真实 AiService 实现与 FallbackAiService 的 Bean 共存机制未定义，Phase 2+ 装配存在设计漏洞

3.4 节描述真实 AiService 实现（Phase 2+）"标注 `@ConditionalOnMissingBean(AiService.class)` 或相反条件，确保二者不会同时激活"。但 FallbackAiService 作为始终注册的 `@Primary` Bean（装饰器模式），本身是 AiService 接口的实现，因此 `@ConditionalOnMissingBean(AiService.class)` 在 Phase 2+ 永远不会匹配——FallbackAiService 的 Bean 已占用 AiService 类型。即使改变条件策略，FallbackAiService 的装饰器逻辑在 Phase 2+ 中同样需要被调用者注入，而 FallbackAiService 标注了 `@Primary` 且实现 AiService，它与真实实现的二义性问题目前在文档中未得到清晰的共存策略定义。

**改进建议**：
- 方案 A：取消 FallbackAiService 的 `@Primary`，改由 `AiServiceConfig` 配置类显式构造并暴露一个装饰器 Bean（包装 MockAiService 或真实实现），开发者注入时按名称选择。
- 方案 B：明确 Phase 2+ 时 FallbackAiService 仍为 `@Primary`，真实实现不使用 `@ConditionalOnMissingBean` 而使用 `@ConditionalOnProperty(name = "ai.mock.enabled", havingValue = "false")`，FallbackAiService 通过 `ObjectProvider` 获取真实实现（MockAiService 在 `ai.mock.enabled=false` 时被排除，自然无冲突）。
- 无论采用哪种方案，均需更新「装配条件汇总表」增加 Phase 2+ 的列或说明。

### 问题 3（一般）：`ScheduleRequest.doctorIds` 字段类型与系统其他 doctor ID 字段类型不一致

8.2 节 `ScheduleRequest` 的 `doctorIds` 字段类型定义为 `List<String>`，但系统中所有其他 doctor ID 相关字段均为 `Long` 类型，包括 `RecommendedDoctor.doctorId: Long`、`ScheduleItem.doctorId: Long`、`TaskItem.taskId: Long`（同为 ID 字段也使用 Long）。类型不一致会导致编码时产生不必要的类型转换和潜在的运行时 ClassCastException 风险。

**改进建议**：将 `ScheduleRequest.doctorIds` 的类型统一为 `List<Long>`，与其他 ID 字段类型保持一致。

### 问题 4（一般）：`springdoc-openapi` 集成缺少依赖归属声明

8.3 节要求集成 springdoc-openapi（Swagger 3）并给出了 YAML 配置示例，但全文档未说明 `springdoc-openapi-starter-webmvc-ui`（或其他具体 starter）依赖的归属模块。各业务模块（patient、doctor、admin）的 Controller 需要使用 `@Tag`、`@Operation` 注解，这些注解来自 springdoc-openapi 依赖。若该依赖未被明确声明在任何模块的 POM 中，开发者需要自行判断放置位置，可能导致版本不一致或多处重复声明。

**改进建议**：在 8.3 节明确 springdoc-openapi 依赖的归属模块（推荐在父 POM 的 `<dependencyManagement>` 中统一版本声明，各业务模块按需引入），并标注 Phase 0 的 springdoc-openapi 版本号或说明从父 POM 的 Spring Boot BOM 继承。

## 历史迭代回顾

### 已解决的问题（出现在历史反馈但当前反馈中不再提及的问题）

以下在 Round 7 历史反馈中存在的问题均已在上轮（v7 设计）中修复，当前审查未再检出：
- 数据库驱动策略缺失（H2 内存数据库）：已在 2.2 节 application 模块依赖描述中补充 H2 运行时依赖声明，9.1 节补充 H2 数据源配置示例及 Phase 1+ 切换策略
- 前端 Vite 代理跨域配置不完整：已在 9.3 节补充 vite.config.ts proxy 配置示例
- CI 流水线第三阶段重复行：已删除
- common-module 包命名规范缺少 api 子包：已在 2.3 节补充

### 持续存在的问题（需重点解决）

以下 4 个问题在 Round 7 历史反馈和当前的 v7 诊断报告中同时出现，历经至少 2 轮迭代仍未修复，需在本轮（v8）重点解决：
1. **common 模块缺少 `spring-boot-starter-data-jpa` 依赖**（问题 1/严重）：自第 2 轮引入 `@MappedSuperclass`、`@Id` 等 JPA 注解后即应声明该依赖，但历经 6 轮迭代仍被遗漏，属编译阻断级别问题
2. **真实 AiService 与 FallbackAiService 的 Bean 共存机制未定义**（问题 2/一般）：v4 曾解决 FallbackAiService 的注入方式（改为 ObjectProvider），但 v7 诊断发现 `@ConditionalOnMissingBean(AiService.class)` 与 `@Primary` 的语义冲突仍未解决
3. **`ScheduleRequest.doctorIds` 类型不一致**（问题 3/一般）：`List<String>` 应为 `List<Long>`，简单类型修改但多次迭代未检出
4. **`springdoc-openapi` 依赖归属未声明**（问题 4/一般）：v5 在 8.3 节补充 springdoc-openapi 配置，但遗漏了 POM 依赖归属声明

### 新发现的问题（本轮新识别的问题）

无。本轮 4 个问题均已在 Round 7 历史反馈中出现。

## 上一轮产出路径
C:\Develop\Software\AIMedicalSys\Harness\redeliberations\202606151714_Phase0_OOD\a_v7_design_v1.md

## 用户需求
C:\Develop\Software\AIMedicalSys\Harness\redeliberations\202606151714_Phase0_OOD\requirement.md
