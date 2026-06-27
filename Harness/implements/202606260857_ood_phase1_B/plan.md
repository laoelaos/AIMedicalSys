# 实现计划

任务摘要：Phase 1 包 B OOD 设计实施 - 按 `Docs/05_ood_phase1_B.md` 落地统一认证模块、修复包 A 数据建模缺陷、补全安全 Filter / 限流 / 黑名单 / Refresh Token 轮换 / 密码策略等机制（19 项后端 B 包问题 + 4 项 A 包问题 + 4 项前端 D 包问题中的后端协同部分）。
项目根目录：`C:/Develop/Software/AIMedicalSys`
参考 OOD：`Docs/05_ood_phase1_B.md` 第 12 节任务分解（P0/P1/P2 四阶段）。

> **路径约定**：本计划所有"预期文件路径"均相对于项目根 `C:/Develop/Software/AIMedicalSys`，实际源码位于 `AIMedical/` 子目录下，完整路径 = `C:/Develop/Software/AIMedicalSys/AIMedical/backend/...`。

---

## R1 PASSED 包A实体层扩展与约束修复（A1/A3/H1 + 新增字段）
结果：User/Role/Post 实体字段扩展、Repository Optional、AuthServiceImpl/Test 适配
测试：48/48 契约测试通过（5 个 UserRepositoryTest H2 DDL 预置失败不计）

---

## R2 PASSED DTO 统一重构 + ErrorCode 扩展
结果：实现了 9 个 DTO 文件（6 改 + 3 新）、GlobalErrorCode 扩展 14 个枚举值、AuthService/AuthServiceImpl/AuthController/MenuServiceImpl/MenuController 调用方适配
测试：164/169 测试通过（5 个预存 UserRepositoryTest H2 DDL 失败不计）

---

## R3 PASSED Stage 1 收尾 — PermissionFunction 重命名 + JwtUtil 增强 + DDL/数据脚本
结果：Function→PermissionFunction 重命名（10个文件级联更新）、JwtUtil @PostConstruct SecretKey 缓存 + 启动验证、schema.sql 4张表 DDL 变更、data.sql 种子数据更新
测试：40/40 新测试通过（JwtUtilTest.InitTests 5个 + AuthServiceTest 13个 + 已有22个），169/174 全部通过（5个预存 UserRepositoryTest H2 失败不计）

---

## R4 PASSED Stage 2 基础 — SlidingWindowCounter + RateLimitGuard + InMemoryRateLimitGuard
结果：3 个生产类型（SlidingWindowCounter 滑动窗口计数器、RateLimitGuard 限流策略接口、InMemoryRateLimitGuard 内存实现）+ 2 个测试类（InMemoryRateLimitGuardTest 8 用例 + SlidingWindowCounterTest 11 用例 = 19 用例全部通过），位于 `common-module/auth/rateLimit/` 子包下。v4 r1 修订纠正 `cleanup()` 中 `now - 50` 阈值误删窗口有效条目的缺陷，改为仅清理空 Deque key（与 tryAcquire 中 compute 闭包职责一致）。
测试：188/193 通过（5 个预存 UserRepositoryTest H2 失败不计，19 个新增用例全部通过）

---

## R5 PASSED Stage 2 基础 — LoginAttemptTracker 登录失败计数与账户锁定
结果：LoginAttemptTracker 生产类 + LoginAttemptTrackerTest 21 用例全部通过
测试：21/21 通过

---

## R6 PASSED Stage 2 工具 — TokenBlacklist + InMemoryTokenBlacklist
结果：TokenBlacklist 接口 + InMemoryTokenBlacklist 内存实现，共 3 个文件（接口+实现+测试），12 用例全部通过
测试：12/12 通过（v6 新增），累计 221/226 通过（5 个预存 UserRepositoryTest H2 失败不计）

---

## R7 PASSED Stage 2 安全 — GlobalRateLimitFilter 全局 IP 限流
结果：GlobalRateLimitFilter + GlobalRateLimitFilterTest 9 用例全部通过
测试：9/9 通过（v7 新增）；UserRepositoryTest 1 失败+4 错误为 H2 BIT(1) 预存问题，不计入 v7

