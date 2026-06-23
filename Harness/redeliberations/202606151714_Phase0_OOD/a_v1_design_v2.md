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
| `PageRequest` / `PageResponse<T>` | class | 分页请求与响应规范 |
| `ErrorCode` | enum | 错误码命名空间，按业务域分组 |
| `BaseEntity` | abstract class | 数据实体基类，提供 id、createdAt、updatedAt、逻辑删除标记 |
| `GlobalExceptionHandler` | class | 全局异常处理机制，将业务/系统异常统一转换为 Result |
| `AiService` | interface | AI 能力接口集合，定义 13 项能力的类型化方法签名 |
| `Role` / `Post` / `Function` | entity class | 三级权限模型的核心实体 |
| `User` | entity class | 统一用户实体，关联角色与岗位 |

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
│   │       ├── result/               # Result<T>, PageRequest, PageResponse
│   │       ├── exception/            # 业务异常基类, ErrorCode
│   │       ├── util/                 # 通用工具
│   │       └── config/               # 全局配置（Jackson, CORS, 异常处理器）
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
│   │   └── ai/                       # AI 能力模块（含 Mock 占位）
│   │       └── src/main/java/com/aimedical/modules/ai/
│   │           ├── api/              # AI 能力接口契约（AiService 接口族）
│   │           ├── mock/             # Mock 实现
│   │           ├── dto/              # AI 输入/输出 DTO
│   │           └── degradation/      # 降级策略框架
│   └── integration/                  # 集成测试模块（选配）
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
common (无依赖)
  ↑
modules/common-module (依赖 common)
  ↑  ↑  ↑
modules/patient  modules/doctor  modules/admin
  ↑              ↑              ↑
  └──────────────┼──────────────┘
                 ↓
          modules/ai (依赖 common, 业务模块依赖 ai 的接口层)
                 ↑
          application (依赖所有 business 模块, 负责聚合启动)
```

**模块间依赖规则**：
- `common`：零依赖（仅依赖 Spring Boot Starter 基础库），所有模块可依赖它
- `modules/common-module`：依赖 `common`，提供权限、字典、配置等跨模块共享的服务接口与实体定义
- `modules/patient`、`modules/doctor`、`modules/admin`：依赖 `common` 和 `common-module`，三者之间**不允许互相依赖**
- `modules/ai`：依赖 `common`，对外暴露接口契约（interface），业务模块**仅依赖 ai 的 api 子包**
- `application`：依赖所有业务模块，作为 Spring Boot 启动入口
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
│   │   ├── permission     # 权限相关 entity/service
│   │   ├── config         # 业务级配置
│   │   └── dict           # 字典管理
│   └── ai                 # AI 能力模块
│       ├── api            # AI 能力接口契约
│       ├── mock           # Mock 实现
│       ├── dto            # 输入/输出 DTO
│       └── degradation    # 降级策略框架
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
```

三端各自独立路由，通过 `packages/shared` 共享 API 客户端和类型定义。

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

#### `PageRequest` / `PageResponse<T>` — 分页规范（class）

**职责**：为列表类接口提供统一的分页请求参数与响应格式。

- `PageRequest`：包含 `page`（从 0 或 1 开始）、`size`、`sort` 字段
- `PageResponse<T>`：包含 `content`（列表）、`totalElements`、`totalPages`、`page`、`size` 字段

**为何使用独立 class 而非内联参数**：统一分页契约避免各模块自行定义分页结构，确保前端分页组件可复用。

#### `ErrorCode` — 错误码命名空间（enum）

**职责**：定义全局错误码体系，每个错误码包含 `code`（整型或字符串）和 `message`（用户可读描述）。

**设计要点**：
- 按业务域分配错误码段（如 `COMMON_XXXX`、`PATIENT_XXXX`、`DOCTOR_XXXX`、`AI_XXXX`）
- 每个模块维护自己的错误码枚举，按需扩展
- 错误码一经发布不可修改含义（可废弃但不复用）

**为何使用 enum**：TypeScript 枚举和 Java 枚举均可穷举所有错误码，编译期检查避免 magic number，且易于维护错误码清单文档。

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

**协作关系**：所有业务实体继承它。无其他协作。

**为何使用 abstract class**：实体需要共享 id、createdAt、updatedAt 等字段及 JPA 注解（`@MappedSuperclass`、`@Id`、`@PrePersist`、`@PreUpdate`），抽象类是最自然的复用方式。interface 无法携带字段和 JPA 注解。

