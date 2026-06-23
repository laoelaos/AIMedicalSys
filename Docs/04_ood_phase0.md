# Phase 0 最小化骨架 — 架构级 OOD 设计方案

## 1. 概述

### 1.1 设计目标

Phase 0 交付的是多人并行协作所必需的共享工程"地形"，不包含任何业务功能。设计目标如下：

- **解耦并行**：模块边界清晰，支持后端 4+ 模块、前端 3 个终端独立开发、独立部署验证
- **契约先行**：统一响应包装、分页、错误码、全局异常处理等接口契约在 Phase 0 冻结，后续业务模块遵循契约即可
- **骨架可运行**：三端前端可一键启动到占位首页；后端基础骨架可独立启动并响应 `/actuator/health`（或自定义 ping）健康检查
- **可演进**：骨架预留 AI 能力抽象层、权限模型扩展点、微服务化拆分演进路径，后续阶段无需重构骨架

### 1.2 整体架构思路

采用 **单仓多模块（Monorepo）** 模式，后端为 Maven 多模块 Spring Boot 3 应用，前端为基于 Vite 的多应用单仓，并使用 npm workspaces 管理内部包。整体遵循**分层依赖 + 模块隔离**原则：

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
| `BaseEnum` | interface | 枚举通用基接口，提供 `code` / `desc` 通用取值方法，归属 common 模块 |
| `GlobalExceptionHandler` | class | 全局异常处理机制，将业务/系统异常统一转换为 Result |
| `AiService` | interface | AI 能力接口集合，定义 13 项能力的类型化方法签名 |
| `Role` / `Post` / `Function` | entity class | 三级权限模型的核心实体，归属 common-module-impl |
| `User` | entity class | 统一用户实体，关联角色与岗位，归属 common-module-impl |

### 1.4 运行时环境要求

Phase 0 骨架的本地开发环境最低要求如下：

| 运行时 | 最低版本 | 说明 |
|-------|---------|------|
| JDK | 17+ | Spring Boot 3.2.5 最低要求 |
| Node.js | 18+ 或 20 LTS | 与 Vue 3 + Vite 兼容的主流版本 |
| npm | 9+ | 支持 npm workspaces |

---

## 2. 模块划分

### 2.1 Monorepo 目录布局

仓库采用**双层布局**：根目录（PascalCase `AIMedicalSys/`）承载项目级元数据，业务源码与 monorepo 根（`AIMedical/`）作为子目录独立演化。该布局分离"过程制品"与"产品源码"，便于在不动源码的前提下迭代设计文档、审议记录与配置。

```
AIMedicalSys/                        # 仓库根（项目级元数据）
├── Docs/                            # 项目文档（OOD、roadmap、diagnosis 报告等）
├── Harness/                         # 审议与决策制品（指令、诊断、再审议记录）
├── LICENSE
├── README.md
├── .gitignore
└── AIMedical/                       # 业务源码根（OOD 视图中的 aimedical-sys 实际落点）
    ├── backend/                     # 后端 Maven 多模块
    │   ├── pom.xml                  # 父 POM（聚合 + 依赖管理）
    │   ├── application/             # 启动模块（spring-boot-maven-plugin）
    │   │   └── src/main/resources/
    │   │       ├── application.yml       # 主配置
    │   │       ├── application-dev.yml   # 开发环境配置
    │   │       └── application-prod.yml  # 生产环境配置
    │   ├── common/                  # 共享基础模块（无业务逻辑）
    │   │   └── src/main/java/com/aimedical/common/
    │   │       ├── base/                 # BaseEntity, BaseEnum
    │   │       ├── result/               # Result<T>, PageQuery, PageResponse
    │   │       ├── exception/            # 业务异常基类, ErrorCode
    │   │       ├── util/                 # 通用工具
    │   │       └── config/               # 全局配置（Jackson, 异常处理器）
    │   ├── modules/
    │   │   ├── patient/                  # 患者模块
    │   │   │   └── src/main/java/com/aimedical/modules/patient/
    │   │   │       ├── api/              # 对外 REST 接口（facade）
    │   │   │       ├── dto/              # 请求/响应 DTO
    │   │   │       ├── service/          # 业务逻辑
    │   │   │       ├── repository/       # 数据访问
    │   │   │       ├── entity/           # 领域实体
    │   │   │       └── converter/        # 实体 ↔ DTO 转换
    │   │   ├── doctor/                   # 医生模块
    │   │   ├── admin/                    # 管理员模块
    │   │   ├── common-module/            # 公共业务模块（跨模块共享：权限、字典等）
    │   │   │   ├── pom.xml               # 公共模块父 POM
    │   │   │   ├── common-module-api/    # Phase 0 空壳 API 子模块（仅含 UserType 等实体必需共享类型）
    │   │   │   │   └── src/main/java/com/aimedical/modules/commonmodule/api/
    │   │   │   │       └── UserType.java            # Phase 0 User 实体必需共享枚举
    │   │   │   └── common-module-impl/   # 公共模块实现子模块（依赖 common-module-api）
    │   │   │       └── src/main/java/com/aimedical/modules/commonmodule/
    │   │   │           ├── permission/   # 权限实体与 Repository 骨架
    │   │   │           ├── config/       # 业务级配置
    │   │   │           └── dict/         # 字典管理
    │   │   ├── ai/                       # AI 能力模块
    │   │   │   ├── pom.xml               # AI 模块父 POM
    │   │   │   ├── ai-api/               # AI 能力接口契约子模块（仅含接口 + DTO，依赖 common，不含业务实现依赖）
    │   │   │   │   └── src/main/java/com/aimedical/modules/ai/api/
    │   │   │   │       ├── AiService.java           # AI 能力接口集合
    │   │   │   │       ├── degradation/             # 降级策略接口
    │   │   │   │       └── dto/                     # 输入/输出 DTO
    │   │   │   └── ai-impl/              # AI 实现子模块（依赖 ai-api）
    │   │   │       └── src/main/java/com/aimedical/modules/ai/impl/
    │   │   │           ├── mock/         # Mock 实现
    │   │   │           ├── fallback/     # 降级实现
    │   │   │           └── degradation/  # 降级策略实现
    │   │   └── ... (其他业务模块)
    │   └── integration/                  # 集成测试模块（Phase 0 含占位测试类和 Failsafe 插件配置）
    │
    ├── frontend/                        # 前端 Vite 多应用单仓
    │   ├── package.json                 # workspace root
    │   ├── packages/
    │   │   ├── shared/                  # 共享库（API 客户端、类型定义、工具函数）
    │   │   └── ui-core/                 # 共享 UI 组件库
    │   ├── apps/
    │   │   ├── patient/                 # 患者端（Vue 3 + Vite + TypeScript）
    │   │   ├── doctor/                  # 医生端
    │   │   └── admin/                   # 管理员端
    │   └── tsconfig.base.json           # TypeScript 共享配置
    │
    └── README.md                        # 源码子模块说明
```

> **目录命名约束**：仓库根使用 PascalCase `AIMedicalSys/`（与操作系统/IDE 项目视图一致），源码根使用 PascalCase `AIMedical/`；Maven `artifactId` 与 `spring.application.name` 仍使用 kebab-case `aimedical-sys`（参见 §6.2 与 `05_ood_report.md:311-319` 的名称统一建议，三者用途不同，分别用于构建显示、运行期标识、目录视图）。

**父 POM 基础结构骨架**（`AIMedical/backend/pom.xml`）：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <groupId>com.aimedical</groupId>
    <artifactId>aimedical-sys</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <packaging>pom</packaging>
    <name>aimedical-sys</name>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.2.5</version>
        <relativePath/>
    </parent>

    <modules>
        <module>common</module>
        <module>modules/common-module/common-module-api</module>
        <module>modules/common-module/common-module-impl</module>
        <module>modules/ai/ai-api</module>
        <module>modules/ai/ai-impl</module>
        <module>modules/patient</module>
        <module>modules/doctor</module>
        <module>modules/admin</module>
        <module>application</module>
        <module>integration</module>
    </modules>

    <dependencyManagement>
        <dependencies>
            <!-- 内部模块版本，由父 POM 统一管理 -->
            <dependency>
                <groupId>com.aimedical</groupId>
                <artifactId>common</artifactId>
                <version>${project.version}</version>
            </dependency>
            <dependency>
                <groupId>com.aimedical</groupId>
                <artifactId>common-module-api</artifactId>
                <version>${project.version}</version>
            </dependency>
            <dependency>
                <groupId>com.aimedical</groupId>
                <artifactId>common-module-impl</artifactId>
                <version>${project.version}</version>
            </dependency>
            <dependency>
                <groupId>com.aimedical</groupId>
                <artifactId>ai-api</artifactId>
                <version>${project.version}</version>
            </dependency>
            <dependency>
                <groupId>com.aimedical</groupId>
                <artifactId>ai-impl</artifactId>
                <version>${project.version}</version>
            </dependency>
            <dependency>
                <groupId>com.aimedical</groupId>
                <artifactId>application</artifactId>
                <version>${project.version}</version>
            </dependency>
            <!-- spring-boot-starter-web：common 以 optional 声明；含 Controller 或需启动 Web 容器的模块显式引入 -->
            <dependency>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-starter-web</artifactId>
            </dependency>
            <!-- spring-boot-starter-data-jpa：common 以 optional 声明；需要 JPA 的模块显式引入 -->
            <dependency>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-starter-data-jpa</artifactId>
            </dependency>
            <!-- springdoc-openapi：含 Controller 的业务模块按需引入，版本由父 POM 统一管理 -->
            <dependency>
                <groupId>org.springdoc</groupId>
                <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
                <version>2.5.0</version>
                <!-- springdoc-openapi v2.5.0 与 Spring Boot 3.2.5 的兼容性已由社区验证通过；若后续升级 Spring Boot 至 3.3.x，需重新评估该版本兼容性，并同步验证 Knife4j（若引入）的传导效应 -->
            </dependency>
            <!-- H2：Phase 0 仅 application 模块以 runtime scope 引入 -->
            <dependency>
                <groupId>com.h2database</groupId>
                <artifactId>h2</artifactId>
                <version>2.2.224</version>
            </dependency>
            <!-- spring-boot-starter-security：Phase 0 仅 application 为 permitAll 占位显式声明；真实认证自 Phase 1 起定义 -->
            <dependency>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-starter-security</artifactId>
            </dependency>
            <!-- spring-boot-starter-validation：含 Controller 的业务模块以 compile scope 引入 -->
            <dependency>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-starter-validation</artifactId>
            </dependency>
            <!-- spring-boot-starter-test：各模块以 test scope 引入 -->
            <dependency>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-starter-test</artifactId>
                <scope>test</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>
