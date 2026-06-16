# Phase 0 OOD 设计方案 — 质量审查报告（v8）

## 审查结论

经 7 轮迭代修正，产出在多数维度已达到较高成熟度。但仍存在以下需要解决的问题：

---

## 问题清单

### 问题 1 [严重]：`spring-boot-starter-web` 未在任何模块中显式声明

- **所在位置**：第 2.2 节「模块职责与依赖方向」
- **问题描述**：产出定义了大量依赖 Spring MVC 的组件（`@RestController`、`@ControllerAdvice`、`@ExceptionHandler`、`SecurityConfig` 中的 `HttpSecurity` 配置等），但「Spring Boot Starter 基础库」表述模糊，未在任何模块中显式声明 `spring-boot-starter-web`。`spring-boot-starter-security` 和 `spring-boot-starter-data-jpa` 均不传递 `spring-webmvc`，导致：
  - `@RestController`/`@RequestMapping` 等注解无法解析
  - `@ControllerAdvice`/`@ExceptionHandler` 无法生效
  - 嵌入式 Tomcat 容器不会启动
  - 直接破坏「骨架可运行」目标（`GET /api/ping` 无法响应）
- **严重程度**：严重
- **改进建议**：在第 2.2 节 common 的依赖描述中将「Spring Boot Starter 基础库」明确为 `spring-boot-starter-web`，或在 `<dependencyManagement>` 中统一声明版本后由 application 模块或各业务模块引入。`common` 模块推荐直接声明 `spring-boot-starter-web`，使依赖它的业务模块自动获得 Web 支持。

### 问题 2 [一般]：`spring-boot-starter-test` 未声明，单元测试无法编译

- **所在位置**：第 10 节「CI 占位」、第 2.2 节「模块职责与依赖方向」
- **问题描述**：第 10 节明确要求「每个模块至少一个占位测试类」并在 CI 第五阶段执行 `mvn test`。但产出未在任何模块中声明 `spring-boot-starter-test`（或其包含的 `junit-jupiter`）依赖。若无此依赖，JUnit 5 和 `@SpringBootTest` 等注解不可用，占位测试类无法编译，`mvn test` 将失败。
- **严重程度**：一般
- **改进建议**：在父 POM 的 `<dependencyManagement>` 中声明 `spring-boot-starter-test` 版本，并在需要编写测试的模块（至少 common、application、各业务模块）的 POM 中以 `test` scope 引入该依赖。

### 问题 3 [一般]：`spring-boot-starter-validation` 未声明，参数校验机制无法实际工作

- **所在位置**：第 4.2 节「统一响应流程」、第 5.1 节「错误分类」
- **问题描述**：第 4.2 节定义参数校验异常由 `@Valid` → `MethodArgumentNotValidException` → `GlobalExceptionHandler` 处理。Spring Boot 3 起 `spring-boot-starter-web` 不再自动包含 Bean Validation 实现，`@Valid` 注解需要 `spring-boot-starter-validation` 提供 `hibernate-validator`。无此依赖则 `@Valid` 静默失效，参数校验错误不会被触发。产出的 GlobalExceptionHandler 虽可编译注册 `MethodArgumentNotValidException` 处理方法，但实际校验逻辑不会执行。
- **严重程度**：一般
- **改进建议**：在父 POM 的 `<dependencyManagement>` 中声明 `spring-boot-starter-validation`，并在业务模块（patient/doctor/admin，含 Controller 的模块）的 POM 中引入该依赖。

### 问题 4 [一般]：前端 Monorepo 内部包的依赖配置未定义，无法直接指导编码

- **所在位置**：第 2.4 节「前端模块划分」
- **问题描述**：第 2.4 节定义了 `packages/shared/` 和 `packages/ui-core/` 两个内部包，并说明 `ui-core` 依赖 `shared`、三端 `apps/*` 引用这两个包。但产出未说明：
  - 根 `package.json` 的 `workspaces` 字段如何配置
  - `shared/package.json` 和 `ui-core/package.json` 的导出配置（`exports` 或 `main` 字段）
  - 三端 `apps/*` 如何在 `package.json` 中引用 workspace 内部的包（`"@aimedical/shared": "workspace:*"` 等）
- **严重程度**：一般
- **改进建议**：补充根 `package.json` 的 workspaces 配置示例、各内部包的 `package.json` 导出配置、以及三端应用引用方式的具体示例。

### 问题 5 [轻]：`integration` 模块的 Maven POM 配置未给出具体结构

- **所在位置**：第 10 节「CI 占位」
- **问题描述**：第 10 节描述了 `integration` 模块的职责（含占位集成测试类和 Failsafe 插件配置），但未给出该模块的 POM 结构示例，包括其如何依赖 `application` 模块的运行包、`maven-failsafe-plugin` 的插件配置参数、以及 `spring-boot-starter-test` 的声明方式。
- **严重程度**：轻微
- **改进建议**：在 2.1 节或 10 节补充 `integration/pom.xml` 的核心配置骨架，至少包含：依赖 application 模块的 artifact、failsafe-plugin 的 `<configuration>` 基本参数。

### 问题 6 [轻]：包命名树中 `ai-impl` 的 `degradation` 实现归属路径缺少显式展示

- **所在位置**：第 2.3 节「包命名规范」
- **问题描述**：第 3.4 节定义 `TimeoutDegradationStrategy`（class）归属 `ai-impl`，但第 2.3 节的包命名树中 `impl` 子包下仅展示 `mock/` 和 `fallback/` 两个目录，未包含 `degradation/`。开发者可能将 `TimeoutDegradationStrategy` 错误放入 `fallback/` 包，造成命名语义混淆。
- **严重程度**：轻微
- **改进建议**：在第 2.3 节 `impl` 子包下补充 `degradation/` 目录（`TimeoutDegradationStrategy` 存放位置），或补充注释说明降级策略实现类的归属。

---

## 总体评价

设计文档经 7 轮迭代后主体质量较高，覆盖了需求中全部 7 个维度的设计要求，架构决策清晰、关键抽象定义完整、历史反馈已得到充分响应。上述问题中前 3 项为依赖声明缺失，修正成本低但对骨架可运行性有直接影响。建议在进入编码前优先解决问题 1-3。
