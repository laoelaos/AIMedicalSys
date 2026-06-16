根据以下审查结果，迭代上一轮的产出，形成新版的文件，从而更好地满足用户需求。

## 当前审查结果

### 问题 1 [严重]：`spring-boot-starter-web` 未在任何模块中显式声明
- **所在位置**：第 2.2 节「模块职责与依赖方向」
- **问题描述**：产出定义了大量依赖 Spring MVC 的组件（`@RestController`、`@ControllerAdvice`、`@ExceptionHandler`、`SecurityConfig` 中的 `HttpSecurity` 配置等），但「Spring Boot Starter 基础库」表述模糊，未在任何模块中显式声明 `spring-boot-starter-web`。`spring-boot-starter-security` 和 `spring-boot-starter-data-jpa` 均不传递 `spring-webmvc`，导致 `@RestController`/`@RequestMapping` 等注解无法解析、`@ControllerAdvice`/`@ExceptionHandler` 无法生效、嵌入式 Tomcat 容器不会启动，直接破坏「骨架可运行」目标。
- **改进建议**：在第 2.2 节 common 的依赖描述中将「Spring Boot Starter 基础库」明确为 `spring-boot-starter-web`，或在 `<dependencyManagement>` 中统一声明版本后由 application 模块或各业务模块引入。common 模块推荐直接声明 `spring-boot-starter-web`，使依赖它的业务模块自动获得 Web 支持。

### 问题 2 [一般]：`spring-boot-starter-test` 未声明，单元测试无法编译
- **所在位置**：第 10 节「CI 占位」、第 2.2 节「模块职责与依赖方向」
- **问题描述**：第 10 节明确要求「每个模块至少一个占位测试类」并在 CI 第五阶段执行 `mvn test`。但产出未在任何模块中声明 `spring-boot-starter-test` 依赖。若无此依赖，JUnit 5 和 `@SpringBootTest` 等注解不可用，占位测试类无法编译，`mvn test` 将失败。
- **改进建议**：在父 POM 的 `<dependencyManagement>` 中声明 `spring-boot-starter-test` 版本，并在需要编写测试的模块（至少 common、application、各业务模块）的 POM 中以 `test` scope 引入该依赖。

### 问题 3 [一般]：`spring-boot-starter-validation` 未声明，参数校验机制无法实际工作
- **所在位置**：第 4.2 节「统一响应流程」、第 5.1 节「错误分类」
- **问题描述**：第 4.2 节定义参数校验异常由 `@Valid` → `MethodArgumentNotValidException` → `GlobalExceptionHandler` 处理。Spring Boot 3 起 `spring-boot-starter-web` 不再自动包含 Bean Validation 实现，`@Valid` 注解需要 `spring-boot-starter-validation` 提供 `hibernate-validator`。无此依赖则 `@Valid` 静默失效，参数校验错误不会被触发。
- **改进建议**：在父 POM 的 `<dependencyManagement>` 中声明 `spring-boot-starter-validation`，并在业务模块（patient/doctor/admin，含 Controller 的模块）的 POM 中引入该依赖。

### 问题 4 [一般]：前端 Monorepo 内部包的依赖配置未定义，无法直接指导编码
- **所在位置**：第 2.4 节「前端模块划分」
- **问题描述**：第 2.4 节定义了 `packages/shared/` 和 `packages/ui-core/` 两个内部包，并说明 `ui-core` 依赖 `shared`、三端 `apps/*` 引用这两个包。但产出未说明根 `package.json` 的 `workspaces` 字段如何配置、`shared/package.json` 和 `ui-core/package.json` 的导出配置、三端 `apps/*` 如何在 `package.json` 中引用 workspace 内部的包。
- **改进建议**：补充根 `package.json` 的 workspaces 配置示例、各内部包的 `package.json` 导出配置、以及三端应用引用方式的具体示例。

### 问题 5 [轻]：`integration` 模块的 Maven POM 配置未给出具体结构
- **所在位置**：第 10 节「CI 占位」
- **问题描述**：第 10 节描述了 `integration` 模块的职责（含占位集成测试类和 Failsafe 插件配置），但未给出该模块的 POM 结构示例，包括其如何依赖 `application` 模块的运行包、`maven-failsafe-plugin` 的插件配置参数、以及 `spring-boot-starter-test` 的声明方式。
- **改进建议**：在 2.1 节或 10 节补充 `integration/pom.xml` 的核心配置骨架。

