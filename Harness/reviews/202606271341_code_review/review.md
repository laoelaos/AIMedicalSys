# 代码审查进度

## 概况
- **源分支**: 202606271029_fix_phase1B_issues
- **目标分支**: develop
- **审查依据**: Docs/05_ood_phase1_B.md
- **工作目录**: Harness/reviews/202606271341_code_review/

## R1-A: Package B 核心认证服务 — 严重 0 / 一般 7 / 轻微 2 — `review_v1_A.md`
> 核心认证服务实现整体良好，审计日志覆盖关键事件点。主要问题：AuthController 未使用 CurrentUser、expiresIn 单位错误(毫秒而非秒)、JwtConfig 使用标准 Base64 解码器而非 URL-safe、refreshTimestamps 缺少定时清理、ConcurrentHashMap.compute 内抛异常等。

## R1-B: Package B 安全过滤器与配置 — 严重 0 / 一般 1 / 轻微 3 — `review_v1_B.md`
> Filter 顺序、路径配置、行为契约均正确。主要问题：GlobalExceptionHandler.resolveHttpStatus() 缺少密码相关错误码到 HTTP 200 的映射。Filter 权限收集存在 N+1 潜在问题。

## R1-C: 实体/DTO/安全基础设施 — 严重 0 / 一般 2 / 轻微 1 — `review_v1_C.md`
> 实体变更、DTO record 转换、安全基础设施实现基本正确。主要问题：User.java 缺少 columnDefinition、SlidingWindowCounter.cleanup() TOCTOU 竞态、PermissionFunction.sortOrder 列名不匹配。

## R2-A: 测试 — 服务层与 Provider 层 — 严重 0 / 一般 4 / 轻微 4 — `review_v2_A.md`
> AuthServiceTest 质量高，15 个测试覆盖全部 7 条登录失败路径及 9 种刷新场景。主要问题：deleted 测试重复、JwtTokenProviderTest 缺篡改 Token 测试、UserFacadeImplTest 用 real converter 而非 mock、DebugJwtTest 位置不当。

## R2-B: 测试 — Controller/Filter/Config — 严重 0 / 一般 2 / 轻微 4 — `review_v2_B.md`
> 测试覆盖度较高，AuthControllerTest 覆盖全部 6 个端点。主要问题：JwtAuthenticationFilter 测试重复、SecurityConfigPhase1CoexistenceTest 需提升、MenuControllerTest 缺 /tree 端点、部分 DTO 测试缺 Bean Validation。

## R2-C: 测试 — 安全基础设施/实体/集成 — 严重 0 / 一般 4 / 轻微 4 — `review_v2_C.md`
> SlidingWindowCounterTest/LoginAttemptTrackerTest/InMemoryTokenBlacklistTest 覆盖非常全面。主要问题：PasswordPolicyImplTest 缺 null 和边界测试、实体测试(PermissionFunction/Post)缺字段验证。

### 决定
所有问题已记录至 `todo.md`，共产生 38 个问题项（0 严重 / 20 一般 / 18 轻微）。
