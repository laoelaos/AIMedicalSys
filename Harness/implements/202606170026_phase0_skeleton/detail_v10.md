# 详细设计（v10）

## 概述

创建 application 模块的启动基础设施，包括启动类、健康检查端点、安全占位配置、环境配置文件，并补齐 POM 依赖与 spring-boot-maven-plugin。设计目标：使后端可在 `AIMedical/backend/` 下通过 `mvn spring-boot:run -pl application -am` 一键启动，`GET /api/ping` 返回统一包装的健康响应，为 Phase 0 验收标准"后端基础骨架可独立启动并响应健康检查"提供完整工程支撑。

application 模块是后端多模块的启动聚合层，扮演以下角色：
- **类路径聚合点**：将所有业务模块（patient/doctor/admin）、公共模块（common/common-module-impl）、AI 模块（ai-impl）的编译产物汇集到同一运行时类路径
- **配置统一入口**：承载三文件配置体系（application.yml / application-dev.yml / application-prod.yml），统一管理数据源、JPA、安全、Actuator、springdoc 等全局配置
- **包扫描锚点**：`@SpringBootApplication(scanBasePackages = "com.aimedical")` 作为 Spring 组件扫描的根锚点，配合 `@EntityScan` 和 `@EnableJpaRepositories` 确保所有子模块的 Bean、实体、Repository 被正确发现

## 模块划分

### 目录布局（application 模块）

```
backend/application/
├── pom.xml                                        # 已存在，需补充依赖和插件
└── src/
    ├── main/
    │   ├── java/com/aimedical/
    │   │   ├── Application.java                   # 启动类（新建）
    │   │   ├── HealthController.java               # 健康检查端点（新建）
    │   │   └── config/
    │   │       └── SecurityConfigPhase0.java       # Phase 0 permitAll 安全配置（新建）
    │   └── resources/
    │       ├── application.yml                     # 主配置（新建）
    │       ├── application-dev.yml                 # 开发环境配置（新建）
    │       └── application-prod.yml                # 生产环境配置（新建）
    └── test/java/com/aimedical/
        └── ApplicationPlaceholderTest.java         # 占位单元测试（新建）
```

### 模块职责

| 职责 | 承载者 | 说明 |
|------|--------|------|
| 启动聚合 | `Application.java` | Spring Boot 入口，配置包扫描策略 |
| 健康检查 | `HealthController.java` | 系统级端点 `/api/ping`，不归属任何业务模块 |
| 安全占位 | `SecurityConfigPhase0.java` | Phase 0 permitAll 放通所有请求 |
| 统一配置 | 三 YAML 文件 | 通用/开发/生产三环境配置分离 |
| POM 依赖管理 | `pom.xml` | 补齐所有运行时依赖和构建插件 |

### 依赖方向

```
application ─> common                    (compile) — 全局异常处理、Result、BaseEntity
application ─> common-module-api         (compile) — 共享枚举类型
application ─> common-module-impl        (compile) — 权限实体与 Repository
application ─> ai-api                    (compile) — AI 接口契约
application ─> ai-impl                   (compile) — AI Mock 实现和降级策略
application ─> patient                   (compile) — 患者模块占位 Controller 等
application ─> doctor                    (compile) — 医生模块占位（同 patient）
application ─> admin                     (compile) — 管理员模块占位（同 patient）
application ─> spring-boot-starter-web   (compile) — @SpringBootApplication、@RestController
application ─> spring-boot-starter-data-jpa (compile) — @EntityScan、@EnableJpaRepositories
application ─> spring-boot-starter-security  (compile) — SecurityConfigPhase0（SecurityFilterChain）
application ─> spring-boot-starter-actuator  (compile) — 健康检查端点
application ─> h2                        (runtime)  — Phase 0 内存数据库
application ─> springdoc-openapi-starter-webmvc-ui (compile) — API 文档（推荐补齐项）
application ─> spring-boot-starter-test  (test)     — 单元测试
```

**关键依赖原则**：
- `spring-boot-starter-web` 和 `spring-boot-starter-data-jpa` 在 common 模块中以 `optional` 声明，application 作为实际启动层必须显式引入，否则类路径缺失导致启动失败
- `h2` 以 `runtime` scope 声明，确保编译期不暴露 H2 类型，仅在运行时通过 JDBC 驱动加载
- `spring-boot-starter-actuator` 提供 `/actuator/health` 标准端点，与自定义 `/api/ping` 并存但语义不同：前者由 Actuator 自动注册用于容器编排探针，后者作为系统级功能健康检查供前端和验收使用