</project>
```

**中间层聚合 POM 结构**：

`modules/common-module/pom.xml` — common-module 模块的聚合父 POM，声明其子模块 common-module-api 与 common-module-impl。该中间层 POM 既是子模块共享的 parent 引用点，也是按子树独立构建时的入口：可在 `AIMedical/backend/modules/common-module/` 目录内执行 `mvn -am install`，或在 `AIMedical/backend/` 根目录执行 `mvn -f modules/common-module/pom.xml -am install`。根 POM 仍直接聚合叶子模块，二者并行不冲突：
```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <parent>
        <groupId>com.aimedical</groupId>
        <artifactId>aimedical-sys</artifactId>
        <version>0.0.1-SNAPSHOT</version>
        <relativePath>../../pom.xml</relativePath>
    </parent>
    <artifactId>common-module</artifactId>
    <packaging>pom</packaging>
    <name>Common Module Aggregator</name>
    <modules>
        <module>common-module-api</module>
        <module>common-module-impl</module>
    </modules>
</project>
```

`modules/ai/pom.xml` — AI 模块的聚合父 POM，声明其子模块 ai-api 与 ai-impl。其用途与 common-module 聚合 POM 一致：为子模块提供共享 parent 引用和按子树独立构建时的入口；在 `AIMedical/backend/` 根目录使用时应通过 `mvn -f modules/ai/pom.xml -am install` 指向该聚合 POM，根 POM 继续按叶子模块维度参与总 reactor：
```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <parent>
        <groupId>com.aimedical</groupId>
        <artifactId>aimedical-sys</artifactId>
        <version>0.0.1-SNAPSHOT</version>
        <relativePath>../../pom.xml</relativePath>
    </parent>
    <artifactId>ai</artifactId>
    <packaging>pom</packaging>
    <name>AI Module Aggregator</name>
    <modules>
        <module>ai-api</module>
        <module>ai-impl</module>
    </modules>
</project>
```

### 2.2 模块职责与依赖方向

```
modules/patient ─┬─> common
modules/doctor  ─┤
modules/admin   ─┘

modules/patient ─┬─> modules/common-module/common-module-api ─> common
modules/doctor  ─┤
modules/admin   ─┘

modules/patient ─┬─> modules/ai/ai-api ─> common
modules/doctor  ─┤
modules/admin   ─┘

modules/common-module/common-module-impl ─> modules/common-module/common-module-api
modules/ai/ai-impl                         ─> modules/ai/ai-api

