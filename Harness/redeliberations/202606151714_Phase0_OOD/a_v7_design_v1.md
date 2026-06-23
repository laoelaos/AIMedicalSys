# Phase 0 最小化骨架 — 架构级 OOD 设计方案

## 1. 概述

### 1.1 设计目标

Phase 0 交付的是多人并行协作所必需的共享工程"地形"，不包含任何业务功能。设计目标如下：

- **解耦并行**：模块边界清晰，支持后端 4+ 模块、前端 3 个终端独立开发、独立部署验证
- **契约先行**：统一响应包装、分页、错误码、全局异常处理等接口契约在 Phase 0 冻结，后续业务模块遵循契约即可
- **骨架可运行**：三端前端可一键启动到占位首页；后端基础骨架可独立启动并响应 `/actuator/health`（或自定义 ping）健康检查
- **可演进**：骨架预留 AI 能力抽象层、权限模型扩展点、微服务化拆分演进路径，后续阶段无需重构骨架

### 1.2 整体架构思路

采用 **单仓多模块（Monorepo）** 模式，后端为 Maven 多模块 Spring Boot 3 应用，前端为 Vite 多应用单仓。整体遵循**分层依赖 + 模块隔离**原则：

- 后端：`common`（共享基类与契约）← `modules/*`（业务模块）← `application`（启动聚合层）
- 前端：`packages/*`（共享库）← `apps/*`（三端独立应用）
- 跨模块调用只允许**高层依赖低层**，同层模块之间**不允许直接依赖**，通过公共门面（facade）或事件解耦

### 1.3 核心抽象一览

| 抽象 | 类型形态 | 职责定位 |
|------|---------|---------|
| `Result<T>` | 泛型 class | 统一 API 响应包装，携带状态码、消息、数据载荷 |
| `PageQuery` / `PageResponse<T>` | class | 分页请求与响应规范 |
| `ErrorCode` | interface | 错误码命名空间契约，各模块通过 enum 实现该 interface |
| `BaseEntity` | abstract class | 数据实体基类，提供 id、createdAt、updatedAt、逻辑删除标记 |
| `GlobalExceptionHandler` | class | 全局异常处理机制，将业务/系统异常统一转换为 Result |
| `AiService` | interface | AI 能力接口集合，定义 13 项能力的类型化方法签名 |
| `Role` / `Post` / `Function` | entity class | 三级权限模型的核心实体，归属 common-module |
| `User` | entity class | 统一用户实体，关联角色与岗位，归属 common-module |
| `LoginUser` | class | User 的 Spring Security 适配器，实现 UserDetails 接口 |

---

## 2. 模块划分

### 2.1 Monorepo 目录布局

```
aimedical-sys/
├── backend/                          # 后端 Maven 多模块
│   ├── pom.xml                       # 父 POM（聚合 + 依赖管理）
│   ├── application/                  # 启动模块（spring-boot-maven-plugin）
│   │   └── src/main/resources/
│   │       ├── application.yml       # 主配置
│   │       ├── application-dev.yml   # 开发环境配置
│   │       └── application-prod.yml  # 生产环境配置
│   ├── common/                       # 共享基础模块（无业务逻辑）
│   │   └── src/main/java/com/aimedical/common/
│   │       ├── base/                 # BaseEntity, BaseEnum
│   │       ├── result/               # Result<T>, PageQuery, PageResponse
│   │       ├── exception/            # 业务异常基类, ErrorCode
│   │       ├── util/                 # 通用工具
│   │       └── config/               # 全局配置（Jackson, CORS, 异常处理器, SecurityConfig）
│   ├── modules/
│   │   ├── patient/                  # 患者模块
│   │   │   └── src/main/java/com/aimedical/modules/patient/
│   │   │       ├── api/              # 对外 REST 接口（facade）
│   │   │       ├── service/          # 业务逻辑
│   │   │       ├── repository/       # 数据访问
│   │   │       └── entity/           # 领域实体
│   │   ├── doctor/                   # 医生模块
│   │   ├── admin/                    # 管理员模块
│   │   ├── common-module/            # 公共模块（跨模块共享的业务抽象：权限、字典等）
│   │   ├── ai/                       # AI 能力模块
│   │   │   ├── pom.xml               # AI 模块父 POM
│   │   │   ├── ai-api/               # AI 能力接口契约子模块（零外部依赖，仅依赖 common）
│   │   │   │   └── src/main/java/com/aimedical/modules/ai/api/
│   │   │   │       ├── AiService.java           # AI 能力接口集合
│   │   │   │       ├── degradation/             # 降级策略接口
│   │   │   │       └── dto/                     # 输入/输出 DTO
│   │   │   └── ai-impl/              # AI 实现子模块（依赖 ai-api）
│   │   │       └── src/main/java/com/aimedical/modules/ai/impl/
│   │   │           ├── mock/         # Mock 实现
│   │   │           └── fallback/     # 降级策略实现
│   │   └── ... (其他业务模块)
│   └── integration/                  # 集成测试模块（Phase 0 含占位测试类和 Failsafe 插件配置）
│
├── frontend/                         # 前端 Vite 多应用单仓
│   ├── package.json                  # workspace root
│   ├── packages/
│   │   ├── shared/                   # 共享库（API 客户端、类型定义、工具函数）
│   │   └── ui-core/                  # 共享 UI 组件库
│   ├── apps/
│   │   ├── patient/                  # 患者端（Vue 3 + Vite + TypeScript）
│   │   ├── doctor/                   # 医生端
│   │   └── admin/                    # 管理员端
│   └── tsconfig.base.json            # TypeScript 共享配置
│
├── docs/                             # 项目文档
├── .gitignore
└── README.md
```

### 2.2 模块职责与依赖方向

```
common (依赖 Spring Boot 基础库及 spring-boot-starter-security)
  ↑
modules/common-module (依赖 common)
  ↑  ↑  ↑
modules/patient  modules/doctor  modules/admin
  ↑              ↑              ↑
  └──────────────┼──────────────┘
                 ↓
          modules/ai/ai-api (依赖 common, 业务模块仅依赖 ai-api)
                 ↑
          modules/ai/ai-impl (依赖 ai-api, 仅由 application 引入)
                 ↑
           application (依赖所有 business 模块及 ai-impl, 负责聚合启动)
```

**模块间依赖规则**：
- `common`：依赖 Spring Boot Starter 基础库、spring-boot-starter-security（用于 SecurityConfig）及 spring-boot-starter-data-jpa（用于 BaseEntity JPA 注解及 JpaConfig 审计配置），所有模块可依赖它
- `modules/common-module`：依赖 `common`，提供权限、字典、配置等跨模块共享的服务接口与实体定义
- `modules/patient`、`modules/doctor`、`modules/admin`：依赖 `common`、`common-module` 和 `modules/ai/ai-api`，三者之间**不允许互相依赖**
- `modules/ai/ai-api`：依赖 `common`，对外暴露纯接口契约（interface + DTO），**不含任何实现依赖**。业务模块**仅依赖 ai-api**，Maven 编译期即保障不会误引入实现层依赖
- `modules/ai/ai-impl`：依赖 `ai-api`，包含 MockAiService、降级策略等实现。**仅由 application 模块引入**，业务模块不可见
- `application`：依赖所有业务模块及 ai-impl，作为 Spring Boot 启动入口。Phase 0 额外引入 H2 内存数据库驱动（runtime scope），通过 application-dev.yml 配置 H2 内存数据源；Phase 1+ 切换为 MySQL/PostgreSQL 关系型数据库
- **禁止循环依赖**：Maven 层面通过父 POM 的 `<dependencyManagement>` 统一管控版本，`mvn dependency:analyze` 在 CI 中验证

### 2.3 包命名规范

```
com.aimedical
├── common                 # 共享基础模块
│   ├── base
│   ├── result
│   ├── exception
│   ├── util
│   └── config
├── modules
│   ├── patient
│   │   ├── api            # REST controller 接口定义
│   │   ├── dto            # 请求/响应 DTO
│   │   ├── service        # 业务接口 + 实现
│   │   ├── repository     # Spring Data JPA Repository
│   │   ├── entity         # JPA 实体
│   │   └── converter      # 实体 ↔ DTO 转换
│   ├── doctor             # 同 patient 结构
│   ├── admin              # 同 patient 结构
│   ├── commonmodule       # 公共业务模块
│   │   ├── api            # 跨模块门面接口定义（如 PermissionService）
│   │   ├── permission     # 权限相关 entity/service（User, Role, Post, Function, LoginUser）
│   │   ├── config         # 业务级配置
│   │   └── dict           # 字典管理
│   └── ai                 # AI 能力模块
│       └── api            # AI 能力接口契约子包
│           ├── AiService  # AI 能力接口集合
│           ├── dto        # 输入/输出 DTO
│           └── degradation# 降级策略接口
│       └── impl           # AI 实现子包
│           ├── mock       # Mock 实现
│           └── fallback   # 降级策略实现
└── Application            # 启动类在 application 模块的根包
```

