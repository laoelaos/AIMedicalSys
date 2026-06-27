# R1-C: Package A 实体变更、DTO 定义和安全基础设施审查

审查时间：2026-06-27

### 审查范围

**实体层 (Package A):**
- `common-module-impl/.../permission/User.java`
- `common-module-impl/.../permission/Role.java`
- `common-module-impl/.../permission/Post.java`
- `common-module-impl/.../permission/PermissionFunction.java`
- `common-module-impl/.../permission/UserRepository.java`

**DTO:**
- `common-module-impl/.../dto/request/LoginRequest.java`
- `common-module-impl/.../dto/request/RefreshTokenRequest.java`
- `common-module-impl/.../dto/request/PasswordChangeRequest.java`
- `common-module-impl/.../dto/request/ProfileUpdateRequest.java`
- `common-module-impl/.../dto/request/MenuCreateRequest.java`
- `common-module-impl/.../dto/request/MenuUpdateRequest.java`
- `common-module-impl/.../dto/response/LoginResponse.java`
- `common-module-impl/.../dto/response/TokenRefreshResponse.java`
- `common-module-impl/.../dto/response/MenuResponse.java`
- `common-module-api/.../auth/UserInfoResponse.java`

**安全基础设施:**
- `common-module-impl/.../auth/rateLimit/RateLimitGuard.java`
- `common-module-impl/.../auth/rateLimit/InMemoryRateLimitGuard.java`
- `common-module-impl/.../auth/rateLimit/SlidingWindowCounter.java`
- `common-module-impl/.../auth/login/LoginAttemptTracker.java`
- `common-module-impl/.../auth/blacklist/TokenBlacklist.java`
- `common-module-impl/.../auth/blacklist/InMemoryTokenBlacklist.java`
- `common-module-impl/.../auth/password/PasswordPolicy.java`
- `common-module-impl/.../auth/password/PasswordPolicyImpl.java`
- `common-module-impl/.../auth/password/PasswordChangeService.java`
- `common-module-impl/.../auth/password/PasswordChangeServiceImpl.java`
- `common-module-impl/.../auth/audit/SecurityAuditLogger.java`
- `common-module-impl/.../auth/audit/SecurityAuditEvent.java`
- `common-module-impl/.../auth/audit/SecurityAuditEventType.java`
- `common-module-impl/.../auth/audit/LoggingSecurityAuditLogger.java`

**数据层:**
- `application/src/main/resources/db/schema.sql`
- `application/src/main/resources/db/data.sql`
- `application/src/main/resources/application.yml`

### 发现

#### [一般] User.java passwordChangeRequired 缺少 columnDefinition 指定

- **位置**：`User.java:51`
- **描述**：OOD 第5.1节明确要求 `@Column(nullable=false, columnDefinition="BIT(1) DEFAULT 0")`，但代码仅使用 `@Column(nullable=false)`。虽然 schema.sql 的 DDL 已正确定义 `NOT NULL DEFAULT 0`，且 Java 默认值 `= false` 可覆盖新实体创建场景，但缺少 `columnDefinition` 导致 JPA DDL 自动生成（若启用）不会产生 DEFAULT 约束，与设计契约不完全一致。
- **建议**：补充 `columnDefinition="BIT(1) DEFAULT 0"` 以对齐设计文档，或确认当前省略 columnDefinition 是经评估的降级决策并在注释中记录。

#### [一般] SlidingWindowCounter.cleanup() 存在 TOCTOU 竞态条件

- **位置**：`SlidingWindowCounter.java:52`
- **描述**：`cleanup()` 方法在 `ConcurrentHashMap.forEach` 闭包中先通过 `v.isEmpty()` 检查队列是否为空，然后调用 `windows.remove(k, v)` 移除空条目。两操作非原子：若在 `isEmpty()` 与 `remove()` 之间的时间窗口内，`tryAcquire` 通过 `compute` 向同一 Deque 添加了时间戳，`remove(k, v)` 仍会因引用相等（`==`）而移除该条目，导致已计入的请求时间戳丢失。影响：极端条件下漏计数 1 次，可能使一次应被限流的请求通过。
- **建议**：将 cleanup 替换为 `windows.entrySet().removeIf(entry -> entry.getValue().isEmpty())`，其基于迭代器语义可避免引用比较陷阱；或采用惰性清理策略（仅在 `tryAcquire` 中移除窗口外条目），完全依赖后台定时任务移除长期空队列。

#### [轻微] PermissionFunction.sortOrder 字段与 schema 列名不匹配

- **位置**：`PermissionFunction.java:33`，`schema.sql:88`
- **描述**：实体中 `sortOrder` 字段未指定 `@Column(name = "sort")`，JPA 默认映射为列 `sort_order`；但 schema.sql 中 `sys_function` 表的列名为 `sort`。若项目未启用自定义命名策略，实体查询/写入可能产生列不存在错误或数据错位。此问题为迁移前遗留缺陷，未在当前修复范围内暴露。
- **建议**：为 `sortOrder` 字段补充 `@Column(name = "sort", nullable = false)` 注解，确保与物理列名对齐；或确认项目命名策略已正确处理此映射关系。

### 本轮统计

| 严重程度 | 数量 |
|---------|------|
| 严重 | 0 |
| 一般 | 2 |
| 轻微 | 1 |

### 总评

**实体变更**：User、Role、Post 的新增字段（passwordChangeRequired、tokenVersion、sort）和 NOT NULL 约束补全整体正确，与 OOD 第5.1节一致。`PermissionFunction` 类名重命名在所有引用处同步更新完毕，`component` 字段已正确映射。`UserRepository.findByUsername` 已返回 `Optional<User>`。

**DTO**：所有认证相关 DTO 均按设计使用 Java 17 record 类型（LoginRequest、RefreshTokenRequest、PasswordChangeRequest、ProfileUpdateRequest、LoginResponse、TokenRefreshResponse）。`MenuUpdateRequest` 使用传统 POJO 并标注 `@JsonInclude(NON_NULL)`，正确实现 PATCH 局部更新语义。`MenuController.update()` 中包含 `id` 一致性校验（第123行）。`UserInfoResponse` 已从 `common-module-impl` 迁移至 `common-module-api`，字段命名为 `realName`/`role`，与前端对齐。

**安全基础设施**：`RateLimitGuard` 接口、`InMemoryRateLimitGuard` 实现、`SlidingWindowCounter`（使用 `ConcurrentHashMap.compute` 原子性）、`LoginAttemptTracker`（双维度 IP+用户名锁定，阈值与锁定时间严格匹配 OOD 第4.1节）、`TokenBlacklist` 接口及 `InMemoryTokenBlacklist`、`PasswordPolicy`/`PasswordPolicyImpl`（3/4 字符种类检查、8-64 长度、用户名包含检查）、`PasswordChangeService`/`PasswordChangeServiceImpl`、审计事件字段枚举及 `LoggingSecurityAuditLogger` 实现均与 OOD 设计基本一致。

**数据层**：schema.sql 中所有 enabled/visible 列均已补加 `NOT NULL DEFAULT 1`，seed 数据包含 `password_change_required = 1`。application.yml 已移除 phase0 profile。

代码质量整体良好，未发现严重问题。两处一般性问题涉及设计一致性细微偏差和并发边界条件，建议在后续迭代中修复。
