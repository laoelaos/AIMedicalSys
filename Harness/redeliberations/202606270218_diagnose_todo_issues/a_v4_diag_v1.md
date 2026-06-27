# 诊断报告：Phase 1 代码审查待办事项根因分析

## 概述

本文对 `Harness/reviews/202606270204_fix_phase1B_code_review/todo.md` 中的 34 个待办事项逐一进行根因分析，判断每个问题是：
1. 真实存在还是误报？
2. 根因是 OOD 文档缺陷还是实现编码问题？

## 诊断结果总览

| 分类 | 计数 | 占比 |
|------|------|------|
| 真实存在 - 编码缺陷 | 11 | 32.4% |
| 真实存在 - OOD 文档缺陷（设计不足/不一致/遗漏） | 3 | 8.8% |
| 真实存在 - 测试覆盖不足 | 12 | 35.3% |
| 真实存在 - 测试设计/实现质量 | 6 | 17.6% |
| 真实存在 - 其他（文档对齐/路径偏差） | 1 | 2.9% |
| 误报 | 1 | 2.9% |
| 合计 | 34 | 100% |

---

## T1: [严重] Access Token 无 type claim 导致 JwtAuthenticationFilter 拒绝所有已认证请求

**判定：真实存在 — 编码缺陷**

### 根因分析
- `JwtTokenProvider.generateAccessToken()`（`JwtTokenProvider.java:52-63`）生成的 Access Token **不包含** `type` claim
- `JwtAuthenticationFilter.doFilterInternal()`（`JwtAuthenticationFilter.java:61`）调用 `jwtTokenProvider.validateToken(token, "access")`，要求 `type` claim 必须等于 `"access"`
- `JwtTokenProvider.validateToken()`（`JwtTokenProvider.java:88-105`）中 `expectedType="access"` 时执行 `claims.get("type", String.class)` 比对，因 token 中无此 claim 而返回 `null`
- 返回 `null` 后 Filter 清除 SecurityContext 并放行（`JwtAuthenticationFilter.java:62-66`），导致 **所有携带有效 Access Token 的请求均被当做未认证处理**

### 因果链
```
generateAccessToken() 不写 type claim
  → validateToken(token, "access") 找不到 type="access"
    → 返回 null
      → Filter 清除 SecurityContext
        → 所有请求被视为未认证 → 401
```

### 证据
- 生成方法 `JwtTokenProvider.java:52-63`：Access Token claims 仅含 `sub`, `userId`, `userType`, `jti`, `iat`, `exp`，**无 `type`**
- 验证方法 `JwtTokenProvider.java:88-105`：第 96 行 `claims.get("type", String.class)`，比对 `expectedType`
- 调用方 `JwtAuthenticationFilter.java:61`：传入 `"access"` 作为 expectedType

---

## T2: [严重] 异常刷新检测仅记录日志，未拒绝请求

**判定：真实存在 — OOD 设计不足（代码符合 OOD 但 OOD 规范本身不充分）**

### 根因分析
- `AuthServiceImpl.refresh()`（`AuthServiceImpl.java:270-283`）检测到异常刷新后仅执行 `log.warn(...)`（第 280 行），然后**继续正常返回新 Token**
- OOD 4.2 节明确描述检测动作为："告警方式为 `log.warn(...)` 输出安全日志，对接业务监控系统"——**未要求在检测到异常时拒绝请求**
- 代码**完全符合 OOD 规范**，但 OOD 规范本身存在安全设计缺陷：检测到疑似旧 Refresh Token 重复使用（暗示 token 泄露）时仅告警不阻断，攻击者可在告警响应窗口内持续滥用

### OOD 缺陷定位
`Docs/05_ood_phase1_B.md` 第 503 行描述了异常刷新检测的行为：
> 告警方式为 `log.warn`...输出安全日志，对接业务监控系统（如 Prometheus + AlertManager）消费此日志

未定义**阻断逻辑**——检测到异常后应阻止本次刷新请求返回新 Token，并强制前端清除本地 token。

---

## T3: [一般] 测试文件路径偏差

**判定：真实存在 — 范围描述与模块结构不一致**

### 独立验证

经查阅 scope.md 和实际文件系统：

**范围文件（scope.md）**：未以绝对路径逐条枚举测试文件。在"变更范围"中，`GlobalExceptionHandler.java`、`GlobalErrorCode.java` 列为被修改文件（位于 `common-module-impl` 范围），"对应的测试文件"泛指出其测试应随模块一同移动。

**审查报告（review_v2_D）** 指出审查范围描述中指定以下三条路径：
- `AIMedical/backend/application/src/test/java/com/aimedical/common/config/GlobalExceptionHandlerTest.java`
- `AIMedical/backend/application/src/test/java/com/aimedical/common/exception/GlobalErrorCodeTest.java`
- `AIMedical/backend/application/src/test/java/com/aimedical/integration/EntityMappingIT.java`

**实际路径验证**（三条路径在 `application/` 下均不存在）：

| 文件 | scope.md 所归属模块 | 实际路径 |
|------|---------------------|---------|
| `GlobalExceptionHandlerTest.java` | `common/` | `backend/common/src/test/java/com/aimedical/common/config/GlobalExceptionHandlerTest.java` |
| `GlobalErrorCodeTest.java` | `common/` | `backend/common/src/test/java/com/aimedical/common/exception/GlobalErrorCodeTest.java` |
| `EntityMappingIT.java` | `integration/` | `backend/integration/src/test/java/com/aimedical/integration/EntityMappingIT.java` |