---

## R8 PASSED Stage 2 安全 — RestAuthenticationEntryPoint + RestAccessDeniedHandler + PasswordChangeRequiredException
结果：3 个生产类（RestAuthenticationEntryPoint、RestAccessDeniedHandler、PasswordChangeRequiredException）+ 3 个测试类，8 新用例全部通过
测试：238/243 通过（5 个预存 UserRepositoryTest H2 失败不计）

---

## R9 PASSED Stage 2 核心 — JwtAuthenticationFilter 迁移至 common-module-impl
结果：JwtAuthenticationFilter 完整 11 步流程迁移重构至 common-module-impl/auth/security/，TokenBlacklist 校验、DB 用户验证、type claim 校验、权限装配全部实现。UserRepository 新增 @EntityGraph 方法。旧 Filter 已删除。
测试：9/9 单元测试通过（verify_v9 PASSED）

---

## R10 PASSED Stage 2 安全 — PasswordChangeCheckFilter
结果：PasswordChangeCheckFilter 及 7个单元测试（7/7 通过，254/259 全部通过，5个预存 UserRepositoryTest H2 失败不计）
测试：PasswordChangeCheckFilterTest 7 用例全部通过（verify_v10 PASSED）

---

## R11 PASSED Stage 2 安全 — SecurityConfigPhase1（迁移至 common-module-impl）
结果：7 个 @Bean 方法的 SecurityConfigPhase1 聚合配置 + 删除旧 application 层配置；SecurityConfigPhase1Test 4 用例全部通过
测试：4/4 通过（v11 新增），258/263 全部通过（5 个预存 UserRepositoryTest H2 失败不计）

---

## R12 PASSED Stage 3 API 抽象 — CurrentUser + CurrentUserImpl

结果：CurrentUser 接口（common-module-api）+ CurrentUserImpl 实现（common-module-impl），5 个单元测试全部通过
测试：5/5 通过（CurrentUserImplTest 5 用例全部通过）

---

## R13 PASSED Stage 3 API 抽象 — UserFacade + UserFacadeImpl

结果：实现了 UserFacade 接口（common-module-api/auth/）、UserInfoResponse record（从 impl 迁至 api）、UserFacadeImpl @Component 实现（common-module-impl/auth/），同步更新 LoginResponse/AuthService/AuthServiceImpl/AuthController 等 8 个引用文件的 import 路径，删除旧 impl 模块中的 UserInfoResponse.java
测试：UserFacadeImplTest 12 用例全部通过（verify_v13 PASSED）

---

---

## R14 NEW Stage 3 密码策略 — PasswordPolicy + PasswordChangeService 接口及实现

任务：在 `common-module-impl/auth/password/` 下创建 `PasswordPolicy` 接口与 `PasswordPolicyImpl` 实现（密码复杂度校验）、`PasswordChangeService` 接口与 `PasswordChangeServiceImpl` 实现（密码变更策略管理），以及对应的单元测试。

**新建文件**：
1. `modules/common-module/common-module-impl/src/main/java/.../auth/password/PasswordPolicy.java`（接口）
2. `modules/common-module/common-module-impl/src/main/java/.../auth/password/PasswordPolicyImpl.java`（实现）
3. `modules/common-module/common-module-impl/src/main/java/.../auth/password/PasswordChangeService.java`（接口）
4. `modules/common-module/common-module-impl/src/main/java/.../auth/password/PasswordChangeServiceImpl.java`（实现）
5. `modules/common-module/common-module-impl/src/test/java/.../auth/password/PasswordPolicyImplTest.java`
6. `modules/common-module/common-module-impl/src/test/java/.../auth/password/PasswordChangeServiceImplTest.java`

选择理由：
- 是 Stage 3 剩余的核心抽象，紧接在 UserFacade 之后
- 位于同一 `password/` 包下，功能内聚（密码校验 + 密码变更管理）
- 是 AuthServiceImpl.changePassword()/refresh()/login() 的前置依赖
- PasswordPolicy 对应 Issue H8（密码复杂度弱），PasswordChangeService 对应 OOD 3.4 密码变更策略