### 2.4 前端模块划分

```
apps/patient → 患者端 SPA
apps/doctor  → 医生端 SPA
apps/admin   → 管理员端 SPA
packages/shared/
  ├── api/           # Axios 实例 + API 调用封装（基于 Result<T> 类型）
  ├── types/         # TypeScript 类型定义（与后端 DTO 对应）
  └── utils/         # 通用工具函数
packages/ui-core/
  └── components/    # 共享 UI 组件库（Layout、Sidebar、Table、Form 等基础组件）
```

三端各自独立路由，通过 `packages/shared` 共享 API 客户端和类型定义；通过 `packages/ui-core` 共享基础 UI 组件，三端可在此之上构建各自独有的业务组件。`ui-core` 依赖 `shared`（使用其类型定义），不依赖任何 `apps/*`。

---

## 3. 核心抽象

### 3.1 接口契约框架

#### `Result<T>` — 统一响应包装（泛型 class）

**职责**：封装所有 REST API 的返回值，使前端可以统一处理成功/失败。

**协作对象**：
- 被所有 Controller 方法作为返回类型使用
- `GlobalExceptionHandler` 在异常处理时构造失败状态的 `Result`
- 前端 Axios 拦截器根据 `Result.code` 统一处理

**为何使用泛型 class 而非 interface**：`Result` 是数据传输容器，需携带数据载荷且其类型在编译期确定，泛型 class 是最直接的选择。无需接口抽象，因为不存在多种实现变体。

#### `PageQuery` / `PageResponse<T>` — 分页规范（class）

**职责**：为列表类接口提供统一的分页请求参数与响应格式。

- `PageQuery`：包含 `page`（0-based，与 Spring Data JPA 默认一致；前端分页组件提交时需将 1-based 页码转换为 0-based）、`size`、`sort` 字段
- `PageResponse<T>`：包含 `content`（列表）、`totalElements`、`totalPages`、`page`、`size` 字段

**为何使用独立 class 而非内联参数**：统一分页契约避免各模块自行定义分页结构，确保前端分页组件可复用。

#### `ErrorCode` — 错误码命名空间（interface）

**职责**：定义全局错误码体系契约，每个错误码包含 `code`（字符串）和 `message`（用户可读描述）。

**设计要点**：
- `ErrorCode` 定义为 `interface`，定义在 common 模块中，作为 `BusinessException` 持有的统一引用类型
- 各模块提供 `enum` 实现该 `interface`，按业务域分配错误码段（如 `COMMON_XXXX`、`PATIENT_XXXX`、`DOCTOR_XXXX`、`AI_XXXX`）
- 错误码一经发布不可修改含义（可废弃但不复用）

**为何使用 interface + 各模块 enum 实现**：将 `ErrorCode` 定义为 `interface` 而非具体 `enum`，既允许各模块独立维护自己的错误码枚举（通过 implements ErrorCode），又为 `BusinessException` 提供了统一的 `ErrorCode` 类型引用。`BusinessException` 持有 `ErrorCode` 引用，各模块传入各自实现的 enum 实例。TypeScript 侧同样定义为 `interface` + 各模块 `const enum` 或 `as const` 对象。

#### `GlobalExceptionHandler` — 全局异常处理（class）

**职责**：拦截所有 Controller 层抛出的异常，统一转换为 `Result` 格式返回。

**协作对象**：
- `@ControllerAdvice` + `@ExceptionHandler` 机制
- 根据异常类型（业务异常 `BusinessException`、参数校验异常 `MethodArgumentNotValidException`、系统异常等）决定 HTTP 状态码和 Result 中的 code/message
- 业务异常基类 `BusinessException` 持有 `ErrorCode`，Handler 根据 ErrorCode 构造 Result

**为何不定义为 interface**：全局异常处理器是单一职责的具体实现，只有一种处理方式，不需要多态。

### 3.2 数据实体基类

#### `BaseEntity` — 实体基类（abstract class）

**职责**：为所有 JPA 实体提供公共字段，消除重复。

**字段定义**：
- `id`：`Long` 类型，标注 `@Id` + `@GeneratedValue(strategy = GenerationType.IDENTITY)`，自增主键策略
- `createdAt`：`LocalDateTime` 类型，标注 `@CreatedDate`，由 `@EntityListeners(AuditingEntityListener.class)` 自动填充
- `updatedAt`：`LocalDateTime` 类型，标注 `@LastModifiedDate`，由 `@EntityListeners(AuditingEntityListener.class)` 自动填充
- `deleted`：`boolean` 类型（基本类型），标注 `@Column(nullable = false)`，默认值 `false`，逻辑删除标记。由 `@SQLDelete` + `@SQLRestriction` 注解配合实现软删除：`@SQLDelete` 定义删除时执行的 UPDATE SQL 模板（`UPDATE entity SET deleted = true WHERE id = ?`），`@SQLRestriction("deleted = false")` 替代已废弃的 `@Where` 注解（Hibernate 6.2+），确保普通查询自动过滤已删除记录

**JPA 注解**：`@MappedSuperclass` 标注在类级别，使子类继承映射而无需为 BaseEntity 建表。

**协作关系**：所有业务实体继承它。无其他协作。

**为何使用 abstract class**：实体需要共享字段及 JPA 注解（`@MappedSuperclass`、`@Id`、`@PrePersist`、`@PreUpdate`），抽象类是最自然的复用方式。interface 无法携带字段和 JPA 注解。

#### `JpaConfig` — JPA 审计配置（class，归属 `common.config`）

**职责**：启用 JPA 审计功能，使 `@CreatedDate`、`@LastModifiedDate` 等审计注解生效。

**协作**：标注 `@EnableJpaAuditing`，配合 `BaseEntity` 上的 `@EntityListeners(AuditingEntityListener.class)` 实现 `createdAt`/`updatedAt` 自动填充。BaseEntity 本身不负责审计激活，由 JpaConfig 统一管理。

**为何使用独立配置类**：`@EnableJpaAuditing` 需要声明在 `@Configuration` 类上。独立配置类集中管理 JPA 相关 enable 注解，避免散布在启动类或其他配置中。该配置类放置在 `common` 模块的 `com.aimedical.common.config` 包下。

### 3.3 权限模型核心抽象

所有权限模型实体（User、Role、Post、Function）均归属 `common-module` 模块，位于包路径 `com.aimedical.modules.commonmodule.permission`。各业务模块通过 common-module 引入权限实体和对应 Service 接口。

#### `User` — 统一用户实体（entity class，归属 `common-module`）

**职责**：统一用户实体，覆盖患者、医生、管理员三类使用方。通过 `userType` 枚举区分用户类型，通过多对多关联 `Role` 和 `Post`。

**协作**：`User ↔ Role`（多对多），`User ↔ Post`（多对多，医生端特有）。

**跨模块共享机制**：User 实体定义在 common-module 中，所有业务模块（patient、doctor、admin）均可通过 common-module 暴露的 Service 接口（如 `UserService`）或 Repository 间接访问。业务模块不应直接操作 User Repository，而通过 common-module 的 Service 门面访问。跨模块的 User 查询按需由 common-module 提供类型化的查询方法。

#### `Role` — 角色（entity class，归属 `common-module`）

**职责**：粗粒度角色定义（如"患者"、"门诊医生"、"检查医生"、"管理员"）。一个用户可拥有多个角色。

**协作**：`Role ↔ Post`（一对多），`Role ↔ User`（多对多）。

#### `Post` — 岗位（entity class，归属 `common-module`）

**职责**：细粒度岗位定义（如"门诊医生-神经内科"、"药房医生"）。角色决定大权限范围，岗位决定具体可访问功能和数据范围。医生端通过岗位区分五个子岗位的可操作菜单。

**协作**：`Post ↔ Function`（多对多），`Post ↔ User`（多对多）。

#### `Function` — 功能权限（entity class，归属 `common-module`）

**职责**：最细粒度的操作权限（如"查看挂号列表"、"创建处方"、"AI 审核"）。岗位与功能多对多关联，实现功能级访问控制。

#### `LoginUser` — Spring Security 适配器（class，归属 `common-module`）

**职责**：将领域实体 `User` 适配为 Spring Security 的 `UserDetails` 接口，承接受认证后的安全上下文交互。

