# R2-C: 测试代码审查 — 安全基础设施测试、实体测试、集成测试及跨领域验证

审查时间：2026-06-27

### 审查范围

#### 安全基础设施测试（common-module-impl/src/test/java/.../auth/）
1. `auth/rateLimit/InMemoryRateLimitGuardTest.java`
2. `auth/rateLimit/SlidingWindowCounterTest.java`
3. `auth/login/LoginAttemptTrackerTest.java`
4. `auth/blacklist/InMemoryTokenBlacklistTest.java`
5. `auth/password/PasswordPolicyImplTest.java`
6. `auth/password/PasswordChangeServiceImplTest.java`
7. `auth/audit/LoggingSecurityAuditLoggerTest.java`
8. `auth/exception/AccountDisabledAuthenticationExceptionTest.java`
9. `auth/exception/PasswordChangeRequiredExceptionTest.java`

#### 实体测试（common-module-impl/src/test/java/.../permission/）
10. `permission/UserTest.java`
11. `permission/RoleTest.java`
12. `permission/PostTest.java`
13. `permission/PermissionFunctionTest.java`
14. `permission/UserRepositoryTest.java`

#### 集成测试（application/src/test/java/.../integration/）
15. `integration/EntityMappingIT.java`

#### 全局异常测试（application/src/test/java/.../common/）
16. `exception/GlobalErrorCodeTest.java`
17. `config/GlobalExceptionHandlerTest.java`
18. `util/SimpleMessageInterpolatorTest.java`

### 发现

#### [一般] PasswordPolicyImplTest 缺少 null password 测试

- **位置**：`PasswordPolicyImplTest.java:46-48`
- **描述**：`PasswordPolicyImpl.validate()` 当 password 为 `null` 时返回 `PASSWORD_TOO_SHORT`（源码第14行 `if (password == null || password.length() < MIN_LENGTH)`），但测试用例未覆盖此路径。唯一验证有效密码的测试 `validate_whenMeetsAllRequirements_shouldReturnNull` 只覆盖了正常输入。
- **建议**：新增测试用例验证 `policy.validate(null, "test")` 返回 `GlobalErrorCode.PASSWORD_TOO_SHORT`。

#### [一般] PasswordPolicyImplTest 缺少长度边界测试

- **位置**：`PasswordPolicyImplTest.java:15-24`
- **描述**：太短测试使用 "Ab1!"（4 字符）远小于 8，太长测试使用 257 字符远大于 64。未覆盖精确边界：8 字符应通过验证（`MIN_LENGTH=8`，条件为 `<`），65 字符应返回 `PASSWORD_TOO_LONG`（`MAX_LENGTH=64`，条件为 `>`）。
- **建议**：新增测试：`"Abcd1234"`（8 字符，含 3 种类型）应返回 null；`"A" + "a1!".repeat(16)`（65 字符）应返回 `PASSWORD_TOO_LONG`。

#### [轻微] InMemoryRateLimitGuardTest.shouldAllowAfterWindowExpiry 造成测试缓慢

- **位置**：`InMemoryRateLimitGuardTest.java:57`
- **描述**：使用 `Thread.sleep(10_100)` 等待 10 秒窗口过期，使单个测试耗时超过 10 秒。应当使用更短的窗口时间（如 100ms + 自定义构造器）加速测试。
- **建议**：使用 `InMemoryRateLimitGuard(new SlidingWindowCounter(), 5, 100)` 并 `sleep(150)` 代替。

#### [一般] PermissionFunctionTest 缺少多个字段的 getter/setter 验证

- **位置**：`PermissionFunctionTest.java`
- **描述**：`PermissionFunction` 实体包含 `parent`（ManyToOne）、`sortOrder`（default 0）、`visible`（default true）、`type`、`icon`、`path` 共 6 个字段，但测试仅覆盖了 `code`、`name`、`description`、`enabled`、`posts` 和 `component`。未覆盖字段超过实体总字段的一半。
- **建议**：为 `parent`、`sortOrder`、`visible`、`type`、`icon`、`path` 添加 set/get 测试。

#### [一般] PostTest 缺少 sort 字段验证

- **位置**：`PostTest.java`
- **描述**：`Post` 实体的 `sort` 字段（无默认值，nullable）未在测试中覆盖，而 `RoleTest` 已对 `sort` 做了默认值和 set/get 验证。
- **建议**：新增 `shouldDefaultSortIsNull`（验证默认 null，与 Role 不同）和 `shouldSetAndGetSort`。

