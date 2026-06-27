# R2-E: 审查菜单模块、应用层配置变更与设计的一致性

审查时间：2026-06-27 02:04

### 审查范围

**菜单模块：**
- `AIMedical/backend/modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/controller/MenuController.java`
- `AIMedical/backend/modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/service/MenuService.java`
- `AIMedical/backend/modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/service/impl/MenuServiceImpl.java`
- `AIMedical/backend/modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/dto/response/MenuResponse.java`
- `AIMedical/backend/modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/dto/request/MenuCreateRequest.java`
- `AIMedical/backend/modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/dto/request/MenuUpdateRequest.java`

**应用层配置：**
- `AIMedical/backend/common/src/main/java/com/aimedical/common/config/GlobalExceptionHandler.java`
- `AIMedical/backend/common/src/main/java/com/aimedical/common/exception/GlobalErrorCode.java`
- `AIMedical/backend/application/src/main/resources/application.yml`
- `AIMedical/backend/application/src/main/resources/logback-spring.xml`
- `AIMedical/backend/application/src/main/resources/db/schema.sql`
- `AIMedical/backend/application/src/main/resources/db/data.sql`
- `AIMedical/backend/pom.xml`
- `AIMedical/backend/application/pom.xml`
- `AIMedical/backend/modules/common-module/common-module-impl/pom.xml`
- `AIMedical/backend/modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/jwt/JwtConfig.java`
- `AIMedical/backend/modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/jwt/JwtUtil.java`
- `AIMedical/backend/modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/auth/jwt/JwtTokenProvider.java`
- `AIMedical/backend/modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/auth/security/JwtAuthenticationFilter.java`

### 发现

#### [一般] MenuController 直接操作 SecurityContextHolder 而非使用 CurrentUser 接口

- **位置**：`MenuController.java:152-161`
- **描述**：OOD §1.3 定义 `CurrentUser` 接口旨在"消除 Controller 层对 SecurityContextHolder 的直接操作"。但 `MenuController.getCurrentUserId()` 直接读取 `SecurityContextHolder.getContext().getAuthentication()` 获取当前用户 ID，未使用 `CurrentUser` 接口。这违反了 §2.2"业务模块通过 `CurrentUser` 接口获取当前登录用户标识"的设计约定。T7 修复（AuthController 移除对 JwtTokenProvider 的直接调用）同样应在 MenuController 落地。
- **建议**：注入 `CurrentUser` 接口替代 `getCurrentUserId()` 中的 SecurityContextHolder 直接操作，简化为 `currentUser.getUserId()`。

#### [轻微] JwtUtil.generateToken() 在测试中仍被使用，claims 结构与 §3.2 设计不一致

- **位置**：`JwtUtil.java:71-90`；测试调用处：`JwtUtilTest.java:42,52,53,61,75,106,137,159,180,201,302`
- **描述**：OOD §3.2 规定 Access Token claims 含 `userId`、`userType`、`jti`，不含 `role`/`position`。`JwtUtil.generateToken()` 包含 `role` 和 `position` 字段，缺少 `jti` 和 `userType`。虽然生产代码已使用 `JwtTokenProvider.generateAccessToken()`（符合设计），`JwtUtil.generateToken()` 仍在 `JwtUtilTest` 中被 11 处调用，导致测试产生的 token claims 与设计不一致，降低测试对 claims 结构的验证价值。
- **建议**：在 `JwtUtil` 中标记 `generateToken()` 为 `@Deprecated`，并将 `JwtUtilTest` 迁移为直接使用 `JwtTokenProvider`，彻底消除新旧两套 token 生成逻辑的混淆。

#### [轻微] JwtAuthenticationFilter 仍依赖 JwtUtil 静态方法

- **位置**：`JwtAuthenticationFilter.java:6,111`
- **描述**：OOD §1.3 定义 `JwtTokenProvider` 为"JWT 令牌生成、解析、验证的集中提供者"，但 `JwtAuthenticationFilter.extractToken()` 仍委托 `JwtUtil.extractToken()` 静态方法提取 Authorization header。虽然 `extractToken` 仅为 header 解析工具方法，但残留对 `JwtUtil` 的 import 引用与"集中提供者"设计原则不一致，违反了 T2 修复中"使用 `JwtTokenProvider` 替代 `JwtUtil`"的矫正方向。
- **建议**：将 `extractToken` 逻辑迁移至 `JwtTokenProvider` 作为公共方法，消除对 `JwtUtil` 的编译期依赖。

### 通过项确认

以下审查点经核实与 OOD 设计一致，无问题：

