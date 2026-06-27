# R3: 模块三——支持基础设施审查

审查时间：2026-06-26 17:57

### 审查范围

- `auth/rateLimit/RateLimitGuard.java`
- `auth/rateLimit/InMemoryRateLimitGuard.java`
- `auth/rateLimit/SlidingWindowCounter.java`
- `auth/login/LoginAttemptTracker.java`
- `auth/blacklist/TokenBlacklist.java`
- `auth/blacklist/InMemoryTokenBlacklist.java`
- `auth/password/PasswordPolicy.java`
- `auth/password/PasswordPolicyImpl.java`
- `auth/password/PasswordChangeService.java`
- `auth/password/PasswordChangeServiceImpl.java`
- `auth/config/AuthModuleConfig.java`

### 发现

#### [一般] AuthServiceImpl.login() 使用 encode() 替代 matches() 进行 dummy BCrypt 比对

- **位置**：`service/impl/AuthServiceImpl.java:105`
- **描述**：设计文档 3.1.1 节步骤 5/6 要求"对虚拟哈希值执行 dummy BCrypt 比对"以消除响应时间差异。代码使用了 `passwordEncoder.encode("dummy")` 而非 `passwordEncoder.matches("dummy", "dummy_hash")`。`encode()` 会生成新 salt 并计算 hash，通常比 `matches()` 慢 2-3 倍，可能引入反向的时序差异。且语义上不符合"比对"（comparison）的设计意图。
- **建议**：将 `passwordEncoder.encode("dummy")` 替换为 `passwordEncoder.matches("dummy", "$2a$10$dummyhash...")` 以更准确地模拟真实比对操作的时序特征。

#### [一般] SlidingWindowCounter 中 ReentrantLock 作用域不完整

- **位置**：`auth/rateLimit/SlidingWindowCounter.java:14`
- **描述**：设计文档 4.1 节要求"ReentrantLock 保护窗口内的排序集合，确保每个 IP 的窗口对象独立加锁"，但代码中 `lock` 仅在 `cleanup()` 方法（第 55 行）中加锁，`tryAcquire()` 方法完全未使用该锁。`tryAcquire` 依赖 `ConcurrentHashMap.compute` 的 per-key 原子性，`cleanup` 的锁仅保护了过期条目回收的迭代操作，两者间存在锁策略不一致。虽无实际数据竞争（compute 是原子的，cleanup 仅删除空 deque），但与方法契约描述不匹配。
- **建议**：在 `tryAcquire()` 的 `compute` 闭包外增加 `lock.lock()` 保护，使锁策略与设计一致；或移除 `lock` 字段并更新设计文档。

#### [轻微] ACCOUNT_LOCKED 消息缺少"账户已锁定，"前缀

- **位置**：`service/impl/AuthServiceImpl.java:95,99`
- **描述**：设计文档 10.2 节 ErrorCode 表要求 IP 维度锁定消息为"账户已锁定，请 30 分钟后重试"，用户名维度锁定消息为"账户已锁定，请 15 分钟后重试"。代码中消息为"请30分钟后重试"和"请15分钟后重试"，缺少"账户已锁定，"前缀且缺少空格。虽然登录流程步骤 3（3.1.1 节）的消息格式与 ErrorCode 表一致，但代码未对齐。
- **建议**：将第 95 行消息改为"账户已锁定，请 30 分钟后重试"，第 99 行改为"账户已锁定，请 15 分钟后重试"。

#### [轻微] InMemoryTokenBlacklist 中 ScheduledExecutorService 未作为字段保存

- **位置**：`auth/blacklist/InMemoryTokenBlacklist.java:14`
- **描述**：`ScheduledExecutorService cleanupExecutor` 在构造器中作为局部变量创建，未保存为实例字段。虽然 daemon 线程保证执行器不会因垃圾回收而终止，但无法优雅关闭（应用关闭时资源泄露）。
- **建议**：将 `cleanupExecutor` 保存为字段，并可考虑实现 `DisposableBean` 或 `@PreDestroy` 方法在应用关闭时调用 `shutdown()`。

