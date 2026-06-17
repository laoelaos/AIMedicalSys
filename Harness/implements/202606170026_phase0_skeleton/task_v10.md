# 任务指令（v10）

## 动作
NEW

## 任务描述
创建 application 模块的启动基础设施，包括启动类、健康检查端点、安全占位配置和环境配置文件，更新 application/pom.xml 补齐所有依赖和 spring-boot-maven-plugin，添加占位单元测试。

### application/pom.xml 变更
当前 POM 仅包含 patient/doctor/admin 三个依赖，需补齐以下内容：

**新增依赖**（版本由父 POM dependencyManagement 统一管理）：
- `com.aimedical:common` (compile) — 全局异常处理、Result、BaseEntity
- `com.aimedical:common-module-api` (compile) — 共享枚举类型
- `com.aimedical:common-module-impl` (compile) — 权限实体与 Repository
- `com.aimedical:ai-api` (compile) — AI 接口契约
- `com.aimedical:ai-impl` (compile) — AI Mock 实现和降级策略
- `spring-boot-starter-web` (compile) — Web 容器（@RestController, @SpringBootApplication）
- `spring-boot-starter-data-jpa` (compile) — JPA 支持
- `spring-boot-starter-security` (compile) — SecurityConfigPhase0 所需
- `spring-boot-starter-actuator` (compile) — 健康检查端点
- `com.h2database:h2` (runtime) — Phase 0 内存数据库
- `org.springdoc:springdoc-openapi-starter-webmvc-ui` (compile) — API 文档（推荐补齐项）
- `spring-boot-starter-test` (test) — 单元测试

**spring-boot-maven-plugin 配置**：
- 添加 `<classifier>exec</classifier>`，生成可执行 fat JAR（application-0.0.1-SNAPSHOT-exec.jar），同时保留普通 JAR 供 integration 模块以 test scope 引用

### 新建源文件

#### 1. Application.java
**路径**: `backend/application/src/main/java/com/aimedical/Application.java`
**包**: `com.aimedical`
**注解**: `@SpringBootApplication(scanBasePackages = "com.aimedical")`
**附加注解**: `@EntityScan("com.aimedical")`, `@EnableJpaRepositories("com.aimedical")`
**行为**: `SpringApplication.run(Application.class, args)` main 方法

#### 2. HealthController.java
**路径**: `backend/application/src/main/java/com/aimedical/HealthController.java`
**包**: `com.aimedical`
**注解**: `@RestController`
**行为**: `GET /api/ping` → 返回 `Result.success("pong")`

#### 3. SecurityConfigPhase0.java
**路径**: `backend/application/src/main/java/com/aimedical/config/SecurityConfigPhase0.java`
**包**: `com.aimedical.config`
**注解**: `@Configuration`, `@Profile("phase0")`
**行为**: 提供 `SecurityFilterChain` Bean，`csrf.disable()` + `authorizeHttpRequests().anyRequest().permitAll()`

#### 4. application.yml
**路径**: `backend/application/src/main/resources/application.yml`
**内容**：
- `spring.application.name: aimedical-sys`
- `spring.profiles.active: phase0,dev`
- `server.port: 8080`

#### 5. application-dev.yml
**路径**: `backend/application/src/main/resources/application-dev.yml`
**内容**：
- H2 内存数据源配置（url: `jdbc:h2:mem:aimedical`, driver-class-name, username: sa, password: 空）
- H2 Console 配置（enabled: true, path: /h2-console）
- JPA 配置（hibernate.ddl-auto: update, show-sql: true）
- `ai.mock.enabled: true`
- springdoc 配置（api-docs.enabled: true, swagger-ui.enabled: true）
- Actuator 配置（endpoints.web.exposure.include: health,info,metrics）

#### 6. application-prod.yml
**路径**: `backend/application/src/main/resources/application-prod.yml`
**内容**：
- `spring.h2.console.enabled: false`
- Actuator 缩小暴露范围（`management.endpoints.web.exposure.include: health,info`）
- springdoc 禁用（`springdoc.api-docs.enabled: false`, `springdoc.swagger-ui.enabled: false`）

### 占位单元测试

#### 7. ApplicationPlaceholderTest.java
**路径**: `backend/application/src/test/java/com/aimedical/ApplicationPlaceholderTest.java`
**包**: `com.aimedical`
**行为**: 纯 POJO 占位测试，`@Test` 空方法，验证测试框架可达

## 选择理由
application 模块是后端启动聚合层，所有业务模块的依赖在此汇集，是验收标准"后端 mvn spring-boot:run 启动"和"GET /api/ping 返回 pong"的前置条件。

## 任务上下文
- OOD §9.1: `application.yml` 设 `spring.profiles.active: phase0,dev`，三文件配置规范
- OOD §9.2: `@SpringBootApplication(scanBasePackages = "com.aimedical")` + `@EntityScan` + `@EnableJpaRepositories`
- OOD §4.5: `SecurityConfigPhase0` 标注 `@Profile("phase0")`，permitAll 放通所有请求
- OOD §4.1: `GET /api/ping` → `Result<String> { code: "SUCCESS", data: "pong" }`
- OOD §10.1: Spring Boot Maven Plugin 必须添加 `<classifier>exec</classifier>`，否则 integration 模块无法以 test scope 依赖 application 模块的普通 JAR

## 已有代码上下文
- `backend/application/pom.xml` 当前仅含 patient/doctor/admin 三个 compile 依赖，无 spring-boot-maven-plugin
- 后端父 POM `backend/pom.xml` 已管理所有依赖版本（spring-boot-starter-*, h2, springdoc），子模块无需重复声明 version
- common 模块提供 `Result<T>`（`com.aimedical.common.result`），`Result.success(data)` 静态工厂方法
- 三个业务模块（patient/doctor/admin）的占位 Controller 已在 v9 创建并可编译
- `backend/` 下已存在 `application/` 目录及 `pom.xml`
