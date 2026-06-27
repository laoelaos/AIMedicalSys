# R1: 审查支持服务、实体映射、门面接口实现与 OOD 设计文档一致性

审查时间：2026-06-27

### 审查范围

1. `common-module-impl/.../auth/login/LoginAttemptTracker.java`
2. `common-module-impl/.../auth/blacklist/InMemoryTokenBlacklist.java`
3. `common-module-impl/.../auth/blacklist/TokenBlacklist.java`
4. `common-module-impl/.../auth/password/PasswordPolicy.java`
5. `common-module-impl/.../auth/password/PasswordPolicyImpl.java`
6. `common-module-impl/.../auth/password/PasswordChangeService.java`
7. `common-module-impl/.../auth/password/PasswordChangeServiceImpl.java`
8. `common-module-impl/.../auth/audit/SecurityAuditLogger.java`
9. `common-module-impl/.../auth/audit/SecurityAuditEvent.java`
10. `common-module-impl/.../auth/audit/SecurityAuditEventType.java`
11. `common-module-impl/.../auth/audit/LoggingSecurityAuditLogger.java`
12. `common-module-api/.../auth/CurrentUser.java`
13. `common-module-api/.../auth/UserFacade.java`
14. `common-module-impl/.../auth/UserFacadeImpl.java`
15. `common-module-impl/.../auth/converter/UserConverter.java`
16. `common-module-impl/.../permission/User.java`
17. `common-module-impl/.../permission/Role.java`
18. `common-module-impl/.../permission/PermissionFunction.java`
19. `common-module-impl/.../permission/Post.java`
20. `common-module-impl/.../permission/UserRepository.java`
21. `common-module-impl/.../permission/PermissionFunctionRepository.java`
22. `common-module-impl/.../jwt/JwtConfig.java`
23. `common-module-impl/.../jwt/JwtUtil.java`

### 发现

#### [一般] JwtUtil.generateToken 包含 role/position claims 且缺少 jti，违反 3.2 节设计约束

- **位置**: `jwt/JwtUtil.java:71-89`
- **描述**: 设计文档 3.2 节明确规定 "Access Token 中不包含 role/position/authorities claims，避免角色变更后 claims 与 DB 不一致"，且 Access Token claims 必须包含 `jti`（JWT ID）标准 claim 用于黑名单索引。当前 `JwtUtil.generateToken()` 方法将 `role` 和 `position` 放入 token claims，且未生成 `jti`。若此方法仍被使用（未被 JwtTokenProvider 完全取代），将导致角色变更后旧 claims 与 DB 不一致，且黑名单功能无法通过 jti 索引。
- **建议**: (a) 确认 `JwtUtil.generateToken()` 是否已被 `JwtTokenProvider` 替代——若已替代则标注 `@Deprecated` 并确保所有调用方迁移完毕；(b) 若仍在使用，移除 role/position claims，添加 `UUID.randomUUID().toString()` 生成的 jti，按设计添加 userType 等 claims。

#### [一般] LoginAttemptTracker record 方法缺少窗口过期防御

- **位置**: `auth/login/LoginAttemptTracker.java:32-49`
- **描述**: `recordUsernameFailure()` 和 `recordIpFailure()` 在递增失败计数时始终保留 `prev.firstFailureTime()`，未检查当前窗口是否已过期。当前调用顺序依赖 `isUsernameLocked()` / `isIpLocked()` 先于 record 调用（设计 3.1.1 登录流程步骤 2 先于步骤 5/6/7），此顺序保证了惰性清除有效。但若其他代码路径（如单元测试或未来新增调用方）直接调用 record 方法而不先调用 isLocked，则计数器可能跨窗口累积不重置，导致永久锁定风险。
- **建议**: 在 `record*` 方法的 `compute` 闭包中增加窗口过期判断：若 `now - prev.firstFailureTime() >= lockDuration` 则重置为 `new AttemptRecord(1, now)` 而非继续累加。