application ─> modules/patient
application ─> modules/doctor
application ─> modules/admin
application ─> modules/common-module/common-module-impl
application ─> modules/ai/ai-impl
```

> **依赖方向说明**：业务模块（patient/doctor/admin）在各自 POM 中显式直接依赖 `common`、`common-module-api` 和 `ai-api`。`common-module-api` 与 `ai-api` 都依赖 `common`，二者之间不互相依赖。common-module-impl 和 ai-impl 仅由 application 模块直接引入，对业务模块完全不可见，Maven 编译期强制隔离。

**模块间依赖规则**：
- `common`：**以 `compile + optional` 方式**依赖 spring-boot-starter-web 及 spring-boot-starter-data-jpa（两者均标注 `<optional>true</optional>`）。spring-boot-starter-web 为 common 自身的 `@ControllerAdvice`、`Result<T>` 响应序列化和 MVC 基础设施提供编译支持；spring-boot-starter-data-jpa 仅用于 `BaseEntity` JPA 注解及 `JpaConfig` 审计配置。optional 标记确保纯契约模块（common-module-api、ai-api）不会被动继承完整 Web/JPA 依赖树
- `modules/common-module/common-module-api`：依赖 `common`。Phase 0 保留为空壳 API 模块，仅包含 `User` 实体必需的共享枚举（如 `UserType`）及占位说明，不定义 `PermissionService`、`UserDTO` 等跨模块门面契约。业务模块可依赖 common-module-api 获取 Phase 0 必需共享类型，但不得依赖任何权限门面接口
- `modules/common-module/common-module-impl`：依赖 `common-module-api`，Phase 0 包含权限实体、Repository 骨架、字典目录占位等，不提供权限门面 Service 实现。**仅由 application 模块引入**，业务模块不可见
- `modules/patient`、`modules/doctor`、`modules/admin`：依赖 `common`、`common-module-api` 和 `modules/ai/ai-api`，三者之间**不允许互相依赖**。Phase 0 业务模块仅声明 ai-api 和 common-module-api 的 POM 依赖（compile scope），但代码中尚未直接引用其类型（占位 Controller **不注入 `AiService`**，且 Phase 0 业务模块不含 `UserType` 等共享类型的直接引用）。`mvn dependency:analyze` 会将此类延迟引用的依赖标记为 Unused declared dependency，因此需在 CI 的 `maven-dependency-plugin` 配置中通过 `<ignoredUnusedDeclaredDependencies>` 显式豁免 ai-api 和 common-module-api（配置见下条规则）；Phase 1+ 业务模块产生真实引用后逐步移除相应豁免条目
- `modules/ai/ai-api`：依赖 `common`，对外暴露纯接口契约（interface + DTO），自身**不含任何业务实现依赖**，也不要求安全上下文与 JPA Repository 基础设施。业务模块**仅依赖 ai-api**，Maven 编译期即保障不会误引入实现层依赖
- `modules/ai/ai-impl`：依赖 `ai-api`，包含 MockAiService、降级策略等实现。**仅由 application 模块引入**，业务模块不可见
- `application`：依赖所有业务模块及 ai-impl 与 common-module-impl，作为 Spring Boot 启动入口。Phase 0 额外引入 H2 内存数据库驱动（runtime scope），通过 application-dev.yml 配置 H2 内存数据源；Phase 1+ 切换为 MySQL/PostgreSQL 关系型数据库
- **依赖分析门禁**：CI 中使用 `mvn dependency:analyze` 验证依赖健康状况——包括禁止循环依赖、检测 used undeclared dependencies 以及意外 unused declared dependencies。对于 Phase 0 业务模块声明但暂未引用的 api 模块依赖（ai-api、common-module-api），在父 POM 的 `maven-dependency-plugin` 配置中通过 `<ignoredUnusedDeclaredDependencies>` 显式豁免：

  ```xml
  <plugin>
      <groupId>org.apache.maven.plugins</groupId>
      <artifactId>maven-dependency-plugin</artifactId>
      <configuration>
          <ignoredUnusedDeclaredDependencies>
              <ignoredUnusedDeclaredDependency>com.aimedical:ai-api</ignoredUnusedDeclaredDependency>
              <ignoredUnusedDeclaredDependency>com.aimedical:common-module-api</ignoredUnusedDeclaredDependency>
          </ignoredUnusedDeclaredDependencies>
      </configuration>
  </plugin>
  ```

  Phase 1+ 各业务模块开始注入 `AiService`、`UserDTO` 等 api 模块类型后，逐模块移除对应豁免条目，使 `dependency:analyze` 恢复完整检出力
- **依赖管理（父 POM）**：父 POM 的 `<dependencyManagement>` 统一声明以下 Web、JPA、测试与校验依赖版本，各模块按需引入：
  - `spring-boot-starter-web`：common 以 `optional` 声明；包含 Controller 或需启动 Web 容器的模块（patient、doctor、admin、application）显式声明该依赖，确保不会传递污染 ai-api/common-module-api 这类纯契约模块
  - `spring-boot-starter-data-jpa`：common 以 `optional` 声明；需要 JPA 的模块（patient、doctor、admin、common-module-impl、application）显式声明该依赖
  - `spring-boot-starter-test`：各模块（common、modules/*、application、integration）以 `test` scope 引入，提供 JUnit 5、Mockito、@SpringBootTest 等测试支持
  - `spring-boot-starter-validation`：包含 Controller 的业务模块（patient、doctor、admin）以默认 `compile` scope 引入，提供 Hibernate Validator 实现使 `@Valid` 注解生效；不含 Controller 的模块无需引入

**Common 模块依赖传播决策**：common 模块只保留自身骨架真正需要的 Starter，并区分传播策略如下：
  - `spring-boot-starter-web`（compile，标注 `<optional>true</optional>`）：common 中的 `@ControllerAdvice`、`Result<T>` JSON 序列化及 Spring MVC 基础设施需要该依赖；标记 optional 后，`common-module-api`、`ai-api` 这类契约模块不会被动继承完整 Web 容器依赖树。需要 Web 能力的模块（patient、doctor、admin、application）在自己的 POM 中显式声明该依赖（版本由父 POM 统一管理）
  - `spring-boot-starter-data-jpa`（compile，标注 `<optional>true</optional>`）：仅用于 `BaseEntity` 和 `JpaConfig` 这类 JPA 基础类型。标记 optional 后，`common-module-api`、`ai-api` 这类纯契约模块不会被动继承完整 JPA 依赖树；需要 JPA 的模块（patient、doctor、admin、common-module-impl、application）在自己的 POM 中显式声明该依赖（版本由父 POM 统一管理）
  - `spring-boot-starter-security`：不在 `common` 中声明。Phase 0 仅 application 模块为 `SecurityConfigPhase0` 的 permitAll 占位显式声明该依赖；真实认证、授权、PasswordEncoder、UserDetailsService 等安全基础设施自 Phase 1 起定义

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
│   ├── commonmodule
│   │   └── api            # common-module-api 子模块：Phase 0 仅含 UserType 等共享类型
│   │       └── UserType   # User 实体 userType 字段使用的共享枚举
│   │   └── impl           # common-module-impl 子模块：实现
│   │       ├── permission # 权限相关 entity/repository（User, Role, Post, Function）
│   │       ├── config     # 业务级配置
│   │       └── dict       # 字典管理
│   └── ai                 # AI 能力模块
│       └── api            # ai-api 子模块：AI 能力接口契约
│           ├── AiService  # AI 能力接口集合
│           ├── dto        # 输入/输出 DTO
│           └── degradation# 降级策略接口
│       └── impl           # ai-impl 子模块：AI 实现
│           ├── mock       # Mock 实现
│           ├── fallback   # 降级实现
│           └── degradation# 降级策略实现（NoOpDegradationStrategy）
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

**根 `package.json` 的 workspaces 配置**：

```json
{
  "name": "aimedical-frontend",
  "private": true,
  "workspaces": [
    "packages/*",
    "apps/*"
  ]
}
```

**内部包导出配置**：

`packages/shared/package.json`：
```json
{
  "name": "@aimedical/shared",
  "private": true,
  "version": "0.0.0",
  "main": "src/index.ts",
  "types": "src/index.ts"
}
```

`packages/shared/src/index.ts` 导出内容：
- `api/` — 导出 `ApiClient` 实例（已配置 baseURL 和拦截器）、各模块 API 调用函数
- `types/` — 导出所有 TypeScript 类型定义（与后端 DTO 对应的 interface）
- `utils/` — 导出通用工具函数（日期格式化、分页参数转换等）

`packages/ui-core/package.json`：
```json
{
  "name": "@aimedical/ui-core",
  "private": true,
  "version": "0.0.0",
  "main": "src/index.ts",
  "types": "src/index.ts",
  "dependencies": {
    "@aimedical/shared": "0.0.0"
  }
}
```

`packages/ui-core/src/index.ts` 导出内容：导出 `components/` 下所有共享 UI 组件（Layout、Sidebar、Table、Form 等），三端应用按需 import 使用。

**三端应用引用方式**（以 `apps/patient/package.json` 为例）：

```json
{
  "name": "@aimedical/app-patient",
  "private": true,
  "dependencies": {
    "@aimedical/shared": "0.0.0",
    "@aimedical/ui-core": "0.0.0"
  }
}
```

npm workspaces 会在依赖名与本地 workspace 包匹配、且版本满足声明时，将内部包链接到根 `node_modules`；Phase 0 统一为内部包声明固定占位版本 `0.0.0`，并由消费方以相同版本号引用，避免 `workspace:*` 在当前 npm 工具链下出现兼容性问题。Vite 直接复用该解析结果，无需额外 `resolve.alias` 配置。

**三端占位首页入口文件结构**（每个 `apps/*` 均包含以下三个入口文件，Phase 0 的占位页面仅渲染系统名称 + 占位提示文本）：

```
apps/{patient,doctor,admin}/
├── index.html              # HTML 入口，挂载点 <div id="app">
├── src/
│   ├── main.ts             # Vue 应用入口：createApp + router
│   └── App.vue             # 根组件：占位页面内容
```

`index.html` 挂载点骨架：
```html
<!DOCTYPE html>
<html lang="zh-CN">
<head><meta charset="UTF-8" /><title>智慧云脑诊疗平台</title></head>
<body><div id="app"></div><script type="module" src="/src/main.ts"></script></body>
</html>
```

`src/main.ts` 骨架：`import { createApp } from 'vue'; import App from './App.vue'; createApp(App).mount('#app');`

`src/App.vue` 占位内容：渲染 `<h1>智慧云脑诊疗平台 - {患者端/医生端/管理员端}</h1>` 及占位提示。

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

- `PageQuery`：包含 `page`（0-based，标注 `@Min(0)`，与 Spring Data JPA 默认一致；前端分页组件提交时需将 1-based 页码转换为 0-based）、`size`（默认值 20，上限 500，标注 `@Max(500)` 防止恶意大分页导致 OOM；需结合 `@Valid` 使校验生效）、`sort`（`List<String>` 类型，复用 Spring Data 原生多值绑定约定，单项格式为 `"fieldName,direction"`，direction 为 `asc` 或 `desc`，如 `?sort=createdAt,desc&sort=id,asc`；Controller 或 Service 在组装 `Pageable` 时逐项转换为 `Sort.Order`，格式无效时抛出 `BusinessException(PARAM_INVALID)` 由 `GlobalExceptionHandler` 统一返回 400 响应）。所有分页 Controller 参数需标注 `@Valid` 以触发校验注解。`@Valid` 对 `@ModelAttribute`（GET 查询参数绑定）和 `@RequestBody`（POST JSON 绑定）均生效，前提是 `spring-boot-starter-validation` 在类路径上（父 POM 的 `<dependencyManagement>` 中已统一声明，含 Controller 的业务模块以 `compile` scope 引入即可）。
- `PageResponse<T>`：包含 `content`（列表）、`totalElements`、`totalPages`、`page`、`size` 字段

**为何使用独立 class 而非内联参数**：统一分页契约避免各模块自行定义分页结构，确保前端分页组件可复用。

#### `ErrorCode` — 错误码命名空间（interface）

**职责**：定义全局错误码体系契约，每个错误码通过方法暴露 `code`（字符串）和 `message`（用户可读描述）。

**设计要点**：
- `ErrorCode` 定义为 `interface`，定义在 common 模块中，作为 `BusinessException` 持有的统一引用类型，方法签名固定为 `String code();` 和 `String message();`
- 各模块提供 `enum` 实现该 `interface`，按业务域分配错误码段（如 `COMMON_XXXX`、`PATIENT_XXXX`、`DOCTOR_XXXX`、`AI_XXXX`）
- 错误码一经发布不可修改含义（可废弃但不复用）

```java
public interface ErrorCode {
    String code();
    String message();
}
```

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
- `deleted`：`Boolean` 类型（包装类型），标注 `@Column(nullable = false)`，默认值 `false`，逻辑删除标记。使用包装类型而非基本类型 `boolean`，可区分"未设置"（`null`）与"未删除"（`false`）状态，避免原生 SQL 插入绕过 Hibernate 时 `null` 默认值与 `@Column(nullable = false)` 约束冲突。由 `@SQLDelete` + `@SQLRestriction` 注解配合实现软删除：`@SQLDelete` 定义删除时执行的 UPDATE SQL 模板（`UPDATE {h-table} SET deleted = true WHERE id = ?`），其中 `{h-table}` 为 Hibernate 6 的表名占位符；`@SQLRestriction("deleted = false")` 替代已废弃的 `@Where` 注解（Hibernate 6.2+），确保通过 Hibernate 发起的常规实体查询自动过滤已删除记录。原生 SQL 查询不自动继承该限制，需显式附加 `deleted = false` 条件

**JPA 注解**：`@MappedSuperclass` 标注在类级别，使子类继承映射而无需为 BaseEntity 建表。

**协作关系**：所有业务实体继承它。无其他协作。

**为何使用 abstract class**：实体需要共享字段及 JPA 注解（`@MappedSuperclass`、`@Id`、`@PrePersist`、`@PreUpdate`），抽象类是最自然的复用方式。interface 无法携带字段和 JPA 注解。

#### `BaseEnum` — 枚举通用基接口（interface，归属 `common.base`）

**职责**：为系统中所有"对外暴露码值与描述"的枚举提供统一的取值契约。任何需要将枚举值与外部字符串/显示文本双向映射的场景（如字典项、状态码、业务类型等）均应实现该接口，使基础设施（序列化、字典转换、前端下拉渲染）可按统一方式消费。

**方法签名**：
- `String getCode()`：返回枚举值的对外编码（字符串类型，避免 int 编码的位数限制与跨语言兼容性问题），在整个系统内必须唯一且稳定
- `String getDesc()`：返回枚举值的可读描述，通常用于前端展示、日志输出与异常消息填充

**接口定义**：
```java
public interface BaseEnum {
    String getCode();
    String getDesc();
}
```

**协作对象**：
- 通用工具方法（如 `BaseEnumUtils.getByCode(Class<? extends BaseEnum>, String)`、`BaseEnumUtils.getDescByCode(...)` 等）按 `getCode()` 反射取值，避免在业务侧书写重复的 `valueOf` 与 `switch` 分支
- 序列化层（如 Jackson 自定义序列化器）按 `getCode()` 输出枚举的字符串码值，与 `ErrorCode` 共用同一套"枚举 → 字符串"序列化约定，确保前端接收到的枚举字段为稳定字符串而非 Java 枚举名

**与 `ErrorCode` 的边界**：
- `BaseEnum` 是**枚举通用基接口**，面向所有需要 `code` / `desc` 暴露的枚举（字典、状态、业务类型等）
- `ErrorCode` 是**错误码命名空间契约**，专门服务于异常链路，方法签名为 `code()` / `message()`，与 `BaseEnum` 的 `getCode()` / `getDesc()` 在命名上不重合
- 二者职责正交：枚举可同时 `implements BaseEnum` 以支持字典/序列化场景；当且仅当该枚举用于 `BusinessException` 错误码时才 `implements ErrorCode`；不允许将 `ErrorCode` 与 `BaseEnum` 合并为单一接口
- Phase 0 仅定义 `BaseEnum` 接口本身与占位使用约定，具体业务枚举（如 `common` 中的 `GlobalErrorCode`、`UserType`）按需选择实现，Phase 0 不强制所有 enum 立即实现 `BaseEnum`

**为何使用 interface 而非 abstract class**：枚举在 Java 中已隐式继承 `java.lang.Enum`，无法再继承其他类；interface 是唯一可被 enum 实现的多态扩展点。`BaseEnum` 不携带任何字段或默认行为（不提供 `fromCode` 静态方法等），仅声明取值契约。

#### `JpaConfig` — JPA 审计配置（class，归属 `common.config`）

**职责**：启用 JPA 审计功能，使 `@CreatedDate`、`@LastModifiedDate` 等审计注解生效。

**协作**：标注 `@EnableJpaAuditing`，配合 `BaseEntity` 上的 `@EntityListeners(AuditingEntityListener.class)` 实现 `createdAt`/`updatedAt` 自动填充。BaseEntity 本身不负责审计激活，由 JpaConfig 统一管理。

**为何使用独立配置类**：`@EnableJpaAuditing` 需要声明在 `@Configuration` 类上。独立配置类集中管理 JPA 相关 enable 注解，避免散布在启动类或其他配置中。该配置类放置在 `common` 模块的 `com.aimedical.common.config` 包下。

### 3.3 权限模型核心抽象

所有权限模型实体（User、Role、Post、Function）归属 `common-module-impl` 子模块，位于包路径 `com.aimedical.modules.commonmodule.permission`；`UserType` 等 Phase 0 实体必需共享类型归属 `common-module-api` 子模块。common-module-impl 仅由 application 模块引入，从 Maven 编译层面确保业务模块不误入实现细节。Phase 0 的 common-module-impl 仅提供实体与 Repository 骨架，不定义跨模块门面接口、DTO 或门面实现；`PermissionService`、`UserDTO`、`UserDetailsService`、`LoginUser` 等认证/权限服务契约自 Phase 1 起补齐。

#### `User` — 统一用户实体（entity class，归属 `common-module-impl`）

**职责**：统一用户实体，覆盖患者、医生、管理员三类使用方。通过 `userType` 枚举区分用户类型，通过多对多关联 `Role` 和 `Post`。

**`UserType` 归属约定**：`UserType` 枚举定义在 `common-module-api` 子模块中，由 Phase 0 的 `User` 实体引用。Phase 1 定义 `UserDTO` 时复用该枚举。这样 impl 层可在不反向依赖未来 DTO 的前提下使用共享用户类型。

**协作**：`User ↔ Role`（多对多），`User ↔ Post`（多对多，医生端特有）。

**跨模块共享机制**：User 实体定义在 common-module-impl 中。Phase 0 不提供跨模块 User 查询接口，业务模块不得直接操作 User Repository，也不得定义临时门面绕过阶段边界。Phase 1 起由 common-module-api 提供类型化门面方法，业务模块通过门面接口访问用户与权限数据。

#### `Role` — 角色（entity class，归属 `common-module-impl`）

**职责**：粗粒度角色定义（如"患者"、"门诊医生"、"检查医生"、"管理员"）。一个用户可拥有多个角色。

**协作**：`Role ↔ Post`（一对多），`Role ↔ User`（多对多）。

#### `Post` — 岗位（entity class，归属 `common-module-impl`）

**职责**：细粒度岗位定义（如"门诊医生-神经内科"、"药房医生"）。角色决定大权限范围，岗位决定具体可访问功能和数据范围。医生端通过岗位区分五个子岗位的可操作菜单。

**协作**：`Post ↔ Function`（多对多），`Post ↔ User`（多对多）。

#### `Function` — 功能权限（entity class，归属 `common-module-impl`）

**职责**：最细粒度的操作权限（如"查看挂号列表"、"创建处方"、"AI 审核"）。岗位与功能多对多关联，实现功能级访问控制。

#### JPA 关系映射约定

所有权限实体统一遵循以下 JPA 关系映射约定：

- **`User ↔ Role`（多对多）**：User 端标注 `@ManyToMany(fetch = FetchType.LAZY)` + `@JoinTable(name = "user_role", joinColumns = @JoinColumn(name = "user_id"), inverseJoinColumns = @JoinColumn(name = "role_id"))`，Role 端标注 `@ManyToMany(mappedBy = "roles", fetch = FetchType.LAZY)`，两端均不设 cascade
- **`User ↔ Post`（多对多）**：User 端标注 `@ManyToMany(fetch = FetchType.LAZY)` + `@JoinTable(name = "user_post", joinColumns = @JoinColumn(name = "user_id"), inverseJoinColumns = @JoinColumn(name = "post_id"))`，Post 端标注 `@ManyToMany(mappedBy = "posts", fetch = FetchType.LAZY)`，两端均不设 cascade
- **`Role ↔ Post`（一对多）**：Post 端为 owning side，标注 `@ManyToOne(fetch = FetchType.LAZY)` + `@JoinColumn(name = "role_id")`，Role 端标注 `@OneToMany(mappedBy = "role", fetch = FetchType.LAZY)`，不设 cascade，`orphanRemoval = false`
- **`Post ↔ Function`（多对多）**：Post 端标注 `@ManyToMany(fetch = FetchType.LAZY)` + `@JoinTable(name = "post_function", joinColumns = @JoinColumn(name = "post_id"), inverseJoinColumns = @JoinColumn(name = "function_id"))`，Function 端标注 `@ManyToMany(mappedBy = "functions", fetch = FetchType.LAZY)`，两端均不设 cascade

**关联表命名约定**：多对多关联表统一命名为 `{entity1}_{entity2}`（如 `user_role`、`user_post`、`post_function`），字段名按 entity 单数形式 + `_id` 后缀。**Fetch 策略**：所有关系注解统一使用 `FetchType.LAZY`，避免 N+1 问题通过 `@EntityGraph` 或 `fetch join` 在 Service 层显式控制。**Cascade 策略**：Entity 端不设任何 cascade，所有关联实体的持久化、更新、删除由 Service 层通过 Repository 调用显式管理，避免级联操作的不确定性和意外数据变更。对于多对多删除场景，Service 层必须先显式清理关联表记录（如 `user_role`、`user_post`、`post_function`），再删除主实体；Phase 0 不依赖数据库 `ON DELETE CASCADE` 作为默认策略。

**为何使用"角色—岗位—功能"三级而非简单的 RBAC**：
- 角色（Role）解决"你是谁"——患者/门诊医生/管理员
- 岗位（Post）解决"你在这个角色下能做什么"——同是医生，门诊医生与检查医生的菜单与权限不同
- 功能（Function）解决"你能操作哪些具体功能"——岗位确定功能集合，易于维护
- 三级模型可在不修改角色/岗位代码的前提下通过调整岗位-功能关联来变更权限，满足支持后续扩展新角色与新岗位的要求

#### 数据范围扩展点（承接需求 2.6 的 `○¹` / `○²`）

需求文档 2.6 中的 `○¹`（创建者/责任人）与 `○²`（接诊/经手人）属于**数据级权限**语义，不等价于 `Function` 的功能级授权。Phase 0 仅预留扩展点，不在本阶段定义数据范围语义枚举或落地实现，约定如下：

- Phase 0 仅在文档层记录数据级权限扩展点，不定义 `DataPermissionEvaluator`、最小上下文 DTO 或数据范围枚举
- Phase 1 起在 common-module-api 中定义数据权限门面接口，并由各业务模块在 Service/Repository 层通过该门面拼装查询谓词或 Specification，落实行级过滤
- `PermissionService` 自 Phase 1 起负责功能权限；数据范围判定不混入 `PermissionService#getUserPermissions(...)` 的返回集合，避免把行级规则误建模为静态功能码

### 3.4 AI 能力模块抽象

#### `AiService` — AI 能力接口集合（interface，归属 `ai-api` 子模块）

**职责**：定义 13 项 AI 能力的类型化方法签名，每项能力对应一个方法，输入/输出类型由具体 DTO 确定。涵盖成功、降级、超时、不可用等调用状态。

**协作**：
- 业务模块通过 `AiService` 接口调用各自需要的 AI 能力方法，不依赖具体实现
- `MockAiService` 在 Phase 0 实现该接口的全部 13 个方法，每个方法返回 `CompletableFuture.completedFuture(...)` 包装的占位数据
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

**协作**：实现 `AiService` 接口的 13 个方法，每个方法返回对应能力的固定结构占位数据，并用 `CompletableFuture.completedFuture(...)` 包装。

**Mock 数据占位约定**：
- Phase 0 仅对 8.2 节标记的 `@phase0-mock-field` 生效；未标记字段不在本阶段冻结
- 被标记字段按以下规则填充：集合字段固定返回 1-2 条占位数据；字符串字段填充 `"mock_" + 字段名`；数值字段填充 0 或 1；布尔字段填充 `false`
- 若标记字段为嵌套路径，则仅递归实例化该路径上必需的父对象；未标记的兄弟字段返回 `null`、空集合或语言默认值
- 标注 `@Nullable` 或在 Javadoc 中标记为"可选"/"optional"的已标记字段，Mock 返回 `null`；可选集合字段返回空集合
- 未进入 Phase 0 Mock 字段子集的 DTO 允许保持空壳，仅保证 `AiService` 方法签名与基础序列化链路可编译、可演示

**Bean 装配策略**：
- `MockAiService`：标注 `@ConditionalOnProperty(name = "ai.mock.enabled", havingValue = "true", matchIfMissing = true)`。当配置项 `ai.mock.enabled=true` 时（默认开发环境），MockAiService 被注册为 Spring Bean；未配置时同样默认激活 MockAiService
- 真实 `AiService` 实现（Phase 2+）：标注 `@ConditionalOnProperty(name = "ai.mock.enabled", havingValue = "false")`，通过同一配置键的反向条件与 MockAiService 互斥，避免 `@ConditionalOnMissingBean` 与 `@Primary` 装饰器的语义冲突
- `FallbackAiService`：作为装饰器始终注册为 Bean，并通过 application 模块统一暴露默认 `AiService` 注入点。业务模块只按类型注入 `AiService`，不使用 `@Qualifier` 感知 `fallbackAiService` 这一实现名。FallbackAiService 内部通过构造器注入 `List<AiService>` 获取所有已注册的 AiService Bean，在构造器中排除自身（`! (s instanceof FallbackAiService)`）后选定委托对象，从根本上避免自引用循环依赖
- 应用启动时，application 模块的配置决定 `ai.mock.enabled` 值，从而控制使用 Mock 还是真实实现

**装配条件汇总**：

| 配置 `ai.mock.enabled` | 激活的 AiService 实现 | FallbackAiService 委托对象 | 适用场景 |
|------------------------|----------------------|----------------------------|----------|
| `true`（默认）或未配置 | MockAiService | MockAiService（从 List<AiService> 排除自身后获得） | Phase 0 本地开发、前端联调、CI 集成测试 |

> **Phase 0 说明**：Phase 0 不存在真实 AiService 实现，`ai.mock.enabled` 应始终为 `true` 或未配置（`matchIfMissing=true` 默认激活 MockAiService）。Phase 0 禁止将 `ai.mock.enabled` 设为 `false`——此时 MockAiService 因 `@ConditionalOnProperty(havingValue = "true")` 条件不满足不注册，FallbackAiService 的 `List<AiService>` 排除自身后为空，所有 AI 调用将返回降级结果（`degraded=true`），且启动期输出 `ERROR` 日志、运行期输出 `WARN` 日志。Phase 2+ 引入真实 AiService 实现后，`false` 配置恢复正常行为（route 表届时扩展为 `true`/`false`/未配置 三行）。业务模块的注入代码在各阶段保持 `private final AiService aiService;` 不变。Phase 0 的占位 Controller / Service 不应编写对 `AiResult.data` 的业务解引用逻辑；如确需演示调用，仅允许将 `degraded` 与 `success` 作为 UI 占位判断条件。

#### 降级策略框架（interface + class 体系，归属 `ai-api` 和 `ai-impl` 子模块）

- `DegradationContext`（class，`ai-api`）：封装降级判定所需的上下文信息。Phase 0 仅保留零值构造器（无参构造器，字段取语言默认值），不声明任何业务字段；Phase 2+ 根据真实降级场景需求扩展字段（如 `invocationCount`、`lastFailureTime`、`elapsedTime`、`requestType` 等），扩展时不得修改 `DegradationStrategy.shouldDegrade(DegradationContext)` 方法签名（签名在 Phase 0 冻结）。Phase 0 中 FallbackAiService 构造默认 DegradationContext 实例（零值对象而非 `null`），为未来真实策略预留扩展点
- `DegradationStrategy`（interface，`ai-api`）：定义降级判定逻辑 `boolean shouldDegrade(DegradationContext context)`。取消泛型 fallback 方法，降级后的返回值由 FallbackAiService 在被调用方直接构造 `AiResult(success=false, degraded=true, data=null)`。DegradationStrategy 仅关注"是否触发降级"的判定，不关注降级结果的构造
- `NoOpDegradationStrategy`（class，`ai-impl`）：Phase 0 默认注册的降级策略，`shouldDegrade()` 始终返回 `false`（永不主动降级）。标注 `@ConditionalOnMissingBean(DegradationStrategy.class)`，确保 Phase 2+ 被真实策略替换时自动退让
- `FallbackAiService`（class，`ai-impl`）：包装降级逻辑的 `AiService` 装饰器，在 AI 调用失败时按策略执行降级。内部通过构造器注入 `List<DegradationStrategy>` 策略列表（Phase 0 仅含 NoOpDegradationStrategy，Phase 2+ 可传入多个策略）；遍历策略列表，任一策略 `shouldDegrade()` 返回 `true` 即触发降级。同时通过构造器注入 `List<AiService>` 获取所有已注册的 AiService Bean，在构造器中排除自身后选定委托对象，避免自引用循环依赖。降级触发时构造 `DegradationContext` 实例并传递给 `DegradationStrategy.shouldDegrade()`。**兜底保护**：当 `List<AiService>` 中排除自身后为空（无可用的非 Fallback AiService 实现时），FallbackAiService 不抛出异常，直接返回 `AiResult` 实例（`success=false`、`degraded=true`、`data=null`），确保业务链路不因 AI 模块缺失而断裂；同时启动期输出 `ERROR` 日志、运行期输出 `WARN` 日志，明确提示当前实例处于“无委托对象”的异常配置状态。Phase 0 的构造器签名为：
  ```
  public FallbackAiService(List<AiService> aiServiceList,
                           List<DegradationStrategy> strategies)
  ```

**为何使用 interface + 装饰器模式**：降级策略未来会扩展（超时降级、熔断降级、Mock 降级），用 interface 抽象使主逻辑与降级策略解耦；用装饰器包装 `AiService` 使降级逻辑对业务模块透明。

### 3.5 前端共享抽象

#### `ApiClient` — API 客户端（class，封装 Axios）

**职责**：统一封装 Axios 实例，配置 baseURL、请求/响应拦截器，并对 `Result<T>` 响应做统一拆包。Phase 0 仅实现无认证场景必需的基础能力；认证相关拦截逻辑作为 Phase 1+ 扩展点预留。

**协作**：
- 响应拦截器检查 `Result.code`，非成功码统一走错误处理
- Phase 1+ 可在请求拦截器中从认证状态容器读取 JWT token 并附加到请求头；Phase 0 不启用该逻辑
- 错误拦截器：Axios 请求/网络错误（DNS 解析失败、连接超时、请求被取消等）由 Axios 错误拦截器统一捕获，返回格式 `{ code: "NETWORK_ERROR", message: "网络不可达，请检查网络连接" }`；前端根据 `code === "NETWORK_ERROR"` 决定是否弹出全局错误提示，避免未捕获的 Promise 异常

#### `AuthStore` — 认证状态管理（Pinia store）

**职责**：管理登录态、当前用户信息、token 的存取。该抽象自 Phase 1 起启用，Phase 0 前端骨架不要求提供实际登录态实现。

**协作**：Phase 1+ 中，`ApiClient` 在请求拦截器中通过 `AuthStore` 获取 token；`ApiClient` 在响应 401 时通知 `AuthStore` 清除登录态并跳转登录页。Phase 0 保留类型和目录约定即可，不实现登录跳转。

---

## 4. 关键行为契约

### 4.1 健康检查

```
GET /api/ping
→ 200 OK, Result<String> { code: "SUCCESS", data: "pong" }
```

`/api/ping` 作为 Phase 0 的系统级健康检查端点，**不归属任何业务模块**，由 `application` 模块中的 `HealthController`（或同等命名的系统控制器）提供；这是 8.1 节 `/api/{module}` 业务模块路径约定的唯一显式例外。

Phase 0 验收标准：后端可独立启动并响应 `GET /api/ping`。

前端验收标准：
- 在 `AIMedical/frontend/` 目录下，三端应用（patient/doctor/admin）执行 `npm run dev` 可正常启动 Vite 开发服务器
- 浏览器访问各端端口显示占位页面（包含系统名称及占位提示文本）
- 各端开发服务器可正常代理 `/api/ping` 请求到后端并收到 `"pong"` 响应

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
  AiService.triage(request)          // 或其他具体能力方法，返回 CompletableFuture<AiResult<T>>
    ├── MockAiService → 返回占位数据（Phase 0）
    ├── 真实实现    → 调用大模型 API（Phase 2+）
    └── FallbackAiService(调用失败) → DegradationStrategy.shouldDegrade(context)
          ↓ degraded=true
          └── 直接返回 AiResult(success=false, degraded=true, data=null)
```

AI 调用结果统一由 `CompletableFuture<AiResult<T>>` 承载，业务模块在同步链路中可直接 `join()` 获取结果，在需要编排的场景下可继续组合异步调用；最终仍根据 `AiResult.degraded` 决定前端是否显示"AI 暂不可用"标识。

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

> **阶段边界说明**：本节仅说明未来权限校验链路在系统中的挂载位置。Phase 0 运行时采用 `SecurityConfigPhase0` 的 `permitAll` 占位策略，不实际触发认证、授权或 401/403 响应分支；以下认证/授权结果语义自 Phase 1 安全 OOD 起正式冻结。

```
请求到达 → Spring Security FilterChain
  ├── 未认证 → 401, Result.fail(AUTH_UNAUTHORIZED)
  └── 已认证 →
       ├── 无权限 → 403, Result.fail(FORBIDDEN)
       └── 有权限 → 正常处理
```

**SecurityConfig 设计骨架**（归属 `application` 模块；`common` 仅保留与安全无关的全局基础配置）：

**Phase 0 策略**：Phase 0 作为最小化骨架阶段，无登录页面、无认证 Controller、无 token 签发机制。application 模块仅保留最小 `SecurityConfigPhase0` 占位，使用 `permitAll` 放通所有接口，确保后端可独立启动并通过健康检查。该占位配置不依赖 `AuthenticationEntryPoint`、`AccessDeniedHandler`、`PasswordEncoder`、CORS 安全配置或 `UserDetailsService`，真实认证与授权基础设施自 Phase 1 起定义。`spring.profiles.active=phase0,dev` 已在 `application.yml`（9.1 节）中设置，`phase0` 激活 SecurityConfigPhase0，`dev` 同时加载开发环境配置（H2 数据库、日志级别等）：

```
@Configuration
@Profile("phase0")                                   // Phase 0 激活此配置
public class SecurityConfigPhase0 {
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .anyRequest().permitAll()            // Phase 0 全员放通
            );
        return http.build();
    }
}
```

Phase 1 切换方式：Phase 1 OOD 重新定义真实 SecurityConfig、认证流程、`AuthenticationEntryPoint`、`AccessDeniedHandler`、`PasswordEncoder`、CORS 策略、`UserDetailsService` 与 `LoginUser`。Phase 0 不冻结这些实现细节，也不预设 `@Profile("!phase0")` 等切换机制。Phase 0 前端三端联调通过 Vite proxy 访问后端 `/api/ping`，不依赖后端 CORS 安全配置。

---

## 5. 错误处理策略

### 5.1 错误分类

> **阶段边界说明**：下表中的认证错误、授权错误为 Phase 1+ 预留错误类别。Phase 0 由于 `permitAll` 放通所有请求，正常运行路径下不会进入这两类分支。

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
| 配置加载失败 | 必填配置项缺失、配置格式错误 | 应用启动失败，由 Spring Boot 的失败分析器（FailureAnalyzer）输出诊断信息 | N/A（启动失败，无 HTTP 响应） |
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
  - **Phase 0（当前）**：AiService 接口已统一定义为返回 `CompletableFuture<AiResult<T>>`；MockAiService 通过 `CompletableFuture.completedFuture(...)` 立即返回占位数据，调用方可直接 `join()`，无需额外线程管理
  - **Phase 2+（未来）**：沿用同一 `CompletableFuture<AiResult<T>>` 契约，引入 Spring Async 或等价异步执行机制实现非阻塞调用，由业务层决定是否等待或编排多个 AI 调用

---

## 7. 设计决策

| 决策 | 选项 | 选择 | 理由 |
|------|------|------|------|
| 架构风格 | 微服务 vs 模块化单体 | 模块化单体（Maven 多模块） | Phase 0 团队规模与需求复杂度不支撑微服务运维成本；模块边界即为未来微服务拆分边界 |
| 后端构建工具 | Gradle vs Maven | Maven | 技术栈文档明确使用 `mvn spring-boot:run`，与项目规范一致 |
| 前端 Monorepo 方案 | Nx vs Turborepo vs npm workspaces + Vite | npm workspaces + Vite | 保持 Vite 作为构建工具，同时使用 npm workspaces 管理内部包，避免引入额外的 Monorepo 工具链依赖 |
| AI 能力接口形态 | 独立接口 vs 单泛型门面 vs 方法集合 | 统一接口方法集合 `AiService` | 13 项 AI 能力的输入/输出类型各不相同，统一为单一接口的 13 个方法可兼顾类型安全与门面统一性；相比独立接口可共享降级、超时、熔断等横切逻辑；相比单泛型方法无需运行时类型分发 |
| AI 模块拆分策略 | 单模块 vs ai-api/ai-impl 子模块 | 拆分 ai-api / ai-impl 两个 Maven 子模块 | ai-api 仅含接口和 DTO，不含业务实现依赖；业务模块编译期强制只依赖 ai-api；ai-impl 包含实现，仅由 application 引入。从 POM 依赖树层面杜绝业务模块误引用实现层的隐患 |
| 权限模型 | RBAC vs 三级模型 | 角色—岗位—功能三级 | 需求 2.5 明确要求三级模型，且角色-岗位-功能三层的解耦使得权限配置更灵活，可映射复杂的医疗岗位权限场景 |
| 错误码形式 | int vs String | String（如 `PATIENT_MOBILE_EXISTS`） | String 可读性强，易于前端根据错误码做差异化处理；int 需要文档映射 |
| ErrorCode 类型形态 | enum vs interface + 各模块 enum 实现 | `interface` + 各模块 enum 实现 | enum 为 final 类，各模块无法独立扩展自己的错误码枚举，且 BusinessException 无法持有统一 ErrorCode 引用类型；interface 定义契约，各模块通过 enum 实现该 interface，兼顾类型统一与模块独立扩展 |
| API 版本管理 | URL 路径 vs 请求头 vs 无版本 | 无显式版本（通过模块路径隐含） | Phase 0 至 Phase 6 为同一个大版本演进，模块内接口变更通过新增接口、废弃旧接口的方式管理。如需对外暴露版本，后续引入 URL 路径版本 `api/v2/...` |
| 全局异常处理 | `@ControllerAdvice` vs AOP | `@ControllerAdvice` | Spring 原生机制，自然适配 Spring MVC 的异常处理流程，无需额外 AOP 配置 |
| AI Mock 实现形态 | Spring @Profile vs 条件注解 | `@ConditionalOnProperty` | 通过配置开关 `ai.mock.enabled` 控制是否启用 Mock，同一 jar 包可在不同环境切换，无需 Profile 切换；同时支持条件化装配的唯一激活保障 |
| 前端共享包结构 | 独立 npm 包 vs workspace 内部包 | npm workspace 内部包 | 避免 npm 发布流程，简化开发期引用 |
| FallbackAiService 装配策略 | application 统一装配默认 AiService vs 业务模块 `@Qualifier` 指定实现名 | application 模块统一暴露默认 `AiService` 注入点；FallbackAiService 构造器注入 `List<AiService>`，排除自身后选定委托对象 | 业务模块只依赖 `AiService` 接口即可，避免硬编码 `fallbackAiService` 这一实现细节；同时保留 `List<AiService>` 排除自身的确定性委托选择逻辑，规避自引用循环依赖 |
| 公共模块拆分策略 | 单模块 common-module vs common-module-api/common-module-impl 子模块 | 拆分 common-module-api / common-module-impl 两个 Maven 子模块 | 单模块时业务模块 POM 依赖 common-module 后可直接访问 Service 实现类、Repository 和 Entity，绕开门面接口约束，与 ai-api/ai-impl 的架构不一致。拆分后 common-module-api 在 Phase 0 仅保留实体必需共享类型，Phase 1 起承载门面接口和 DTO；common-module-impl 包含实现，仅由 application 引入，从 POM 依赖树层面实现编译期强制隔离 |
| 降级判定上下文 | 无参 shouldDegrade() vs 携带 DegradationContext | `shouldDegrade(DegradationContext context)` | 无参接口无法支撑有意义的降级判定逻辑；DegradationContext 为 Phase 2+ 真实策略预留扩展点；Phase 0 仅保留零值构造器，字段待 Phase 2+ 按真实降级场景扩展 |
| CI 分阶段构建策略 | mvn compile 分阶段 vs mvn install -DskipTests 分阶段 | `mvn install -DskipTests` 分阶段构建 | `mvn compile` 不安装产物到本地仓库，后续阶段依赖解析失败；`mvn install -DskipTests` 将编译产物安装到本地仓库，后续阶段可正常解析跨阶段依赖 |

---

## 8. 模块间 API 通信规范

### 8.1 模块暴露规范

**API 版本管理策略**：Phase 0~Phase 6 在同一主版本内演进，API 路径不含版本号段；业务模块 Controller 基路径统一为 `/api/{module}`；系统级端点仅保留显式例外 `/api/ping`；如需对外暴露版本，Phase 6+ 引入 `/api/v2/` 版本路径。

- 每个业务模块通过 `api` 子包暴露 REST Controller，路径前缀为 `/api/{module}`（如 `/api/patient/...`）
- Controller 方法统一返回 `Result<T>` 或 `Result<PageResponse<T>>`
- 模块内部类（entity、repository）不对外暴露，其他模块不能直接访问

### 8.2 AI 能力方法清单（Phase 0 Mock 占位）

Phase 0 定义 `AiService` 接口中包含以下 13 个方法契约，每个方法对应一项 AI 能力，具有独立的输入/输出 DTO 类型。所有方法统一返回 `CompletableFuture<AiResult<T>>`。本节在 Phase 0 **只冻结方法签名、DTO 类名、包路径和对外命名策略**，不冻结除 Mock 演示子集之外的字段级契约，避免越过路线图中“模块级接口契约在对应阶段启动前冻结”的边界。方法签名模式为：

**边界合规论证**：路线图 Phase 0.4 声明“模块级接口契约冻结（在对应阶段启动前冻结）”，其中的“模块级接口契约”指模块在对应阶段启动时需要锁定的完整接口规范（含字段级契约、校验规则等）。本节冻结的方法签名层面是完整接口契约的子集，仅锁定方法名、输入/输出 DTO 类名和包路径，字段级契约延后到各能力首次落地阶段。因此本节的冻结范围未违反 Phase 0.4 边界。标注为“可调整”的方法签名需在 Javadoc 中显式声明“该接口方法在 Phase N（首次落地阶段）启动前可调整，调整必须走接口变更评审流程”，任何此类调整必须由架构评审审批通过

```
CompletableFuture<AiResult<TriageResponse>> triage(TriageRequest request);
CompletableFuture<AiResult<DiagnosisResponse>> diagnosis(DiagnosisRequest request);
// 其余 11 个方法同理，输出 DTO 作为 AiResult 的类型参数
```

| 方法标识 | 对应 AI 能力 | 输入 DTO | 输出 DTO（AiResult 包装） |
|---------|------------|---------|-------------------------|
| `triage` | 智能分诊 | `TriageRequest` | `CompletableFuture<AiResult<TriageResponse>>` |
| `prescriptionCheck` | AI 处方审核 | `PrescriptionCheckRequest` | `CompletableFuture<AiResult<PrescriptionCheckResponse>>` |
| `generateMedicalRecord` | AI 病历生成 | `MedicalRecordGenRequest` | `CompletableFuture<AiResult<MedicalRecordGenResponse>>` |
| `diagnosis` | AI 智能诊断 | `DiagnosisRequest` | `CompletableFuture<AiResult<DiagnosisResponse>>` |
| `analysisReportForInspection` | AI 智能检查报告 | `InspectionReportRequest` | `CompletableFuture<AiResult<InspectionReportResponse>>` |
| `analysisReportForLabTest` | AI 智能检验报告 | `LabTestReportRequest` | `CompletableFuture<AiResult<LabTestReportResponse>>` |
| `imageAnalysis` | AI 影像分析 | `ImageAnalysisRequest` | `CompletableFuture<AiResult<ImageAnalysisResponse>>` |
| `knowledgeBaseQuery` | AI 知识库问答 | `KbQueryRequest` | `CompletableFuture<AiResult<KbQueryResponse>>` |
| `recommendExamination` | AI 开立检查/检验 | `ExaminationRecommendRequest` | `CompletableFuture<AiResult<ExaminationRecommendResponse>>` |
| `prescriptionAssist` | AI 辅助开方 | `PrescriptionAssistRequest` | `CompletableFuture<AiResult<PrescriptionAssistResponse>>` |
| `recommendExecutionOrder` | AI 执行顺序推荐 | `ExecutionOrderRequest` | `CompletableFuture<AiResult<ExecutionOrderResponse>>` |
| `schedule` | AI 医生排班 | `ScheduleRequest` | `CompletableFuture<AiResult<ScheduleResponse>>` |
| `discussionConclusion` | AI 综合讨论结论 | `DiscussionConclusionRequest` | `CompletableFuture<AiResult<DiscussionConclusionResponse>>` |

Phase 0 各 DTO 采用“两层冻结”策略（归属 `ai-api` 子模块的 `com.aimedical.modules.ai.api.dto` 包）：

1. **本阶段冻结**：DTO 类名、输入/输出归属关系、Java 包路径、对外 JSON 的 snake_case 命名策略。
2. **延后冻结**：除 Phase 0 Mock 演示子集外，其余字段结构、校验规则、枚举语义、流式协议细节，均在对应 AI 能力首次落地阶段的 OOD 中定义。

**DTO 占位约定**：
- 所有 13 组 request/response DTO 在 Phase 0 均保留类声明和默认构造器，满足 `AiService` 类型化方法签名的编译期约束
- 未进入 Mock 演示子集的 DTO 允许为空壳 class，不要求在 Phase 0 携带正式业务字段
- 若某能力在 Phase 0 需要支持前端占位展示，可仅保留不超过 3 个 `@phase0-mock-field` 字段；该字段子集只服务于 Mock 联调，不代表后续阶段的完整正式契约
- 以下字段名以 Java DTO 属性的 camelCase 形式定义；**对外 JSON 契约统一采用 snake_case**，由 Jackson `PropertyNamingStrategies.SNAKE_CASE`（或等价 `@JsonNaming` / `@JsonProperty` 配置）完成序列化映射

**Phase 0 Mock 字段子集示例**：

```text
TriageRequest
  - String chiefComplaint                  // @phase0-mock-field

TriageResponse
  - List<RecommendedDepartment> recommendedDepartments // @phase0-mock-field
  - String reason                          // @phase0-mock-field

RecommendedDepartment
  - String departmentName                  // @phase0-mock-field

其余 DTO（如 DiagnosisRequest、KbQueryRequest、ScheduleRequest、DiscussionConclusionRequest 及对应 response）
  - Phase 0 保留类声明和默认构造器
  - 字段级契约在各自首次落地阶段补齐
```

**统一约束补充**：
- 3.4.x AI 能力错误码统一采用 `<能力前缀>_AI_<错误类型>` 命名规则；Phase 0 的 Mock、后续真实实现及对外接口文档均遵循同一命名约定
- `packages/shared/types/` 中的 TypeScript 类型与 OpenAPI 导出字段，均以 snake_case 对外暴露，不直接复用 Java 字段名
- `@phase0-mock-field` 仅表示“当前阶段允许用于 Mock 展示的最小字段子集”，不表示后续阶段字段冻结完成。**语义区分**：该注解在输入 DTO 上表示“纳入 Phase 0 契约冻结”（如 `TriageRequest.chiefComplaint` 标注该注解表示该字段在 Phase 0 冻结，而非要求 MockAiService 填充输入字段）；在输出 DTO 上表示“Mock 数据填充”（MockAiService 为该字段生成占位数据）

Phase 0 的 Mock 实现为每个方法返回固定结构占位数据并包装为 `CompletableFuture`（遵循 3.4 节 Mock 数据占位约定），后续 Phase 替换为真实 AI 推理实现。字段级契约按 roadmap 在对应阶段逐步冻结。

### 8.3 API 文档工具集成

springdoc-openapi（Swagger 3）在 Phase 0 中定义为**推荐补齐项而非硬性验收项**。若团队在本阶段一并集成，各模块 Controller 添加 `@Tag` 和 `@Operation` 注解，开发期即可通过 `swagger-ui.html` 查阅 API 契约；若工期紧张，可顺延到 Phase 1 首期补齐，而不影响 Phase 0 骨架验收。

**选型说明**：技术栈文档（`Docs/02_tech.md`）指定 API 文档工具为 Knife4j（Swagger3，访问路径 `doc.html`）。本 OOD 采用 springdoc-openapi 而非 Knife4j，决策理由如下：(1) Knife4j v4+ 底层依赖 springdoc-openapi 作为 OpenAPI 规范生成引擎，springdoc-openapi 是正确且必要的底层选型；(2) springdoc-openapi 生成标准 OpenAPI v3 规范（`/v3/api-docs`），对 Phase 1+ openapi-generator 自动生成 TypeScript 类型无功能影响；(3) springdoc-openapi v2.5.0 与 Spring Boot 3.2.5 的兼容性已由社区验证通过，Knife4j 依赖 springdoc-openapi，兼容性风险仅在后续升级 Spring Boot 主版本时需重新评估（见父 POM 依赖声明注释）。若后续团队需要 Knife4j 的接口调试、文档聚合等增强功能，可在父 POM 中补充 `knife4j-openapi3-jakarta-spring-boot-starter` 依赖声明并切换到 `doc.html` 访问路径；若维持当前方案，前端通过 `swagger-ui.html` 作为唯一 API 文档入口

**依赖归属**：`springdoc-openapi-starter-webmvc-ui` 在父 POM 的 `<dependencyManagement>` 中统一声明版本（与 Spring Boot 3 BOM 对齐），各业务模块（patient、doctor、admin）按需在自身 POM 中引入该依赖（scope 默认 compile，无需重复声明版本号）。ai-api 和 common 模块无需引入（不包含 Controller）。

**前后端类型同步机制**：Phase 0 由前端团队人工维护 `packages/shared/types/` 中的 TypeScript 类型定义，与后端 DTO 同步纳入 Code Review 门禁。TypeScript 与 OpenAPI 对外字段命名统一采用 snake_case，并与 8.2 节的 Jackson 命名策略保持一致。Phase 1+ 引入 openapi-generator，通过 springdoc-openapi 生成的 OpenAPI 规范自动生成 TypeScript 类型定义，消除人工同步的类型漂移风险。springdoc-openapi 配置声明如下（Phase 0 预留配置占位）：

```yaml
# application-dev.yml（仅在 dev profile 下生效）
springdoc:
  api-docs:
    enabled: true
    path: /v3/api-docs
  swagger-ui:
    enabled: true
    path: /swagger-ui.html
```

> **生产环境安全说明**：`application-prod.yml` 中应显式设置 `springdoc.api-docs.enabled: false` 和 `springdoc.swagger-ui.enabled: false`，避免 Swagger UI 和 OpenAPI 文档在生产环境意外暴露。该处理方式与 9.1 节 H2 Console 的生产关闭策略一致。

### 8.4 跨模块调用规范

跨业务模块之间的方法调用不允许直接依赖对方模块的 Repository 或 Entity，须通过以下两种模式之一实现：

**模式一：接口门面（Facade Interface）**

每个提供跨模块服务的模块将其接口门面定义在独立的 API 子模块中（如 `common-module-api`），实现定义在独立的 impl 子模块中（如 `common-module-impl`）。其他模块的 POM **仅依赖 API 子模块（common-module-api）**，不依赖 impl 子模块，Maven 编译期即保障不会误引入实现层依赖。接口在 `api` 子包中定义，实现在 impl 模块的 `service` 子包中。

Phase 0 约束补充：Phase 0 不定义任何具体跨模块门面接口、方法签名、DTO 字段或权限编码格式，也不在业务模块占位 Service / Controller 中注入跨模块门面。`common-module-api` 在本阶段仅保留 `UserType` 等实体必需共享类型。权限相关门面接口、用户 DTO、权限编码格式和调用示例均在 Phase 1 权限 OOD 中定义。

**模式二：Spring ApplicationEvent 事件驱动**

适用于需要异步广播或一对多通知的场景（如用户注册后通知其他模块）。事件类定义在 common 模块的 `event` 子包中，发布方通过 `ApplicationEventPublisher.publishEvent()` 发布，订阅方通过 `@EventListener` 处理：

```
// 事件定义（common.event 包），普通 POJO 无需继承 ApplicationEvent（Spring 4.2+ 支持任意 POJO 作为事件对象）
public class UserRegisteredEvent {
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

**Phase 0 约束**：Phase 0 暂不实现任何跨模块调用（各业务模块仅有占位 Controller），上述规范为 Phase 1+ 预留。Phase 0 的 common-module-impl 仅提供权限实体定义和 Repository，不提供门面接口实现。

---

## 9. 本地开发体验

### 9.1 统一配置管理

- `application.yml`：存放通用配置及多环境共享配置，设置 `spring.profiles.active: phase0,dev`（Phase 0 同时激活骨架 profile 和开发环境 profile）
- `application-dev.yml`：开发环境配置（数据库连接、AI Mock 开关、日志级别）
- `application-prod.yml`：生产环境配置
- 前端通过 `.env.development` / `.env.production` 管理环境变量

`application.yml` 最小示例如下：

```yaml
spring:
  application:
    name: aimedical-sys
  profiles:
    active: phase0,dev

server:
  port: 8080
```

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

ai:
  mock:
    enabled: true
```

`application/pom.xml` 中 H2 依赖以 `runtime` scope 声明，仅 application 模块引入，确保 Phase 0 开发者无需额外安装数据库即可启动后端：

```xml
<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <scope>runtime</scope>
</dependency>
```

**Phase 1+ 切换策略**：注释 H2 配置块，改为 MySQL（或 PostgreSQL）数据源配置（url、driver-class-name、username、password），并将 h2 依赖 scope 从 `runtime` 调整为 `test`（保留用于单元测试的嵌入式数据库）。MySQL/PostgreSQL 驱动同样以 `runtime` scope 引入，保持 JDBC 驱动依赖策略一致。

**H2 Console 生产关闭策略**：H2 Console 仅在 `dev` profile 中启用。`prod` profile 的 `application-prod.yml` 应显式设置 `spring.h2.console.enabled: false`，避免开发期控制台在生产环境意外暴露。Phase 1+ 切换 MySQL 后将 h2 依赖 scope 从 `runtime` 调整为 `test`，此时 H2 Console 配置自动失效。

### 9.2 多模块构建依赖

- 父 POM 管理所有依赖版本（`<dependencyManagement>`）
- 公共模块（common、common-module-api）优先构建，common-module-impl 随业务层构建
- ai-api 子模块仅依赖 common，优先于所有业务模块构建；业务模块仅依赖 ai-api
- ai-impl 子模块依赖 ai-api，仅由 application 模块引入，业务模块完全不感知 ai-impl 的存在
- application 模块最后构建，聚合所有模块

**Spring Boot 包扫描配置**：`Application.java` 放置在 `com.aimedical` 根包下。为避免后续将启动类下沉到 `com.aimedical.application` 时遗漏扫描边界，application 模块的 `@SpringBootApplication` 仍显式指定 `scanBasePackages = "com.aimedical"` 以扫描所有子模块的 Spring Bean。同时配置 `@EntityScan("com.aimedical")` 发现 JPA 实体，`@EnableJpaRepositories("com.aimedical")` 发现 Repository 接口：

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

> **执行目录约定**：本节命令均假设已 `cd` 至对应源码根。后端命令须在 `AIMedical/backend/` 目录下执行；前端命令须在 `AIMedical/frontend/` 目录下执行。

- 后端（在 `AIMedical/backend/` 下执行）：`mvn spring-boot:run -pl application -am`（`-am` 即 `--also-make`，将 application 依赖的模块一并纳入当前 reactor 构建；执行 `spring-boot:run` 前会先完成这些模块的编译与类路径组装，但不会因此安装到本地仓库）。`spring.profiles.active=phase0,dev` 已在 `application.yml` 中设置（见 9.1 节），启动时无需额外指定 profile 参数；Phase 1 的认证策略切换方式由后续安全 OOD 定义，不在 Phase 0 文档中预设
- 前端（在 `AIMedical/frontend/` 下执行）：在各 `apps/*` 目录执行 `npm run dev`，Vite 开发服务器通过 npm workspaces 安装的依赖树解析内部包，并通过代理配置将请求转发到后端

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

> **执行目录约定**：第一至五阶段的 mvn 命令须在 `AIMedical/backend/` 目录下执行；第六阶段的前端构建命令须在 `AIMedical/frontend/` 目录下执行。

```
第一阶段（基础层）: checkout → mvn install -DskipTests -pl common,modules/common-module/common-module-api,modules/ai/ai-api
第二阶段（业务层）: mvn install -DskipTests -pl modules/common-module/common-module-impl,modules/patient,modules/doctor,modules/admin,modules/ai/ai-impl
第三阶段（聚合层）: mvn install -DskipTests -pl application
第四阶段（依赖分析）: mvn dependency:analyze（父 POM 中 maven-dependency-plugin 的 <ignoredUnusedDeclaredDependencies> 已配置 ai-api 和 common-module-api 豁免；仍会检出其他意外 unused / used undeclared 依赖及循环依赖。该目标仅需 class 文件，无需测试或打包）
第五阶段（集成测试）: mvn verify -pl integration（运行集成测试，含 Failsafe 插件；integration 模块仅含 *IT.java 测试类，Surefire 默认命名模式不拾取 *IT.java，由 Failsafe 拾取并执行，无需额外参数干预）
第六阶段（测试 + 前端）: mvn test → 构建前端（在 AIMedical/frontend/ 下执行 npm ci → npm run build:all）→ 归档制品。根 `package.json` 中定义 `build:all` 脚本（`"build:all": "npm run build --workspaces --if-present"`），仅含 `build` 脚本的 workspace（`apps/*`）执行构建，`packages/shared` 和 `packages/ui-core` 无 `build` 脚本时被 `--if-present` 静默跳过，CI 执行 `npm run build:all` 统一构建三端应用
```

- 第一阶段构建零依赖的基础模块，确保共享契约先就绪，产物安装到本地仓库
- 第二阶段构建依赖基础层的业务模块，可从本地仓库解析第一阶段产物
- 第三阶段构建依赖所有业务模块的启动聚合层，可从本地仓库解析前两阶段产物
- 第四阶段运行 `mvn dependency:analyze` 验证依赖健康状况。ai-api 和 common-module-api 在父 POM 的 `<ignoredUnusedDeclaredDependencies>` 中已显式豁免，不会误报；其他未声明或未使用的依赖将被检出，确保模块间依赖符合架构设计
- 第五阶段运行集成测试模块（integration），使用 Maven Failsafe 插件执行，依赖前四个阶段产物的完整后端可运行包
- 第六阶段运行单元测试并验证前端构建
- `mvn test` 运行已有单元测试（Phase 0 仅测试占位，每个模块至少一个占位测试类）
- 完整 CI 门禁（OpenAPI diff、覆盖率阈值等）归 Phase 1

### 10.1 最小流水线文件骨架

为避免本节停留在“阶段说明”层，Phase 0 额外冻结一个**最小可落地的 CI 文件骨架**。若仓库使用 GitHub Actions，推荐文件路径为 `.github/workflows/phase0-ci.yml`；若团队后续改用 Jenkins / GitLab CI，也应保持与本节六阶段顺序一致。

**推荐职责边界**：
- CI 文件只负责编排本节六个阶段的命令顺序，不在 Phase 0 引入缓存矩阵、并行分片、覆盖率上报、制品签名等增强能力
- 后端命令均在 `AIMedical/backend/` 目录执行，前端命令均在 `AIMedical/frontend/` 目录执行
- 若某平台不支持单 job 多工作目录切换，则拆为 backend / frontend 两个 job，但阶段顺序与依赖关系不变

**GitHub Actions 骨架示例**：

```yaml
name: phase0-ci

on:
  push:
    branches: [ main, develop ]
  pull_request:

jobs:
  build-and-verify:
    runs-on: ubuntu-latest
    steps:
      - name: Checkout
        uses: actions/checkout@v4

      - name: Setup JDK 17
        uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: '17'
          cache: maven

      - name: Setup Node 20
        uses: actions/setup-node@v4
        with:
          node-version: '20'
          cache: npm
          cache-dependency-path: AIMedical/frontend/package-lock.json

      - name: Phase 1 Foundation
        working-directory: AIMedical/backend
        run: mvn install -DskipTests -pl common,modules/common-module/common-module-api,modules/ai/ai-api

      - name: Phase 2 Business Modules
        working-directory: AIMedical/backend
        run: mvn install -DskipTests -pl modules/common-module/common-module-impl,modules/patient,modules/doctor,modules/admin,modules/ai/ai-impl

      - name: Phase 3 Application
        working-directory: AIMedical/backend
        run: mvn install -DskipTests -pl application

      - name: Phase 4 Dependency Analyze
        working-directory: AIMedical/backend
        run: mvn dependency:analyze

      - name: Phase 5 Integration Tests
        working-directory: AIMedical/backend
        run: mvn verify -pl integration

      - name: Phase 6 Unit Tests
        working-directory: AIMedical/backend
        run: mvn test

      - name: Install Frontend Dependencies
        working-directory: AIMedical/frontend
        run: npm ci

      - name: Build Frontend Workspaces
        working-directory: AIMedical/frontend
        run: npm run build:all
```

**落地约束**：
- 若 `.github/workflows/phase0-ci.yml` 与本节命令不一致，以本节为准进行回收统一
- Phase 0 不要求将后端可执行 JAR、前端静态资源或测试报告上传为 CI artifact；如平台默认支持可选归档，可在不改变主流程的前提下补充
- Phase 1 起若引入并行化构建，必须先验证 `mvn install -DskipTests` 的本地仓库安装顺序仍满足跨模块依赖解析

### 10.2 可观测性骨架

> **⚠️ 可选 (Phase 0 跳过)**：本节全部内容为推荐补齐项，**不影响 Phase 0 骨架验收**。Phase 0 不强制实现 `MeterRegistryCustomizer`、不强制补充 `micrometer-observation` 依赖、不强制配置 Actuator 暴露端点；所有审查工具应以本标记为准，跳过对以下条目的缺失检测。代码中保留相应扩展点即可（`common.config` 包下未来按需新增配置类）。

Phase 0 预留基础监控埋点接入骨架，确保后续阶段可通过 `/actuator` 端点查看系统指标。

**Micrometer + Spring Boot Actuator 配置骨架**：

- `[可选]` `MeterRegistry` 预留声明：在 common 模块的 `com.aimedical.common.config` 包中声明 `MeterRegistryCustomizer` 配置类占位，确保 Micrometer 核心依赖已在父 POM 中声明。Phase 0 的 customizer 仅设置通用标签（如 `application=aimedical-sys`），不注册自定义指标
- `[可选]` 父 POM `<dependencyManagement>` 中补充 `micrometer-observation` 依赖声明（版本由 Spring Boot BOM 管理，无需显式指定 version）；`spring-boot-starter-actuator` 在 application 模块以 compile scope 引入
- `[可选]` `application-dev.yml` 中配置 Actuator 暴露端点：

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
  endpoint:
    health:
      show-details: when-authorized
```

- `[可选]` 指标命名约定：自定义指标统一采用 `aimedical.<module>.<metric>` 前缀（如 `aimedical.ai.request.duration`、`aimedical.patient.request.count`），标签规范为 `{module, capability, status}`
- `[可选]` Phase 0 验收时可通过 `curl http://localhost:8080/actuator/health` 和 `curl http://localhost:8080/actuator/metrics` 验证骨架可用

**生产环境安全说明**：`[可选]` `application-prod.yml` 应显式缩小 Actuator 暴露范围（`management.endpoints.web.exposure.include=health,info`），避免 metrics 端点在生产环境意外暴露内部指标数据。

**Application 模块 Spring Boot Maven Plugin 配置**：application 模块的 `spring-boot-maven-plugin` 默认将普通 JAR 替换为可执行 fat JAR（BOOT-INF/lib 布局），该 fat JAR 无法被 Maven 依赖解析器正常解析 transitive 依赖。integration 模块以 `test` scope 依赖 application 后，`@SpringBootTest` 将因缺少关键依赖而启动失败。解决方案：在 `application/pom.xml` 的 `spring-boot-maven-plugin` 配置中添加 `<classifier>exec</classifier>`，使 Maven 同时生成两个 JAR 产物——普通 JAR（artifact-id-version.jar，作为 Maven 依赖被其他模块引用）和可执行 JAR（artifact-id-version-exec.jar，用于 `java -jar` 直接启动）。integration 模块依赖普通 JAR，可正常解析 transitive 依赖。配置如下：

```xml
<plugin>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-maven-plugin</artifactId>
    <configuration>
        <classifier>exec</classifier>
    </configuration>
</plugin>
```

**Integration 模块职责**：归属 `integration/` 目录，Phase 0 包含一个占位集成测试类（验证 Spring 上下文可正常加载和 `/api/ping` 端点的基本可达性），配置 Maven Failsafe Plugin 以 `mvn verify` 生命周期执行。该模块依赖 application 模块的启动包，作为完整后端的冒烟测试入口。

**`integration/pom.xml` 核心配置骨架**：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <parent>
        <groupId>com.aimedical</groupId>
        <artifactId>aimedical-sys</artifactId>
        <version>0.0.1-SNAPSHOT</version>
        <relativePath>../pom.xml</relativePath>
    </parent>
    <artifactId>integration</artifactId>
    <dependencies>
        <dependency>
            <groupId>com.aimedical</groupId>
            <artifactId>application</artifactId>
            <version>${project.version}</version>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>
    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-failsafe-plugin</artifactId>
                <configuration>
                    <skipTests>false</skipTests>
                    <includes>
                        <include>**/*IT.java</include>
                    </includes>
                </configuration>
                <executions>
                    <execution>
                        <goals>
                            <goal>integration-test</goal>
                            <goal>verify</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