## 核心抽象

### Application — 启动入口（class）

**形态**：class，标注 `@SpringBootApplication` + `@EntityScan` + `@EnableJpaRepositories`

**职责**：作为 Spring Boot 应用入口，通过 `SpringApplication.run(Application.class, args)` 启动容器。显式指定 `scanBasePackages = "com.aimedical"` 扫描所有子模块的 Spring Bean（Controller、Service、Configuration 等）。独立声明 `@EntityScan("com.aimedical")` 发现各模块的 JPA 实体，`@EnableJpaRepositories("com.aimedical")` 发现各模块的 Repository 接口。

**为何将启动类放在 `com.aimedical` 根包而非 `com.aimedical.application` 子包**：Spring Boot 默认从启动类所在包开始扫描组件。将启动类放在根包 `com.aimedical` 下，所有子模块（`com.aimedical.common`、`com.aimedical.modules.patient` 等）都在其子包范围内，无需额外配置 scanBasePackages。但 OOD §9.2 要求显式指定 `scanBasePackages = "com.aimedical"`，确保后续将启动类下沉到子包时扫描边界不变，本设计遵循该约定。

**为何不将 Application 定义为 interface 或多例**：启动入口是单例且具体的流程起点，不存在多态变体。

### HealthController — 健康检查端点（class）

**形态**：class，标注 `@RestController`

**职责**：提供 `GET /api/ping` 端点，返回 `Result.success("pong")`。作为 Phase 0 的系统级健康检查端点，不归属任何业务模块，由 application 模块直接提供。

**协作**：
- 返回类型为 `Result<String>`（common 模块提供的统一响应包装）
- 不注入任何 Service、Repository 或其它模块依赖
- 不访问数据库、不调用 AI、不依赖外部资源

**为何使用 class 而非 interface**：Controller 是 Spring 管理的具体端点容器，单一职责、无多态需求。该端点的行为在 Phase 0 冻结（始终返回 pong），不存在多实现变体。

**`/api/ping` 与 Actuator `/actuator/health` 的职责边界**：
- `/api/ping`：业务层面的简单可达性验证，返回统一 `Result` 格式，供前端应用和后端验收流程直接消费
- `/actuator/health`：由 Spring Boot Actuator 自动注册，返回格式遵循 Actuator 协议，供容器编排（K8s liveness/readiness probe）和运维监控使用
- 两者并存但不冲突，Phase 0 验收标准仅要求 `/api/ping` 可达

### SecurityConfigPhase0 — Phase 0 安全占位配置（class）

**形态**：class，标注 `@Configuration` + `@Profile("phase0")`

**职责**：提供 `SecurityFilterChain` Bean，配置 `csrf.disable()` + `authorizeHttpRequests().anyRequest().permitAll()`，使 Phase 0 所有端点免认证访问。

**协作**：
- 仅提供 `SecurityFilterChain` Bean，不定义 `AuthenticationEntryPoint`、`AccessDeniedHandler`、`PasswordEncoder`、CORS 策略或 `UserDetailsService`
- 由 `@Profile("phase0")` 条件化激活，仅在 `spring.profiles.active` 包含 `phase0` 时生效（已在 `application.yml` 中设置 `spring.profiles.active: phase0,dev`）

**为何使用 `@Profile` 而非 `@ConditionalOnProperty`**：安全配置的切换是 Profile 级别的语义——Phase 0 使用 permitAll 占位，Phase 1+ 切换为真实安全配置。`@Profile` 天然对应多阶段 Profile 切换，与 `spring.profiles.active` 机制一致。`@ConditionalOnProperty` 更适合功能粒度的条件装配（如 `ai.mock.enabled`），两者关注维度不同。

**为何不定义为 interface**：SecurityFilterChain 配置是 Spring Security 框架的具体装配逻辑，单一职责、无多态需求。真实 SecurityConfig 在 Phase 1 定义时同样为 `@Configuration` class，两者通过 Profile 切换而非接口抽象解耦。

### 三文件配置体系（YAML 配置资源）

