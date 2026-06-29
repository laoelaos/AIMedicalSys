# R6: 集成验证与整体架构一致性

审查时间：2026-06-26

### 审查范围

核心设计文件：
- `Docs/05_ood_phase1_B.md` — 设计文档（查阅用）

全局配置：
- `AIMedical/backend/application/src/main/resources/application.yml`
- `AIMedical/backend/common/src/main/java/com/aimedical/common/exception/GlobalErrorCode.java`
- `AIMedical/backend/common/src/main/java/com/aimedical/common/config/GlobalExceptionHandler.java`
- `AIMedical/backend/common/src/main/java/com/aimedical/common/result/Result.java`
- `AIMedical/backend/common/src/main/java/com/aimedical/common/exception/BusinessException.java`

旧文件清理确认：
- `application/.../config/JwtAuthenticationFilter.java` — 已删除
- `application/.../config/SecurityConfigPhase1.java` — 已删除
- `commonmodule/permission/Function.java` — 已删除（重命名为 PermissionFunction）
- `commonmodule/permission/FunctionRepository.java` — 已删除（重命名为 PermissionFunctionRepository）
- `commonmodule/dto/response/UserInfoResponse.java` — 已删除（迁移至 common-module-api）
- `application/.../config/SecurityConfigPhase0.java` — 仍存在

跨模块引用检查：
- `modules/patient`, `modules/doctor`, `modules/admin` — import 和 pom.xml 依赖检查
- `application/src/main/java/.../Application.java` — @ComponentScan 检查

集成测试：
- `AIMedical/backend/integration/src/test/java/.../EntityMappingIT.java`

Auth 服务调用链：
- `AuthServiceImpl.java` — 对 LoginAttemptTracker/RateLimitGuard/TokenBlacklist/JwtTokenProvider 的调用

Bean 装配检查：
- `AuthModuleConfig.java` — 基础设施 Bean 定义
- `SecurityConfigPhase1.java` — Security Filter Bean 定义

### 发现

#### [严重] ACCOUNT_LOCKED 消息模板未解析，客户端收到模板原文

- **位置**: `GlobalErrorCode.java:13`、`GlobalExceptionHandler.java:27`、`Result.java:38`
- **描述**: `GlobalErrorCode.ACCOUNT_LOCKED` 的消息定义为模板字符串 `"账户已锁定，请{锁定时间}后重试"`。`AuthServiceImpl.java:95,99` 调用 `new BusinessException(GlobalErrorCode.ACCOUNT_LOCKED, "请30分钟后重试")` 传入动态参数。但 `GlobalExceptionHandler.handleBusinessException()` 调用 `Result.fail(errorCode)` 仅取 `errorCode.getMessage()`（模板原文），忽略 `BusinessException` 中存储的 `args`。`Result.fail(ErrorCode)` 内部为 `fail(errorCode.getCode(), errorCode.getMessage())`，args 完全未参与消息合成。最终客户端收到的消息为模板原文 `"账户已锁定，请{锁定时间}后重试"`，而非预期的 `"账户已锁定，请30分钟后重试"`。
- **建议**: 方案 A：修改 `GlobalExceptionHandler` 中 `handleBusinessException` 方法检测 `e.getArgs()` 非空时格式化消息（如 `MessageFormat.format(errorCode.getMessage(), e.getArgs())`）；方案 B：简化处理——移除 `ACCOUNT_LOCKED` 消息中的模板占位符，在 `AuthServiceImpl` 中直接传入完整消息 `new BusinessException(GlobalErrorCode.ACCOUNT_LOCKED, "账户已锁定，请30分钟后重试")`。

#### [严重] UNAUTHORIZED 消息与设计规范不一致

- **位置**: `GlobalErrorCode.java:9`
- **描述**: OOD 文档 10.2 节明确规定 UNAUTHORIZED 的错误消息为 `"未认证或令牌已失效"`（3.3 节 AuthenticationEntryPoint 行为契约亦有相同描述）。代码中该枚举定义为 `UNAUTHORIZED("UNAUTHORIZED", "未认证")`，缺少 `"或令牌已失效"` 部分。与前端契约约定（6.3 节 JSON 示例）也不一致，前者返回的 message 字段不完全匹配。
- **建议**: 将 `UNAUTHORIZED` 消息改为 `"未认证或令牌已失效"`。

#### [一般] RATE_LIMITED / ACCOUNT_LOCKED HTTP 状态码映射缺失 429

- **位置**: `GlobalExceptionHandler.java:38-57`
- **描述**: `resolveHttpStatus` 方法仅映射了 UNAUTHORIZED(401)、FORBIDDEN(403)、NOT_FOUND(404)、PARAM_INVALID(400)、SYSTEM_ERROR(500) 五种错误码，其余全部走默认 `HttpStatus.BAD_REQUEST`（400）。OOD 10.1 节规定 `RATE_LIMITED` 和 `ACCOUNT_LOCKED` 应返回 HTTP 429。当前实现导致限流和锁定场景返回 400 而非 429，前端无法按 429 做差异化处理，API 网关也无法据此执行重试策略。
- **建议**: 在 `resolveHttpStatus` 中新增 `RATE_LIMITED`/`RATE_LIMITED_GLOBAL` 和 `ACCOUNT_LOCKED` 到 `HttpStatus.TOO_MANY_REQUESTS`（429）的映射。同时 `TOKEN_REFRESH_FAILED` 应映射到 `HttpStatus.UNAUTHORIZED`（401）。