</project>
```

---

## 11. 关联交付物清单

本节列出 Phase 0 需并行产出的非 OOD 设计文档类交付物，OOD 正文不包含这些交付物的具体内容，仅规划其创建责任与完成节点。

### 11.1 协作规范

| 交付物 | 文件路径 | 性质归属 | 责任人 | 完成节点 | 优先级 |
|-------|---------|---------|--------|---------|--------|
| 分支约定 | `CONTRIBUTING.md` 或 `Docs/branch-convention.md` | 项目级交付物 | 项目负责人 | Phase 0 骨架验收前 | 中 |
| Commit 格式 | `CONTRIBUTING.md` 或 `Docs/commit-convention.md` | 项目级交付物 | 项目负责人 | Phase 0 骨架验收前 | 中 |
| PR 模板 | `.github/pull_request_template.md` | 项目级交付物 | 项目负责人 | Phase 0 骨架验收前 | 中 |
| Code Review 必查项 | `CONTRIBUTING.md` 或 `Docs/cr-checklist.md` | 项目级交付物 | 项目负责人 | Phase 0 骨架验收前 | 中 |

> **边界说明**：协作规范属于项目级交付物，不属于 OOD 架构设计文档的固有职责范围。OOD 的核心产出是模块划分、接口契约、依赖方向、关键抽象等设计决策。

### 11.2 新人入门引导文档

| 交付物 | 文件路径 | 性质归属 | 责任人 | 完成节点 | 优先级 |
|-------|---------|---------|--------|---------|--------|
| 新人入门引导文档 | `Docs/QUICKSTART.md` | 项目级交付物 | 技术负责人 | Phase 0 骨架验收前 | 高 |

内容要求：前置条件检查（JDK 17+、Node.js 18+、npm 9+）、完整命令序列（git clone → mvn install -DskipTests → npm ci → npm run dev）、验证步骤（`curl http://localhost:8080/api/ping`）、常见问题排查。技术素材来源于 OOD §9（本地开发体验）。