**结论**：文件确实不在 `application/` 下，路径偏差真实存在。但 scope.md 本身未明确承诺这三条文件所属模块，偏差源于审查者对范围归属的预期（将变更涉及的测试文件想象在 application 模块中）与实际模块结构之间的不一致。此问题不影响运行时正确性，属于审查流程中的文档对齐问题。

### 修改建议
更新 scope.md 或审查范围说明，明确列出这三条测试文件的模块归属路径，消除下次审查的路径歧义。

---

## T4: [一般] PasswordChangeCheckFilterTest 未测试 SecurityContext 存在但 principal 为 null/非 Long 类型场景

**判定：真实存在 — 测试覆盖不足**

### 根因分析
- `PasswordChangeCheckFilterTest.java` 现有 7 个测试方法，全部使用 `UsernamePasswordAuthenticationToken(1L, null, ...)`——principal 均为 `Long` 类型
- 未覆盖以下场景：
  - SecurityContext 存在但 Authentication 的 principal 为 null（如匿名认证对象）
  - principal 为非 Long 类型（如 String username）
- `PasswordChangeCheckFilter` 内部（需查看其代码确认）可能通过这些路径访问 principal，缺失测试意味着这些路径可能被静默错误跳过

---

## T5: [严重] UserFacadeImplTest 完全 mock UserConverter，无法发现真实转换逻辑缺陷

**判定：真实存在 — 测试设计缺陷**

### 根因分析
- `UserFacadeImplTest.java:24`：`private final UserConverter userConverter = mock(UserConverter.class);`
- 全部测试方法均通过 `when(userConverter.toUserInfoResponse(user)).thenReturn(expectedResponse)` 预设转换结果
- `UserConverter` 的核心逻辑（角色过滤、sort 排序选主角色、权限合并、字段映射）**完全未被测试覆盖**
- OOD 5.2 节明确声明 `UserConverter` 是"转换逻辑的唯一来源"，`UserFacadeImpl` 只是委托者。测试 mock 掉被委托方意味着测试的是"委托关系存在性"而非"逻辑正确性"

---

## T6: [严重] 缺失异常刷新检测（Suspicious Refresh Detection）的单元测试

**判定：真实存在 — 测试覆盖不足**

`AuthService.refreshToken()` 中的异常刷新检测逻辑（`AuthServiceImpl.java:270-283`）无对应的单元测试验证检测条件和告警行为。

---

## T7: [严重] 缺少 SecurityAuditLogger 写入失败降级路径的测试

**判定：真实存在 — 测试覆盖不足**

`LoggingSecurityAuditLogger` 的写入失败降级（OOD 4.8 节定义的"捕获 IOException 等异常，记录到业务日志，不向调用方抛出"）无测试覆盖。

---

## T8: [一般] AuthServiceImpl.login() ACCOUNT_LOCKED args 与设计文档示例不一致

**判定：真实存在 — OOD 文档内部不一致**

### 根因分析
- **代码行为**（`AuthServiceImpl.java:104,110`）：传递 `"30分钟"`/`"15分钟"` 作为 args
- **OOD 10.3 节**：模板消息为 `"账户已锁定，请{锁定时间}后重试"`，args[0] 应为锁定时间短语
- **OOD 3.1.1 节（登录流程·ACCOUNT_LOCKED 消息插值机制段落）**：`BusinessException(GlobalErrorCode.ACCOUNT_LOCKED, "请30分钟后重试")`——第二个参数是完整消息而非时间短语
- **OOD 10.3 节示例**同样显示 `throw new BusinessException(GlobalErrorCode.ACCOUNT_LOCKED, "请30分钟后重试")`
- **代码的 args `"30分钟"` 与模板 `{锁定时间}` 配合后**得到正确结果 `"账户已锁定，请30分钟后重试"`
- **OOD 示例的 args `"请30分钟后重试"` 与模板 `{锁定时间}` 配合后**会得到错误结果 `"账户已锁定，请请30分钟后重试后重试"`（重复替换）

### 结论
代码行为与 OOD 10.3 节**模板设计**一致，与 OOD 3.1.1 和 10.3 节的**代码示例**不一致。OOD 示例写错了 args 值。

---

## T9: [一般] AuthServiceImpl.logout() 对过期 token 提前返回，跳过审计日志

**判定：真实存在 — 编码缺陷**

### 根因分析
- `AuthServiceImpl.logout()`（`AuthServiceImpl.java:183-185`）：当 `validateToken(token, null)` 返回 null（token 过期或无效）时，直接 `return`，**跳过审计日志记录**
- OOD 3.1.4 Step 4："安全审计日志记录：通过 SecurityAuditLogger.logAudit(...) 记录登出事件，无论是否携带 refreshToken 均记录"
- OOD 4.8 节 `SecurityAuditEvent` 的 `userId` 和 `username` 字段标记为"否"（可选），明确支持未知用户场景
- 即使 token 已过期，也应尝试从过期 token 中提取可用信息，或记录带 null 标识的审计事件

---

## T10: [一般] AuthServiceImpl.logout() 重复 JWT 解析

**判定：真实存在 — 编码质量缺陷**