**协作**：
- 实现 `UserDetails` 接口，委托 `User` 实体提供用户名、密码、状态信息
- 由自定义 `UserDetailsService` 在认证流程中构造，存入 SecurityContextHolder
- 业务 Controller 或 Service 可通过 SecurityContextHolder 获取 LoginUser，再解引用获取 User

**为何使用独立的 Adapter class 而非让 User 直接实现 UserDetails**：User 是 JPA 实体，职责是数据持久化和领域行为；UserDetails 是安全框架契约，两者关注点不同。Adapter 模式使得 User 不耦合于 Spring Security，权限模型变更不影响认证适配。

**为何使用"角色—岗位—功能"三级而非简单的 RBAC**：
- 角色（Role）解决"你是谁"——患者/门诊医生/管理员
- 岗位（Post）解决"你在这个角色下能做什么"——同是医生，门诊医生与检查医生的菜单与权限不同
- 功能（Function）解决"你能操作哪些具体功能"——岗位确定功能集合，易于维护
- 三级模型可在不修改角色/岗位代码的前提下通过调整岗位-功能关联来变更权限，满足支持后续扩展新角色与新岗位的要求

### 3.4 AI 能力模块抽象

#### `AiService` — AI 能力接口集合（interface，归属 `ai-api` 子模块）

**职责**：定义 13 项 AI 能力的类型化方法签名，每项能力对应一个方法，输入/输出类型由具体 DTO 确定。涵盖成功、降级、超时、不可用等调用状态。

**协作**：
- 业务模块通过 `AiService` 接口调用各自需要的 AI 能力方法，不依赖具体实现
- `MockAiService` 在 Phase 0 实现该接口的全部 13 个方法，每个方法返回对应能力的占位数据
- 真实 AI 在后续阶段实现该接口，连接真实模型

**为何使用 interface 而非 abstract class**：
- AI 接入方案可能从 HTTP API 变为 Spring AI 或混合模式，interface 可以将"做什么"（调用 AI）与"怎么做"完全解耦
- `AiService` 无共享状态或默认行为，无需 abstract class 的 protected 方法
- 每个 AI 能力方法有独立的输入/输出类型，interface 的方法签名为各能力提供编译期类型检查

#### `AiResult<T>` — AI 调用结果（class，归属 `ai-api` 子模块）

**职责**：封装 AI 调用的结果状态，包括 `success`、`data`、`errorCode`、`degraded`（是否降级）、`fallbackReason`（降级原因）。

**为何独立于 `Result<T>`**：AI 调用结果需要额外携带降级/兜底语义，与普通 API 响应语义不同。将二者分离避免 `Result` 承载过多维度。

#### `MockAiService` — AI Mock 占位实现（class，归属 `ai-impl` 子模块）

**职责**：在 Phase 0 提供所有 AI 能力接口的 Mock 实现，返回结构正确的占位数据，支持前端独立开发和后端契约验证。

**协作**：实现 `AiService` 接口的 13 个方法，每个方法返回对应能力的固定结构占位数据。

**Mock 数据占位约定**：
- 集合字段（如 List、Set）：固定返回 2-3 条占位数据，类型匹配目标 DTO 字段类型
- 字符串字段：填充 `"mock_" + 字段名` 格式，确保可读性和字段标识性
- 数值字段：整型填充 0 或 1，浮点型填充 0.0 或 1.0
- 枚举字段：填充目标枚举类型的第一个枚举值（`EnumType.values()[0]`）
- 嵌套 DTO：递归填充上述规则，确保全结构层级的占位数据完整性

**Bean 装配策略**：
- `MockAiService`：标注 `@ConditionalOnProperty(name = "ai.mock.enabled", havingValue = "true", matchIfMissing = true)`。当配置项 `ai.mock.enabled=true` 时（默认开发环境），MockAiService 被注册为 Spring Bean；未配置时同样默认激活 MockAiService
- 真实 `AiService` 实现（Phase 2+）：标注 `@ConditionalOnProperty(name = "ai.mock.enabled", havingValue = "false")`，通过同一配置键的反向条件与 MockAiService 互斥，避免 `@ConditionalOnMissingBean` 与 `@Primary` 装饰器的语义冲突
- `FallbackAiService`：作为装饰器始终注册为 Bean，通过 `@Primary` 确保其被注入到业务模块；内部通过 `@Autowired` + `@Lazy` + `ObjectProvider<AiService>` 延迟获取当前可用的底层 AiService 实现（MockAiService 或真实实现），避免硬编码 Bean 名称和 `@Primary` 自引用循环依赖
- 应用启动时，application 模块的配置决定 `ai.mock.enabled` 值，从而控制使用 Mock 还是真实实现

**装配条件汇总**：

| 配置 `ai.mock.enabled` | 激活的 AiService 实现 | FallbackAiService 底层获取的目标 | 适用场景 |
|------------------------|----------------------|-------------------------------|----------|
| `true`（默认） | MockAiService | MockAiService（由 ObjectProvider 返回） | 本地开发、前端联调、CI 集成测试 |
| `false` | 真实 AiService 实现 | 真实 AiService 实现（由 ObjectProvider 返回） | 生产环境、UAT 验收 |
| 未配置 | MockAiService（matchIfMissing=true） | MockAiService（由 ObjectProvider 返回） | 默认激活 MockAiService，等价于 true |

#### 降级策略框架（interface + class 体系，归属 `ai-api` 和 `ai-impl` 子模块）

- `DegradationContext`（class，`ai-api`）：封装降级判定所需的上下文信息，包含字段：
  - `int invocationCount`：当前接口累计调用次数
  - `LocalDateTime lastFailureTime`：上次失败的时间戳
  - `Duration elapsedTime`：当前调用的已耗时
  - `String requestType`：请求类型标识（如 triage/prescriptionCheck 等）
  - Phase 0 中 FallbackAiService 构造默认 DegradationContext 实例（字段取零值或 null），为未来真实策略预留扩展点
- `DegradationStrategy`（interface，`ai-api`）：定义降级判定逻辑 `boolean shouldDegrade(DegradationContext context)` 和降级行为 `<T, R> R fallback(T input)`。泛型参数 `T` 为输入类型，`R` 为降级返回值类型，与 `AiResult<T>` 的解包类型一致
- `TimeoutDegradationStrategy`（class，`ai-impl`）：基于超时阈值的降级策略
- `FallbackAiService`（class，`ai-impl`）：包装降级逻辑的 `AiService` 装饰器，在 AI 调用失败时按策略执行降级。内部通过 `@Autowired` + `@Lazy` + `ObjectProvider<AiService>` 延迟获取当前可用的底层 AiService 实现（MockAiService 或真实实现），不依赖硬编码 Bean 名称；模拟降级触发时构造 `DegradationContext` 实例并传递给 `DegradationStrategy.shouldDegrade()`。**兜底保护**：当 `ObjectProvider.getIfAvailable()` 返回 null（无可用的 AiService 实现时），FallbackAiService 不抛出异常，直接返回 `AiResult` 实例（`success=false`、`degraded=true`、`data=null`），确保业务链路不因 AI 模块缺失而断裂

**为何使用 interface + 装饰器模式**：降级策略未来会扩展（超时降级、熔断降级、Mock 降级），用 interface 抽象使主逻辑与降级策略解耦；用装饰器包装 `AiService` 使降级逻辑对业务模块透明。

### 3.5 前端共享抽象

#### `ApiClient` — API 客户端（class，封装 Axios）

**职责**：统一封装 Axios 实例，配置 baseURL、请求/响应拦截器（自动携带 JWT、统一处理 `Result` 异常码）。

**协作**：
- 响应拦截器检查 `Result.code`，非成功码统一走错误处理
- 请求拦截器从 Pinia store 或 localStorage 读取 JWT token 附加到请求头

#### `AuthStore` — 认证状态管理（Pinia store）

**职责**：管理登录态、当前用户信息、token 的存取。

**协作**：`ApiClient` 在请求拦截器中通过 `AuthStore` 获取 token；`ApiClient` 在响应 401 时通知 `AuthStore` 清除登录态并跳转登录页。

---

## 4. 关键行为契约

### 4.1 健康检查

```
GET /api/ping
→ 200 OK, Result<String> { code: "SUCCESS", data: "pong" }
```

Phase 0 验收标准：后端可独立启动并响应 `GET /api/ping`。

### 4.2 统一响应流程

```
Controller.method() → 返回 Result<T>
  正常 → Result.success(data)
  业务异常 → 抛出 BusinessException(errorCode) → GlobalExceptionHandler → Result.fail(errorCode)
  参数校验异常 → @Valid 触发 → GlobalExceptionHandler → Result.fail(PARAM_INVALID)
  系统异常 → 捕获 → GlobalExceptionHandler → Result.fail(SYSTEM_ERROR)
```