#### [一般] AuthModuleConfig 与 SecurityConfigPhase1 存在重复的 TokenBlacklist Bean 定义

- **位置**: `AuthModuleConfig.java:20-22`、`SecurityConfigPhase1.java:42-44`
- **描述**: `AuthModuleConfig`（无 profile 限制）和 `SecurityConfigPhase1`（`@Profile("phase1")`）均定义了名为 `tokenBlacklist` 的 `TokenBlacklist` Bean，两者都返回 `new InMemoryTokenBlacklist()`。项目配置中未设置 `spring.main.allow-bean-definition-overriding=true`，Spring Boot 3.x 默认禁止 bean 覆盖。当 `phase1` profile 激活时，两个配置类同时生效，会导致 `BeanDefinitionOverrideException` 启动失败。
- **建议**: 移除其中一个重复定义。推荐在 `AuthModuleConfig` 中保留（集中管理基础设施 Bean），在 `SecurityConfigPhase1` 中删除 `tokenBlacklist()` 方法，SecurityConfig 仅注册 Filter 和 `PasswordEncoder`。

#### [一般] FORBIDDEN 消息与设计约定不一致

- **位置**: `GlobalErrorCode.java:10`
- **描述**: OOD 10.2 节规定 FORBIDDEN 的错误消息为 `"无权限访问"`，但代码中定义为 `FORBIDDEN("FORBIDDEN", "无权限")`。
- **建议**: 将 FORBIDDEN 消息改为 `"无权限访问"` 以对齐设计文档。

#### [轻微] SecurityConfigPhase0.java 仍保留在 application 模块

- **位置**: `application/src/main/java/com/aimedical/config/SecurityConfigPhase0.java`
- **描述**: `application.yml` 的 profiles 已正确移除 `phase0`（当前为 `phase1,dev`），但 `SecurityConfigPhase0.java` 文件本身未被删除。虽然 `@Profile("phase0 & !phase1")` 条件确保其在 phase1 激活时不加载，但作为已完成迁移的旧代码，保留该文件会造成混淆。
- **建议**: 删除 `SecurityConfigPhase0.java`。

### 通过验证项

| 检查项 | 状态 | 说明 |
|--------|------|------|
| 业务模块依赖方向 | ✅ | patient/doctor/admin 的 pom.xml 均依赖 `common-module-api` 而非 `common-module-impl` |
| 旧包 import 清理 | ✅ | modules 下级联搜索无 `com.aimedical.modules.system.entity` 遗留引用 |
| 旧 Filter 文件删除 | ✅ | `application/.../config/JwtAuthenticationFilter.java` 已删除 |
| 旧 SecurityConfig 删除 | ✅ | `application/.../config/SecurityConfigPhase1.java` 已删除 |
| Function 重命名 | ✅ | `Function.java` / `FunctionRepository.java` 已删除，`PermissionFunction.java` / `PermissionFunctionRepository.java` 已就位 |
| UserInfoResponse 迁移 | ✅ | 已从 `dto/response/` 删除，迁移至 `common-module-api/auth/` |
| application.yml profiles | ✅ | `active: phase1,dev`，已移除 `phase0` |
| @ComponentScan 配置 | ✅ | `@SpringBootApplication(scanBasePackages = "com.aimedical")` 覆盖 `com.aimedical.modules.commonmodule` |
| AuthServiceImpl 调用链 | ✅ | → `RateLimitGuard` (line 90) / `LoginAttemptTracker` (lines 94-125) / `JwtTokenProvider` (lines 129-132) / `TokenBlacklist` (line 160) — 调用完整 |
| EntityMappingIT — 新字段覆盖 | ✅ | `user_shouldMapPasswordChangeRequired` 和 `user_shouldMapTokenVersion` 已覆盖 |
| EntityMappingIT — 核心实体 | ✅ | User/Role/Post/PermissionFunction 全覆盖 |
| EntityMappingIT — 关系映射 | ✅ | ManyToMany User↔Role/Post, OneToMany Role↔Post, ManyToMany Post↔PermissionFunction 全覆盖 |
| CurrentUser / UserFacade 位置 | ✅ | 均位于 `common-module-api/src/.../auth/` 下 |
| 密码策略接口化 | ✅ | `PasswordPolicy` / `PasswordChangeService` 均为 interface + impl 分离 |

### 本轮统计

| 严重程度 | 数量 |
|---------|------|
| 严重 | 2 |
| 一般 | 3 |
| 轻微 | 1 |

### 总评

本次集成审查整体架构方向正确：模块依赖方向已按 OOD 设计对齐（业务模块只依赖 common-module-api，旧文件和重命名已清理，Application 的 @ComponentScan 正确覆盖 common-module-impl），AuthServiceImpl 对基础设施组件的调用链完整。集成测试 EntityMappingIT 对核心实体及新字段（passwordChangeRequired、tokenVersion）的 JPA 映射覆盖充分。

发现的主要问题集中于 **GlobalErrorCode/GlobalExceptionHandler 的异常链路一致性**：(1) ACCOUNT_LOCKED 消息模板未解析，客户端将收到模板原文；(2) UNAUTHORIZED 消息缺少"或令牌已失效"；(3) resolveHttpStatus 未映射 429；(4) AuthModuleConfig 与 SecurityConfigPhase1 的 TokenBlacklist Bean 定义重复可能导致启动失败。建议在进入 Phase 2 前优先修复严重和一般问题，避免运行时异常处理链路断裂。