> **验收关联**：此文档为 Roadmap Phase 0 验收标准"新人按入门引导文档可在 1 小时内完成本地环境搭建"的必要条件，属于硬性验收要求。

### 11.3 路线图"推荐补齐"项覆盖状态

| 路线图推荐补齐项 | OOD 覆盖状态 | 性质归属 | 建议补齐阶段 | 优先级 |
|----------------|-------------|---------|-------------|--------|
| 日志聚合框架占位（日志输出格式规范与采集配置骨架） | 未覆盖 | 开发规范/项目文档范畴 | Phase 1 多模块并行开发前 | 中 |
| 基础监控埋点接入（关键系统指标的基础埋点预留） | 已覆盖（§10.2 可观测性骨架） | OOD 应覆盖 | — | — |
| 容器化开发部署脚本（Docker Compose / Dockerfile） | 未覆盖 | 独立文档范畴（DEPLOY.md） | Phase 1 | 中 |
| 本地代码质量检查工具集成（Checkstyle / ESLint / Prettier） | 未覆盖 | 独立文档范畴（工具配置文件） | Phase 0 骨架验收前 | 高 |
| 硬件接入接口占位 | 未覆盖 | 独立文档范畴 | Phase 4（Phase 0 可声明"待 Phase 4 定义"） | 低 |
| API 文档自动生成 | 已覆盖（§8.3 springdoc-openapi） | — | — | — |
| AI 能力模块 Mock 占位 | 已覆盖（§3.4，降级框架按方案 B 精简：保留骨架、移除 TimeoutDegradationStrategy、精简 route 表） | — | — | — |