#### [轻微] User.passwordChangeRequired 缺少 columnDefinition 注解

- **位置**: `permission/User.java:51-52`
- **描述**: 设计文档 5.1 节 NOT NULL 约束状态表定义 `passwordChangeRequired` 的注解为 `@Column(nullable=false, columnDefinition="BIT(1) DEFAULT 0")`，当前代码仅有 `@Column(nullable = false)`，缺少 `columnDefinition` 属性。虽然项目通过 schema.sql 管理 DDL 而非依赖 JPA 自动建表，但注解与设计不一致可能影响未来基于 JPA 的测试（如 H2 测试环境）。
- **建议**: 补加 `columnDefinition="BIT(1) DEFAULT 0"` 保持与设计一致。

#### [轻微] JwtConfig @PostConstruct 验证未覆盖字符集合法性

- **位置**: `jwt/JwtConfig.java:49-67`
- **描述**: 设计文档 4.7 节要求启动验证三步：(1) null/empty、(2) Base64 解码后字节长度 >= 32、(3) Base64 URL-safe 字符集合法性。当前 `JwtConfig.validate()` 仅执行了 (1) 和 (2)，未显式检查字符集合法性。`JwtUtil.init()` 中有字符集检查但使用标准 Base64 字符集（`+`/`/`）而非设计规定的 URL-safe 字符集（`-`/`_`）。
- **建议**: 在 `JwtConfig.validate()` 中增加字符集检查，并与设计规定的 URL-safe 字符集对齐，或与 `JwtUtil.init()` 中的实际校验模式对齐。

### 本轮统计

| 严重程度 | 数量 |
|---------|------|
| 严重 | 0 |
| 一般 | 2 |
| 轻微 | 2 |

### 总评

本轮审查的 23 个文件中，大部分支持服务、实体映射和门面接口实现与 `Docs/05_ood_phase1_B.md` 设计文档高度一致：

- **支持服务**（LoginAttemptTracker、TokenBlacklist、PasswordPolicy、PasswordChangeService、SecurityAuditLogger）：实现准确反映设计，包括双维度锁定、ConcurrentHashMap+ScheduledExecutor 黑名单、密码复杂度规则、密码变更策略、审计日志接口与事件定义。`LoggingSecurityAuditLogger` 的异常处理（catch Exception 不抛出）严格遵循 4.8 节失败容忍要求。`PasswordPolicyImpl` 实现了所有 4 条复杂度规则（3/4 字符种类、8-64 长度、不含用户名、Phase 2 Top10000 占位未启用为预期行为）。
- **实体映射**（User/Role/Post/PermissionFunction、UserConverter）：实体字段（passwordChangeRequired、tokenVersion、enabled NOT NULL、Role.sort、PermissionFunction.component）均已对齐设计；`UserConverter. resolveRole()` 正确实现了 enabled 过滤、sort nullsLast 排序和 code 提取；`UserConverter.resolvePermissions()` 正确过滤 enabled Function。
- **门面接口**（CurrentUser、UserFacade、UserFacadeImpl）：接口定义与设计完全一致；`UserFacadeImpl` 正确委托 `UserConverter.toUserInfoResponse()`，未独立维护转换逻辑（符合 1.3 节唯一转换来源原则）。
- **Repository**（UserRepository、PermissionFunctionRepository）：`findByUsername` 返回 `Optional<User>`（H1 修复）；`@EntityGraph` 用于 N+1 规避（M9 修复）；`findTokenVersionById` 单独查询（3.1.3 节步骤 9 要求）。

主要问题集中在 `JwtUtil.generateToken()` 的 claims 结构与设计不一致（含 role/position 且缺 jti），以及 `LoginAttemptTracker.record*` 方法缺少窗口过期防御。建议优先确认 `JwtUtil` 是否已被 `JwtTokenProvider` 替代，若仍在活跃使用需按设计修 claims 结构。