- `AuthServiceImpl.logout()` 第 182 行：`jwtTokenProvider.validateToken(token, null)`——解析一次获得 Claims
- 第 187 行：`jwtTokenProvider.getJtiFromToken(token)`——**再次解析同一 token** 提取 jti
- Claims 中已包含 jti，可直接通过 `claims.get("jti", String.class)` 获取，无需二次解析

---

## T11: [一般] AuthService.getCurrentUser(String token) 直接验证 token 参数而非依赖 SecurityContext

**判定：真实存在 — 架构实现偏差**

- `AuthServiceImpl.getCurrentUser()`（`AuthServiceImpl.java:303-314`）接收 `String token` 参数，内部调用 `jwtTokenProvider.validateToken(token, null)` 重新解析 token
- OOD 3.1.5 节规定："`GET /api/auth/me` 从 SecurityContext 获取当前用户 ID"
- JwtAuthenticationFilter 已经将用户信息装入 SecurityContext，Service 层应直接利用
- 当前实现绕过 Filter 装配的 SecurityContext，存在**重新解析 token 带来的 token 验证与 Filter 不一致风险**（如 Filter 拒绝但 Service 放行）

---

## T12: [一般] JwtTokenProvider Base64 字符集校验使用标准 Base64 而非 URL-safe

**判定：真实存在 — 编码与 OOD 不一致**

- `JwtTokenProvider.init()`（`JwtTokenProvider.java:37-38`）：`secret.matches("^[A-Za-z0-9+/]+=*$")`——标准 Base64 字符集（含 `+`、`/`、`=`）
- OOD 4.7 节："合法字符集：Base64 URL-safe 字符集（A-Z a-z 0-9 - _）"——URL-safe 字符集使用 `-` 和 `_` 替代 `+` 和 `/`
- 当前校验会**拒绝**使用 URL-safe Base64 编码的密钥，与设计规范冲突
- **连带影响**：`JwtTokenProvider.java:42` 使用 `Base64.getDecoder()` 解码密钥，若仅改正则校验而不配套改为 `Base64.getUrlDecoder()`，URL-safe 编码密钥的解码将抛出 `IllegalArgumentException`

---

## T13: [一般] SlidingWindowCounter 使用全局锁而非"每个 IP 独立加锁"

**判定：真实存在 — 编码与 OOD 不一致**

- `SlidingWindowCounter.java:14`：`private final ReentrantLock lock;`——**全局单锁**
- `SlidingWindowCounter.java:36-54`：`tryAcquire()` 方法在全局锁保护下执行 `windows.compute(key, ...)`
- OOD 4.1 节："每个 IP 的窗口对象独立加锁，细粒度锁减少竞争"；"确保每个 IP 的窗口对象独立加锁"
- 全局锁将不同 IP 的限流检查串行化，高并发下（不同 IP 同时请求）产生不必要的锁竞争
- **连带影响**：`cleanup()` 方法（`SlidingWindowCounter.java:59-66`）同样依赖全局锁保护 `windows.entrySet().removeIf(...)`。改为每 key 独立锁后需同步调整 cleanup 同步策略（如利用 `ConcurrentHashMap` 自身的原子方法替代全局锁）

---

## T14: [一般] JwtUtil.generateToken 仍包含 role/position claims 且缺 jti

**判定：真实存在 — 遗留代码未清理**

- `JwtUtil.generateToken()`（`JwtUtil.java:71-89`）：
  - 第 75 行：`claims.put("role", role)`——违反 OOD 3.2 节"Access Token 中不包含 role/position/authorities claims"
  - 第 77 行：`claims.put("position", position)`——违反同上约束
  - 缺失 jti claim——不符合 OOD 3.2 节 Access Token 必须含 jti 的要求
- `JwtUtil` 是 Phase 0 遗留工具类，Phase 1 新增的 `JwtTokenProvider` 已包含正确实现，但 `JwtUtil` 仍在被 `JwtAuthenticationFilter.extractToken()` 引用（`JwtAuthenticationFilter.java:111`），而 `generateToken()` 仅用于测试

---

## T15: [一般] LoginAttemptTracker record* 方法缺少窗口过期防御

**判定：真实存在 — 编码缺陷**

- `LoginAttemptTracker.recordUsernameFailure()`（`LoginAttemptTracker.java:32-39`）：`compute` 闭包中始终递增计数，**不检查窗口是否已过期**
- `LoginAttemptTracker.recordIpFailure()`（`LoginAttemptTracker.java:42-49`）：同上
- 窗口过期仅在 `isLocked()` 方法中以惰性清除方式处理（`LoginAttemptTracker.java:58-70`）
- 如果代码绕过 `isLocked()` 直接调用 `record*()`，失败计数会无限累积，导致**永久锁定**（因窗口过期条件永远不会被触发）
- OOD 4.1 节定义了锁定时间窗口，但未要求在 `record*` 方法中做防御性检查

---

## T16: [一般] GlobalExceptionHandler.formatMessage() 对命名占位符先尝试 MessageFormat 产生异常开销

**判定：真实存在 — 编码质量缺陷**