**形态**：Spring Boot 多环境 YAML 文件，位于 `src/main/resources/` 下

**职责**：

| 文件 | 职责定位 | 包含内容 |
|------|---------|---------|
| `application.yml` | 通用配置，加载所有环境共享的默认值 | `spring.application.name`、`spring.profiles.active`、`server.port` |
| `application-dev.yml` | 开发环境专有配置 | H2 内存数据源、H2 Console、JPA show-sql、AI Mock 开关、springdoc 启用、Actuator 暴露范围 |
| `application-prod.yml` | 生产环境专有配置 | H2 Console 关闭、Actuator 缩小暴露范围、springdoc 禁用 |

**为何拆分三文件而非单文件多 Profile 块**：多文件分离策略是 Spring Boot 推荐的最佳实践，每个 Profile 文件职责单一，新增环境（如 `application-staging.yml`）只需创建新文件，无需修改已有配置，避免单文件因 Profile 块增多而难以维护。

## 关键行为契约

### 启动流程

```
mvn spring-boot:run -pl application -am
  → Maven reactor 解析 application 依赖链（common → common-module-* → ai-* → patient/doctor/admin）
  → 编译所有依赖模块
  → spring-boot-maven-plugin 启动 Spring Boot 应用
    → @SpringBootApplication 扫描 com.aimedical 下所有 Bean
    → @EntityScan 发现各模块 JPA 实体
    → @EnableJpaRepositories 发现各模块 Repository
    → Profile "phase0" 激活 SecurityConfigPhase0（permitAll）
    → Profile "dev" 激活 application-dev.yml（H2 数据源等）
    → 应用启动完成，监听 8080 端口
```

### 可达性验证

```
GET /api/ping
→ 200 OK, Result<String> { code: "SUCCESS", data: "pong" }

GET /actuator/health
→ 200 OK, Actuator 健康响应（Phase 0 含数据库健康检查）
```

### application/pom.xml 依赖清单

| 依赖 | scope | 用途说明 |
|------|-------|---------|
| `com.aimedical:common` | compile | 全局异常处理、Result、BaseEntity |
| `com.aimedical:common-module-api` | compile | 共享枚举类型 |
| `com.aimedical:common-module-impl` | compile | 权限实体与 Repository |
| `com.aimedical:ai-api` | compile | AI 接口契约 |
| `com.aimedical:ai-impl` | compile | AI Mock 实现和降级策略 |
| `com.aimedical:patient` | compile | 患者模块占位 |
| `com.aimedical:doctor` | compile | 医生模块占位 |
| `com.aimedical:admin` | compile | 管理员模块占位 |
| `spring-boot-starter-web` | compile | @SpringBootApplication、@RestController |
| `spring-boot-starter-data-jpa` | compile | @EntityScan、@EnableJpaRepositories |
| `spring-boot-starter-security` | compile | SecurityConfigPhase0 所需 |
| `spring-boot-starter-actuator` | compile | 健康检查端点 |
| `com.h2database:h2` | runtime | Phase 0 内存数据库 |
| `org.springdoc:springdoc-openapi-starter-webmvc-ui` | compile | API 文档 |
| `spring-boot-starter-test` | test | 单元测试 |

## 错误处理策略

application 模块不定义独立的异常处理逻辑。所有 Controller 层的异常统一由 common 模块的 `GlobalExceptionHandler` 处理：

| 场景 | 处理方式 | 责任模块 |
|------|---------|---------|
| HealthController 正常返回 `Result.success("pong")` | 无错误处理 | application |
| 启动期配置错误（如必填配置缺失） | Spring Boot FailureAnalyzer 输出诊断信息，启动失败 | Spring Boot 框架 |
| 系统异常（NPE、序列化失败等） | 由 common 的 `GlobalExceptionHandler` 统一捕获并返回 `Result.fail(SYSTEM_ERROR)` | common |
| SecurityConfigPhase0 未生效导致 403 | Phase 0 通过 `@Profile("phase0")` 确保生效，不应出现 | — |

**关键原则**：
- application 模块不重复定义 `@ControllerAdvice` 或 `@ExceptionHandler`
- `SecurityConfigPhase0` 因配置错误未加载时应确保可通过日志快速定位：Phase 0 `permitAll` 未生效时，Spring Security 默认拒绝所有请求，此时 `GET /api/ping` 返回 403 而非 200。验收流程中 `mvn spring-boot:run` 启动后的第一个请求应当是 `GET /api/ping`，若返回 403 应立即检查 `@Profile("phase0")` 条件和 `spring.profiles.active` 配置
- Phase 0 无需处理认证/授权异常（permitAll 放通所有请求）

