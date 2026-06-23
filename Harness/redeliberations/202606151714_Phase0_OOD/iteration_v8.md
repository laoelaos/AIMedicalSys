# 再审议判定报告（v8）

## 判定结果

RETRY

## 判定理由

组件B诊断报告经质询确认（LOCATED），报告包含 1 个严重问题（`spring-boot-starter-web` 未声明）和 3 个一般问题（`spring-boot-starter-test`、`spring-boot-starter-validation` 未声明、前端 Monorepo 依赖配置未定义），满足 RETRY 条件。内部循环实际轮次 1 < 最大轮次 12，质询结果为 LOCATED，审查结论有效，需返回组件A修正。

## 需要解决的问题（仅 RETRY 时存在）

- **问题描述**：`spring-boot-starter-web` 未在任何模块中显式声明，导致 `@RestController`、`@ControllerAdvice`、嵌入式 Tomcat 等不可用，破坏「骨架可运行」目标
- **所在位置**：第 2.2 节「模块职责与依赖方向」
- **严重程度**：严重
- **改进建议**：在 common 模块依赖描述中将「Spring Boot Starter 基础库」明确为 `spring-boot-starter-web`，或在 `<dependencyManagement>` 中统一声明后由 application 或各业务模块引入

- **问题描述**：`spring-boot-starter-test` 未声明，JUnit 5 和 `@SpringBootTest` 不可用，占位测试类无法编译，`mvn test` 将失败
- **所在位置**：第 10 节「CI 占位」、第 2.2 节「模块职责与依赖方向」
- **严重程度**：一般
- **改进建议**：在父 POM 的 `<dependencyManagement>` 中声明 `spring-boot-starter-test`，在需要编写测试的模块中以 `test` scope 引入

- **问题描述**：`spring-boot-starter-validation` 未声明，Spring Boot 3 起 `spring-boot-starter-web` 不再自动包含 Bean Validation，`@Valid` 静默失效，参数校验错误不会触发
- **所在位置**：第 4.2 节「统一响应流程」、第 5.1 节「错误分类」
- **严重程度**：一般
- **改进建议**：在父 POM 的 `<dependencyManagement>` 中声明 `spring-boot-starter-validation`，在含 Controller 的业务模块 POM 中引入

- **问题描述**：前端 Monorepo 内部包的依赖配置未定义，包括根 `package.json` 的 `workspaces` 字段、内部包的导出配置、三端应用引用 workspace 包的方式
- **所在位置**：第 2.4 节「前端模块划分」
- **严重程度**：一般
- **改进建议**：补充根 `package.json` 的 workspaces 配置示例、各内部包的 `package.json` 导出配置、三端应用引用方式的具体示例