- `GlobalExceptionHandler.formatMessage()`（`GlobalExceptionHandler.java:38-48`）：
  - 第 39 行：优先尝试 `MessageFormat.format(template, args)`
  - 对于 `ACCOUNT_LOCKED` 的模板 `"账户已锁定，请{锁定时间}后重试"`，`{锁定时间}` 不是 `MessageFormat` 合法占位符（合法格式为 `{0}`、`{1}`）
  - `MessageFormat.format()` 抛出 `IllegalArgumentException`
  - 第 42-44 行：捕获异常后降级为 `replaceFirst`，正确完成替换
- 每次 ACCOUNT_LOCKED 异常都触发一次异常抛出 + 捕获，产生不必要的性能开销

### 建议修复方向
先检测模板是否含命名占位符（仅含 `{非数字}`），跳过多余的 MessageFormat 尝试，直接使用 replaceFirst。

---

## T17: [一般] RestAuthenticationEntryPoint/RestAccessDeniedHandler 的 Result 未经过消息插值管线

**判定：真实存在 — OOD 文档遗漏 + 编码未覆盖**

### 根因分析
- `RestAuthenticationEntryPoint.java:33`：`objectMapper.writeValueAsString(Result.fail(errorCode))`
- `RestAccessDeniedHandler.java:34`：`objectMapper.writeValueAsString(Result.fail(errorCode))`
- `Result.fail(errorCode)` 直接将 `errorCode.getMessage()` 作为 message 字段输出，**未经过 `GlobalExceptionHandler.formatMessage()` 插值管线**
- 根源在于 OOD 10.3 节定义的消息插值管线仅覆盖了 `GlobalExceptionHandler.handleBusinessException()` 路径，**未显式包含 `AuthenticationEntryPoint`/`AccessDeniedHandler` 这一出口**。编码者按 OOD 实现，自然未在此路径应用插值
- 当前 `UNAUTHORIZED`、`FORBIDDEN`、`ACCOUNT_DISABLED`、`PASSWORD_CHANGE_REQUIRED` 均无动态参数，因此功能上不受影响。但若未来扩展有动态参数的 ErrorCode 通过此路径，消息将显示模板原文而非插值后文本

### 分类说明
根因归属：**OOD 文档遗漏（主因）**。OOD 10.3 节应显式覆盖此路径，当前仅定义了 `GlobalExceptionHandler` 一个出口。编码实现随之缺失插值调用（次因）。

---

## T18: [一般] abnormal refresh 检测窗口无过期清理

**判定：真实存在 — 编码缺陷**

- `AuthServiceImpl.java:68`：`private final ConcurrentHashMap<Long, Deque<Long>> refreshTimestamps`
- `refresh()` 方法（`AuthServiceImpl.java:270-283`）在 `compute` 闭包中惰性清理单个 key 的过期条目，但 **从未移除不活跃用户的整个 entry**
- 用户登出后或停止刷新后，对应的 `Deque<Long>` 对象永久驻留在 Map 中
- 随系统运行时间增长，不活跃用户的条目持续累积，导致**内存泄漏**

### 影响范围
假设系统有 10000 活跃用户，每用户平均 1KB 窗口数据 → 约 10MB 堆内。但若系统经历过大量用户（如批量导入后的完整生命周期），不活跃条目可累积到不可忽视的量级。

---

## T19: [一般] MenuController 直接操作 SecurityContextHolder 而非使用 CurrentUser 接口

**判定：真实存在 — 架构实现偏差**

- `MenuController.getCurrentUserId()`（`MenuController.java:152-161`）直接访问 `SecurityContextHolder.getContext().getAuthentication()` 获取 principal
- OOD 1.3 节定义了 `CurrentUser` 接口（`Long getUserId()`）作为"消除 Controller 层对 SecurityContextHolder 的直接操作"的抽象
- 当前实现未使用 `CurrentUser` 接口，耦合于 Spring Security 的具体实现

---

## T20: [一般] GlobalExceptionHandlerTest 未覆盖消息插值管线回退路径

**判定：真实存在 — 测试覆盖不足**

`GlobalExceptionHandlerTest.java` 未测试以下场景：
- args=null 时有占位符的模板（测试回退行为）
- 无占位符的模板传入非空 args（测试多余 args 被忽略）
- 占位符数量 > args 数量（测试未填充占位符保留原文）

---

## T21: [一般] AuthServiceImpl.login() deleted 分支测试中预设 passwordEncoder.matches=true 但不必要的 mock

**判定：真实存在 — 测试设计冗余**

### 独立验证

经查阅 `AuthServiceTest.java` 实际代码：

- **实际位置**：`AuthServiceTest.java:268`，位于 `login_shouldThrowUserDeleted()` 方法（第 262-284 行），非 todo.md 标注的 186-209 行（186-209 行为 `login_shouldThrowUserDisabled`，该测试无此冗余 mock）
- `when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true)`（第 268 行）
- OOD 3.1.1 Step 6 定义：`deleted == true` 时，执行 dummy BCrypt 比对后立即抛出 `BusinessException`，**不依赖 `matches()` 的返回值决定流程**
- 无论 `matches()` 返回 `true` 还是 `false`，deleted 分支的异常抛出路径一致，该 stub 的结果不会被消费
- 删除此 stub 后测试行为不变（Mockito 默认返回 `false`，同样不影响流程）
- `login_shouldThrowUserDisabled`（第 186-209 行，`enabled=false` 分支）**无**此冗余 mock，仅通过 `verify(passwordEncoder).matches(eq("dummy"), anyString())` 验证 dummy BCrypt 被调用——这是合理的验证