| 审查点 | 状态 | 说明 |
|--------|------|------|
| MenuController 正确适配 common-module 目录结构 | ✅ | 包路径 `com.aimedical.modules.commonmodule.controller`，`@RequestMapping("/api/menu")` |
| MenuService 接口无直接 SecurityContext 操作 | ✅ | 仅接受 `Long userId` 参数，无 SecurityContext 引用 |
| MenuServiceImpl Repository 引用已迁移至新路径 | ✅ | 使用 `com.aimedical.modules.commonmodule.permission.UserRepository` 和 `PermissionFunctionRepository` |
| MenuResponse 树形结构构建 | ✅ | `buildMenuTree()` 通过 `parentIdMap` 正确构建父子层级 |
| GlobalExceptionHandler 消息模板插值管线 | ✅ | `formatMessage()` 方法实现命名占位符替换，`ACCOUNT_LOCKED` 模板 `"账户已锁定，请{锁定时间}后重试"` 配合 args `"30分钟"/"15分钟"` 正确插值 |
| GlobalErrorCode 定义设计规定的错误码 | ✅ | `ACCOUNT_LOCKED`、`RATE_LIMITED`、`RATE_LIMITED_GLOBAL`、`PASSWORD_CHANGE_REQUIRED`、`CHILDREN_EXIST` 等均正确定义 |
| SecurityConfigPhase1 已从 application 层删除 | ✅ | `application/config/` 下仅剩 `SecurityConfigPhase0.java`（phase0 配置） |
| JwtAuthenticationFilter 已从 application 层删除 | ✅ | application 模块无 Filter 类残留 |
| application.yml profiles 已移除 phase0 | ✅ | `profiles.active: phase1,dev`，不含 phase0 |
| schema.sql `enabled` NOT NULL DEFAULT | ✅ | `enabled TINYINT(1) NOT NULL DEFAULT 1` |
| schema.sql `password_change_required` NOT NULL DEFAULT | ✅ | `password_change_required TINYINT(1) NOT NULL DEFAULT 0` |
| pom.xml common-module-impl 新增依赖 | ✅ | `common-module-impl/pom.xml` 含 `jjwt-api`、`jjwt-impl`、`jjwt-jackson`、`h2`、`spring-boot-starter-security` |
| pom.xml application 引入 common-module-impl | ✅ | `application/pom.xml` 声明 `common-module-impl` 依赖 |
| logback-spring.xml 审计日志独立存储 | ✅ | 独立 `SECURITY_AUDIT_FILE` appender，默认路径 `logs/audit/security-audit.log`，rolling policy 配置完整 |
| RATE_LIMITED/ACCOUNT_LOCKED HTTP 429 映射 | ✅ | `resolveHttpStatus()` 中 `RATE_LIMITED` 和 `ACCOUNT_LOCKED` 映射为 `HttpStatus.TOO_MANY_REQUESTS`（T25 修复） |
| FORBIDDEN 消息"无权限访问" | ✅ | `GlobalErrorCode.FORBIDDEN("FORBIDDEN", "无权限访问")`（T27 修复） |
| UNAUTHORIZED 消息"未认证或令牌已失效" | ✅ | `GlobalErrorCode.UNAUTHORIZED("UNAUTHORIZED", "未认证或令牌已失效")`（T4 修复） |
| MenuServiceImpl `@EntityGraph` 避免 N+1 | ✅ | `UserRepository.findWithDetailsForMenuById()` 标注 `@EntityGraph(attributePaths = {"roles", "posts", "posts.functions"})`（T23 修复） |
| MenuController update 路径 id 与 body id 一致性校验 | ✅ | `MenuController.java:121-123`（T21 修复） |
| MenuServiceImpl deleteMenu 使用 CHILDREN_EXIST | ✅ | `MenuServiceImpl.java:165` 抛出 `GlobalErrorCode.CHILDREN_EXIST`（T20 修复） |
| JwtTokenProvider generateAccessToken claims 结构正确 | ✅ | 含 `userId`、`userType`、`jti`，不含 `role`/`position`（符合 §3.2） |

### 本轮统计

| 严重程度 | 数量 |
|---------|------|
| 严重 | 0 |
| 一般 | 1 |
| 轻微 | 2 |

### 总评

菜单模块和应用层配置变更整体上与 OOD 设计高度一致，上一轮诊断报告中的 T4/T20/T21/T23/T25/T27 等缺陷已正确修复。`GlobalExceptionHandler` 正确实现了消息模板插值管线，`JwtTokenProvider` 的 claims 结构符合 §3.2 设计。

主要待改进点为 MenuController 仍直接操作 `SecurityContextHolder` 而非使用 `CurrentUser` 接口（OOD §1.3 明确要求），此项与 T7（AuthController 修复方向）同属 Controller 层对 SecurityContext 依赖的统一治理。此外 `JwtUtil.generateToken()` 在测试代码中残留旧 claims 结构，建议清理。