前端 Axios 响应拦截器：
```
response.data.code === "SUCCESS" → 返回 response.data.data
response.data.code !== "SUCCESS" → 弹出错误提示（或走统一错误处理）
```

### 4.3 AI 能力调用契约

```
业务模块调用 AI:
  AiService.triage(request)          // 或其他具体能力方法
    ├── MockAiService → 返回占位数据（Phase 0）
    ├── 真实实现    → 调用大模型 API（Phase 2+）
    └── FallbackAiService(调用失败) → DegradationStrategy.fallback(request)
          └── 返回降级结果 + degraded=true
```

AI 调用结果统一由 `AiResult<T>` 包装，业务模块根据 `AiResult.degraded` 决定前端是否显示"AI 暂不可用"标识。

### 4.4 分页查询契约

```
请求: GET /api/resource?page=0&size=20&sort=createdAt,desc
响应: Result<PageResponse<ResourceDTO>> {
  code: "SUCCESS",
  data: {
    content: [...],
    page: 0,
    size: 20,
    totalElements: 100,
    totalPages: 5
  }
}
```

### 4.5 权限校验契约

```
请求到达 → Spring Security FilterChain
  ├── 未认证 → 401, Result.fail(AUTH_UNAUTHORIZED)
  └── 已认证 →
       ├── 无权限 → 403, Result.fail(FORBIDDEN)
       └── 有权限 → 正常处理
```

**SecurityConfig 设计骨架**（归属 `common.config`，模块 `common` 的 `spring-boot-starter-security` 为其必需依赖）：

**Phase 0 策略**：Phase 0 作为最小化骨架阶段，无登录页面、无认证 Controller、无 token 签发机制。因此 Phase 0 的 SecurityConfig 统一使用 `permitAll` 放通所有接口，确保后端可独立启动并通过健康检查。在代码中添加注释标记未来需要认证的位置。

```
// Phase 0：全员放通，确保骨架可运行
// Phase 1+：按以下注释策略切换为真实认证规则
http.authorizeHttpRequests(auth -> auth
    .requestMatchers("/api/ping").permitAll()      // Phase 1+ 保持放通
    // .requestMatchers("/api/auth/**").permitAll() // Phase 1 放开登录注册
    // .anyRequest().authenticated()                // Phase 1 启用
    .anyRequest().permitAll()                      // Phase 0 临时放通
);
```

骨架中保留真实的 `AuthenticationEntryPoint`、`AccessDeniedHandler` 和 `PasswordEncoder` 配置，使 Phase 1 切换认证时只需修改 `authorizeHttpRequests` 规则，无需新增组件。

- `PasswordEncoder` Bean：采用 `BCryptPasswordEncoder`，所有密码存储和验证均通过该编码器
- CORS 配置：定义 `CorsConfigurationSource` Bean，允许前端三端（patient、doctor、admin）的开发服务器域名
- 异常处理协同：SecurityConfig 的 `AuthenticationEntryPoint` 和 `AccessDeniedHandler` 返回与 `GlobalExceptionHandler` 一致的 `Result<T>` 格式，确保认证/授权错误与业务错误的响应格式统一

**认证适配机制**：
- `LoginUser`（位于 `common-module.permission`）包装 `User` 实体并实现 `UserDetails` 接口
- 自定义 `UserDetailsService` 通过 `UserRepository` 加载 User，构造 `LoginUser` 实例
- 认证成功后 `LoginUser` 存入 `SecurityContextHolder`，业务代码可通过 `(LoginUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal()` 获取当前用户信息，再访问 `LoginUser.getUser()` 获取 User 实体

Phase 0 配置允许所有请求通过 permitAll，仅添加注释标记未来需要认证的位置。Phase 1 取消注释 `anyRequest().authenticated()` 即可启用认证。

---

## 5. 错误处理策略

### 5.1 错误分类

| 错误类别 | 代表场景 | 处理方式 | HTTP 状态码 |
|---------|---------|---------|-------------|
| 参数校验错误 | 必填字段缺失、格式不合法 | `MethodArgumentNotValidException` → `Result` | 400 |
| 请求体序列化错误 | JSON 解析失败、字段类型不匹配 | `HttpMessageNotReadableException` → `Result` | 400 |
| 响应体序列化错误 | 返回对象无法序列化为 JSON | `HttpMessageNotWritableException` → `Result` | 500 |
| 业务逻辑错误 | 手机号已存在、挂号时间冲突 | `BusinessException(ErrorCode)` → `Result` | 400 / 409 |
| 认证错误 | 未登录、token 过期 | `AuthenticationException` → `Result` | 401 |
| 授权错误 | 无权限访问 | `AccessDeniedException` → `Result` | 403 |
| 资源不存在 | 查询 ID 不存在 | `BusinessException(NOT_FOUND)` → `Result` | 404 |
| 数据完整性冲突 | 唯一约束违反、外键关联失败 | `DataIntegrityViolationException` → `Result` | 409 |
| 配置加载失败 | 必填配置项缺失、配置格式错误 | 应用启动失败，由 Spring Boot 的失败分析器（FailureAnalyzer）输出诊断信息 | 500（启动时） |
| 系统异常 | 数据库连接失败、空指针、未知异常 | 统一捕获 → `Result` + 服务端日志 | 500 |
| AI 调用异常 | AI 超时、AI 不可用 | `AiResult.degraded=true` + 降级数据 | 200（业务层处理） |

### 5.2 业务异常基类

`BusinessException` 继承 `RuntimeException`，持有 `ErrorCode` 和可选的动态参数（用于消息模板填充），由 `GlobalExceptionHandler` 统一转换为 `Result`。

继承 `RuntimeException` 确保 Spring 事务管理在遇到 BusinessException 时默认回滚，无需额外配置 `@Transactional(rollbackFor = ...)`。

构造方式：
- `new BusinessException(ErrorCode)`：仅指定错误码
- `new BusinessException(ErrorCode, Object... args)`：指定错误码及消息模板动态参数
- `new BusinessException(ErrorCode, Throwable cause)`：指定错误码及原始异常（保留完整异常链用于日志记录）

### 5.3 异常处理原则

- 不将异常栈信息暴露给前端
- 系统异常需记录完整堆栈到服务端日志
- AI 降级是正常业务流程的一部分，不视作异常
- `HttpMessageNotReadableException` / `HttpMessageNotWritableException` 等序列化异常统一在 `GlobalExceptionHandler` 中注册 `@ExceptionHandler` 方法，避免 Spring 默认的错误响应格式污染统一响应契约

---

## 6. 并发设计

Phase 0 无业务功能，不涉及复杂并发问题。骨架层面做以下预留：

- 后端使用 Spring Boot 默认的 Tomcat 线程池（200），后续按需调优
- 前端 Axios 请求无需额外并发控制
- AI 调用接口分两个阶段设计：
  - **Phase 0（当前）**：MockAiService 直接返回占位数据，调用为同步阻塞模式，无需额外线程管理
  - **Phase 2+（未来）**：引入 Spring Async + `CompletableFuture` 实现异步非阻塞调用，AiService 实现类返回 `CompletableFuture<AiResult<T>>`，由业务层决定是否等待或编排多个 AI 调用

---

## 7. 设计决策