### 11.4 Phase 0 交付完成判定

为避免 11.1~11.3 仅停留在“列清单”，本节补充**完成判定口径**。Phase 0 在项目管理层面视为“交付完成”，至少需同时满足以下条件：

| 判定项 | 最低要求 | 来源章节 |
|-------|---------|---------|
| OOD 设计文档 | `Docs/04_ood_phase0.md` 完整评审通过 | 本文档全文 |
| 新人引导文档 | `Docs/QUICKSTART.md` 已创建，且命令序列与 §9 一致 | §11.2 |
| CI 骨架 | 已存在可执行的 CI 文件，命令顺序与 §10 一致 | §10.1 |
| 协作规范 | 分支、Commit、PR 模板、CR 必查项至少落地其一体化承载文件 | §11.1 |
| 本地启动验证 | 后端可响应 `/api/ping`，三端可启动占位首页 | §4.1, §9.3 |

**承载方式约束**：
- 11.1 中的协作规范允许集中收敛到一个 `CONTRIBUTING.md`，不强制拆分多个文件
- 11.2 中的 `Docs/QUICKSTART.md` 为硬性单文件交付物，不建议仅以内嵌 README 片段替代
- 10.1 中的 CI 骨架允许按平台差异改写语法，但阶段命令、工作目录和构建顺序不得偏离

**缺失处理规则**：
- 若 OOD 已完成，但 `QUICKSTART.md`、CI 文件或协作规范文件仍缺失，则 Phase 0 设计完成但**项目交付未完成**
- 若已存在对应文件，但内容与本文档冲突，应优先回收为与本文档一致，再进入骨架验收