#### [轻微] PasswordChangeServiceImpl 缺少 @Transactional

- **位置**：`auth/password/PasswordChangeServiceImpl.java:23-36`
- **描述**：`markChangeRequired()` 和 `clearChangeRequired()` 分别执行 `findById()` 和 `save()` 两个 DB 操作，但未标注 `@Transactional`。每个调用在独立事务中执行，影响了效率（两次事务边界开销）。
- **建议**：为两个方法添加 `@Transactional` 注解，使 find + save 在同一事务中完成。

### 符合设计确认

以下要点经审查确认与设计文档一致：

1. **SlidingWindowCounter**: 方法签名 `tryAcquire(String key, int limit, long windowMs)` 与 4.1 节契约一致；`ConcurrentHashMap.compute` 保证原子性。
2. **InMemoryRateLimitGuard**: 默认阈值 5 次/10 秒与 4.1 节匹配；`AuthServiceImpl.login()` 中返回 `GlobalErrorCode.RATE_LIMITED`。
3. **GlobalRateLimitFilter 与 InMemoryRateLimitGuard 独立性**: `InMemoryRateLimitGuard` 在其构造函数中自建 `SlidingWindowCounter` 实例；`GlobalRateLimitFilter` 通过 `SecurityConfigPhase1` 注入独立的 `SlidingWindowCounter` bean。两计数器实例完全独立，符合 4.1 节。
4. **LoginAttemptTracker**: 用户名 5 次/15 分钟、IP 20 次/30 分钟与 4.1 节一致；"连续失败"语义为窗口内累计次数，与设计一致；惰性清除在 `isLocked` 的 compute 中实现；AuthServiceImpl 中 IP 维度计数的递增逻辑（步骤 5→IP，步骤 6→IP+用户名，步骤 7→用户名）与 3.1.1 节完全对齐。
5. **TokenBlacklist/InMemoryTokenBlacklist**: 仅黑名单 Access Token（`add(String jti, long expirationTime)` + `isBlacklisted(String jti)`），符合 Phase 1 策略；6.5MB 估算合理；`ScheduledExecutorService` 每 5 分钟清理符合设计。
6. **PasswordPolicyImpl**: 最小 8/最大 64/3 种字符/不含用户名规则与 4.3 节一致；方法签名 `GlobalErrorCode validate(String password, String username)` 一致。
7. **PasswordChangeServiceImpl**: 方法签名 `isChangeRequired/markChangeRequired/clearChangeRequired` 与 1.3 节一致；实现覆盖首次登录强制修改和管理员过期标记。
8. **AuthModuleConfig**: Bean 装配正确注册了 `RateLimitGuard`（`InMemoryRateLimitGuard`）、`TokenBlacklist`（`InMemoryTokenBlacklist`）、`LoginAttemptTracker`；`PasswordPolicyImpl` 和 `PasswordChangeServiceImpl` 通过 `@Component` 自动扫描注册，覆盖完整。
9. **异常刷新检测**: `AuthServiceImpl.refreshToken()` 中实现了 5 秒窗口内 2 次阈值检测（`ConcurrentHashMap<Long, Deque<Long>>` + `log.warn`），与 4.2 节一致。

### 本轮统计

| 严重程度 | 数量 |
|---------|------|
| 严重 | 0 |
| 一般 | 2 |
| 轻微 | 3 |

### 总评

模块三（支持基础设施）的实现质量良好，整体与 OOD 设计文档 `05_ood_phase1_B.md` 高度一致。11 个源文件的接口/类定义、方法签名、阈值常量、线程安全策略均正确映射了设计契约。发现的问题均为非关键性的实现偏差或可改进点，无严重缺陷。最值得关注的是 dummy BCrypt 比对使用了 `encode()` 而非 `matches()`，可能引入反时序差异；`SlidingWindowCounter` 的锁策略与文档描述不完全匹配。