| 决策 | 选项 | 选择 | 理由 |
|------|------|------|------|
| 架构风格 | 微服务 vs 模块化单体 | 模块化单体（Maven 多模块） | Phase 0 团队规模与需求复杂度不支撑微服务运维成本；模块边界即为未来微服务拆分边界 |
| 后端构建工具 | Gradle vs Maven | Maven | 技术栈文档明确使用 `mvn spring-boot:run`，与项目规范一致 |
| 前端 Monorepo 工具 | Nx vs Turborepo vs Vite | Vite workspace | 技术栈文档明确使用 Vite，避免引入额外的工具链依赖 |
| AI 能力接口形态 | 独立接口 vs 单泛型门面 vs 方法集合 | 统一接口方法集合 `AiService` | 13 项 AI 能力的输入/输出类型各不相同，统一为单一接口的 13 个方法可兼顾类型安全与门面统一性；相比独立接口可共享降级、超时、熔断等横切逻辑；相比单泛型方法无需运行时类型分发 |
| AI 模块拆分策略 | 单模块 vs ai-api/ai-impl 子模块 | 拆分 ai-api / ai-impl 两个 Maven 子模块 | ai-api 仅含接口和 DTO，零外部依赖，业务模块编译期强制只依赖 ai-api；ai-impl 包含实现，仅由 application 引入。从 POM 依赖树层面杜绝业务模块误引用实现层的隐患 |
| 权限模型 | RBAC vs 三级模型 | 角色—岗位—功能三级 | 需求 2.5 明确要求三级模型，且角色-岗位-功能三层的解耦使得权限配置更灵活，可映射复杂的医疗岗位权限场景 |
| User 与 UserDetails 关系 | 直接实现 vs Adapter 模式 | LoginUser（Adapter 模式） | User 是 JPA 实体不应耦合 Spring Security；LoginUser 包装 User 并实现 UserDetails，职责分离，认证关注点变更不影响领域实体 |
| 错误码形式 | int vs String | String（如 `PATIENT_MOBILE_EXISTS`） | String 可读性强，易于前端根据错误码做差异化处理；int 需要文档映射 |
| ErrorCode 类型形态 | enum vs interface + 各模块 enum 实现 | `interface` + 各模块 enum 实现 | enum 为 final 类，各模块无法独立扩展自己的错误码枚举，且 BusinessException 无法持有统一 ErrorCode 引用类型；interface 定义契约，各模块通过 enum 实现该 interface，兼顾类型统一与模块独立扩展 |
| API 版本管理 | URL 路径 vs 请求头 vs 无版本 | 无显式版本（通过模块路径隐含） | Phase 0 至 Phase 6 为同一个大版本演进，模块内接口变更通过新增接口、废弃旧接口的方式管理。如需对外暴露版本，后续引入 URL 路径版本 `api/v2/...` |
| 全局异常处理 | `@ControllerAdvice` vs AOP | `@ControllerAdvice` | Spring 原生机制，自然适配 Spring MVC 的异常处理流程，无需额外 AOP 配置 |
| AI Mock 实现形态 | Spring @Profile vs 条件注解 | `@ConditionalOnProperty` | 通过配置开关 `ai.mock.enabled` 控制是否启用 Mock，同一 jar 包可在不同环境切换，无需 Profile 切换；同时支持条件化装配的唯一激活保障 |
| 前端共享包结构 | 独立 npm 包 vs workspace 内部包 | Vite workspace 内部包 | 避免 npm 发布流程，简化开发期引用 |
| FallbackAiService 底层注入方式 | @Resource 按名称注入 vs @Autowired + @Lazy + ObjectProvider | `@Autowired` + `@Lazy` + `ObjectProvider<AiService>` 延迟获取 | 按名称注入在 `ai.mock.enabled=false` 时 MockAiService 未注册导致 NoSuchBeanDefinitionException；ObjectProvider 延迟获取当前可用的任意 AiService 实现，装配路径灵活且无硬编码 Bean 名称依赖 |
| 降级判定上下文 | 无参 shouldDegrade() vs 携带 DegradationContext | `shouldDegrade(DegradationContext context)` | 无参接口无法支撑有意义的降级判定逻辑；DegradationContext 封装调用次数、失败时间、请求类型等上下文，为 Phase 2+ 真实策略预留扩展点；Phase 0 将 context 字段取零值/默认值 |
| CI 分阶段构建策略 | mvn compile 分阶段 vs mvn install -DskipTests 分阶段 | `mvn install -DskipTests` 分阶段构建 | `mvn compile` 不安装产物到本地仓库，后续阶段依赖解析失败；`mvn install -DskipTests` 将编译产物安装到本地仓库，后续阶段可正常解析跨阶段依赖 |

---

## 8. 模块间 API 通信规范

### 8.1 模块暴露规范

**API 版本管理策略**：Phase 0~Phase 6 在同一主版本内演进，API 路径不含版本号段；Controller 基路径统一为 `/api/{module}`；如需对外暴露版本，Phase 6+ 引入 `/api/v2/` 版本路径。

- 每个业务模块通过 `api` 子包暴露 REST Controller，路径前缀为 `/api/{module}`（如 `/api/patient/...`）
- Controller 方法统一返回 `Result<T>` 或 `Result<PageResponse<T>>`
- 模块内部类（entity、repository）不对外暴露，其他模块不能直接访问

### 8.2 AI 能力方法清单（Phase 0 Mock 占位）

Phase 0 定义 `AiService` 接口中包含以下 13 个方法契约，每个方法对应一项 AI 能力，具有独立的输入/输出 DTO 类型。所有方法返回 Mock 占位数据：

| 方法标识 | 对应 AI 能力 | 输入 DTO | 输出 DTO |
|---------|------------|---------|---------|
| `triage` | 智能分诊 | `TriageRequest` | `TriageResponse` |
| `prescriptionCheck` | AI 处方审核 | `PrescriptionCheckRequest` | `PrescriptionCheckResponse` |
| `generateMedicalRecord` | AI 病历生成 | `MedicalRecordGenRequest` | `MedicalRecordGenResponse` |
| `diagnosis` | AI 智能诊断 | `DiagnosisRequest` | `DiagnosisResponse` |
| `analysisReportForInspection` | AI 智能检查报告 | `InspectionReportRequest` | `InspectionReportResponse` |
| `analysisReportForLabTest` | AI 智能检验报告 | `LabTestReportRequest` | `LabTestReportResponse` |
| `imageAnalysis` | AI 影像分析 | `ImageAnalysisRequest` | `ImageAnalysisResponse` |
| `knowledgeBaseQuery` | AI 知识库问答 | `KbQueryRequest` | `KbQueryResponse` |
| `recommendExamination` | AI 开立检查/检验 | `ExaminationRecommendRequest` | `ExaminationRecommendResponse` |
| `prescriptionAssist` | AI 辅助开方 | `PrescriptionAssistRequest` | `PrescriptionAssistResponse` |
| `recommendExecutionOrder` | AI 执行顺序推荐 | `ExecutionOrderRequest` | `ExecutionOrderResponse` |
| `schedule` | AI 医生排班 | `ScheduleRequest` | `ScheduleResponse` |
| `discussionConclusion` | AI 综合讨论结论 | `DiscussionConclusionRequest` | `DiscussionConclusionResponse` |

Phase 0 各 DTO 的核心字段结构定义如下（归属 `ai-api` 子模块的 `com.aimedical.modules.ai.api.dto` 包）：

```text
TriageRequest
  - String chiefComplaint           // 患者主诉
  - Integer age                     // 患者年龄
  - String gender                   // 患者性别

TriageResponse
  - String recommendedDept          // 推荐科室
  - List<RecommendedDoctor> doctors // 医生列表（含 doctorId、name、dept）

RecommendedDoctor
  - Long doctorId                   // 医生 ID
  - String name                     // 医生姓名
  - String dept                     // 所属科室

PrescriptionCheckRequest
  - List<PrescriptionDrug> drugs    // 处方药品列表
  - PatientInfo patientInfo         // 患者基本信息

PrescriptionDrug
  - String drugName                 // 药品名称
  - String dosage                   // 剂量
  - String frequency                // 用药频率（如 bid/tid）
  - Integer days                    // 用药天数

PatientInfo
  - Long patientId                  // 患者 ID
  - String name                     // 患者姓名
  - Integer age                     // 年龄
  - String gender                   // 性别
  - List<String> allergies          // 过敏史列表

PrescriptionCheckResponse
  - String auditResult              // 审核结果（PASS/FLAG/REJECT）
  - String riskLevel                // 风险等级（LOW/MEDIUM/HIGH）
  - List<String> warnings           // 告警列表

MedicalRecordGenRequest
  - String dialogueText             // 医患对话文本

MedicalRecordGenResponse
  - String structuredRecord         // 结构化病历 JSON 字符串

DiagnosisRequest
  - String conditionText            // 病情文本
  - List<String> labResults         // 检验结果列表

DiagnosisResponse
  - String conclusion               // 诊断结论
  - Double confidence               // 置信度 [0, 1]

InspectionReportRequest
  - String rawData                  // 检查原始数据

InspectionReportResponse
  - String reportDraft              // 检查报告草稿

LabTestReportRequest
  - String rawData                  // 检验原始数据

LabTestReportResponse
  - String reportDraft              // 检验报告草稿

ImageAnalysisRequest
  - String imageUrl                 // 影像文件 URL
  - String modality                 // 模态（CT/MRI/X-Ray 等）

ImageAnalysisResponse
  - List<Finding> findings          // 识别结果列表

Finding
  - String type                     // 发现类型
  - String description              // 描述
  - Double confidence               // 置信度 [0, 1]
  - String location                 // 位置描述（可选）

KbQueryRequest
  - String question                 // 问题文本
  - String department               // 科室限定（可选）

KbQueryResponse
  - String answer                   // 回答文本
  - List<String> references         // 参考资料

ExaminationRecommendRequest
  - String conditionText            // 病情文本
  - List<String> existingResults    // 已有检查结果

ExaminationRecommendResponse
  - List<RecommendedExam> items     // 推荐检查/检验项目列表

RecommendedExam
  - String examName                 // 检查/检验项目名称
  - String reason                   // 推荐理由
  - String urgency                  // 紧急程度（ROUTINE/URGENT/EMERGENCY）

PrescriptionAssistRequest
  - String diagnosis                // 诊断
  - List<String> examinationResults // 检查结果

PrescriptionAssistResponse
  - List<PrescriptionDrug> drugs    // 处方草案药品列表
  - String note                     // 用药注意事项

ExecutionOrderRequest
  - List<TaskItem> pendingTasks     // 待执行任务列表

TaskItem
  - Long taskId                     // 任务 ID
  - String taskType                 // 任务类型
  - String description              // 任务描述
  - Integer priority                // 优先级（1-5，数值越小优先级越高）

ExecutionOrderResponse
  - List<TaskItem> sortedTasks      // 排序后的任务列表

ScheduleRequest
  - String department               // 科室
  - String dateRange                // 排班日期范围
  - List<Long> doctorIds            // 参与排班医生 ID 列表

ScheduleResponse
  - List<ScheduleItem> schedule     // 排班方案

ScheduleItem
  - Long doctorId                   // 医生 ID
  - String date                     // 排班日期
  - String shift                    // 班次（MORNING/AFTERNOON/NIGHT）
  - String location                 // 出诊地点

DiscussionConclusionRequest
  - String discussionRecord         // 讨论记录文本

DiscussionConclusionResponse
  - String conclusion               // 结论摘要
  - List<String> keyPoints          // 关键要点列表
```