### 结论
冗余 stub 真实存在，但 todo.md 标注的位置有误（应为 262-268 行而非 186-209 行）。删除第 268 行 `when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true)` 使测试语义更精确。

---

## T22: [一般] AuthControllerTest PUT /api/auth/profile 使用路径 /me 而非 OOD 定义的 /profile

**判定：误报 — 原 Controller 路由偏差已于前序修复中更正，当前测试与 Controller 一致**

### 核实分析
- `AuthController.java:69` 已使用 `@PutMapping("/profile")`（此前在 fix commit a14b672 中从 `/me` 修正为 `/profile`）
- `AuthControllerTest.java:225` 的 `@DisplayName` 明确标为 `"PUT /api/auth/profile"`
- 测试通过 `authController.updateMe(...)` 直接调用 Controller 方法，无 HTTP 路径参数，不存在路由路径偏差
- todo.md 所列"测试使用路径 /me"很可能源于原审查时将 `updateMe` 方法名误认为路由路径，而 `@DisplayName` 始终正确描述为 `/profile`

### 结论
当前测试行为与 Controller 路由 `/profile` 完全一致，不存在路径偏差。T22 应标记为误报。

---

## T23: [一般] AuthControllerTest.changePassword 未覆盖 SecurityContext 为空的异常路径

**判定：真实存在 — 测试覆盖不足**

未测试当 SecurityContext 为空时 `changePassword` 的行为。

---

## T24: [一般] SlidingWindowCounterTest 并发测试使用 <= limit 而非严格验证并发下的精确限流

**判定：真实存在 — 测试设计弱**

`SlidingWindowCounterTest.java:84-103` 的并发测试断言 `<= limit`，而非验证在并发下精确遵守 `limit` 阈值。

---

## T25: [一般] SlidingWindowCounterTest 锁释放测试通过反射间接验证

**判定：真实存在 — 测试实现脆弱**

`SlidingWindowCounterTest.java:127-152` 通过反射访问私有 `lock` 字段验证锁状态，对实现变更敏感。

---

## T26: [一般] PasswordPolicyImplTest 缺少全字符集（4/4 类型）边界测试

**判定：真实存在 — 测试覆盖不足**

未测试密码包含全部 4 种字符类型（大写、小写、数字、特殊字符）的边界场景。

---

## T27: [一般] UserConverterTest 缺少角色 sort=null + enabled=false 组合场景

**判定：真实存在 — 测试覆盖不足**

OOD 5.2 节规定 `Comparator.nullsLast(Comparator.naturalOrder())` 防止 sort 为 null 时 NPE。缺少 sort=null 与 enabled=false 组合的测试。

---

## T28: [一般] UserFacadeImplTest 未测试 Repository 查询失败（抛出异常）场景

**判定：真实存在 — 测试覆盖不足**

所有测试仅覆盖 Repository 返回 Optional.empty() 的路径，未测试 Repository 抛出 DataAccessException/RuntimeException 时的行为。

---

## T29: [一般] CurrentUserImplTest 缺少 principal 类型不匹配（非 Long）测试

**判定：真实存在 — 测试覆盖不足**

---

## T30: [一般] SecurityConfigPhase1Test Filter 注册顺序测试依赖反射操作 HttpSecurity.filterOrders

**判定：真实存在 — 测试实现脆弱**

通过反射访问内部状态验证 filter 顺序，对 Spring Security 版本变更脆弱。

---

## T31: [一般] EntityMappingIT 未覆盖 Role.enabled NOT NULL 约束验证

**判定：真实存在 — 测试覆盖不足**

OOD 5.1 节指定了 Role.enabled 的 `@Column(nullable=false)` 修复，集成测试未验证该约束的 DDL 是否正确生效。

---

## T32: [一般] PasswordChangeRequest 缺少 oldPassword 边界值（1 字符最小长度）测试

**判定：真实存在 — 测试覆盖不足**

`PasswordChangeRequest` 的 `oldPassword` 字段仅有 `@NotBlank @Size(max = 128)` 约束，测试未覆盖 1 字符最小长度边界。

---

## T33: [一般] MenuServiceTest shouldNotFilterDeletedInJavaLayer 测试名称语义与设计意图不符

**判定：真实存在 — 测试命名问题**

测试名称 `shouldNotFilterDeletedInJavaLayer` 与实际测试意图（验证 Java 层不过滤已删除菜单？验证 Filter 行为？）不符，影响可读性。

---

## T34: [一般] RoleTest 缺少 sort 字段 NOT NULL 约束验证

**判定：真实存在 — 测试覆盖不足**

OOD 5.1 节要求 Role.sort 新增 `@Column(nullable=false)`，单元测试未验证该约束。

---

## 汇总：OOD 文档缺陷

以下问题根因可追溯至 OOD 文档的不一致或设计不足：

| 问题 | OOD 缺陷类型 | 具体位置 |
|------|-------------|---------|
| T2 | 设计不充分 | `05_ood_phase1_B.md` 4.2 节：异常刷新检测仅定义 log.warn，未定义阻断逻辑 |
| T8 | 示例不一致 | `05_ood_phase1_B.md` 3.1.1 Step 3 和 10.3 节的 BusinessException args 示例与模板设计冲突 |
| T17 | 设计遗漏 | `05_ood_phase1_B.md` 10.3 节：插值管线未显式覆盖 AuthenticationEntryPoint/AccessDeniedHandler 路径 |