### 3.3 权限模型核心抽象

#### `User`（entity class）

**职责**：统一用户实体，覆盖患者、医生、管理员三类使用方。通过 `userType` 枚举区分用户类型，通过多对多关联 `Role`。

**协作**：`User ↔ Role`（多对多），`User ↔ Post`（多对多，医生端特有）。

#### `Role` — 角色（entity class）

**职责**：粗粒度角色定义（如"患者"、"门诊医生"、"检查医生"、"管理员"）。一个用户可拥有多个角色。

**协作**：`Role ↔ Post`（一对多），`Role ↔ User`（多对多）。

#### `Post` — 岗位（entity class）

**职责**：细粒度岗位定义（如"门诊医生-神经内科"、"药房医生"）。角色决定大权限范围，岗位决定具体可访问功能和数据范围。医生端通过岗位区分五个子岗位的可操作菜单。

**协作**：`Post ↔ Function`（多对多），`Post ↔ User`（多对多）。

#### `Function` — 功能权限（entity class）

**职责**：最细粒度的操作权限（如"查看挂号列表"、"创建处方"、"AI 审核"）。岗位与功能多对多关联，实现功能级访问控制。

**为何使用"角色—岗位—功能"三级而非简单的 RBAC**：
- 角色（Role）解决"你是谁"——患者/门诊医生/管理员
- 岗位（Post）解决"你在这个角色下能做什么"——同是医生，门诊医生与检查医生的菜单与权限不同
- 功能（Function）解决"你能操作哪些具体功能"——岗位确定功能集合，易于维护
- 三级模型可在不修改角色/岗位代码的前提下通过调整岗位-功能关联来变更权限，满足 2.5 节"支持后续扩展新角色与新岗位"的要求

### 3.4 AI 能力模块抽象

#### `AiService` — AI 能力接口集合（interface）

**职责**：定义 13 项 AI 能力的类型化方法签名，每项能力对应一个方法，输入/输出类型由具体 DTO 确定。涵盖成功、降级、超时、不可用等调用状态。

**协作**：
- 业务模块通过 `AiService` 接口调用各自需要的 AI 能力方法，不依赖具体实现
- `MockAiService` 在 Phase 0 实现该接口的全部 13 个方法，每个方法返回对应能力的占位数据
- 真实 AI 在后续阶段实现该接口，连接真实模型

**为何使用 interface 而非 abstract class**：
- AI 接入方案可能从 HTTP API 变为 Spring AI 或混合模式，interface 可以将"做什么"（调用 AI）与"怎么做"完全解耦
- `AiService` 无共享状态或默认行为，无需 abstract class 的 protected 方法
- 每个 AI 能力方法有独立的输入/输出类型，interface 的方法签名为各能力提供编译期类型检查

#### `AiResult<T>` — AI 调用结果（class）

**职责**：封装 AI 调用的结果状态，包括 `success`、`data`、`errorCode`、`degraded`（是否降级）、`fallbackReason`（降级原因）。

**为何独立于 `Result<T>`**：AI 调用结果需要额外携带降级/兜底语义，与普通 API 响应语义不同。将二者分离避免 `Result` 承载过多维度。

#### `MockAiService` — AI Mock 占位实现（class）

**职责**：在 Phase 0 提供所有 AI 能力接口的 Mock 实现，返回结构正确的占位数据，支持前端独立开发和后端契约验证。

**协作**：实现 `AiService` 接口的 13 个方法，每个方法返回对应能力的固定结构占位数据。

#### 降级策略框架（interface + class 体系）

- `DegradationStrategy`（interface）：定义降级判定逻辑 `shouldDegrade()` 和降级行为 `fallback(input)`
- `TimeoutDegradationStrategy`（class）：基于超时阈值的降级策略
- `FallbackAiService`（class）：包装降级逻辑的 `AiService` 装饰器，在 AI 调用失败时按策略执行降级

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

Phase 0 需配置 Security 骨架，允许 `/api/ping` 匿名访问，其余接口要求认证（可在 Phase 0 用 permitAll 临时放通，Phase 1 冻结权限规则）。

---

## 5. 错误处理策略

### 5.1 错误分类

