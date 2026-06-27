# R1: 审查安全基础设施实现是否与 OOD 设计文档 Docs/05_ood_phase1_B.md 一致

审查时间：2026-06-27

### 审查范围

1. `AIMedical/backend/modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/auth/jwt/JwtTokenProvider.java`
2. `AIMedical/backend/modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/auth/security/JwtAuthenticationFilter.java`
3. `AIMedical/backend/modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/auth/security/PasswordChangeCheckFilter.java`
4. `AIMedical/backend/modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/auth/security/GlobalRateLimitFilter.java`
5. `AIMedical/backend/modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/auth/security/SecurityConfigPhase1.java`
6. `AIMedical/backend/modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/auth/security/RestAuthenticationEntryPoint.java`
7. `AIMedical/backend/modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/auth/security/RestAccessDeniedHandler.java`
8. `AIMedical/backend/modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/auth/security/CurrentUserImpl.java`
9. `AIMedical/backend/modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/auth/exception/AccountDisabledAuthenticationException.java`
10. `AIMedical/backend/modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/auth/exception/PasswordChangeRequiredException.java`
11. `AIMedical/backend/modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/auth/config/AuthModuleConfig.java`
12. `AIMedical/backend/modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/auth/rateLimit/SlidingWindowCounter.java`
13. `AIMedical/backend/modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/auth/rateLimit/InMemoryRateLimitGuard.java`
14. `AIMedical/backend/modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/auth/rateLimit/RateLimitGuard.java`

### 发现

#### [一般] JwtTokenProvider Base64 字符集校验与设计文档不一致

- **位置**：`JwtTokenProvider.java:37-38`
- **描述**：设计文档 4.7 节要求"合法字符集：Base64 URL-safe 字符集（A-Z a-z 0-9 - _）"，且应使用 `Base64.getUrlDecoder()`。然而代码中的正则 `^[A-Za-z0-9+/]+=*$` 校验的是标准 Base64 字符集（含 `+` 和 `/`，不含 `-` 和 `_`），第 42 行使用 `Base64.getDecoder()` 解码。这意味着一个符合设计要求的 URL-safe Base64 密钥（如包含 `-` 或 `_`）会在 `@PostConstruct` 启动校验中被拒绝，导致应用无法启动。
- **建议**：将正则改为 `^[A-Za-z0-9\\-_]+=*$`，并将 `Base64.getDecoder()` 替换为 `Base64.getUrlDecoder()`，对齐设计文档的 URL-safe 要求。

#### [一般] SlidingWindowCounter 全局锁与设计文档的"每个 IP 独立加锁"不一致

- **位置**：`SlidingWindowCounter.java:36, 53-55`
- **描述**：设计文档 4.1 节要求"每个 IP 的窗口对象独立加锁"（per-key 细粒度锁），但 `tryAcquire` 方法中 `ReentrantLock.lock()` 包裹了所有 `ConcurrentHashMap.compute` 调用（第 36-55 行），形成一把全局锁。`ConcurrentHashMap.compute` 本身已提供 per-key 原子性，外层全局锁将不同 IP 的 `tryAcquire` 操作串行化，带来不必要的锁竞争。`cleanup` 方法（第 60-65 行）因遍历 `entrySet()` 需要锁保护，但不应与 `tryAcquire` 共享同一把锁。
- **建议**：移除 `tryAcquire` 中的 `lock.lock()/unlock()`，仅依赖 `ConcurrentHashMap.compute` 的 per-key 原子性；`cleanup` 改用独立的 `ReentrantLock` 或使用 `ConcurrentHashMap` 的线程安全迭代器。

### 本轮统计

| 严重程度 | 数量 |
|---------|------|
| 严重 | 0 |
| 一般 | 2 |
| 轻微 | 0 |

### 总评

安全基础设施实现整体与 OOD 设计文档一致。核心认证流程（SecurityFilterChain Filter 注册顺序、路径权限配置、AuthenticationEntryPoint/AccessDeniedHandler 行为契约、JwtAuthenticationFilter 静默跳过/禁用用户处理/passwordChangeRequired attribute、PasswordChangeCheckFilter 白名单/阻断逻辑、GlobalRateLimitFilter 独立计数器/限流阈值、SlidingWindowCounter 数据结构/线程安全）均正确实现。发现 2 个设计一致性偏差（均为 `[一般]` 级别），不影响核心功能，建议在下一次迭代中修复。