Phase 0 的 Mock 实现为每个方法返回固定结构占位数据（遵循 3.4 节 Mock 数据占位约定），后续 Phase 替换为真实 AI 推理实现。方法清单未来可按 roadmap 逐步冻结细化。

### 8.3 API 文档工具集成

Phase 0 同时集成 springdoc-openapi（Swagger 3），各模块 Controller 添加 `@Tag` 和 `@Operation` 注解，开发期即可通过 `swagger-ui.html` 或 `doc.html`（Knife4j 增强）查阅 API 契约。

**依赖归属**：`springdoc-openapi-starter-webmvc-ui` 在父 POM 的 `<dependencyManagement>` 中统一声明版本（与 Spring Boot 3 BOM 对齐），各业务模块（patient、doctor、admin）按需在自身 POM 中引入该依赖（scope 默认 compile，无需重复声明版本号）。ai-api 和 common 模块无需引入（不包含 Controller）。

**前后端类型同步机制**：Phase 0 由前端团队人工维护 `packages/shared/types/` 中的 TypeScript 类型定义，与后端 DTO 同步纳入 Code Review 门禁。Phase 1+ 引入 openapi-generator，通过 springdoc-openapi 生成的 OpenAPI 规范自动生成 TypeScript 类型定义，消除人工同步的类型漂移风险。springdoc-openapi 配置声明如下（Phase 0 预留配置占位）：

```yaml
# application.yml（Phase 0 预留，Phase 1+ 激活）
springdoc:
  api-docs:
    enabled: true           # Phase 0 开启以支持开发期查阅
    path: /v3/api-docs
  swagger-ui:
    enabled: true
    path: /swagger-ui.html
```

### 8.4 跨模块调用规范

跨业务模块之间的方法调用不允许直接依赖对方模块的 Repository 或 Entity，须通过以下两种模式之一实现：

**模式一：接口门面（Facade Interface）**

每个提供跨模块服务的模块在 `api` 子包中定义一个接口门面暴露给其他模块调用。门面接口的定义与实现分属不同包路径——接口在 `api` 子包中定义，实现在 `service` 子包中，其他模块的 POM 仅依赖含接口的模块，不依赖含实现的模块：

```
// common-module 门面接口（路径：.../commonmodule/api/PermissionService.java）
public interface PermissionService {
    User getUserById(Long userId);
    Set<String> getUserPermissions(Long userId);
}

// 业务模块调用（通过构造器注入接口，不感知实现）
@Service
public class PatientServiceImpl implements PatientService {
    private final PermissionService permissionService;
    // 构造器注入，依赖接口而非实现类
}
```

**模式二：Spring ApplicationEvent 事件驱动**

适用于需要异步广播或一对多通知的场景（如用户注册后通知其他模块）。事件类定义在 common 模块的 `event` 子包中，发布方通过 `ApplicationEventPublisher.publishEvent()` 发布，订阅方通过 `@EventListener` 处理：

```
// 事件定义（common.event 包）
public class UserRegisteredEvent extends ApplicationEvent {
    private Long userId;
}

// 发布方（如 patient 模块）
applicationEventPublisher.publishEvent(new UserRegisteredEvent(userId));

// 订阅方（如 admin 模块）
@EventListener
public void handleUserRegistered(UserRegisteredEvent event) {
    // 执行本模块的处理逻辑
}
```

**模式选择原则**：
- 同步调用优先选择模式一（接口门面），编译期类型安全，调用链路清晰
- 异步解耦选择模式二（事件驱动），避免模块间直接运行时依赖
- 所有跨模块调用必须通过门面接口或事件，不允许直接引用其他模块的 Repository 或 Entity 类

**Phase 0 约束**：Phase 0 暂不实现任何跨模块调用（各业务模块仅有占位 Controller），上述规范为 Phase 1+ 预留。Phase 0 的 common-module 仅提供权限实体定义和 Repository，不提供门面接口实现。

---

## 9. 本地开发体验

### 9.1 统一配置管理

- `application.yml`：存放通用配置及多环境共享配置
- `application-dev.yml`：开发环境配置（数据库连接、AI Mock 开关、日志级别）
- `application-prod.yml`：生产环境配置
- 前端通过 `.env.development` / `.env.production` 管理环境变量

**Phase 0 数据库配置**：使用 H2 内存数据库，无需安装外部数据库服务。application-dev.yml 的 datasource 配置如下：

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:aimedical
    driver-class-name: org.h2.Driver
    username: sa
    password:
  h2:
    console:
      enabled: true
      path: /h2-console
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
```

POM 中 h2 数据库驱动以 `runtime` scope 声明，仅 application 模块引入，确保 Phase 0 开发者无需额外安装数据库即可启动后端。

**Phase 1+ 切换策略**：注释 H2 配置块，改为 MySQL（或 PostgreSQL）数据源配置（url、driver-class-name、username、password），并将 h2 依赖 scope 从 `runtime` 调整为 `test`（保留用于单元测试的嵌入式数据库）。MySQL/PostgreSQL 驱动以 `compile`（默认）scope 引入。

### 9.2 多模块构建依赖

- 父 POM 管理所有依赖版本（`<dependencyManagement>`）
- 公共模块（common、common-module）优先构建
- ai-api 子模块仅依赖 common，优先于所有业务模块构建；业务模块仅依赖 ai-api
- ai-impl 子模块依赖 ai-api，仅由 application 模块引入，业务模块完全不感知 ai-impl 的存在
- application 模块最后构建，聚合所有模块

**Spring Boot 包扫描配置**：application 模块的 `@SpringBootApplication` 需指定 `scanBasePackages = "com.aimedical"` 以扫描所有子模块的 Spring Bean。同时配置 `@EntityScan("com.aimedical")` 发现 JPA 实体，`@EnableJpaRepositories("com.aimedical")` 发现 Repository 接口：

```
@SpringBootApplication(scanBasePackages = "com.aimedical")
@EntityScan("com.aimedical")
@EnableJpaRepositories("com.aimedical")
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

### 9.3 一键启动

- 后端：`mvn spring-boot:run -pl application`（开发期）
- 前端：在各 `apps/*` 目录执行 `npm run dev`，Vite 开发服务器通过代理配置将请求转发到后端

**Vite 代理配置**（各端 `vite.config.ts` 均需配置）：

```typescript
export default defineConfig({
  server: {
    port: 5173,               // 各端使用不同端口（patient: 5173, doctor: 5174, admin: 5175）
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
});
```

后端统一监听 `localhost:8080`，三端前端通过不同的 Vite dev server 端口启动，均通过代理规则将 `/api` 前缀的请求转发至后端。Phase 1+ 引入认证后此配置无需变更（JWT token 通过 Authorization Header 传递，代理透传请求头）。

---

## 10. CI 占位

Phase 0 CI 流水线按模块依赖顺序分阶段构建，每个阶段使用 `mvn install -DskipTests` 将编译产物安装到本地 Maven 仓库，确保后续阶段可以正确解析跨阶段依赖：