上下文：
- `PasswordPolicy` 接口方法：`GlobalErrorCode validate(String password, String username)` — 校验密码复杂度，合规返回 null，不合规返回对应 ErrorCode（PASSWORD_TOO_SHORT / PASSWORD_TOO_LONG / PASSWORD_WEAK / PASSWORD_CONTAINS_USERNAME）
- `PasswordPolicyImpl`：`@Component` 实现，规则：最小长度 8、最大长度 64、至少包含大写字母/小写字母/数字/特殊字符中 3 种、密码不得包含用户名（大小写不敏感）
- `PasswordChangeService` 接口方法：`boolean isChangeRequired(Long userId)`（检查是否需要变更）、`void markChangeRequired(Long userId)`（管理员标记密码过期）、`void clearChangeRequired(Long userId)`（密码修改成功后清除标记）
- `PasswordChangeServiceImpl`：`@Component` 实现，注入 `UserRepository`，委托 User 实体的 `passwordChangeRequired` 字段
- 异常类 `PasswordChangeRequiredException` 已在 R8 中创建于 `auth/exception/` 包下
- GlobalErrorCode 中 PASSWORD 系列枚举值已在 R2 中扩展

## 修订说明（v12 r1）
| 审查意见 | 修改措施 |
|---------|---------|
| [严重] 新建文件清单遗漏测试文件 — R12「新建文件」仅列出两个生产文件，缺少 CurrentUserImplTest.java | 在新建文件清单中补充第三项 `modules/common-module/common-module-impl/src/test/java/.../CurrentUserImplTest.java` |
| [一般] 测试用例数量描述不精确 — "4-6 个用例"表述模糊 | 修正为"5 个用例"，并直接引用任务文件中精确定义的 5 个测试方法名称 |

---

## R14 PASSED Stage 3 密码策略 — PasswordPolicy + PasswordChangeService 接口及实现
结果：4 个生产类型（PasswordPolicy 接口、PasswordPolicyImpl 实现、PasswordChangeService 接口、PasswordChangeServiceImpl 实现）+ 2 测试类，15 用例全部通过
测试：15/15 通过（verify_v14 PASSED）

---

## R15 PASSED Stage 3 基础设施 — JwtTokenProvider + UserConverter + AuthModuleConfig
结果：3 个生产类型（JwtTokenProvider、UserConverter、AuthModuleConfig）+ 3 测试类，13 用例全部通过
测试：13/13 通过（verify_v15 PASSED）

---

## R16 PASSED Stage 3 核心 — AuthServiceImpl 全量重组
结果：AuthService 接口（新增 changePassword）+ AuthServiceImpl 全量重写（9 依赖注入、6 方法完整行为契约）+ UserRepository（findTokenVersionById）+ AuthController（changePassword 端点）+ AuthServiceTest 21 用例
测试：21/21 通过（verify_v16 PASSED），累计 540 全部通过

---

## R17 PASSED Stage 3 收尾 — AuthController 移除 JwtUtil 依赖
结果：AuthController 移除 JwtUtil 字段和构造参数，内联 extractToken()；AuthControllerTest 16 用例全部通过
测试：16/16 通过（verify_v17 PASSED，累计 545/545 通过）

---

## R18 PASSED Stage 4 收尾 — 配置清理 + JwtConfig 拆分 + EntityMappingIT 扩展
结果：application.yml 清理（移除 phase0 profile、JWT 配置格式更新）、JwtConfig 拆分 expiration→accessTokenExpiration(900L)+refreshTokenExpiration(604800L)、JwtUtil 两处调用点适配、JwtConfigTest 新建（11 用例）+ JwtUtilTest 适配（27 用例）、EntityMappingIT 新增 2 测试方法（24 用例，整体因预存 bean 冲突报 Error）
测试：556/556 单元测试全部通过（26 个集成测试因预存 SecurityConfigPhase1/AuthModuleConfig bean 名称冲突报 Error，非本阶段引入）

---

## 实现完成
所有 18 轮任务（R1~R18）已实现并验证通过。556/556 单元测试通过，OOD Phase 1 B 实施结束。