## 汇总：编码缺陷导致的问题

| 问题 | 文件 | 根因简述 |
|------|------|---------|
| T1 | JwtTokenProvider.java, JwtAuthenticationFilter.java | Access Token 无 type claim 但 Filter 要求 type="access" |
| T9 | AuthServiceImpl.java | 过期 token 提前返回跳过审计日志 |
| T10 | AuthServiceImpl.java | getJtiFromToken 二次解析同一 token |
| T11 | AuthServiceImpl.java | getCurrentUser 重新解析 token 而非用 SecurityContext |
| T12 | JwtTokenProvider.java | 使用标准 Base64 而非 URL-safe 字符集 |
| T13 | SlidingWindowCounter.java | 全局锁而非每 IP 独立锁 |
| T14 | JwtUtil.java | 遗留工具类仍含 role/position claims |
| T15 | LoginAttemptTracker.java | record* 无窗口过期防御 |
| T16 | GlobalExceptionHandler.java | 命名占位符先尝试 MessageFormat 触发异常 |
| T18 | AuthServiceImpl.java | refreshTimestamps 无过期清理 |
| T19 | MenuController.java | 直接操作 SecurityContextHolder |

## 汇总：测试问题（T3-T7, T20-T34）

20 个测试相关事项中：
- **T5、T21、T24、T25、T30、T33** — 测试设计/实现质量问题（共 6 项）
- **T4、T6、T7、T20、T23、T26、T27、T28、T29、T31、T32、T34** — 测试覆盖缺失（共 12 项）
- **T3** 归入"其他（文档对齐/路径偏差）"分类
- **T22** — 误报（原 Controller 路由已在先前修复中更正，当前测试与 Controller 一致）

## 修复建议优先级

### P0（必须修复，功能不可用或安全隐患）
- **T1**: Access Token 无 type claim — Blocking 级别功能缺陷
- **T2**: 异常刷新不拒绝 — 安全隐患

### P1（功能正确性受影响）
- **T9**: 跳过审计日志
- **T11**: 绕过 Filter 验证
- **T12**: 密钥字符集校验不匹配
- **T13**: 全局锁性能问题
- **T14**: 遗留工具类含违规 claims
- **T15**: LoginAttemptTracker 防御性缺失
- **T17**: 绕过插值管线
- **T18**: 内存泄漏
- **T19**: 未使用 CurrentUser 接口
- **T5、T6、T7**: 关键测试缺失

### P2（代码质量、测试完备性）
- T3、T4、T8、T10、T16、T20、T21、T23-T34（T22 为误报，无需修复）

## 修改流程建议

### 批量与切分策略

1. **修复批 1：阻断缺陷**（T1 + T2）
   - T1 修复单一（给 Access Token 补加 type claim），影响范围仅 `JwtTokenProvider.java:52-63`
   - T2 修改 OOD 文档 + 代码（异常刷新检测改为拒绝请求并强制前端清除本地 token）
   - 两者无耦合，可并行，但建议分两次提交以明确职责：T1 为纯编码修复，T2 包含 OOD 文档更新 + 编码修复
   - **T2 注意事项**：异常刷新检测改为拒绝请求后，若检测逻辑存在误报（如正常用户在短时间内连续刷新），将导致合法用户被拒登录。建议实施时同步添加监控告警阈值，在生产环境观察误报率，确认检测条件充分合理后再完全阻断

2. **修复批 2：OOD 文档对齐**（T8、T17）
   - T8：更正 OOD 3.1.1 和 10.3 节的 args 示例值
   - T17：在 OOD 10.3 节显式补充 AuthenticationEntryPoint/AccessDeniedHandler 路径 → 编码同步更新 Result.fail 调用
   - 建议：先更新 OOD 文档（同一次提交），再更新编码（另一次提交）

3. **修复批 3：AuthServiceImpl 编码缺陷**（T9、T10、T11、T18）
   - 聚焦同一文件 `AuthServiceImpl.java`
   - 建议一次性完成：T10（二次解析）和 T9（跳过日志）问题位置相邻，可一并修复
   - T11（getCurrentUser 参数签名）涉及接口签名变更，需同步更新调用方（AuthController）
   - T18（refreshTimestamps 内存泄漏）独立
   - 建议切为两个子任务：① T9+T10+T18（不涉及 API 变更） ② T11（涉及 API 变更）

4. **修复批 4：安全基础设施对齐**（T12、T13、T15、T19）
   - T12（JwtTokenProvider 字符集）：单一文件，独立修复
   - **T12 注意事项**：从标准 Base64 改为 URL-safe 字符集属于不兼容变更——已在使用标准 Base64 密钥的生产环境需重新生成密钥或双密钥过渡。建议在 OOD 中明确迁移策略（如配置文件中同时支持两种编解码，按密钥前缀或标记自动选择），避免启动失败
   - T13（SlidingWindowCounter 锁粒度）：将全局 ReentrantLock 改为 `ConcurrentHashMap.compute` 内原子操作 + 每个 key 独立锁，涉及锁策略变更
   - T15（LoginAttemptTracker 防御）：在 record* 方法中增加窗口过期检查（惰性清除），不涉及接口变更
   - T19（MenuController 改用 CurrentUser）：注入 CurrentUser 接口，替换 SecurityContextHolder 直接调用
   - 四个文件互不依赖，建议分四次独立提交

