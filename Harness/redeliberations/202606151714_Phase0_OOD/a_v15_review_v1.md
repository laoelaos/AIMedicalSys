# OOD 设计方案审查报告（v15）

## 审查结果

APPROVED

## 逐维度审查

### 1. 类型系统可行性

**[通过]** 所有类型形态选择与 Java 17+ 类型系统能力完全匹配：

- `Result<T>`、`PageResponse<T>`、`AiResult<T>` 为泛型 class，符合标准 Java 泛型用法
- `ErrorCode` 定义为 interface + 各模块 enum 实现，Java enum 可 implements interface，满足跨模块错误码统一引用需求
- `BaseEntity` 为 abstract class + `@MappedSuperclass`，子类继承共享字段，符合 JPA 规范
- `AiService` 为 interface 含 13 个具名类型化方法，可在 Java 中直接实现
- `LoginUser` 实现 `UserDetails` 为 Adapter 模式，单继承 + 多接口实现在 Java 范围内
- `DegradationStrategy` 取消泛型 fallback 后仅含 `boolean shouldDegrade(DegradationContext context)`，无泛型约束问题
- `FallbackAiService` 构造器注入 `List<AiService>` 排除自身（`instanceof` 检查），逻辑在 Java 类型系统中表达无障碍
- 协作关系中的类型交互（interface 调用、回调、事件驱动 POJO）均为 Java 标准模式

### 2. 标准库与生态覆盖

**[通过]** 设计中的能力全部在标准库/常用库覆盖范围内：

- Spring Boot 3（MVC、Data JPA、Security）覆盖后端全部基础设施需求
- H2 内存数据库满足 Phase 0 开发期运行需求；Phase 1+ 切换 MySQL/PostgreSQL 的过渡策略合理
- springdoc-openapi (Swagger 3) 覆盖 API 文档自动生成需求
- Vue 3 + Vite + TypeScript + Axios + Pinia 覆盖前端全部需求，Vite workspace 管理 Monorepo 内部包
- Maven Failsafe + Surefire 覆盖集成测试与单元测试需求
- Hibernate Validator (`@Valid`) 覆盖参数校验需求
- `@Profile("phase0")` 覆盖 SecurityConfig 阶段切换需求，零代码变更
- `@ConditionalOnProperty` / `@ConditionalOnMissingBean` 覆盖 Bean 条件化装配需求
- `@SQLDelete` + `@SQLRestriction`（Hibernate 6.2+）覆盖软删除需求
- 所有假设的库能力（`npm run build --workspaces --if-present`、`mvn install -DskipTests` 分阶段构建、`<classifier>exec</classifier>` 等）均经验证可行

### 3. 语言特性可行性

**[通过]** 所有语言特性方案在 Java/TypeScript 中可行：

- 错误处理：`BusinessException extends RuntimeException` → `@ControllerAdvice` + `@ExceptionHandler` → `Result`，标准 Spring 错误处理链
- 并发设计：Phase 0 同步阻塞（MockAiService 直接返回），Phase 2+ 预留 Spring Async + `CompletableFuture` 异步方案，分阶段清晰
- 资源管理：Tomcat 线程池（Spring Boot 默认）、JPA 连接池（HikariCP）、H2 内存库由 Spring Boot 自动管理
- 模块组织：Maven 多模块 + api/impl 子模块拆分实现编译期强制隔离，包命名规范统一
- Bean 装配互斥：`@ConditionalOnProperty` 正反条件实现 MockAiService 与真实实现互斥，避免 `@ConditionalOnMissingBean` + `@Primary` 语义冲突
- 降级策略：`@ConditionalOnMissingBean` 实现 NoOpDegradationStrategy 默认注册 + 被真实策略替换的自动退让机制
- 认证阶段切换：`@Profile("phase0")` / `@Profile("!phase0")` 实现配置驱动零代码切换

### 4. 设计一致性

**[通过]** 各抽象的职责清晰、协作闭环、无缺失环节：

- 模块依赖方向正确：patient/doctor/admin → common-module-api（已修复 v14 箭头矛盾）；common-module-impl 和 ai-impl 仅由 application 引入
- 父 POM dependencyManagement 已补充 spring-boot-starter-security 条目（已修复 v14 缺失问题）
- 前端 build:all 已改用 `--if-present` 静默跳过无 build 脚本的 workspace（已修复 v14 兼容性风险）
- DegradationStrategy 已取消泛型 fallback，降级结果由 FallbackAiService 直接构造 `AiResult(success=false, degraded=true, data=null)`（已修复 v14 泛型对齐问题）
- SecurityConfig 已改为 `@Profile("phase0")` 条件化配置（已修复 v14 手动改代码问题）
- 行为契约（健康检查、统一响应、AI 调用、分页查询、权限校验）完整可指导编码
- 模块间依赖方向合理，无循环依赖（Maven 层面 `dependencyManagement` 统一管控 + `mvn dependency:analyze` CI 验证）
- 跨模块调用规范（接口门面 + 事件驱动）已定义，模式选择原则清晰
- Spring Boot 包扫描配置（`scanBasePackages`、`@EntityScan`、`@EnableJpaRepositories`）完整

### 5. 设计质量

**[通过]** 职责划分遵循 SRP、抽象层次恰当、便于测试和演进：

- 职责划分：
  - `Result<T>`：仅负责响应包装；`BusinessException`：仅负责业务异常；`GlobalExceptionHandler`：仅负责异常转换
  - `AiService`（interface）：仅定义 AI 能力契约；`MockAiService`：仅提供 Mock 占位；`FallbackAiService`（Decorator）：仅负责降级装饰
  - `LoginUser`（Adapter）：仅负责 User ↔ UserDetails 适配，User 实体不耦合 Spring Security
  - `ErrorCode`（interface）：仅定义契约，各模块 enum 独立实现，互不干扰
- 抽象层次恰当：
  - Phase 0 聚焦最小骨架，不过度设计（如 DegradationStrategy Phase 0 仅 NoOp 实现）
  - 预留了充分的扩展点（Real AiService、TimeoutDegradationStrategy、异步 AI 调用等）
  - 设计决策表中每项选择均附有明确理由
- 可测试性：
  - 接口抽象便于 Mock（AiService、DegradationStrategy、PermissionService 均为 interface）
  - `@ConditionalOnProperty` 允许测试环境切换实现
  - `MockAiService` 本身就是测试替身，可直接用于集成测试
  - Integration 模块（Failsafe）+ Unit 测试（Surefire）均有占位
  - 占位测试类要求覆盖每个模块，尽早建立测试规范
- minor note：「BaseEnum」在 2.1 节目录树中列举但设计正文中未定义其职责或使用方式。评审意见将其归类为轻微——这在架构级设计中是可接受的（目录树仅提示包结构），不影响设计可行性与编码指导，故不阻塞通过

## 修改要求

无严重或一般问题，不予驳回。