### 问题 6 [轻]：包命名树中 `ai-impl` 的 `degradation` 实现归属路径缺少显式展示
- **所在位置**：第 2.3 节「包命名规范」
- **问题描述**：第 3.4 节定义 `TimeoutDegradationStrategy`（class）归属 `ai-impl`，但第 2.3 节的包命名树中 `impl` 子包下仅展示 `mock/` 和 `fallback/` 两个目录，未包含 `degradation/`。开发者可能将 `TimeoutDegradationStrategy` 错误放入 `fallback/` 包，造成命名语义混淆。
- **改进建议**：在第 2.3 节 `impl` 子包下补充 `degradation/` 目录（`TimeoutDegradationStrategy` 存放位置），或补充注释说明降级策略实现类的归属。

## 历史迭代回顾

### 已解决的问题（出现在历史反馈但当前反馈中不再提及）
以下问题在前序轮次中已发现，经过反复迭代已在本轮反馈中不再出现，表明已得到充分解决：
- 第 1 轮：AI 方法标识中文括号命名问题、权限模型实体归属与共享引用、同步非阻塞表述、BaseEntity 字段定义、MockAiService 装配策略、SecurityConfig 骨架、User-UserDetails 适配、ai-api/ai-impl 拆分
- 第 2 轮：ai.mock.enabled 默认值、DTO 类型名缺失、配置加载失败错误分类、ui-core 包定义、SecurityConfig 模块归属、CI 模块构建顺序
- 第 3 轮：CI mvn compile 产物安装问题、FallbackAiService @Primary 循环依赖、DegradationStrategy 无参签名、嵌套 DTO 字段定义
- 第 4 轮：@EnableJpaAuditing 配置、SecurityConfig 认证策略矛盾、FallbackAiService ObjectProvider 方案、PageQuery 起始值歧义、BaseEntity.deleted Boolean→boolean、OpenAPI 前后端同步、ErrorCode enum→interface、Integration 模块用途、API 版本管理策略声明
- 第 5 轮：common 模块依赖与 SecurityConfig 矛盾、跨模块调用规范、Spring Boot 包扫描配置、BusinessException 继承层次、PageRequest 命名冲突
- 第 6 轮：H2 数据库策略与配置示例、Vite 代理配置、CI 重复命令行、common-module api 子包路径
- 第 7 轮：spring-boot-starter-data-jpa 依赖声明、AiService 真实实现与 FallbackAiService 装配条件、ScheduleRequest.doctorIds 字段类型统一、springdoc-openapi 模块归属

### 持续存在的问题（在多轮反馈中反复出现，需重点解决）
以下问题在第 8 轮反馈中即已提出，但在本轮（第 8 轮 v8 设计）审查中依然存在，表明前一轮迭代未完全修复：
- **spring-boot-starter-web 未显式声明**（问题 1，严重）—— 第 8 轮已提出，v8 设计仍以「Spring Boot Starter 基础库」模糊表述，未明确为 spring-boot-starter-web
- **spring-boot-starter-test 未声明**（问题 2，一般）—— 第 8 轮已提出，v8 设计仍未补充
- **spring-boot-starter-validation 未声明**（问题 3，一般）—— 第 8 轮已提出，v8 设计仍未补充
- **前端 Monorepo 内部包依赖配置未定义**（问题 4，一般）—— 第 8 轮已提出，v8 设计仍未补充 package.json workspaces/导出/引用配置示例

### 新发现的问题（本轮新识别的问题）
以下问题为第 8 轮审查中新识别的问题，前序轮次反馈中未出现：
- **integration 模块 POM 配置缺具体结构**（问题 5，轻）
- **包命名树缺少 degradation/ 目录**（问题 6，轻）

## 上一轮产出路径
C:\Develop\Software\AIMedicalSys\Harness\redeliberations\202606151714_Phase0_OOD\a_v8_design_v1.md

## 用户需求
C:\Develop\Software\AIMedicalSys\Harness\redeliberations\202606151714_Phase0_OOD\requirement.md