| 错误类别 | 代表场景 | 处理方式 | HTTP 状态码 |
|---------|---------|---------|-------------|
| 参数校验错误 | 必填字段缺失、格式不合法 | `MethodArgumentNotValidException` → `Result` | 400 |
| 业务逻辑错误 | 手机号已存在、挂号时间冲突 | `BusinessException(ErrorCode)` → `Result` | 400 / 409 |
| 认证错误 | 未登录、token 过期 | `AuthenticationException` → `Result` | 401 |
| 授权错误 | 无权限访问 | `AccessDeniedException` → `Result` | 403 |
| 资源不存在 | 查询 ID 不存在 | `BusinessException(NOT_FOUND)` → `Result` | 404 |
| 系统异常 | 数据库连接失败、空指针 | 统一捕获 → `Result` + 服务端日志 | 500 |
| AI 调用异常 | AI 超时、AI 不可用 | `AiResult.degraded=true` + 降级数据 | 200（业务层处理） |

### 5.2 业务异常基类

`BusinessException` 持有 `ErrorCode` 和可选的动态参数（用于消息模板填充），由 `GlobalExceptionHandler` 统一转换为 `Result`。

### 5.3 异常处理原则

- 不将异常栈信息暴露给前端
- 系统异常需记录完整堆栈到服务端日志
- AI 降级是正常业务流程的一部分，不视作异常

---

## 6. 并发设计

Phase 0 无业务功能，不涉及复杂并发问题。骨架层面做以下预留：

- 后端使用 Spring Boot 默认的 Tomcat 线程池（200），后续按需调优
- 前端 Axios 请求无需额外并发控制
- AI 调用接口设计为**同步非阻塞**（Spring Async + CompletableFuture 在 Phase 2+ 引入），Phase 0 的 MockAiService 直接返回

---

## 7. 设计决策

| 决策 | 选项 | 选择 | 理由 |
|------|------|------|------|
| 架构风格 | 微服务 vs 模块化单体 | 模块化单体（Maven 多模块） | Phase 0 团队规模与需求复杂度不支撑微服务运维成本；模块边界即为未来微服务拆分边界 |
| 后端构建工具 | Gradle vs Maven | Maven | 技术栈文档明确使用 `mvn spring-boot:run`，与项目规范一致 |
| 前端 Monorepo 工具 | Nx vs Turborepo vs Vite | Vite workspace | 技术栈文档明确使用 Vite，避免引入额外的工具链依赖 |
| AI 能力接口形态 | 独立接口 vs 单泛型门面 vs 方法集合 | 统一接口方法集合 `AiService` | 13 项 AI 能力的输入/输出类型各不相同，统一为单一接口的 13 个方法可兼顾类型安全与门面统一性；相比独立接口可共享降级、超时、熔断等横切逻辑；相比单泛型方法无需运行时类型分发 |
| 权限模型 | RBAC vs 三级模型 | 角色—岗位—功能三级 | 需求 2.5 明确要求三级模型，且角色-岗位-功能三层的解耦使得权限配置更灵活，可映射复杂的医疗岗位权限场景 |
| 错误码形式 | int vs String | String（如 `PATIENT_MOBILE_EXISTS`） | String 可读性强，易于前端根据错误码做差异化处理；int 需要文档映射 |
| API 版本管理 | URL 路径 vs 请求头 vs 无版本 | 无显式版本（通过模块路径隐含） | Phase 0 至 Phase 6 为同一个大版本演进，模块内接口变更通过新增接口、废弃旧接口的方式管理。如需对外暴露版本，后续引入 URL 路径版本 `api/v2/...` |
| 全局异常处理 | `@ControllerAdvice` vs AOP | `@ControllerAdvice` | Spring 原生机制，自然适配 Spring MVC 的异常处理流程，无需额外 AOP 配置 |
| AI Mock 实现形态 | Spring @Profile vs 条件注解 | `@ConditionalOnProperty` | 通过配置开关 `ai.mock.enabled=true` 控制是否启用 Mock，同一 jar 包可在不同环境切换，无需 Profile 切换 |
| 前端共享包结构 | 独立 npm 包 vs workspace 内部包 | Vite workspace 内部包 | 避免 npm 发布流程，简化开发期引用 |

---

## 8. 模块间 API 通信规范

### 8.1 模块暴露规范

- 每个业务模块通过 `api` 子包暴露 REST Controller，路径前缀为 `/api/{module}`（如 `/api/patient/...`）
- Controller 方法统一返回 `Result<T>` 或 `Result<PageResponse<T>>`
- 模块内部类（entity、repository）不对外暴露，其他模块不能直接访问

### 8.2 AI 能力方法清单（Phase 0 Mock 占位）