## 并发设计

Phase 0 的 application 模块无业务功能，不涉及复杂并发问题：

- 使用 Spring Boot 默认的 Tomcat 线程池（200），可满足健康检查端点的简单请求处理
- `HealthController` 为无状态 Controller，不持有共享状态，天然线程安全
- `SecurityConfigPhase0` 仅在启动时配置 SecurityFilterChain，不涉及运行时状态
- 无异步任务、无定时任务、无并发数据结构

## 设计决策

| 决策 | 选项 | 选择 | 理由 |
|------|------|------|------|
| 启动类包路径 | `com.aimedical` vs `com.aimedical.application` | `com.aimedical` | Spring Boot 默认从启动类所在包开始组件扫描；根包 `com.aimedical` 可自动覆盖所有子模块的 Bean，`scanBasePackages` 配置作为防御性声明确保后续包结构调整时扫描边界不变 |
| 安全配置激活方式 | `@Profile("phase0")` vs `@ConditionalOnProperty` | `@Profile("phase0")` | 安全配置的阶段切换是 Profile 级语义，与 `spring.profiles.active` 机制一致；Phase 1 真实 SecurityConfig 通过 `@Profile("!phase0")` 或独立 Profile 名切换，两阶段配置不共存 |
| 健康检查端点路径 | `/api/ping` vs `/actuator/health` vs 两者并存 | 两者并存 | `/api/ping` 作为业务级健康检查，返回统一 `Result` 格式供前端和验收使用；`/actuator/health` 由 Actuator 自动注册供容器编排使用。两者职责不同，并存提供最佳可观测性覆盖率 |
| H2 数据库 scope | compile vs runtime vs test | runtime | runtime scope 确保编译期不暴露 H2 类型，仅在运行时通过 JDBC 驱动加载；Phase 1+ 切换 MySQL 时将 h2 降为 test scope，替换为 mysql-connector-j（runtime）|
| springdoc 集成优先级 | 硬性验收项 vs 推荐补齐项 | 推荐补齐项 | API 文档工具不影响骨架启动和验收标准中的 `/api/ping` 可达性；若工期紧张可顺延到 Phase 1 补齐 |
| Spring Boot Maven Plugin classifier | 无 classifier vs `exec` classifier | `exec` classifier | 无 classifier 时 plugin 将普通 JAR 替换为可执行 fat JAR，integration 模块以 test scope 依赖 application 后因 BOOT-INF/lib 布局无法解析 transitive 依赖；`exec` classifier 同时生成普通 JAR（供 Maven 依赖解析）和可执行 JAR（供 `java -jar` 启动）|
| application-dev.yml H2 Console path | 默认 `/h2-console` vs 自定义路径 | 默认 `/h2-console` | Phase 0 无安全认证，自定义路径的安全收益有限；默认路径在 H2 Console 生产关闭（application-prod.yml 设置 `spring.h2.console.enabled: false`）后自动失效，无需自定义 |
| Actuator 端点暴露策略 | dev 全暴露 vs dev 选择暴露 vs prod 缩小 | dev 选择暴露（health,info,metrics） + prod 缩小（health,info） | dev 暴露 metrics 便于开发期调试；prod 缩小暴露范围避免指标数据泄露；两者均不暴露 env、beans、shutdown 等高危端点 |
| application/pom.xml ignoredUnusedDeclaredDependencies | 需要 vs 不需要 | 不需要 | application 模块的所有依赖都在运行时真实使用（启动类需要 web/data-jpa/security/actuator；内部模块依赖在各模块 Bean 注册时被 Spring 容器消费），`mvn dependency:analyze` 不会报告 unused declared dependency。已有父 POM 的豁免清单覆盖 `ai-api`、`common-module-api`、`patient`、`doctor`、`admin`，application 无额外豁免需求 |

DESIGN_WRITTEN:C:\Develop\Software\AIMedicalSys\Harness\implements\202606170026_phase0_skeleton\detail_v10.md
主Agent请勿阅读产出文件内容，直接将路径转发给相关方。