```
第一阶段（基础层）: checkout → mvn install -DskipTests -pl common,modules/common-module,modules/ai/ai-api
第二阶段（业务层）: mvn install -DskipTests -pl modules/patient,modules/doctor,modules/admin,modules/ai/ai-impl
第三阶段（聚合层）: mvn install -DskipTests -pl application
第四阶段（集成测试）: mvn verify -pl integration -Dskip.unit.tests=true（运行集成测试，含 Failsafe 插件）
第五阶段（测试 + 前端）: mvn test → 构建前端（各 apps/* 中 npm run build）→ 归档制品
```

- 第一阶段构建零依赖的基础模块，确保共享契约先就绪，产物安装到本地仓库
- 第二阶段构建依赖基础层的业务模块，可从本地仓库解析第一阶段产物
- 第三阶段构建依赖所有业务模块的启动聚合层，可从本地仓库解析前两阶段产物
- 第四阶段运行集成测试模块（integration），使用 Maven Failsafe 插件执行，依赖前三个阶段产物的完整后端可运行包
- 第五阶段运行单元测试并验证前端构建
- `mvn test` 运行已有单元测试（Phase 0 仅测试占位，每个模块至少一个占位测试类）
- 完整 CI 门禁（OpenAPI diff、覆盖率阈值等）归 Phase 1

**Integration 模块职责**：归属 `integration/` 目录，Phase 0 包含一个占位集成测试类（验证 Spring 上下文可正常加载和 `/api/ping` 端点的基本可达性），配置 Maven Failsafe Plugin 以 `mvn verify` 生命周期执行。该模块依赖 application 模块的启动包，作为完整后端的冒烟测试入口。

---

## 修订说明（v2）

| 审查意见 | 修改措施 |
|---------|---------|
| `AiService` 统一门面（3.4 节）与 13 项 AI 能力接口（8.2 节）之间的关系不清晰，Java 单泛型方法无法分发到不同能力 | 采用方向 A：`AiService` 由单一 `invoke(input)` 方法改为包含 13 个具名方法的接口（如 `triage()`、`prescriptionCheck()` 等），每项能力对应一个类型安全的专用方法；更新 3.4 节职责描述、删除泛型 invoke 的表述；8.2 节标题改为"AI 能力方法清单"并建立与 AiService 方法的显式映射；设计决策表中的对应条目更新为"统一接口方法集合" |
| 2.2 节依赖关系图中 `modules/ai → application` 箭头方向与正文描述相反 | 箭头方向反转为 `application ↑ modules/ai`，匹配"application 依赖所有业务模块"的正文描述 |
| 2.1 节 `modules/ai/` 目录树下缺少 `degradation/` 子包，与 2.3 节包命名不一致 | 在 `modules/ai/` 目录树中补充 `degradation/` 子包及其职责注释 |

---

## 修订说明（v2 迭代）

| 审查意见 | 修改措施 |
|---------|---------|
| AI 方法标识 `analysisReport(检查)` / `analysisReport(检验)` 使用括号和中文，不合法 | 重命名为 `analysisReportForInspection` 与 `analysisReportForLabTest`，更新 8.2 节方法清单表 |
| 权限模型实体（User、Role、Post、Function）未标注归属模块和包路径 | 在 3.3 节开头明确所有权限模型实体归属 `common-module` 模块的 `com.aimedical.modules.commonmodule.permission` 包；补充 User 跨模块共享的门面访问机制说明 |
| 6 节"同步非阻塞"表述存在逻辑矛盾 | 区分为两阶段描述：Phase 0 为同步阻塞（MockAiService 直接返回），Phase 2+ 为异步非阻塞（Spring Async + CompletableFuture），消除同一句话中的语义冲突 |
| 3.2 节 BaseEntity 缺少字段级详细定义 | 补充 BaseEntity 的四个字段（id、createdAt、updatedAt、deleted）的 Java 类型、JPA 注解（@Id、@GeneratedValue、@CreatedDate、@LastModifiedDate、@MappedSuperclass）及 ID 策略（IDENTITY） |
| 3.4 节 MockAiService 注入机制与 Bean 装配策略不完整 | 补充 AiService 的 Bean 装配策略：MockAiService 使用 @ConditionalOnProperty，真实实现使用 @ConditionalOnMissingBean，FallbackAiService 使用 @Primary 装饰器；增加装配条件汇总表 |
| 4.5 节 Spring Security 配置骨架未定义 | 补充 SecurityConfig 设计骨架，包含 SecurityFilterChain、BCryptPasswordEncoder、CORS 配置、与 GlobalExceptionHandler 一致的异常处理协同 |
| User 实体与 Spring Security UserDetails 的适配关系未定义 | 在 3.3 节新增 LoginUser 抽象（Adapter 模式，包装 User + 实现 UserDetails）；在 4.5 节补充认证适配机制说明（UserDetailsService → LoginUser → SecurityContextHolder）；在设计决策表中增加对应条目 |
| 2.2 节"仅依赖 ai 的 api 子包"缺乏编译期强制保障 | 采用方案 A：将 ai 模块拆分为 ai-api 和 ai-impl 两个 Maven 子模块（目录结构和依赖关系已在 2.1/2.2/2.3 节全面更新）。ai-api 仅含接口和 DTO，业务模块 POM 仅依赖 ai-api；ai-impl 由 application 引入，从 Maven 依赖树层面实现编译期强制隔离 |
| AI 能力 Mock 占位数据结构未定义 | 在 3.4 节 MockAiService 下方补充 Mock 数据占位约定（集合返回 2-3 条、字符串 `"mock_" + 字段名`、数值 0/1、枚举第一个值、嵌套 DTO 递归填充） |
| 5.1 节异常分类中 Jackson 序列化异常、配置加载失败、DataIntegrityViolationException 未覆盖 | 在错误分类表中新增四行：HttpMessageNotReadableException（400）、HttpMessageNotWritableException（500）、DataIntegrityViolationException（409）、配置加载失败（500）及其处理方式 |

---

## 修订说明（v3）

| 审查意见 | 修改措施 |
|---------|---------|
| `ai.mock.enabled` 未配置时 Phase 0 无 AiService Bean 可用（3.4 节） | 将 `MockAiService` 的条件注解从 `matchIfMissing = false` 改为 `matchIfMissing = true`；更新装配条件汇总表第三行；未配置时默认激活 MockAiService |
| AI 方法输入/输出使用中文自然语言描述，缺乏具体 DTO 类型名（8.2 节） | 将 8.2 节方法清单表的"输入/输出"列替换为具体的 DTO 类名（如 `TriageRequest`/`TriageResponse`）；在 8.2 节末尾补充全部 26 个 DTO 的核心字段伪代码骨架定义 |
| "配置加载失败"无法被 GlobalExceptionHandler 捕获，属事实错误（5.1 节） | 将 5.1 节错误分类表中"配置加载失败"的处理方式从"运行时由 GlobalExceptionHandler 统一捕获 → Result"改为"应用启动失败，由 Spring Boot 的失败分析器（FailureAnalyzer）输出诊断信息" |
| 前端 `ui-core` 包出现在目录树中但完全未定义（2.1 节、2.4 节） | 在 2.4 节补充 `packages/ui-core/components/` 的定义、职责（共享 UI 组件库）及依赖关系（依赖 `shared`，不依赖 `apps/*`） |
| SecurityConfig 模块归属存在内部矛盾（4.5 节与 2.1 节） | 统一 SecurityConfig 归属为 `common.config`，删除 4.5 节中"或 `common-module.config`"的歧义表述；在 4.5 节明确 `common` 模块需依赖 `spring-boot-starter-security` |
| CI 流水线未体现模块依赖构建顺序（第 10 节） | 将 CI 流水线重构为四阶段：第一阶段（基础层）、第二阶段（业务层）、第三阶段（聚合层）、第四阶段（测试+前端），并在各阶段标注具体 Maven 模块 |
| `DegradationStrategy.fallback` 输入参数类型未定义（3.4 节） | 明确 `DegradationStrategy` 的泛型签名为 `<T, R> R fallback(T input)`，并说明 `T` 为输入类型、`R` 为降级返回值类型 |

---

## 修订说明（v4）