5. **修复批 5：遗留代码清理**（T14、T16）
   - T14（JwtUtil 遗留 claims）：清理或废弃 JwtUtil.generateToken 方法，确保新路径（JwtTokenProvider）已覆盖所有调用方
   - T16（MessageFormat 异常开销）：在 formatMessage 中增加命名占位符预检，跳过多余的 MessageFormat 尝试
   - 独立提交

6. **修复批 6：测试增强**（T3-T7、T20-T34）
   - 按测试文件分组，每组文件仅涉及一个测试任务：
     - T3 → scope.md（更新测试文件路径说明，消除路径歧义）
     - T4 → PasswordChangeCheckFilterTest
     - T5 → UserFacadeImplTest
     - T6 → AuthServiceTest（新增异常刷新检测测试）
     - T7 → LoggingSecurityAuditLoggerTest
     - T20 → GlobalExceptionHandlerTest
     - T21 → AuthServiceTest（删除冗余 stub）
     - T22 → 误报，无需修复（原 Controller 路由已在前序修复中更正）
     - T23 → AuthControllerTest
     - T24+T25 → SlidingWindowCounterTest
     - T26 → PasswordPolicyImplTest
     - T27 → UserConverterTest
     - T28 → UserFacadeImplTest
     - T29 → CurrentUserImplTest
     - T30 → SecurityConfigPhase1Test
     - T31 → EntityMappingIT
     - T32 → PasswordChangeRequestTest
     - T33 → MenuServiceTest
     - T34 → RoleTest
   - 建议切分为 5-6 个子任务，每个子任务覆盖 3-5 个测试文件，避免单一 PR 文件过多

### 任务依赖图
```
P0 ── T1, T2（无代码级依赖；T1 的 Filter 修复与 T2 的异常刷新阻断位于不同代码路径，仅集成测试场景存在先后依赖关系，不构成实质阻断）
       │
       └──→ 其余 P0 无依赖

P1 ── T11, T17（T11 与 T17 无代码依赖，T11 属于 Service 层，T17 属于 Security Handler 层，可并行实施）
       │
       T12/T13/T15/T18/T19 ──→ 无交叉依赖，可并行
       │
       T5/T7 ──→ 与被测代码修复批独立，可先行或并行
       T6 ──→ T2（测试对象行为变更依赖：T6 的检测条件验证可先行，但响应行为验证需在 T2 实施后更新）

P2 ── T3/T4/T8/T10/T16/T20/T21/T23-T34（T22 为误报，无需修复）──→ 无交叉依赖，可任意顺序
```

### 单次任务大小指导
- 每个子任务修改文件数 ≤ 5（测试增强类可放宽至 ≤ 8 文件，因均为新增测试方法）
- 每个子任务涉及功能点 ≤ 3（独立不相关的修复不合并）
- OOD 文档修改与对应编码修建议分两次提交，避免 commit 语义混合

## 检查清单

- [x] 所有 34 个待办事项已完成根因分析
- [x] 每个问题已判断真实/误报
- [x] 已区分 OOD 缺陷与编码缺陷
- [x] 所有关键推断有代码或 OOD 文档支撑
- [x] 修复者可依据本报告判断"改哪里"和"为什么"

## 修订说明（v2）

| 质询意见 | 回应 |
|---------|------|
| T3 证据未独立验证：诊断未查阅范围文件和实际目录确认路径偏差 | 已独立验证：读取 scope.md 和实际模块结构，三条路径在 application/ 下均不存在，与实际路径的偏差是真实存在的。scope.md 本身未明确承诺这些文件的模块归属，偏差源于审查范围预期与实际模块结构不一致。已更新 T3 分析段落。 |
| T21 证据未独立验证：诊断未实际审阅 AuthServiceTest.java:186-209 | 已独立验证：读取 `AuthServiceTest.java:262-284`（`login_shouldThrowUserDeleted`），冗余 stub 真实存在于第 268 行。todo.md 标注的位置（186-209）有误，实际位置是 262-268。已更新 T21 分析段落。 |
| T17 分类内部不一致：正文判"编码缺陷"但汇总表列入"OOD 文档缺陷" | 已修正：根因重新评估为 **OOD 文档遗漏（主因）+ 编码未覆盖（次因）**。OOD 10.3 节未显式覆盖 AuthenticationEntryPoint/AccessDeniedHandler 路径，编码实现自然缺失插值调用。正文分类已统一为"OOD 文档遗漏 + 编码未覆盖"。 |

## 修订说明（v3）

