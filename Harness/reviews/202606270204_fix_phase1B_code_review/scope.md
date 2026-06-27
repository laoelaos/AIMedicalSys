# 代码审查范围界定

## 审查目标

审查当前分支 `202606262033_fix_phase1B_report` 相对于 `develop` 分支的源码头变更，依据 `Docs/05_ood_phase1_B.md` 设计文档，验证代码实现是否与 OOD 设计一致。

## 审查依据

- `Docs/05_ood_phase1_B.md` — Phase 1 包 B OOD 设计文档（认证流程、安全设计、模块划分、契约定义）
- `Docs/Diagnosis/impl/04_phase1B_report.md` — 问题诊断报告（已发现的 19+4 项问题）

## 审查重点（按优先级）

1. **核心认证业务逻辑**：AuthServiceImpl 登录/登出/刷新/密码变更/获取当前用户的实现是否符合 3.1 节流程设计
2. **安全基础设施**：JwtTokenProvider、JwtAuthenticationFilter、PasswordChangeCheckFilter、GlobalRateLimitFilter 是否符合 3.3/3.4/4 节安全设计
3. **支持服务实现**：LoginAttemptTracker、TokenBlacklist、RateLimitGuard、PasswordPolicy、PasswordChangeService、SecurityAuditLogger 是否符合 4.1/4.2/4.3/4.8 节设计
4. **模块边界与依赖**：CurrentUser、UserFacade、UserConverter 是否符合 1.3/2.2 节定义的接口契约和门面模式
5. **测试覆盖**：单元测试是否充分覆盖核心逻辑和边界场景

## 变更范围（源代码，排除 Harness/ Docs/）

### 新增文件（~50 个）
- `common-module-api/auth/CurrentUser.java`、`UserFacade.java`
- `common-module-impl/auth/` 下 controller、service、jwt、security、rateLimit、login、audit、blacklist、password、converter、config 等子包
- `common-module-impl/permission/PermissionFunctionRepository.java`
- `common-module-impl/dto/request/` 下新增 DTO
- `common-module-impl/dto/response/` 下新增 DTO
- 对应的测试文件

### 修改文件（~30 个）
- `AuthServiceImpl.java`、`AuthService.java` — 认证服务核心逻辑重构
- `AuthController.java` — 认证端点调整
- `MenuServiceImpl.java`、`MenuController.java` — 菜单服务适配
- `User.java`、`Role.java`、`Post.java` 等实体 — 字段补充
- `JwtConfig.java`、`JwtUtil.java` — JWT 配置迁移
- `GlobalExceptionHandler.java`、`GlobalErrorCode.java` — 异常处理适配
- 现有 DTO 重构（LoginRequest、LoginResponse、MenuRequest/Response 等）
- 现有测试适配

### 删除文件（~6 个）
- 旧 `application/.../JwtAuthenticationFilter.java`
- 旧 `application/.../SecurityConfigPhase1.java`
- 旧 `UserInfoResponse.java`（impl 层 → api 层迁移）
- 旧 `Function.java`（→ PermissionFunction.java）
- 旧 `FunctionRepository.java`（→ PermissionFunctionRepository.java）
- 旧 `FunctionTest.java`（→ PermissionFunctionTest.java）

## 排除范围

- Harness/ 目录（审议流程记录）
- Docs/ 目录（设计/诊断文档本身）
- 第三方依赖变更（pom.xml 仅限 common-module-impl 依赖调整）

## 背景

本次变更是基于前序 OOD 设计审议（202606260857_ood_phase1_B）和问题诊断审议（202606261837_ood_phase1_B_diagnosis）后产生的修复实现。变更实现了 Phase 1 包 B 的统一认证模块，包括 JWT 认证、速率限制、登录防护、密码策略、审计日志等完整安全基础设施。