Phase 0 定义 `AiService` 接口中包含以下 13 个方法契约，每个方法对应一项 AI 能力，具有独立的输入/输出 DTO 类型。所有方法返回 Mock 占位数据：

| 方法标识 | 对应 AI 能力 | 输入 | 输出 |
|---------|------------|------|------|
| `triage` | 智能分诊 | 患者主诉 | 推荐科室、医生列表 |
| `prescriptionCheck` | AI 处方审核 | 处方药品列表 + 患者信息 | 审核结果、风险等级 |
| `generateMedicalRecord` | AI 病历生成 | 医患对话文本 | 结构化病历字段 |
| `diagnosis` | AI 智能诊断 | 病情文本 | 诊断结论、置信度 |
| `analysisReport(检查)` | AI 智能检查报告 | 检查原始数据 | 检查报告草稿 |
| `analysisReport(检验)` | AI 智能检验报告 | 检验原始数据 | 检验报告草稿 |
| `imageAnalysis` | AI 影像分析 | 影像数据 | 识别结果 |
| `knowledgeBaseQuery` | AI 知识库问答 | 问题文本 | 回答文本 |
| `recommendExamination` | AI 开立检查/检验 | 病情文本 | 项目推荐列表 |
| `prescriptionAssist` | AI 辅助开方 | 诊断 + 检查结果 | 处方草案 |
| `recommendExecutionOrder` | AI 执行顺序推荐 | 待执行任务列表 | 排序建议 |
| `schedule` | AI 医生排班 | 排班约束 | 排班方案 |
| `discussionConclusion` | AI 综合讨论结论 | 讨论记录 | 结论摘要 |

Phase 0 的 Mock 实现为每个方法返回固定结构占位数据，后续 Phase 替换为真实 AI 推理实现。方法清单未来可按 roadmap 逐步冻结细化。

### 8.3 API 文档工具集成

Phase 0 集成 Knife4j（Swagger 3），各模块 Controller 添加 `@Tag` 和 `@Operation` 注解，开发期即可通过 `doc.html` 查阅 API 契约。

---

## 9. 本地开发体验

### 9.1 统一配置管理

- `application.yml`：存放通用配置及多环境共享配置
- `application-dev.yml`：开发环境配置（数据库连接、AI Mock 开关、日志级别）
- `application-prod.yml`：生产环境配置
- 前端通过 `.env.development` / `.env.production` 管理环境变量

### 9.2 多模块构建依赖

- 父 POM 管理所有依赖版本（`<dependencyManagement>`）
- 公共模块（common、common-module）优先构建
- ai 模块优先于业务模块构建（业务模块依赖 ai 的接口）
- application 模块最后构建，聚合所有模块

### 9.3 一键启动

- 后端：`mvn spring-boot:run -pl application`（开发期）
- 前端：在各 `apps/*` 目录执行 `npm run dev`，Vite 开发服务器代理跨域到后端

---

## 10. CI 占位

Phase 0 CI 流水线包含：

```
checkout → mvn compile → mvn test (占位单测) → 构建前端 → 归档制品
```

- `mvn compile` 验证所有模块编译通过
- `mvn test` 运行已有单元测试（Phase 0 仅测试占位，每个模块至少一个占位测试类）
- 前端在每个 `apps/*` 中运行 `npm run build` 验证构建成功
- 完整 CI 门禁（OpenAPI diff、覆盖率阈值等）归 Phase 1

---

## 修订说明（v2）

| 审查意见 | 修改措施 |
|---------|---------|
| `AiService` 统一门面（3.4 节）与 13 项 AI 能力接口（8.2 节）之间的关系不清晰，Java 单泛型方法无法分发到不同能力 | 采用方向 A：`AiService` 由单一 `invoke(input)` 方法改为包含 13 个具名方法的接口（如 `triage()`、`prescriptionCheck()` 等），每项能力对应一个类型安全的专用方法；更新 3.4 节职责描述、删除泛型 invoke 的表述；8.2 节标题改为"AI 能力方法清单"并建立与 AiService 方法的显式映射；设计决策表中的对应条目更新为"统一接口方法集合" |
| 2.2 节依赖关系图中 `modules/ai → application` 箭头方向与正文描述相反 | 箭头方向反转为 `application ↑ modules/ai`，匹配"application 依赖所有业务模块"的正文描述 |
| 2.1 节 `modules/ai/` 目录树下缺少 `degradation/` 子包，与 2.3 节包命名不一致 | 在 `modules/ai/` 目录树中补充 `degradation/` 子包及其职责注释 |