#### [轻微] UserTest 缺少 enabled 默认值验证

- **位置**：`UserTest.java:56-63`
- **描述**：`User` 实体中 `enabled` 的默认值为 `true`，但测试仅覆盖了 set/get 操作，未验证默认值。`RoleTest` 和 `PostTest` 均未覆盖自身的 enabled 默认值验证。
- **建议**：新增 `shouldDefaultEnabledIsTrue()` 验证 `new User().getEnabled()` 为 `true`。

#### [轻微] SlidingWindowCounterTest 缺少清理逻辑的显式验证

- **位置**：`SlidingWindowCounterTest.java`
- **描述**：`SlidingWindowCounter` 有清理空 deque 的 `cleanup()` 后台线程（源码第51-53行），但测试仅通过窗口过期隐式覆盖（tryAcquire 内部会 poll 过期条目）。未显式验证后台定时任务对空 key 的移除行为。
- **建议**：可通过反射调用 `cleanup()` 或观察 `windows` map 大小来验证空 key 被清理。

#### [轻微] GlobalErrorCodeTest.shouldHaveExpectedConstants 枚举数量硬编码

- **位置**：`GlobalErrorCodeTest.java:11`
- **描述**：`assertEquals(20, GlobalErrorCode.values().length)` 硬编码了枚举数量，新增枚举值会导致该测试失败。虽然有意防止遗漏，但更好的实践是遍历验证每个枚举的 code/message 完整性后，再计数以确保无遗漏。
- **建议**：考虑使用动态方式验证（如遍历 `values()` 逐项检查），仅在确保全覆盖后保留计数断言。

### 本轮统计

| 严重程度 | 数量 |
|---------|------|
| 严重 | 0 |
| 一般 | 4 |
| 轻微 | 4 |

### 总评

本轮审查的 18 个测试文件整体质量较高，覆盖了审查重点中的绝大部分路径：

- **SlidingWindowCounterTest** ✅ 覆盖 tryAcquire 边界（limit=0/-1、windowMs=0/-1、null key）、并发安全（同 key 精确 assert、异 key、死锁安全），清理逻辑通过窗口过期测试隐式覆盖，建议增加显式清理验证。
- **LoginAttemptTrackerTest** ✅ 全面覆盖双维度（IP+用户名）计数器、阈值锁定、锁定到期惰性清除（isLocked 内部重置）、成功登录重置、并发安全、窗口重置，无遗漏。
- **InMemoryTokenBlacklistTest** ✅ 覆盖添加/检查/过期清理/并发同 key 与异 key/重复添加容忍，无遗漏。
- **PasswordPolicyImplTest** ⚠️ 覆盖所有规则（最小/最大长度、字符种类、用户名包含），但缺少 null password 和精确边界测试。
- **PasswordChangeServiceImplTest** ✅ 覆盖首次登录强制修改、管理员标记过期、修改后清除、用户不存在静默跳过。
- **LoggingSecurityAuditLoggerTest** ✅ 覆盖正常写入、异常容忍（null event）、key=value 格式校验（含可选字段有无）、null 字段输出、logger 名称验证。
- **实体测试** ⚠️ UserTest 和 RoleTest 较完整，但 PostTest 缺少 sort 验证，PermissionFunctionTest 缺失一半字段的覆盖。
- **UserRepositoryTest** ✅ 验证 findByUsername 的 Optional 返回（含反射验证）、存在/不存在场景、@EntityGraph 注解属性路径。
- **EntityMappingIT** ✅ 全面覆盖 User/Role/Post/PermissionFunction 的 JPA 映射，含列名、精度约束、唯一约束、NOT NULL、枚举映射、关联关系、级联持久化，覆盖此前报告的所有实体-SQL 失配问题。
- **GlobalExceptionHandlerTest** ✅ 覆盖消息插值链路（含 ACCOUNT_LOCKED 模板插值、numbered placeholder、原模板日志记录）、鉴权状态码映射（400/429/401/500）、各异常处理器。
- **GlobalErrorCodeTest** ✅ 验证全部 20 个错误码的 code 和 message。
- **异常测试** ✅ 正确验证两个自定义异常的构造（消息、原因、继承层次）。

主要改进方向为补全实体测试中缺失字段的验证，以及 PasswordPolicyImplTest 的边界和 null 用例。