| 审查意见 | 修改措施 |
|---------|---------|
| CI 多阶段流水线 `mvn compile` 不安装到本地仓库，后续阶段依赖解析将失败 | 将所有阶段的 `mvn compile` 改为 `mvn install -DskipTests`；在 10 节补充各阶段产物供后续阶段解析的说明；在设计决策表中新增对应条目 |
| FallbackAiService 的底层 AiService 实例获取方式未定义，可能导致 `@Primary` 自引用循环依赖 | 在 3.4 节 FallbackAiService 的 Bean 装配策略中明确内部通过 `@Resource(name = "mockAiService")` 或 `@Qualifier("mockAiService")` 按名称注入底层实现；同步更新 FallbackAiService 职责描述；在设计决策表中新增对应条目 |
| `DegradationStrategy.shouldDegrade()` 缺少上下文参数，实现类无法做出有意义的降级判定 | 将方法签名调整为 `boolean shouldDegrade(DegradationContext context)`；在 3.4 节新增 `DegradationContext` 抽象定义（含 invocationCount、lastFailureTime、elapsedTime、requestType 四个字段）；Phase 0 构造默认实例（零值/null），为未来预留扩展点；在设计决策表中新增对应条目 |
| 8 个嵌套 DTO 类型（RecommendedDoctor、PrescriptionDrug、PatientInfo、Finding、RecommendedExam、TaskItem、ScheduleItem）缺少字段结构定义 | 在 8.2 节每个引用了嵌套 DTO 的 Request/Response 之后补充对应嵌套 DTO 的核心字段伪代码定义（共 8 个类型） |
| patient/doctor/admin 模块的依赖规则未显式列出 ai-api（2.2 节） | 在 2.2 节依赖规则中，将 patient/doctor/admin 的依赖从"依赖 `common` 和 `common-module`"更新为"依赖 `common`、`common-module` 和 `modules/ai/ai-api`" |

---

## 修订说明（v5）

| 审查意见 | 修改措施 |
|---------|---------|
| `@EnableJpaAuditing` 未声明，`createdAt`/`updatedAt` 自动填充将静默失效 | 在 3.2 节末尾新增 `JpaConfig` 抽象，声明 `@EnableJpaAuditing` 配置类归属 `common.config` 包，配套伪代码骨架 |
| SecurityConfig 认证策略与 Phase 0「骨架可运行」目标冲突 | 4.5 节 SecurityConfig 明确 Phase 0 统一使用 `permitAll` 放通所有接口，添加注释标记未来需要认证的位置；保留 `AuthenticationEntryPoint`/`AccessDeniedHandler` 等组件供 Phase 1 切换 |
| FallbackAiService 在 `ai.mock.enabled=false` 时注入失败 | FallbackAiService 内部改为 `@Autowired` + `@Lazy` + `ObjectProvider<AiService>` 延迟获取可用实现，取消硬编码 Bean 名称依赖；同步更新设计决策表对应条目 |
| PageRequest.page 起始值未约定，存在后续模块间 off-by-one 风险 | 明确 `PageRequest.page` 为 0-based（与 Spring Data JPA 默认一致），添加前端分页适配说明 |
| `Boolean` 类型的 `deleted` 字段存在空指针风险 | BaseEntity 中 `deleted` 字段类型从 `Boolean` 改为 `boolean`（基本类型）；补充 `@SQLDelete` + `@Where` 逻辑删除实现说明 |
| 前端 26+ DTO 类型定义与后端同步机制未定义 | 在 8.3 节补充 springdoc-openapi 规范生成配置；Phase 0 人工维护 + Code Review，Phase 1+ 引入 openapi-generator 自动生成 TypeScript 类型 |
| Integration 模块用途完全未定义 | 在 10 节补充 Integration 模块职责描述（占位集成测试类 + Failsafe 插件配置）；在目录布局和 CI 流水线中同步更新 |
| API 版本管理策略不足以指导编码 | 在 8.1 节开头用一句话总结 API 版本管理策略 |
| ErrorCode 类型架构存在设计矛盾——BusinessException 无法引用多模块错误码 | 将 ErrorCode 从 `enum` 改为 `interface`，各模块提供 enum 实现该 interface；更新 1.3 节类型表、3.1 节描述及设计决策表对应条目 |

---

## 修订说明（v6）

| 审查意见 | 修改措施 |
|---------|---------|
| P1：common 模块依赖描述矛盾——声明零依赖但 SecurityConfig 需要 spring-boot-starter-security | 第 2.2 节 common 的依赖定义从"零依赖（仅依赖 Spring Boot Starter 基础库）"修订为"依赖 Spring Boot Starter 基础库及 spring-boot-starter-security（用于 SecurityConfig）" |
| P2：跨业务模块调用机制未定义，不足以指导编码 | 新增第 8.4 节「跨模块调用规范」，定义接口门面（Facade Interface）和 Spring ApplicationEvent 事件驱动两种模式，附编码示例，明确模式选择原则和 Phase 0 约束 |
| P3：Spring Boot 包扫描策略缺失，骨架可运行缺少关键前提 | 在第 9.2 节补充 `@SpringBootApplication(scanBasePackages = "com.aimedical")`、`@EntityScan` 和 `@EnableJpaRepositories` 配置示例 |
| P4：BusinessException 未明确继承层次，事务行为不确定 | 在第 5.2 节明确 `BusinessException` 继承 `RuntimeException`（Spring 默认回滚），补充三种构造方式（仅 ErrorCode、带动态参数、带原始异常） |
| P5：自定义 PageRequest 与 Spring Data 的 PageRequest 类名冲突 | 全文档（第 1.3、2.1、3.1 节）将自定义分页类名 `PageRequest` 重命名为 `PageQuery`，消除导入冲突 |
| P6：FallbackAiService 零实现回退的兜底路径未定义 | 在第 3.4 节 FallbackAiService 描述中补充兜底保护逻辑：`ObjectProvider.getIfAvailable()` 返回 null 时直接返回 `AiResult(success=false, degraded=true, data=null)` |
| P7：逻辑删除注解 @Where 在 Hibernate 6.2+ 中已废弃 | 在第 3.2 节 BaseEntity 字段描述中将 `@Where` 替换为 `@SQLRestriction("deleted = false")`，补充 `@SQLDelete` 的 SQL 模板说明 |

---

## 修订说明（v7）

| 审查意见 | 修改措施 |
|---------|---------|
| P1：Phase 0 骨架缺少数据库驱动策略，无法满足"骨架可运行"需求 | 在 2.2 节 application 模块依赖描述中补充 H2 内存数据库驱动运行时依赖说明；在 9.1 节补充 H2 数据源配置示例和 Phase 1+ 切换 MySQL/PostgreSQL 的过渡策略 |
| P2：前端 Vite 代理跨域配置仅一句话提及，无法直接指导编码 | 在 9.3 节补充 Vite 代理配置示例（vite.config.ts 中 proxy 规则），说明各端端口分配和 Phase 1+ 认证透传机制 |
| P3：CI 流水线第三阶段重复行 | 删除 CI 流水线中第三阶段的重复行 |
| P4：common-module 包命名规范缺少 api 子包 | 在 2.3 节 common-module 包命名列表中补充 `api` 子包，标明职责为"跨模块门面接口定义" |

---

## 修订说明（v8）

| 审查意见 | 修改措施 |
|---------|---------|
| common 模块缺少 `spring-boot-starter-data-jpa` 依赖声明，BaseEntity 的 JPA 注解及 JpaConfig 审计配置将导致编译失败 | 在 2.2 节 common 模块的依赖描述中补充 `spring-boot-starter-data-jpa`，标注用途为"用于 BaseEntity JPA 注解及 JpaConfig 审计配置" |
| 真实 AiService 实现与 FallbackAiService 的 Bean 共存机制未定义，`@ConditionalOnMissingBean` 与 `@Primary` 语义冲突 | 采用方案 B：真实 AiService 实现从 `@ConditionalOnMissingBean(AiService.class)` 改为 `@ConditionalOnProperty(name = "ai.mock.enabled", havingValue = "false")`，与 MockAiService 通过同一配置键的正反两个条件互斥；FallbackAiService 保持 `@Primary`，通过 ObjectProvider 获取当前底层实现。更新 3.4 节装配策略及装配条件汇总表，增加 FallbackAiService 底层目标列 |
| `ScheduleRequest.doctorIds` 字段类型为 `List<String>`，与系统中其他 doctor ID 字段（均为 `Long`）不一致 | 在 8.2 节 `ScheduleRequest` 中将 `doctorIds` 类型从 `List<String>` 改为 `List<Long>` |
| `springdoc-openapi` 集成缺少依赖归属声明 | 在 8.3 节补充依赖归属说明：父 POM 的 `<dependencyManagement>` 统一声明 `springdoc-openapi-starter-webmvc-ui` 版本，各业务模块按需引入 |

---

DESIGN_WRITTEN:C:\Develop\Software\AIMedicalSys\Harness\redeliberations\202606151714_Phase0_OOD\a_v7_design_v1.md
主Agent请勿阅读产出文件内容，直接将路径转发给相关方。
