# 审查范围界定

## 审查依据

- **设计文档**: `Docs/05_ood_phase1_B.md` — Phase 1 包 A/B/D 统一修复与包 B OOD 设计方案
- **源分支**: `202606260857_ood_phase1_B`
- **目标分支**: `develop`
- **审查模式**: 暂存区全量变更（git diff --cached）

## 审查目标

本次审查的目的：验证代码实现与 OOD 设计文档的一致性，检查设计正确性、实现正确性、安全性、测试充分性。

## 范围划分

### 模块一: Auth 核心服务
- `AuthController.java` — 认证 REST 端点
- `AuthService.java` — 认证服务接口
- `AuthServiceImpl.java` — 认证服务实现
- `LoginRequest.java` — 登录请求 DTO
- `RefreshTokenRequest.java` — 刷新请求 DTO
- `PasswordChangeRequest.java` — 密码修改请求 DTO
- `ProfileUpdateRequest.java` — 资料更新请求 DTO
- `LoginResponse.java` — 登录响应 DTO
- `TokenRefreshResponse.java` — 刷新响应 DTO
- `UserInfoResponse.java` (api) — 用户信息响应 DTO
- `CurrentUser.java` — 当前用户访问器接口
- `UserFacade.java` — 用户数据门面接口
- `UserFacadeImpl.java` — 用户数据门面实现
- `UserConverter.java` — User → DTO 转换器

### 模块二: Security & JWT
- `JwtTokenProvider.java` — JWT 令牌管理
- `JwtConfig.java` — JWT 配置
- `JwtUtil.java` — JWT 工具类（旧）
- `JwtAuthenticationFilter.java` — JWT 鉴权过滤器
- `PasswordChangeCheckFilter.java` — 密码变更检查过滤器
- `GlobalRateLimitFilter.java` — 全局 IP 限流过滤器
- `CurrentUserImpl.java` — CurrentUser 实现
- `SecurityConfigPhase1.java` — Security 配置
- `RestAuthenticationEntryPoint.java` — 认证入口点
- `RestAccessDeniedHandler.java` — 拒绝访问处理器
- `PasswordChangeRequiredException.java` — 密码变更异常

### 模块三: 支持基础设施
- `RateLimitGuard.java` — 限流策略接口
- `InMemoryRateLimitGuard.java` — 内存限流实现
- `SlidingWindowCounter.java` — 滑动窗口计数器
- `LoginAttemptTracker.java` — 登录失败计数+锁定
- `TokenBlacklist.java` — Token 黑名单接口
- `InMemoryTokenBlacklist.java` — 内存黑名单实现
- `PasswordPolicy.java` — 密码策略接口
- `PasswordPolicyImpl.java` — 密码策略实现
- `PasswordChangeService.java` — 密码变更策略接口
- `PasswordChangeServiceImpl.java` — 密码变更策略实现
- `AuthModuleConfig.java` — Bean 装配配置

### 模块四: 实体、仓库、Schema
- `User.java` — 用户实体
- `Role.java` — 角色实体
- `Post.java` — 岗位实体
- `PermissionFunction.java` — 功能实体
- `UserRepository.java` — 用户仓库
- `RoleRepository.java` — 角色仓库
- `PostRepository.java` — 岗位仓库
- `PermissionFunctionRepository.java` — 功能仓库
- `schema.sql` — 数据库 Schema
- `data.sql` — 种子数据
- `Application.yml` — 应用配置

### 模块五: 菜单模块
- `MenuController.java` — 菜单端点
- `MenuService.java` — 菜单服务接口
- `MenuServiceImpl.java` — 菜单服务实现
- `MenuResponse.java` — 菜单响应 DTO
- `MenuCreateRequest.java` — 菜单创建请求 DTO
- `MenuUpdateRequest.java` — 菜单更新请求 DTO
- `MenuConverter.java` — 菜单转换器

### 模块六: 测试
- AuthControllerTest
- AuthServiceTest
- JwtTokenProviderTest
- JwtAuthenticationFilterTest
- PasswordChangeCheckFilterTest
- GlobalRateLimitFilterTest
- SecurityConfigPhase1Test
- RestAccessDeniedHandlerTest
- RestAuthenticationEntryPointTest
- CurrentUserImplTest
- LoginAttemptTrackerTest
- InMemoryRateLimitGuardTest
- SlidingWindowCounterTest
- InMemoryTokenBlacklistTest
- PasswordPolicyImplTest
- PasswordChangeServiceImplTest
- PasswordChangeRequiredExceptionTest
- UserFacadeImplTest
- UserConverterTest
- AuthModuleConfigTest
- UserTest / RoleTest / PostTest / PermissionFunctionTest
- UserRepositoryTest
- MenuControllerTest / MenuServiceTest
- DTO tests (LoginRequest, RefreshTokenRequest, etc.)
- JwtConfigTest / JwtUtilTest
- GlobalErrorCodeTest
- EntityMappingIT

### 旧代码清理
- 删除: `application/.../config/JwtAuthenticationFilter.java` (迁移至 common-module-impl)
- 删除: `application/.../config/SecurityConfigPhase1.java` (迁移至 common-module-impl)
- 删除: `Function.java` → 重命名为 `PermissionFunction.java`
- 删除: `FunctionRepository.java` → 重命名为 `PermissionFunctionRepository.java`
- 删除: `dto/response/UserInfoResponse.java` (迁移至 common-module-api)
- 删除: `FunctionTest.java` → 重命名为 `PermissionFunctionTest.java`

## 审查重点

1. **设计一致性**: 代码实现是否严格遵循 `05_ood_phase1_B.md` 中定义的契约和流程
2. **安全机制**: JWT 认证、Token 黑名单、限流、登录锁定、密码策略是否按设计实现
3. **边界处理**: 异常路径、输入校验、空值处理、并发安全
4. **依赖方向**: 业务模块对 common-module-api 的依赖是否符合设计
5. **测试充分性**: 关键路径是否有覆盖，mock 是否合理，边界条件是否测试

## 不包含范围

- 非 Java 文件（前端代码、部署脚本）
- `Harness/` 和 `.opencode/` 目录下的流程文件
- 本次变更未触及的其他模块