| 质询意见 | 回应 |
|---------|------|
| 问题1 [高]：汇总统计表与详细分析之间的数量矛盾 | 已修正：重新逐项核对分类，汇总表改为"编码缺陷11项、OOD文档缺陷3项、测试覆盖不足12项、测试设计/实现质量6项、其他2项"，合计34项。编码缺陷从22更正为11（仅计入纯编码实现问题），测试覆盖不足从8更正为12（含所有覆盖缺失），测试设计/实现质量问题独立为6项。 |
| 问题2 [高]："23个测试相关事项"为算术错误 | 已修正：将"23"更正为"20"。实际范围 T3-T7（5项）+ T20-T34（15项）= 20项。 |
| 问题3 [中]：T3严重度标签与优先级自相矛盾 | 已修正：T3 严重度从"[严重]"降为"[一般]"，与"不影响运行时正确性，属于文档对齐问题"的分析结论及P2优先级一致。 |
| 问题4 [中]：T4严重度标签与优先级自相矛盾 | 已修正：T4 严重度从"[严重]"降为"[一般]"，与P2优先级一致。 |
| 问题5 [中]：T12修复建议遗漏配套的Base64解码器修改 | 已修正：在 T12 分析中增加"连带影响"说明，指出 `JwtTokenProvider.java:42` 的 `Base64.getDecoder()` 需同步改为 `Base64.getUrlDecoder()`。 |
| 问题6 [中]：T13修复建议未讨论cleanup方法的同步依赖 | 已修正：在 T13 分析中增加"连带影响"说明，指出 `cleanup()` 方法在无全局锁后需同步调整同步策略。 |
| 问题7 [低]：优先级排序中缺失对修复副作用的风险评估 | 已修正：在修复批1的T2条目下增加注意事项（误报导致合法用户被拒的风险），在修复批4的T12条目下增加注意事项（不兼容变更及迁移策略建议）。 |
| 问题8 [低]：T8引用了错误的OOD章节标题 | 已修正：将"OOD 3.1.1 节 Step 3 示例"更正为"OOD 3.1.1 节（登录流程·ACCOUNT_LOCKED 消息插值机制段落）"。3.1.1 节无"Step 3"子标题，且 BusinessException 示例实际出现在步骤列表后的消息插值机制段落中。 |

## 修订说明（v4）

| 质询意见 | 回应 |
|---------|------|
| [严重] 汇总表分类计数与逐项分析不一致：测试设计/实现质量显示6项但逐项列出8项（T3、T5、T21、T22、T24、T25、T30、T33） | 已修正：将汇总表"测试设计/实现质量"计数从6修正为7（排除归入"其他"的T3后剩余7项）；"其他"计数从2修正为1（仅T3）。测试问题汇总之章节已同步将T3从"测试设计/实现质量问题"列表移除，明确标注"归入其他分类"。 |
| [一般] T3未纳入任何修复批次：修复批6标题声明覆盖T3-T7但任务列表从T4开始 | 已修正：在修复批6任务列表首行新增"T3 → scope.md（更新测试文件路径说明，消除路径歧义）"。 |
| [一般] T1→T2依赖关系缺乏充分论证：两者位于不同代码路径，仅在集成测试层面存在先后关系 | 已修正：将任务依赖图中"P0 ── T1 ──→ T2"修改为"P0 ── T1, T2（无代码级依赖）"，并补充说明两者的实际关系：位于不同代码路径，仅集成测试场景存在先后依赖，不构成实质阻断。 |
| [低] T14分析中"可能被引用"措辞模糊：实际JwtUtil正在被JwtAuthenticationFilter.java:111调用 | 已修正：将"JwtUtil 仍存留在代码库中且可能被引用"改为"JwtUtil 仍在被 JwtAuthenticationFilter.extractToken() 引用（JwtAuthenticationFilter.java:111），而 generateToken() 仅用于测试"。 |

## 修订说明（v5）

| 质询意见 | 回应 |
|---------|------|
| [问题一] T22 诊断结论与代码事实不符：Controller 已使用 `@PutMapping("/profile")`，测试 `@DisplayName` 正确描述端点，测试直接调用方法无 HTTP 路径参数 | 已修正：T22 判定从"真实存在 — 测试与 OOD 不一致"改为"误报"。Controller 路由 `/me`→`/profile` 已在 fix commit a14b672 中更正；测试 `@DisplayName("PUT /api/auth/profile")` 始终正确；测试通过 `authController.updateMe()` 直接调用 Controller 方法，不存在路径偏差。对应更新汇总表（测试设计/实现质量从 7 降为 6，新增误报行 1 项）、测试问题汇总、P2 优先级列表及修复批 6 任务列表。 |

## 修订说明（v6）

| 质询意见 | 回应 |
|---------|------|
| [中] T6"与被测代码独立"断言与 T2 行为变更存在矛盾：T6 被测对象正是 AuthServiceImpl.refresh() 第 270-283 行的异常刷新检测逻辑，而 T2 将更改此行为 | 已修正：将依赖图中 `T5/T6/T7` 独立组拆分为 `T5/T7`（仍标记为独立）和 `T6 ──→ T2（测试对象行为变更依赖：T6 的检测条件验证可先行，但响应行为验证需在 T2 实施后更新）`。 |
| [中] T11→T17 依赖箭头缺乏代码级依据：T11 修改 AuthServiceImpl.getCurrentUser()（Service 层），T17 修改 RestAuthenticationEntryPoint/RestAccessDeniedHandler（Security 层），无任何代码引用关系 | 已修正：删除 `T11 ──→ T17` 依赖箭头，改为 `T11, T17（T11 与 T17 无代码依赖，T11 属于 Service 层，T17 属于 Security Handler 层，可并行实施）`。 |
| [低] T18 内存估算术语"堆外"使用错误：refreshTimestamps 为 ConcurrentHashMap，数据存储在 JVM 堆内，非堆外 | 已修正：将"约 10MB 堆外"改为"约 10MB 堆内"。 |
