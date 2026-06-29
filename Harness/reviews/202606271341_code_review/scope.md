# 代码审查范围

## 审查依据
Docs/05_ood_phase1_B.md — Phase 1 包 A/B/D 统一修复与包 B OOD 设计方案

## 审查目标
对当前分支（202606271029_fix_phase1B_issues）相对于 develop 分支的所有变更进行代码审查，验证实现是否遵循 OOD 设计文档。

## 审查重点

### Package B — 统一认证模块后端
- **核心认证服务**: AuthServiceImpl 实现（登录、登出、刷新、密码变更、用户信息获取）
- **JWT 令牌管理**: JwtTokenProvider（生成、解析、验证）、JwtConfig
- **用户门面**: UserFacadeImpl、UserConverter
- **安全审计**: SecurityAuditLogger、LoggingSecurityAuditLogger、SecurityAuditEvent

### Package B — 安全过滤器与配置
- **JwtAuthenticationFilter**: Token 提取、验证、黑名单检查、用户状态检查
- **PasswordChangeCheckFilter**: 密码变更检查阻断
- **GlobalRateLimitFilter**: 全局限流
- **SecurityConfigPhase1**: Spring Security 配置链
- **异常处理**: RestAuthenticationEntryPoint、RestAccessDeniedHandler
- **Audit & Exception**: GlobalExceptionHandler、GlobalErrorCode

### Package A — 实体与数据层
- **实体变更**: User、Role、Post、PermissionFunction（重命名、字段新增、约束补全）
- **DTO**: LoginRequest/Response、RefreshTokenRequest、PasswordChangeRequest、ProfileUpdateRequest、TokenRefreshResponse、UserInfoResponse、MenuRequest/Response DTOs
- **Repository**: UserRepository（findByUsername 返回 Optional）
- **Schema/Data**: schema.sql、data.sql 变更

### 安全基础设施
- **限流**: InMemoryRateLimitGuard、SlidingWindowCounter、RateLimitGuard
- **登录追踪**: LoginAttemptTracker
- **黑名单**: InMemoryTokenBlacklist、TokenBlacklist
- **密码策略**: PasswordPolicyImpl、PasswordChangeServiceImpl

### 测试代码
- AuthServiceTest、JwtTokenProviderTest、JwtConfigTest、JwtUtilTest
- AuthControllerTest、MenuControllerTest
- Filter 测试（JwtAuthenticationFilterTest、PasswordChangeCheckFilterTest、GlobalRateLimitFilterTest）
- SecurityConfig 测试（SecurityConfigPhase1Test、SecurityConfigPhase1CoexistenceTest）
- 限流测试（InMemoryRateLimitGuardTest、SlidingWindowCounterTest）
- 黑名单测试（InMemoryTokenBlacklistTest）
- 登录追踪测试（LoginAttemptTrackerTest）
- 密码策略测试（PasswordPolicyImplTest、PasswordChangeServiceImplTest）
- 审计测试（LoggingSecurityAuditLoggerTest）
- DTO 测试（各 Request/Response Test）
- 实体测试（UserTest、RoleTest、PostTest、PermissionFunctionTest、UserRepositoryTest）
- 集成测试（EntityMappingIT）
- 用户门面测试（UserFacadeImplTest、UserConverterTest）
- 异常测试

### 配置文件
- application.yml
- pom.xml
- .gitignore

## 排除范围
- Docs/ 目录下的文档变更（作为审查依据不纳入审查对象）
- Harness/ 目录下的历史工作产物
- 前端代码（Package D，已在之前审查中覆盖）
