# 诊断报告：OOD Phase1_B 实现审查问题分析（v5）

## 概述

基于审查报告 `Harness/reviews/202606261757_ood_phase1_B_code_review/todo.md` 所列 26 项问题（T1-T23, T25-T27），逐项核查代码实现与 OOD 设计文档 `Docs/05_ood_phase1_B.md` 的一致性。核查范围包括 `AIMedical/backend/modules/common-module/common-module-impl/` 下的实现代码及 `AIMedical/backend/common/` 下的基础设施代码。

---

## 汇总

| 优先级 | 编号 | 判定 | 根因归属 | 影响模块 | 相关项/修复依赖 |
|--------|------|------|---------|---------|----------------|
| P0 | T26 | 真实缺陷 | 实现编码偏差 | AuthModuleConfig / SecurityConfigPhase1 | - |
| P1 | T23 | 真实缺陷 | 实现编码偏差 | MenuServiceImpl | - |
| P1 | T1 | 真实缺陷 | 实现编码偏差 | JwtTokenProvider | - |
| P1 | T2 | 真实缺陷 | 实现编码偏差 | JwtAuthenticationFilter | T12（同一文件，userId提取） |
| P1 | T3 | 真实缺陷 | 实现编码偏差（OOD 设计粒度不足亦有贡献） | GlobalExceptionHandler / GlobalErrorCode | - |
| P1 | T6 | 真实缺陷 | 实现编码偏差 | AuthController | - |
| P1 | T7 | 真实缺陷 | 实现编码偏差 | AuthController | - |
| P1 | T8 | 真实缺陷 | 实现编码偏差（OOD 设计粒度不足亦有贡献） | AuthService / AuthServiceImpl | - |
| P1 | T22 | 真实缺陷 | 实现编码偏差 | PermissionFunction | - |
| P1 | T25 | 真实缺陷 | 实现编码偏差 | GlobalExceptionHandler | - |
| P2 | T4 | 真实缺陷 | 实现编码偏差 | GlobalErrorCode | T27（同类消息偏差） |
| P2 | T5 | 真实缺陷 | 实现编码偏差 | AuthServiceImpl | - |
| P2 | T9 | 真实缺陷 | 实现编码偏差（OOD 设计粒度不足亦有贡献） | UserConverter / UserFacadeImpl | - |
| P2 | T10 | 真实缺陷 | 实现编码偏差 | JwtConfig | T1（同为JWT启动验证） |
| P2 | T11 | 真实缺陷 | 实现编码偏差 | RestAuthenticationEntryPoint | T4/T27（T11 修复前，文本变更会造成匹配静默失效） |
| P2 | T12 | 真实缺陷 | 实现编码偏差 | JwtAuthenticationFilter | T2（同一文件，集中JwtTokenProvider） |
| P2 | T13 | 真实缺陷 | 实现编码偏差 | AuthServiceImpl | T3（同为消息管线）、T16（测试内容随T3修复变化） |
| P3 | T14 | 真实缺陷 | 实现与设计约定不一致（功能正确） | SlidingWindowCounter | - |
| P2 | T19 | 真实缺陷 | 实现编码偏差 | MenuController | - |
| P2 | T20 | 真实缺陷 | 实现编码偏差 | MenuServiceImpl | - |
| P2 | T21 | 真实缺陷 | 实现编码偏差 | MenuController | - |
| P2 | T27 | 真实缺陷 | 实现编码偏差 | GlobalErrorCode | T4（同类消息偏差） |
| P3 | T15 | 真实缺陷 | 测试覆盖不完整 | AuthServiceTest | - |
| P3 | T16 | 真实缺陷 | 测试覆盖不完整 | LoginAttemptTrackerTest / AuthServiceTest | T3（T3修复后T16的测试内容将改变） |
| P3 | T17 | 真实缺陷 | 测试覆盖不完整 | MenuServiceTest | - |
| P3 | T18 | 真实缺陷 | 测试覆盖不完整 | SecurityConfigPhase1Test | - |

**统计：** 26/26 项确认为真实缺陷（0 项误报）。其中 18 项根因归属于纯实现编码偏差，3 项归属于实现编码偏差（OOD 设计粒度不足亦有贡献）（T3/T8/T9），1 项归属于设计与实现约定不一致（T14），4 项归属于测试覆盖不完整（T15/T16/T17/T18）。

**模式识别：** 缺陷集中在以下几类：
1. **GlobalErrorCode 消息定义偏差：** T4、T27 的消息文本与 OOD 10.2 不一致；T3 的消息模板机制未与异常处理管线集成。
2. **OOD 设计规范的编码遗漏：** T1、T5、T10、T21、T22 均为 OOD 明确要求但实现未遵循。
3. **旧代码残留：** T2、T12 反映 `JwtUtil` 旧代码未被 `JwtTokenProvider` 集中替换。
4. **重复代码：** T9（UserConverter vs UserFacadeImpl）、T12（userId 提取）、T26（TokenBlacklist Bean）。
5. **测试缺口：** T15-T18 四类场景缺少测试断言。

**OOD 设计覆盖度评价：** 21/26 项归因为"实现编码偏差"，但其中部分项的根因不宜完全归咎于实现侧。T3（消息模板管线）中，OOD 10.2 定义了动态消息但未设计模板插值管线的具体实现路径（BusinessException args → GlobalExceptionHandler → Result 的完整传递链未在 OOD 中显式建模），实现者需自行推导整个插值管线，该推导偏差并非纯粹的编码错误。T8（安全审计日志）中，OOD 3.1.4 仅写"记录安全日志"但未定义审计日志的存储机制、接口抽象或日志格式，实现者无从得知应使用 `log.info()`、`AuditLogger` 还是数据库审计表。T9（重复转换逻辑）中，OOD 定义了 `UserConverter` 和 `UserFacadeImpl` 但未明确指定 `UserFacadeImpl.toUserInfoResponse()` 应委托 `UserConverter`，两者间的职责划分缺少 OOD 层面的显式决策。上述三项的根因分类调整为`"实现编码偏差（OOD 设计粒度不足亦有贡献）"`，其余 18 项仍为纯实现编码偏差。单项 OOD 粒度不足不影响整体结论——OOD 文档在认证流程契约、安全策略、接口定义等核心维度已提供充分指导，局部粒度不足主要出现在 cross-cutting 基础设施（消息管线、审计日志）和工具类职责划分上。

---

## 修复执行策略

基于以上依赖关系分析，将 26 项修复整理为以下执行批次：

| 批次 | 包含项 | 策略说明 |
|------|--------|---------|
| **批次 1：阻塞修复** | T26 | 修复重复 Bean 定义使应用可启动。无前置依赖，优先执行。 |
| **批次 2：编码规范对齐** | T4, T27, T20, T14, T22 | 纯文本修改/枚举值替换/字段补充，无运行时行为变更风险，可并行修复 |
| **批次 3：JWT 认证重构** | T2, T12, T1, T10 | T2↔T12 必须同步提交（同一文件的注入切换 + userId 提取归一化）；T1 和 T10 在该文件修改后独立修复。注意 T2 将 `JwtUtil` 替换为 `JwtTokenProvider` 后，SecurityConfigPhase1 的 Bean 组装方法签名需同步调整。**本批次必须在批次 1 之后执行**，因 T2/T12 修改的 `SecurityConfigPhase1.java` 在批次 1（T26，删除重复 Bean）中已被修改，若并行执行会产生同一文件修改冲突 |
| **批次 4：消息管线修复** | T3, T13, T5, T25 | T3（消息模板插值）与 T13（dummy BCrypt）均在 AuthServiceImpl 中修改，建议同一提交以减少合并冲突。T5（刷新流程 IP 计数）也在 AuthServiceImpl 中，可一并处理。T25（状态码映射）在 GlobalExceptionHandler 中，无依赖。 |
| **批次 5：Controller 层修复** | T6, T7, T19, T21 | T6（路径变更）和 T7（SecurityContext 重构）均修改 `AuthController.java`，**需同一开发者顺序修改（同一文件）**。T19 和 T21 均修改 `MenuController.java`，同理需顺序修改。T6/T7 修改涉及 API 契约变化，需前端同步配合 |
| **批次 6：基础设施修复** | T8, T9, T11, T23 | T8 安全审计日志需先确认项目中的审计框架；T9 统一转换逻辑需确认不产生循环依赖；T11 需新增异常类型（AccountDisabledAuthenticationException）；T23 需扩展 Repository 查询方法。四者无交叉依赖。 |
| **批次 7：测试补充** | T15, T16, T17, T18 | **启动条件：批次 1-6 涉及的全部单元测试和集成测试通过后，执行者方可开始批次 7 的测试补充。** T16 与 T3 强依赖——需 T3 修复后再确认消息插值结果。 |

**批次间执行依赖：** 批次 1 → 批次 3（因 SecurityConfigPhase1.java 冲突声明）→ 批次 2, 4-6（可并行，但与批次 3 无依赖关系，批次 3 修复后其他批次可继续）→ 批次 7（功能修复验收标准满足后启动）。批次 2, 4, 5, 6 彼此无依赖，可并行执行以缩短修复周期。

---

## 逐项诊断

### T1: JwtTokenProvider@PostConstruct 缺少启动验证（P1）

- **判定：** 真实缺陷
- **根因：** 实现编码偏差
- **分析：**
  - `JwtTokenProvider.init()`（`auth/jwt/JwtTokenProvider.java:31-36`）直接调用 `Base64.getDecoder().decode(secret)`，未执行以下任一启动验证：
    - null/空值检查（secret 为 null 时 decode() 抛出 NPE，非规范 IllegalStateException）
    - Base64 解码后字节长度 >= 32 检查（`Keys.hmacShaKeyFor(keyBytes)` 虽隐式校验但异常信息为库默认文本，不符合 OOD 4.7 要求的明确消息）
    - Base64 URL-safe 字符集检查
  - `JwtUtil.init()`（`jwt/JwtUtil.java:46-60`）已完整实现 OOD 4.7 的所有三项验证，但 `JwtTokenProvider` 作为 OOD 指定的集中提供者未复用该逻辑。
  - **OOD 4.7 节要求：** secret 为空时、字节不足 32 时、含非法字符时均抛出明确的 `IllegalStateException`。
- **修改建议：**
  - 代码位置：`auth/jwt/JwtTokenProvider.java:31-36`（init() 方法）
  - 修改方式：在 `Base64.getDecoder().decode(secret)` 之前添加 null/空字符串检查，在 decode 之后添加 `keyBytes.length < 32` 检查，均抛出 IllegalStateException 并附带明确消息。URL-safe 字符集可依赖 Base64.getDecoder() 自身的校验（非法字符时抛出 IllegalArgumentException），包装为 IllegalStateException。
  - 涉及的相关文件：`JwtTokenProvider.java`
  - 验证方式：编写 `JwtTokenProviderTest`，分别注入 null secret、短 secret（<32 字节解码后）、含非法字符 secret，验证抛出 IllegalStateException 并包含对应描述。
- **风险与注意事项：**
  - JwtConfig.validate()（T10）和 JwtTokenProvider.init() 存在重叠的密钥校验逻辑。修复后应统一校验职责：JwtConfig.validate() 负责配置层校验（存在性 + 基本长度），JwtTokenProvider.init() 负责密码学层校验（解码后字节长度）。若 T10 同时修复，需协调两者不重复。

### T2: JwtAuthenticationFilter 依赖 JwtUtil（旧代码）而非 JwtTokenProvider（P1）

- **判定：** 真实缺陷
- **根因：** 实现编码偏差
- **分析：**
  - `JwtAuthenticationFilter`（`auth/security/JwtAuthenticationFilter.java:5,34,60`）注入并使用 `JwtUtil`（`jwt/JwtUtil.java`）完成 token 验证和解析。
  - OOD 1.3 明确将 `JwtTokenProvider` 定义为 JWT 令牌生成、解析、验证的集中提供者。
  - `JwtTokenProvider.validateToken()` 支持 type claim 验证（`validateToken(token, expectedType)`），但 Filter 未使用此方法，而是用 `JwtUtil.validateTokenAndGetClaims()`（无 type 过滤），然后在 Filter 层手动重复 type 检查逻辑（`auth/security/JwtAuthenticationFilter.java:68-74`）。
- **修改建议：**
  - **⚠ T2 与 T12 必须在单次提交中同步修复**，因两者均修改 `JwtAuthenticationFilter.java` 且存在操作顺序依赖：T12 的 userId 提取逻辑替换（`extractUserId` → `jwtTokenProvider.getUserIdFromClaims`）依赖于 T2 的 `JwtUtil` → `JwtTokenProvider` 注入切换。
  - 代码位置：`auth/security/JwtAuthenticationFilter.java:34,38,60,68-74,142-150`
  - 修改方式：
    1. 将 `JwtAuthenticationFilter` 的构造注入从 `JwtUtil` 改为 `JwtTokenProvider`
    2. `doFilterInternal()` 中第60行替换为 `jwtTokenProvider.validateToken(token, "access")` —— 利用 JwtTokenProvider 的内置 type claim 校验，删除 Filter 中 68-74 行的手动 type 检查
    3. 第84行 `extractUserId(claims)` 替换为 `jwtTokenProvider.getUserIdFromClaims(claims)` —— 与 T12 同步修复
    4. 删除 `extractUserId()` 方法（142-150 行）
    5. 若 `JwtUtil` 不再被其他组件引用，评估是否可废弃 JwtUtil
  - 涉及的相关文件：`JwtAuthenticationFilter.java`、`SecurityConfigPhase1.java`（更新 Bean 组装，将 JwtUtil 参数替换为 JwtTokenProvider）、`JwtUtil.java`（若无人引用则废弃）
  - 验证方式：运行现有 `SecurityConfigPhase1Test` 验证 Filter 链仍正常组装；运行集成测试验证 JWT 认证流程仍正常。
- **风险与注意事项：**
  - 若 `JwtUtil` 在其他地方仍有引用（如 JwtUtil.extractToken 静态方法在 `JwtAuthenticationFilter.extractToken()` 中被调用），不能直接删除 JwtUtil，需先提取公共静态方法到 JwtTokenProvider 或保留 extractToken 的静态工具方法。
  - 实际代码中 `JwtAuthenticationFilter.java:118` 使用 `JwtUtil.extractToken(authHeader, jwtUtil.getTokenType())`，该静态方法可保留为 JwtUtil 的公共方法，或迁移到 JwtTokenProvider。

### T3: ACCOUNT_LOCKED 消息模板未解析，客户端收到模板原文（P1）

- **判定：** 真实缺陷
- **根因：** 实现编码偏差（OOD 设计粒度不足亦有贡献）
- **分析：**
  - `GlobalErrorCode.ACCOUNT_LOCKED`（`common/.../GlobalErrorCode.java:13`）的消息定义为模板字符串 `"账户已锁定，请{锁定时间}后重试"`。
  - `AuthServiceImpl.login()` 在 IP 锁定和用户名锁定场景（`service/impl/AuthServiceImpl.java:95,99`）调用 `new BusinessException(GlobalErrorCode.ACCOUNT_LOCKED, "请30分钟后重试")` 传入动态参数，但 `BusinessException` 的 args 参数从未被消费。
  - `GlobalExceptionHandler.handleBusinessException()`（`common/.../GlobalExceptionHandler.java:27`）调用 `Result.fail(errorCode)` → `errorCode.getMessage()`，直接返回模板原文 `"账户已锁定，请{锁定时间}后重试"`，args 被丢弃。
  - **因果链：** OOD 10.2 定义了 ACCOUNT_LOCKED 的动态消息机制（IP 锁定 30 分钟 vs 用户名锁定 15 分钟），但 BusinessException → GlobalExceptionHandler → Result 的整个消息处理管线未实现模板参数插值。
- **修改建议：**
  - 代码位置：
    - `GlobalExceptionHandler.java:27`：将 `Result.fail(errorCode)` 改为带 args 的调用，如 `Result.fail(errorCode.getCode(), formatMessage(errorCode.getMessage(), e.getArgs()))`
    - 或 `Result.java` 新增 `Result.fail(ErrorCode errorCode, Object... args)` 方法
    - 或 `BusinessException` 中预先完成模板插值，将结果存入 message 字段
  - 修改方式：推荐在 `GlobalExceptionHandler` 中完成插值——因为它是消息管线的统一出口。实现一个 `formatMessage(String template, Object[] args)` 方法，将 `{锁定时间}` 替换为 args[0]。
  - 涉及的相关文件：`GlobalExceptionHandler.java`、`Result.java`（可能需要新增重载方法）
  - 验证方式：编写 `GlobalExceptionHandlerTest`，Mock 带有 args 的 BusinessException，验证响应中的 message 包含插值后的内容而非模板原文。
- **风险与注意事项：**
  - **API 响应契约影响：** 当前前端若直接从 `response.message` 字段读取模板原文 `"账户已锁定，请{锁定时间}后重试"` 作为显示文本（而非提取 args 自行构造），T3 修复后前端将收到插值文本 `"账户已锁定，请30分钟后重试"`。需确认前端是否依赖消息文本格式（如校验特定占位符模式、关键词匹配）。若前端仅将 message 展示给用户，则插值文本优于模板原文，属于向前兼容的改进。
  - 当前插值逻辑仅处理 ACCOUNT_LOCKED 这一个模板消息（`{锁定时间}` 占位符）。若后续增加更多模板消息，需确保 `formatMessage` 方法可扩展。建议使用 `MessageFormat` 或简单的 `String.replace`。
  - T3 修复后，T16 的锁定消息内容验证测试将自动通过，T16 的测试用例应调整为验证动态插值结果（IP 锁定 → 30 分钟，用户名锁定 → 15 分钟）。

### T4: UNAUTHORIZED 消息与设计规范不一致（P2）

- **判定：** 真实缺陷
- **根因：** 实现编码偏差
- **分析：**
  - `GlobalErrorCode.UNAUTHORIZED`（`GlobalErrorCode.java:9`）消息为 `"未认证"`。
  - OOD 10.2 节明确规定 UNAUTHORIZED 消息为 `"未认证或令牌已失效"`，3.3 节 AuthenticationEntryPoint 行为契约亦有相同描述。
  - 两者不一致。
- **修改建议：**
  - 代码位置：`common/.../GlobalErrorCode.java:9`
  - 修改方式：将第9行 `UNAUTHORIZED("UNAUTHORIZED", "未认证")` 改为 `UNAUTHORIZED("UNAUTHORIZED", "未认证或令牌已失效")`
  - 涉及的相关文件：`GlobalErrorCode.java`
  - 验证方式：搜索所有 `GlobalErrorCode.UNAUTHORIZED.getMessage()` 的调用点，确认测试中的预期消息已更新；运行 `GlobalErrorCodeTest` 确认编译通过。
- **风险与注意事项：**
  - T4 修复（UNAUTHORIZED 消息变更）不影响 T11 的 instanceof 判断逻辑（T11 建议采用异常类型判断而非消息字符串匹配）。但 T11 本身的字符串匹配方式存在设计级脆弱性，建议在 T11 修复中一并关注，而非作为 T4 的修复依赖。
  - 与 T27 属于同类问题（消息文本与设计规范不一致），建议和 T27 同步修复。

### T5: 刷新流程中用户禁用/删除时未递增 IP 失败计数（P2）

- **判定：** 真实缺陷
- **根因：** 实现编码偏差
- **分析：**
  - `AuthServiceImpl.refreshToken()`（`service/impl/AuthServiceImpl.java:180-184`）在发现用户 `!enabled` 或 `deleted` 时直接抛出 `BusinessException(TOKEN_REFRESH_FAILED)`，未调用 `loginAttemptTracker.recordIpFailure(clientIp)`。
  - OOD 3.1.3 步骤 7 明确规定："若用户已被禁用或被删除，需递增 LoginAttemptTracker IP 维度的失败计数，然后返回 TOKEN_REFRESH_FAILED"。
  - 缺少此调用降低了刷新场景下 IP 维度攻击检测的完整性。
- **修改建议：**
  - 代码位置：`service/impl/AuthServiceImpl.java:180-184`
  - 修改方式：在 `throw new BusinessException(GlobalErrorCode.TOKEN_REFRESH_FAILED)` 之前插入 `loginAttemptTracker.recordIpFailure(getClientIp())`
  - 涉及的相关文件：`AuthServiceImpl.java`
  - 验证方式：编写 `AuthServiceTest` 测试用例——mock 用户 enabled=false 或 deleted=true，调用 refreshToken，验证 `loginAttemptTracker.recordIpFailure()` 被调用且参数为合理 IP。
- **风险与注意事项：**
  - `refreshToken()` 方法当前未声明 `@Transactional`，但 `recordIpFailure()` 可能涉及持久化状态（取决于 LoginAttemptTracker 的实现）。需确认 LoginAttemptTracker 的线程安全性。当前 `LoginAttemptTracker` 使用 `ConcurrentHashMap`，无事务冲突风险。

### T6: ProfileUpdate 端点路径与设计不一致（P1）

- **判定：** 真实缺陷
- **根因：** 实现编码偏差
- **分析：**
  - `AuthController`（`controller/AuthController.java:70`）使用 `@PutMapping("/me")`，实际映射为 `PUT /api/auth/me`。
  - OOD 4.4 保护清单和 6.1 接口清单均定义资料更新端点为 `PUT /api/auth/profile`。
  - 路径与设计契约不一致。
- **修改建议：**
  - 代码位置：`controller/AuthController.java:70`
  - 修改方式：将 `@PutMapping("/me")` 改为 `@PutMapping("/profile")`。同时确认前端请求 URL 已匹配新路径。
  - 涉及的相关文件：`AuthController.java`、前端请求配置
  - 验证方式：启动应用后通过 `curl -X PUT http://localhost:8080/api/auth/profile` 验证返回非 404；运行 `AuthController` 相关集成测试。
- **风险与注意事项：**
  - **API 契约变更评估：**
    ① 对前端的契约影响范围：此为 API 路径变更（PUT /api/auth/me → PUT /api/auth/profile），前端请求 URL 需同步调整，否则产生 404。
    ② 版本发布协调：因路径变更导致前后端强耦合，必须在前端发版同时部署后端变更，不支持灰度过渡。
    ③ 灰度/兼容期处理：可在旧路径 `/api/auth/me` 添加 `@RequestMapping` 别名或将旧路径保留为 `@Deprecated` 临时重定向，给前端迁移缓冲期。

### T7: changePassword 端点跳过 SecurityContext，直接依赖 JwtTokenProvider（P1）

- **判定：** 真实缺陷
- **根因：** 实现编码偏差
- **分析：**
  - `AuthController.changePassword()`（`controller/AuthController.java:82-91`）在 Controller 层直接调用 `jwtTokenProvider.validateToken()` 和 `jwtTokenProvider.getUserIdFromClaims()` 获取用户 ID。
  - OOD 3.1.6 步骤 2 要求：JwtAuthenticationFilter 已验证 Access Token，Controller 应从 SecurityContext（或 CurrentUser 接口）获取当前用户 ID。
  - 此做法与 M4 修复方案不一致（M4 明确要求引入 CurrentUser 接口，Controller 通过 SecurityContext 获取用户 ID，消除对 JwtTokenProvider 的直接依赖）。
- **修改建议：**
  - 代码位置：`controller/AuthController.java:82-91`
  - 修改方式：将 `changePassword()` 方法中的 token 解析逻辑替换为从 SecurityContextHolder 获取当前用户 ID（参考 `MenuController.getCurrentUserId()` 的实现模式），删除 Controller 层的 `jwtTokenProvider` 直接调用。将 token 验证职责完全委派给 JwtAuthenticationFilter。
  - 涉及的相关文件：`AuthController.java`
  - 验证方式：编写 `AuthController` 集成测试，在 SecurityContext 中设置 Authentication，调用 changePassword 端点，验证 `authService.changePassword()` 被调用且 userId 参数与 Authentication 中的 principal 一致。
- **风险与注意事项：**
  - 删除 Controller 对 JwtTokenProvider 的依赖后，需确认 AuthController 的构造函数注入可移除 `JwtTokenProvider`（若 Controller 中其他方法仍使用则保留，但 changePassword 不应依赖）。查看代码，`AuthController` 的 `getCurrentUser()` 和 `updateProfile()` 也通过 `jwtTokenProvider` 解析 token，这些也属于 OOD 要求修正的范围，建议一并修复。

### T8: Logout 服务方法忽略 refreshToken 请求体（P1）

- **判定：** 真实缺陷
- **根因：** 实现编码偏差（OOD 设计粒度不足亦有贡献）
- **分析：**
  - `AuthService` 接口（`service/AuthService.java:13`）定义 `void logout(String token)`，未接收 refreshToken 参数。
  - `AuthServiceImpl.logout()`（`service/impl/AuthServiceImpl.java:147-164`）同样只接受 token，未处理 refreshToken。
  - `AuthController.logout()`（`controller/AuthController.java:43-52`）虽接收了 `refreshTokenRequest` 参数，但未将其传递给 `authService.logout()`。
  - OOD 3.1.4 步骤 1 和 4.4 规定：登出请求可选携带 refreshToken 字段，Phase 1 应记录安全日志。
- **修改建议：**
  - **默认行动路径（推荐）：** 新增 `SecurityAuditLogger` 组件，提供 `logAudit(String action, String operator, String target, boolean success)` 方法，将审计日志写入独立日志文件或数据库审计表。即使项目后续发现已有审计框架，`SecurityAuditLogger` 的适配成本也很低（在实现上包装为委托调用即可）。安全审计日志应满足可审计性（不可篡改、可追溯、包含操作人/时间/结果），`log.info()` 不满足审计要求。
  - **备选方案：** 若项目已有 `AuditLogger`、`AuditAspect`、`@AuditLog` 或 `AuditEventPublisher` 等现成审计框架（在 `AIMedical/backend/` 下搜索 `class.*Audit` 或检查 `common-module`/`framework` 模块），可直接复用其记录登出审计事件（含 refreshToken 掩码、操作人 userId、时间戳、操作结果）。前置调研步骤已压缩至此脚注，不影响常规修复路径。
  - 代码位置：
    - `service/AuthService.java:13`：接口签名改为 `void logout(String token, String refreshToken)`
    - `service/impl/AuthServiceImpl.java:147-164`：接收 refreshToken 参数，在方法内通过审计日志组件记录登出事件
    - `controller/AuthController.java:49`：将 `authService.logout(token)` 改为 `authService.logout(token, refreshTokenRequest != null ? refreshTokenRequest.refreshToken() : null)`
  - 涉及的相关文件：`AuthService.java`、`AuthServiceImpl.java`、`AuthController.java`
  - 验证方式：编写 `AuthControllerTest` 验证 logout 请求携带 refreshToken 时，AuthService.logout 被调用时 refreshToken 参数非 null。
- **风险与注意事项：**
  - 接口签名变更影响所有 AuthService 的实现者和调用者。目前仅 AuthServiceImpl 一个实现类，影响范围可控。若后续有 mock 测试，需同步更新 mock 调用。

### T9: UserConverter 与 UserFacadeImpl 存在重复且不一致的转换逻辑（P2）

- **判定：** 真实缺陷
- **根因：** 实现编码偏差（OOD 设计粒度不足亦有贡献）
- **分析：**
  - 两处维护了几乎相同的 `User → UserInfoResponse` 转换逻辑（`auth/converter/UserConverter.java:35-44` 和 `auth/UserFacadeImpl.java:57-66`），但存在三处不一致：
    1. **角色过滤器：** `UserConverter.resolveRole()` 未按 `Role::getEnabled` 过滤已禁用角色；`UserFacadeImpl.resolvePrimaryRole()` 有 `filter(Role::getEnabled)`。
    2. **NPE 风险：** `UserConverter.resolveRole()` 使用 `Comparator.comparingInt(Role::getSort)`（sort 为 null 时 NPE）；`UserFacadeImpl.resolvePrimaryRole()` 使用 `Comparator.nullsLast(Comparator.naturalOrder())`。
    3. **权限过滤器：** `UserConverter.resolvePermissions()` 未过滤 `PermissionFunction::getEnabled`；`UserFacadeImpl.resolvePermissions()` 有 `filter(f -> Boolean.TRUE.equals(f.getEnabled()))`。
- **修改建议：**
  - 代码位置：`auth/converter/UserConverter.java:35-44`、`auth/UserFacadeImpl.java:57-66`
  - 修改方式：将 `UserFacadeImpl` 中的 `toUserInfoResponse()` 及辅助方法提取为公共工具或将两者统一为单一实现。推荐方案：
    - 移除 `UserFacadeImpl` 中的重复转换逻辑，由 `UserFacadeImpl` 直接委托 `UserConverter` 完成转换，即 `UserFacadeImpl.toUserInfoResponse()` 直接调用 `userConverter.toUserInfoResponse(user)`
    - 或选择 `UserFacadeImpl` 的实现为正确基准，修正 `UserConverter` 的三处不一致，然后删除 `UserFacadeImpl` 中的重复代码。
  - 更推荐的方案是废弃 `UserFacadeImpl` 中的私有转换方法，统一使用 `UserConverter`，因为 `UserConverter` 是专司转换的 @Component，职责更单一。
  - 涉及的相关文件：`UserConverter.java`、`UserFacadeImpl.java`
  - 验证方式：对 `UserConverter` 补充测试，覆盖已禁用角色、已禁用权限、Role.sort 为 null 的场景，验证转换结果与 `UserFacadeImpl` 一致。
- **风险与注意事项：**
  - 若 `UserFacadeImpl` 委托 `UserConverter`，需在 `UserFacadeImpl` 中注入 `UserConverter` 可能形成循环依赖 —— 检查两个类的依赖关系。当前 `UserFacadeImpl` 和 `UserConverter` 均仅依赖 UserRepository 和 User 实体，无交叉依赖，可安全注入。

### T10: JwtConfig.validate() 检查的是原始字符串长度而非解码后字节长度（P2）

- **判定：** 真实缺陷
- **根因：** 实现编码偏差
- **分析：**
  - `JwtConfig.validate()`（`jwt/JwtConfig.java:55-58`）检查 `secret.length() < 32`——检查的是 Base64 原始字符串的字符长度。
  - OOD 4.7 节要求"Base64 解码后的字节长度 ≥ 32"（HMAC-SHA256 最小 256 位密钥）。
  - Base64 编码后长度约为解码后字节长度的 4/3 倍。原始字符串长 32 仅对应约 24 字节，不满足 256 位最小密钥要求。
  - 正确的检查应为 `Base64.getDecoder().decode(secret).length < 32`（`JwtUtil.init()` 已正确实现此检查）。
- **修改建议：**
  - 代码位置：`jwt/JwtConfig.java:55-58`
  - 修改方式：将 `secret.length() < 32` 改为 `Base64.getDecoder().decode(secret).length < 32`。注意 decode() 可能在非法 Base64 字符时抛出 `IllegalArgumentException`，需用 try-catch 包裹并提供明确异常消息。
  - 涉及的相关文件：`JwtConfig.java`
  - 验证方式：编写 `JwtConfigTest`，配置短于 32 字节解码结果的 Base64 字符串（如 32 个 'A' 字符，解码后仅 24 字节），验证抛出 IllegalStateException；配置合法长度 Base64 字符串，验证不抛异常。
- **风险与注意事项：**
  - JwtConfig.validate() 与 T1（JwtTokenProvider.init()）涉及重叠的密钥校验职责。建议的职责划分：JwtConfig.validate() 负责配置层校验（存在性 + 基本长度），JwtTokenProvider.init() 负责密码学层校验（解码后字节长度）。**交叉引用：** 执行者应同时查看 T1 的"风险与注意事项"中关于校验职责协调的描述（第 88 行），确保 T1 和 T10 的校验方案在校验边界上不矛盾。JwtConfig.validate() 执行时间在 JwtTokenProvider.init() 之前（均为 @PostConstruct，但 JwtConfig 是 @ConfigurationProperties，先于 @Component 初始化），因此 JwtConfig 的校验应限定在无需 SecretKey 解码的浅层检查。

### T11: RestAuthenticationEntryPoint 使用消息字符串匹配识别 ACCOUNT_DISABLED（P2）

- **判定：** 真实缺陷
- **根因：** 实现编码偏差
- **分析：**
  - `RestAuthenticationEntryPoint`（`auth/security/RestAuthenticationEntryPoint.java:16,27`）通过 `authException.getMessage().contains(ACCOUNT_DISABLED_MESSAGE)` 判断是否为禁用账户。
  - 若 `GlobalErrorCode.ACCOUNT_DISABLED` 的消息因国际化或多语言需求变更，此检测逻辑静默失效，禁用用户将收到 `"UNAUTHORIZED"` 而非正确的 `"ACCOUNT_DISABLED"` 响应。
  - OOD 3.3 规定 AuthenticationEntryPoint 的行为契约依赖错误码区分，但代码实现依赖消息字符串匹配，与设计意图不符。
- **修改建议：**
  - 代码位置：`auth/security/RestAuthenticationEntryPoint.java:16,27`
  - 修改方式：将消息字符串匹配改为错误码匹配。推荐方案：在 `JwtAuthenticationFilter.throwAccountDisabled()` 中将错误码信息注入到 AuthenticationException 的可区分字段中。具体方式：
    - 选项 A：在 `JwtAuthenticationFilter` 中，不通过消息字符串传递禁用状态，而是在 response 中直接设置错误码，跳过 AuthenticationEntryPoint 处理（修改 Filter 逻辑使禁用用户直接返回 ACCOUNT_DISABLED 而非抛 AuthenticationException）
    - 选项 B：自定义 `AccountDisabledAuthenticationException` 继承 `AuthenticationException`，使 Filter 抛出该具体异常类型，然后在 RestAuthenticationEntryPoint 中通过 `instanceof` 判断
  - 推荐选项 B，改动最小、语义最清晰。
  - 涉及的相关文件：`RestAuthenticationEntryPoint.java`、`JwtAuthenticationFilter.java`（新增异常类或修改抛出方式）
  - 验证方式：编写 `RestAuthenticationEntryPointTest`，分别传入 AccountDisabledAuthenticationException 和普通 AuthenticationException，验证返回的错误码分别为 ACCOUNT_DISABLED 和 UNAUTHORIZED。
- **风险与注意事项：**
  - 若采用选项 B，需新增一个异常类，此类应放在 exception 包中。注意 JwtAuthenticationFilter 目前在第 138-140 行匿名创建 AuthenticationException 子类，改为抛出具体类型后，这一匿名类可以删除。
  - 与 T4/T27 联动：T4/T27 的消息文本变更不影响此处的 instanceof 判断逻辑，因此修复 T11 后 T4/T27 的变更不会导致此处静默失效。

### T12: userId 提取逻辑重复（C3 修复未完成）（P2）

- **判定：** 真实缺陷
- **根因：** 实现编码偏差
- **分析：**
  - `JwtAuthenticationFilter.extractUserId()`（`auth/security/JwtAuthenticationFilter.java:142-150`）与 `JwtTokenProvider.getUserIdFromClaims()`（`auth/jwt/JwtTokenProvider.java:93-101`）方法体完全一致（Integer→Long 转换逻辑）。
  - OOD 8.1 节 C3 明确提出抽取 `JwtTokenProvider.getUserIdFromClaims(Claims)` 消除重复。
  - Filter 未复用后者。
- **修改建议：**
  - 代码位置：`auth/security/JwtAuthenticationFilter.java:84,142-150`
  - 修改方式：将第84行 `Long userId = extractUserId(claims)` 改为 `Long userId = jwtTokenProvider.getUserIdFromClaims(claims)`，删除 `extractUserId()` 方法（142-150行）。注意需要将 JwtTokenProvider 注入到 JwtAuthenticationFilter 中（与 T2 同步修复）。
  - 涉及的相关文件：`JwtAuthenticationFilter.java`（与 T2 同步修改）
  - 验证方式：与 T2 共享测试场景，验证 Filter 通过 JwtTokenProvider 获取 userId 与直接提取结果一致。
- **风险与注意事项：**
  - 与 T2 强制联动：T2 已要求 Filter 由依赖 JwtUtil 切换为 JwtTokenProvider，T12 是此变更的自然延伸。应一并修复，避免分步修复导致中间状态不一致。

### T13: AuthServiceImpl.login() 使用 encode() 替代 matches() 进行 dummy BCrypt 比对（P2）

- **判定：** 真实缺陷
- **根因：** 实现编码偏差
- **分析：**
  - `AuthServiceImpl.login()` 第 105 行（用户不存在场景）和第 113 行（用户禁用/删除场景）：使用 `passwordEncoder.encode("dummy")`。
  - OOD 3.1.1 节步骤 5/6 要求"对虚拟哈希值执行 dummy BCrypt 比对"以消除响应时间差异。
  - `encode()` 语义上不符合"比对"的设计意图：encode 生成新的哈希值，而 matches 执行比对操作。两者在 BCrypt 中均执行完整的哈希计算，核心差异在于语义错误而非性能。
  - `encode()` 会产生无意义的哈希值，虽然从响应时间消除的角度功能正确（encode 和 matches 均执行 BCrypt 计算，时间开销在同一量级），但语义上 violates OOD 的明确设计要求。
- **修改建议：**
  - 代码位置：`service/impl/AuthServiceImpl.java:105,113`
  - 修改方式：将 `passwordEncoder.encode("dummy")` 替换为 `passwordEncoder.matches("dummy", "$2a$10$dummyHash...")`。需在类中定义一个虚拟哈希常量，如 `private static final String DUMMY_HASH = "$2a$10$..."`（使用一个预先生成的 BCrypt 哈希值）。
  - 涉及的相关文件：`AuthServiceImpl.java`
  - 验证方式：编写 `AuthServiceTest`，对不存在的用户名和禁用用户场景，验证 login() 调用 `passwordEncoder.matches()` 而非 `encode()`。同时验证方法仍抛出 LOGIN_FAILED。
- **风险与注意事项：**
  - 虚拟哈希值应使用一个实际生成的 BCrypt 哈希，确保 matches() 不会因格式错误而抛出异常。可直接使用以下已验证的 BCrypt 哈希常量（对应明文 `"dummy"`，cost factor 10）：`$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy`。生成方法：运行 `java -cp spring-security-crypto-6.x.jar org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder "dummy"`，或使用在线 BCrypt 生成器（cost=10）。
  - 与 T3 的关系：无直接依赖。但两者均在 AuthServiceImpl.login() 中修改代码，建议在一次提交中完成以避免合并冲突。

### T14: SlidingWindowCounter 中 ReentrantLock 作用域不完整（P3）

- **判定：** 真实缺陷
- **根因：** 实现与设计约定不一致（功能正确）
- **分析：**
  - `SlidingWindowCounter`（`auth/rateLimit/SlidingWindowCounter.java:14`）的 `ReentrantLock` 仅在 `cleanup()` 方法中加锁（`auth/rateLimit/SlidingWindowCounter.java:54-60`），`tryAcquire()` 方法完全未使用该锁。
  - OOD 4.1 节要求"ReentrantLock 保护窗口内的排序集合"。
  - 实际 `tryAcquire()` 使用 `ConcurrentHashMap.compute()` 已经提供了原子性保证，锁在 tryAcquire 中并非必需。因此功能正确，不存在数据竞争或并发问题。
  - 但 OOD 设计明确选择了 ReentrantLock 作为锁策略，实现仅部分遵循设计，存在锁策略不一致。
  - **严重性为 Low：** ConcurrentHashMap.compute() 的原子性已满足 tryAcquire 的需求，锁缺失不影响功能正确性。
- **修改建议：**
  - 代码位置：`auth/rateLimit/SlidingWindowCounter.java:28-52`（tryAcquire 方法）
  - 修改方式：在 tryAcquire 方法中添加 ReentrantLock 保护，包裹 windows.compute() 操作。注意：因 ConcurrentHashMap.compute() 已提供原子性，添加锁会使锁策略与设计约定一致但不改变功能行为。
  - 备选方案：与 OOD 设计师确认是否允许将锁策略从 ReentrantLock 变更为 ConcurrentHashMap.compute() 的 CAS 原子性。若允许，则删除未使用的 lock 字段和 cleanup() 中的 lock 加解锁。
  - 验证方式：编写并发的 `SlidingWindowCounterTest`，多线程调用 tryAcquire 验证结果正确性（修改前后行为一致）。
- **风险与注意事项：**
  - 若采用添加锁方案，需注意 tryAcquire 内部 `windows.compute()` 是原子操作，在其外围加锁不会改变功能性但增加了锁竞争开销（虽然低并发场景下可忽略）。
  - 若采用删除 lock 方案，cleanup() 中的 removeIf 操作在 ConcurrentHashMap 上遍历时不会抛出 ConcurrentModificationException（ConcurrentHashMap 的迭代器是弱一致性），但 `entry.getValue().isEmpty()` 可能读到不一致状态。不过对清理操作而言，最终一致性是可接受的。

### T15: AuthServiceTest 缺少 deleted 用户登录场景（P3）

- **判定：** 真实缺陷
- **根因：** 测试覆盖不完整
- **分析：**
  - `AuthServiceTest`（`service/AuthServiceTest.java:141-155`）的 `login_shouldThrowUserDisabled()` 仅测试了 `enabled=false` 的禁用用户场景。
  - OOD 3.1.1 节步骤 6 明确要求 `deleted == true` 也应采用同等的安全策略（dummy BCrypt 比对 + 双维度失败计数 + LOGIN_FAILED 错误码）。
  - 从代码实现看，`AuthServiceImpl.login()` 第 112 行使用 `Boolean.TRUE.equals(user.getDeleted())` 检查 deleted 状态，其行为路径与 enabled=false 相同，但缺少独立测试覆盖。
- **修改建议：**
  - 代码位置：`service/AuthServiceTest.java:141-155`
  - 修改方式：新增测试方法 `login_shouldThrowLoginFailed_whenUserDeleted()`，mock 一个 deleted=true 的 User，验证 login() 抛出 `BusinessException(LOGIN_FAILED)`。可复用现有测试的断言逻辑。
  - 涉及的相关文件：`AuthServiceTest.java`
  - 验证方式：运行新增测试，验证通过。
- **风险与注意事项：**
  - 无副作用风险。新增测试是单纯的覆盖补充。

### T16: LoginAttemptTrackerTest 缺少锁定消息内容验证（P3）

- **判定：** 真实缺陷
- **根因：** 测试覆盖不完整
- **分析：**
  - `LoginAttemptTrackerTest`（`auth/login/LoginAttemptTrackerTest.java`）验证了 `isUsernameLocked()` 和 `isIpLocked()` 的 true/false 返回值，但未验证锁定后返回给用户的错误消息内容。
  - OOD 10.2 节定义：IP 维度锁定返回 `"账户已锁定，请 30 分钟后重试"`，用户名维度锁定返回 `"账户已锁定，请 15 分钟后重试"`。
  - 消息内容的生成位于 `AuthServiceImpl.login()` 中（通过 `BusinessException` 的 args 传递），因此此测试缺口跨越了 `LoginAttemptTrackerTest` 和 `AuthServiceTest` 两个测试类：前者未验证消息参数传递，后者未验证消息内容正确性。
- **修改建议：**
  - 代码位置：
    - `auth/login/LoginAttemptTrackerTest.java`：新增测试验证 `LoginAttemptTracker` 被锁定后，通过 `isIpLocked()` 和 `isUsernameLocked()` 返回 true
    - `service/AuthServiceTest.java`：新增测试验证 IP 锁定时 `BusinessException` 的 args 包含 `"请30分钟后重试"`，用户名锁定时 args 包含 `"请15分钟后重试"`
  - 修改方式：
    - LoginAttemptTrackerTest 中不直接验证消息内容（LoginAttemptTracker 不生成消息），而是验证 lock 状态
    - AuthServiceTest 中增加锁场景的消息内容断言，需同时验证 IP 锁定和用户名锁定两种插值结果
  - 注意：T3 修复后消息插值逻辑发生变化，T16 的测试应验证插值后的结果而非模板原文。
  - 验证方式：运行 AuthServiceTest 和 LoginAttemptTrackerTest 确认全部通过。
- **风险与注意事项：**
  - 与 T3 强依赖：T3 修复前，消息内容为模板原文 `"账户已锁定，请{锁定时间}后重试"`；T3 修复后，消息内容为插值结果。T16 的测试断言必须与 T3 修复后的实际行为对齐。

### T17: MenuServiceTest 未覆盖多级菜单树构建（P3）

- **判定：** 真实缺陷
- **根因：** 测试覆盖不完整
- **分析：**
  - `MenuServiceTest`（`service/MenuServiceTest.java:75-114`）的 `getUserMenuTree` 测试仅包含单层菜单返回验证（无 parent-child 关系）。
  - OOD 5.2 节定义 `MenuResponse` 支持递归 `children` 结构；6.1 节定义 `/api/menu/tree` 返回树形菜单。
  - 当前测试未覆盖：
    - parent-child 关系的树构建逻辑
    - 同级排序（sort 字段）
    - 多级嵌套场景（如 3 层以上）
    - 多个父菜单、多个子菜单的混合场景
- **修改建议：**
  - 代码位置：`service/MenuServiceTest.java`
  - 修改方式：新增测试方法覆盖以下场景：
    - `buildMenuTree_shouldNestChildUnderParent`：一个父菜单一个子菜单
    - `buildMenuTree_shouldSortSiblingsBySortOrder`：多个同级菜单验证排序
    - `buildMenuTree_shouldSupportThreeLevelHierarchy`：三级嵌套
    - `buildMenuTree_shouldHandleMultipleParents`：多个父菜单下各有子菜单
  - Mock PermissionFunction 的 parent 关系时，注意通过 parentId 而非 parent 对象模拟（MenuServiceImpl.buildMenuTree 通过 parentIdMap 构建树）。
  - 验证方式：运行所有 MenuServiceTest 测试，确认覆盖多级树构建。
- **风险与注意事项：**
  - 测试中 mock 的 PermissionFunction 实例需正确设置 id、parentId、sortOrder 等字段。注意 `convertToMenuResponse` 中 `component` 硬编码为 null（T22 修复前），测试断言中 component 应预期为 null。

### T18: SecurityConfigPhase1Test 未测试 Filter 执行顺序（P3）

- **判定：** 真实缺陷
- **根因：** 测试覆盖不完整
- **分析：**
  - `SecurityConfigPhase1Test`（`auth/security/SecurityConfigPhase1Test.java`）仅验证了各 Bean 是否被创建（非 null 断言）。
  - OOD 3.3 节明确规定了 Filter 执行顺序：`GlobalRateLimitFilter` 最先 → `JwtAuthenticationFilter` → `PasswordChangeCheckFilter` 最后。
  - 当前测试未通过 `HttpSecurity` 配置验证 Filter 链的注册顺序。顺序错误会导致安全漏洞（例如限流在 JWT 认证之后执行）。
- **修改建议：**
  - 代码位置：`auth/security/SecurityConfigPhase1Test.java`
  - 修改方式：新增测试验证 `SecurityFilterChain` 的 Filter 注册顺序：
    1. 创建 `SecurityConfigPhase1` 实例
    2. Mock `HttpSecurity`（或使用 Spring Boot 测试切片 `@AutoConfigureMockMvc` + `@Import(SecurityConfigPhase1.class)`）
    3. 调用 `filterChain()` 方法获取 SecurityFilterChain
    4. 通过 `SecurityFilterChain.getFilters()` 获取已注册的 Filter 列表，逐项 assert 类型顺序
  - 更简单的方法：使用 `@WebMvcTest` + `@Import(SecurityConfigPhase1.class)`，通过 MockMvc 验证 Filter 链是否包含预期 Filter 类型。
  - 验证方式：运行 SecurityConfigPhase1Test 确认 Filter 顺序断言通过。
- **风险与注意事项：**
  - Filter 链的注册通过 `addFilterBefore` / `addFilterAfter` 实现 — SecurityConfigPhase1.java:97-99。测试应验证这三个过滤器的相对顺序而非绝对索引位置（因为 Spring Security 还会插入默认 Filter）。

### T19: 菜单更新端点使用 PUT 而非 PATCH 方法（P2）

- **判定：** 真实缺陷
- **根因：** 实现编码偏差
- **分析：**
  - `MenuController.update()`（`controller/MenuController.java:117`）使用 `@PutMapping("/{id}")`。
  - OOD 4.4 节和 6.1 节要求 `PATCH /api/menu/{id}` 实现局部更新语义（RFC 7231 §4.3.4）。
  - `MenuUpdateRequest` 采用 POJO + `@JsonInclude(NON_NULL)` 设计，明确为局部更新 PATCH 语义服务，与 HTTP 方法矛盾。
- **修改建议：**
  - 代码位置：`controller/MenuController.java:117`
  - 修改方式：将 `@PutMapping("/{id}")` 替换为 `@PatchMapping("/{id}")`，并新增 import `org.springframework.web.bind.annotation.PatchMapping`。
  - 涉及的相关文件：`MenuController.java`
  - 验证方式：通过 `curl -X PATCH http://localhost:8080/api/menu/{id}` 验证返回 200；`curl -X PUT http://localhost:8080/api/menu/{id}` 验证返回 405。
- **风险与注意事项：**
  - **API 契约变更评估：**
    ① 对前端的契约影响范围：HTTP 方法由 PUT 变更为 PATCH，前端请求方法需同步调整，否则产生 405 Method Not Allowed。
    ② 版本发布协调：需在前端发版同时部署后端变更，不支持单独灰度。
    ③ 灰度/兼容期处理：可临时同时暴露 PUT 和 PATCH 两个端点（`@PutMapping` 保留并委托给同一方法），待前端迁移完成后移除 PUT 端点。

### T20: 删除菜单时错误码使用 PARAM_INVALID 而非设计约定的 CHILDREN_EXIST（P2）

- **判定：** 真实缺陷
- **根因：** 实现编码偏差
- **分析：**
  - `MenuServiceImpl.deleteMenu()`（`service/impl/MenuServiceImpl.java:165`）使用 `GlobalErrorCode.PARAM_INVALID`。
  - OOD 10.1 节和 6.1 节要求有子菜单阻止删除时返回 `ErrorCode.CHILDREN_EXIST`（HTTP 400）。
  - 前端无法据此区分"参数错误"和"子菜单阻止删除"两种场景，无法给出针对性的用户提示。
- **修改建议：**
  - 代码位置：`service/impl/MenuServiceImpl.java:165`
  - 修改方式：将 `GlobalErrorCode.PARAM_INVALID` 替换为 `GlobalErrorCode.CHILDREN_EXIST`。
  - 涉及的相关文件：`MenuServiceImpl.java`（`GlobalErrorCode.CHILDREN_EXIST` 已存在于枚举中，无需新增）
  - 验证方式：编写 `MenuServiceTest`，mock 存在子菜单的场景，调用 deleteMenu()，验证 BusinessException 的 errorCode 为 `GlobalErrorCode.CHILDREN_EXIST`。
- **风险与注意事项：**
  - `CHILDREN_EXIST` 枚举值已存在（`GlobalErrorCode.java:23`），消息为 `"存在子菜单，无法删除"`，与当前硬编码的消息 `"存在子菜单，无法删除，请先删除子菜单"` 略有差异。需确认是否接受 OOD 约定的消息，或在 BusinessException 的 args 中补充额外信息。

### T21: MenuController 缺少路径 id 与请求体 id 的一致性校验（P2）

- **判定：** 真实缺陷
- **根因：** 实现编码偏差
- **分析：**
  - `MenuController.update()`（`controller/MenuController.java:117-125`）接收路径参数 `{id}` 和请求体 `MenuUpdateRequest`，但未校验 `request.getId()` 与路径 `id` 是否一致。
  - OOD 5.2 节要求：`PATCH /api/menu/{id}` 的路径参数 `{id}` 与请求体中的 `id` 字段（若携带）必须相同，不一致时返回 400（PARAM_INVALID）。
  - 当前实现完全忽略了此一致性校验。
- **修改建议：**
  - 代码位置：`controller/MenuController.java:119`（update 方法体起始处）
  - 修改方式：在 `menuService.updateMenu(id, request)` 调用前插入校验：
    ```java
    if (request.getId() != null && !request.getId().equals(id)) {
        return Result.fail(GlobalErrorCode.PARAM_INVALID);
    }
    ```
  - 涉及的相关文件：`MenuController.java`
  - 验证方式：编写 `MenuControllerTest`，传入路径 id=1 但请求体中 id=2，验证返回 400 且 errorCode 为 PARAM_INVALID。
- **风险与注意事项：**
  - 请求体 `MenuUpdateRequest` 的 `id` 字段通过 `getter` 获取，需确认 DTO 中该字段的类型为 Long（与路径参数类型一致）。
  - 与 T19 独立，可各自单独修复。

### T22: PermissionFunction 实体缺少 component 字段映射（P1）

- **判定：** 真实缺陷
- **根因：** 实现编码偏差
- **分析：**
  - `PermissionFunction` 实体（`permission/PermissionFunction.java`）未声明 `component` 字段。
  - `sys_function` 表存在 `component` 列（存储前端组件路径如 `Layout`、`system/user/index`），但实体未映射。
  - 导致：
    - `MenuResponse` 中的 `component` 始终为 `null`（`MenuServiceImpl.convertToMenuResponse()` 硬编码 `null`）
    - `MenuUpdateRequest` 中的 `component` 变更无法持久化
    - `data.sql` 中写入的 `component` 值无法被业务代码读取
- **修改建议：**
  - 代码位置：
    - `permission/PermissionFunction.java`：新增 `private String component;` 字段，添加 @Column 注解（若列名为 component 则无需显式指定）
    - `MenuServiceImpl.java:182-192`：`convertToMenuResponse()` 中将 `null` 替换为 `function.getComponent()`
  - 涉及的相关文件：`PermissionFunction.java`、`MenuServiceImpl.java`、`MenuResponse.java`（确认 `MenuResponse` 的 `component` 字段）——检查 `MenuResponse` 是否为 record 或 POJO，确保 getter 可访问 `component`。同时 `MenuUpdateRequest` 如已有 `component` 字段（通过 setter 映射），更新后即可正常持久化。
  - 验证方式：集成测试 —— 创建 PermissionFunction 并设置 component 值，通过 `MenuServiceImpl.getMenuById()` 验证返回的 MenuResponse.component 不为 null。涉及 `MenuUpdateRequest` 更新的场景，验证 update 后数据库 component 列正确更新。
- **风险与注意事项：**
  - 新增字段后需添加 getter/setter 方法。`PermissionFunction` 当前未使用 Lombok，需手动添加。推荐显式添加 `@Column(name = "component")` 以提高代码可读性并避免 Hibernate 元模型中的潜在歧义。

### T23: getUserMenuTree 未使用 @EntityGraph，存在 N+1 查询风险（P1）

- **判定：** 真实缺陷
- **根因：** 实现编码偏差
- **分析：**
  - `MenuServiceImpl.getUserMenuTree()`（`service/impl/MenuServiceImpl.java:44`）使用 `userRepository.findById(userId)`（无 EntityGraph），随后遍历 `user.getPosts()`（触发懒加载查询）和 `post.getFunctions()`（触发额外 M 次查询）。
  - `UserRepository`（`permission/UserRepository.java:16-17`）已定义 `findWithDetailsById` 方法带有 `@EntityGraph(attributePaths = {"roles", "posts"})`，但代码未使用。
  - 对于一个用户有 M 个岗位、每个岗位有 N 个功能的场景，查询次数分析如下：
    - 标准 JPA 懒加载下：`userRepository.findById(userId)` 1 次 → `user.getPosts()` 1 次（Hibernate 一次性加载整个集合，而非 M 次）→ 每个 `post.getFunctions()` M 次，合计 `1 + 1 + M = M + 2`
    - 使用 `findWithDetailsById`（EntityGraph 覆盖 `roles`、`posts`）修复后：1 次关联查询加载 user+posts → M 次 `post.getFunctions()` 懒加载，合计 `1 + M`
    - 完整 `@Query JOIN FETCH` 修复后：1 次 JOIN 查询加载全部三层
  - **数据量级估算：** 当前项目的角色-菜单体系为管理后台场景，岗位数通常在 1-5 之间（一般用户仅 1 个主岗），每个岗位关联的功能数在 10-50 之间（按页面按钮级粒度）。因此在懒加载下查询次数约为 `(1+1+M) = 3~7 次`，JOIN FETCH 优化后可降为 `1 次`。单次请求节省 2-6 次额外查询，在管理后台低并发场景下，3-7 次 SQL 查询对响应时间影响有限（单次查询约 1-5ms，合计 3-35ms）。此性能问题在数据量增长至岗位数 >10 或功能数 >100 时才具有实际业务影响。
  - **优先级判定依据：** T23 与 T26 的 P0 定位不同。T26 为**启动阻塞**（应用完全无法启动），属于绝对阻断；T23 为**性能风险**（仅在特定数据量级下产生可感知延迟）。根据当前数据量级估算，T23 降为 P1（核心业务流程缺陷），与 T26 的 P0（启动阻塞/数据安全性类）保持可区分度。
- **修改建议：**
  - 代码位置：`service/impl/MenuServiceImpl.java:44`、`permission/UserRepository.java`
  - 修改方式：两步修复：
    1. 将 `userRepository.findById(userId)` 替换为 `userRepository.findWithDetailsById(userId)`。
    2. 在 `UserRepository` 中扩展 `findWithDetailsById` 的 `@EntityGraph`，新增 `posts.functions` 属性路径，或新增 `@Query("SELECT u FROM User u JOIN FETCH u.roles JOIN FETCH u.posts JOIN FETCH u.posts.functions WHERE u.id = :id")` 方法彻底消除 `post.getFunctions()` 的懒加载。
    3. 若确认数据量级很小（用户岗位数 ≤3 且每个岗位功能数 ≤20），可在 EntityGraph 中仅保留 `posts` 属性路径，在风险与注意事项中注明此决策依据并标注为已知剩余优化空间。
  - 涉及的相关文件：`MenuServiceImpl.java`、`UserRepository.java`
  - 验证方式：开启 SQL 日志（`spring.jpa.show-sql=true`），调用 `getUserMenuTree()` 并验证只产生 1 条 JOIN 查询而非 N+1 条查询。
- **风险与注意事项：**
  - `findWithDetailsById` 的 `@EntityGraph` 只 join 了 `roles` 和 `posts`，未直接 join `posts.functions`。因此 `post.getFunctions()` 仍可能在遍历时触发懒加载，这是本修复方案第二步要解决的问题。

### T25: RATE_LIMITED / ACCOUNT_LOCKED HTTP 状态码映射缺失 429（P1）

- **判定：** 真实缺陷
- **根因：** 实现编码偏差
- **分析：**
  - `GlobalExceptionHandler.resolveHttpStatus()`（`common/.../GlobalExceptionHandler.java:38-57`）仅映射了 `UNAUTHORIZED`(401)、`FORBIDDEN`(403)、`NOT_FOUND`(404)、`PARAM_INVALID`(400)、`SYSTEM_ERROR`(500) 五种错误码，其余全部走默认 `HttpStatus.BAD_REQUEST`(400)。
  - OOD 10.1 节规定：
    - `RATE_LIMITED` 应返回 HTTP 429
    - `ACCOUNT_LOCKED` 应返回 HTTP 429
    - `TOKEN_REFRESH_FAILED` 应映射到 401
  - 当前三种错误码均被映射为 400。
- **修改建议：**
  - 代码位置：`common/.../GlobalExceptionHandler.java:38-57`（resolveHttpStatus 方法）
  - 修改方式：在方法中增加三个映射分支：
    - `GlobalErrorCode.RATE_LIMITED.getCode()` → 返回 `HttpStatus.TOO_MANY_REQUESTS`（429）
    - `GlobalErrorCode.ACCOUNT_LOCKED.getCode()` → 返回 `HttpStatus.TOO_MANY_REQUESTS`（429）
    - `GlobalErrorCode.TOKEN_REFRESH_FAILED.getCode()` → 返回 `HttpStatus.UNAUTHORIZED`（401）
  - 涉及的相关文件：`GlobalExceptionHandler.java`
  - 验证方式：编写 `GlobalExceptionHandlerTest`，分别 mock 带有 RATE_LIMITED、ACCOUNT_LOCKED、TOKEN_REFRESH_FAILED 的 BusinessException，验证响应状态码分别为 429、429、401。
- **风险与注意事项：**
  - **API 契约变更评估：**
    ① 对前端的契约影响范围：HTTP 状态码由 400 变更为 429（限流/锁定场景），前端若已有 429 处理逻辑（如显示"请稍后重试"）则可正常适配；若前端仅处理 400，需补充 429 的异常提示。
    ② 版本发布协调：需确保前端已适配 429 后再部署后端，否则用户可能看到未处理的异常提示。
    ③ 灰度/兼容期处理：若前端无法同步更新，可临时将 RATE_LIMITED/ACCOUNT_LOCKED 保留 400 状态码，在后续版本再切换至 429。

### T26: AuthModuleConfig 与 SecurityConfigPhase1 存在重复的 TokenBlacklist Bean 定义（P0）

- **判定：** 真实缺陷
- **根因：** 实现编码偏差
- **分析：**
  - `AuthModuleConfig`（`auth/config/AuthModuleConfig.java:19-22`）定义了 `@Bean TokenBlacklist tokenBlacklist()`，无 profile 限制。
  - `SecurityConfigPhase1`（`auth/security/SecurityConfigPhase1.java:41-44`）定义了 `@Bean TokenBlacklist tokenBlacklist()`，且带有 `@Profile("phase1")`。
  - Spring Boot 3.x 默认禁止 bean 覆盖（`spring.main.allow-bean-definition-overriding=false`），当 phase1 profile 激活时两个同名的 `tokenBlacklist` Bean 定义冲突，导致 `BeanDefinitionOverrideException` 启动失败。
- **修改建议：**
  - 代码位置：`auth/config/AuthModuleConfig.java:19-22` 或 `auth/security/SecurityConfigPhase1.java:41-44`
  - 修改方式（二选一）：
    - 选项 A（推荐）：删除 `SecurityConfigPhase1` 中的 `tokenBlacklist()` Bean 定义（第41-44行），依赖 `AuthModuleConfig` 中已有的 Bean。因为 `AuthModuleConfig` 是无 profile 限制的全局配置，而 `SecurityConfigPhase1` 的职责应限于安全 Filter 链配置。
    - 选项 B：删除 `AuthModuleConfig` 中的 `tokenBlacklist()` Bean，让 `SecurityConfigPhase1` 作为 phase1 profile 下的唯一提供者。但此时非 phase1 profile 下无法获取 `TokenBlacklist` Bean。
  - 推荐选项 A，因为 `RateLimitGuard` 和 `LoginAttemptTracker` 的 Bean 已在 `AuthModuleConfig` 中定义，`TokenBlacklist` 放在一起符合关注点分离。
  - 涉及的相关文件：`AuthModuleConfig.java`、`SecurityConfigPhase1.java`
  - 验证方式：删除后启动应用并激活 phase1 profile，验证不再抛出 `BeanDefinitionOverrideException`。运行所有测试确认无 Bean 依赖断裂。
- **风险与注意事项：**
  - 若删除 `SecurityConfigPhase1` 中的 Bean 定义，需确认 `SecurityConfigPhase1.jwtAuthenticationFilter()` 方法不再通过参数注入 `TokenBlacklist` —— 检查第52-55行，`jwtAuthenticationFilter(JwtUtil jwtUtil, TokenBlacklist tokenBlacklist, UserRepository userRepository)` 仍可通过 Spring 容器从 `AuthModuleConfig` 中获取 TokenBlacklist，因此操作安全。
  - 与 T2/T12 的联动：当 T2 修复（Filter 由 JwtUtil 切换到 JwtTokenProvider）后，SecurityConfigPhase1 中 jwtAuthenticationFilter 方法的签名发生变化，需同步调整 Bean 定义。

### T27: FORBIDDEN 消息与设计约定不一致（P2）

- **判定：** 真实缺陷
- **根因：** 实现编码偏差
- **分析：**
  - `GlobalErrorCode.FORBIDDEN`（`GlobalErrorCode.java:10`）消息为 `"无权限"`。
  - OOD 10.2 节规定 FORBIDDEN 的错误消息为 `"无权限访问"`。
- **修改建议：**
  - 代码位置：`common/.../GlobalErrorCode.java:10`
  - 修改方式：将第10行 `FORBIDDEN("FORBIDDEN", "无权限")` 改为 `FORBIDDEN("FORBIDDEN", "无权限访问")`
  - 涉及的相关文件：`GlobalErrorCode.java`
  - 验证方式：搜索所有 `GlobalErrorCode.FORBIDDEN.getMessage()` 的调用点，确认测试中的预期消息已更新。运行 `GlobalErrorCodeTest` 确认编译通过。
- **风险与注意事项：**
  - 纯文案修改，无技术风险。与 T4 属于同类问题，建议同步修复。

---

## 修订说明（v2）

| 质询意见 | 回应 |
|---------|------|
| Q1：完整缺失「修改建议」，未响应需求第 3 项定位目标 | 已对每一 T 项补充「修改建议」小节，包含代码位置、修改方式、涉及文件、验证方式。 |
| Q2：汇总数据数值错误与内部矛盾（27 项实为 26 项，无 T24；分类计数不一致） | 已修正：确认 todo.md 中无 T24，总计 26 项（T1-T23, T25-T27）。实现编码偏差修正为 21 项，设计与实现不一致维持 1 项，测试覆盖不完整修正为 4 项。 |
| Q3：缺少优先级排序 | 已在汇总表增加「优先级」列，按 P0（阻塞启动/性能）到 P5（纯文案）分级。T26 设 P0（启动阻塞），T23 设 P0（N+1 性能问题），T27 设 P2（消息文本不严重影响功能）——具体分级已在表中逐项标注。 |
| Q4：未评估修复方案的潜在副作用 | 已对每个 T 项在「修改建议」后补充「风险与注意事项」小节。T2 评估了 JwtUtil 废弃条件和 extractToken 静态方法迁移；T26 评估了删除重复 Bean 定义对其他 Bean 组装的影响；T22 评估了 Hibernate 的 component 字段名歧义问题；T9 评估了循环依赖风险。 |
| Q5：T13 encode() vs matches() 性能分析存在事实偏差 | 已修正：删除了关于性能对比的错误论述（"encode() 通常比 matches() 慢"、"可能引入反向的时序差异"）。将根因聚焦于语义错误：「encode() 语义上不符合'比对'的设计意图」和「encode() 会产生无意义的哈希值」。补充了使用 `matches("dummy", "$2a$10$dummyHash...")` 的建议。 |
| Q6：T14 根因分类「OOD 设计与实现不一致」使用不够精确 | 已将 T14 的根因细化为「实现与设计约定不一致（功能正确）」。在分析中明确说明 ConcurrentHashMap.compute() 已提供原子性，锁缺失不影响功能正确性，仅违反设计约定的锁策略。严重性标注为 Low。 |
| Q7：未分析缺陷之间的修复依赖关系 | 已在汇总表增加「相关项/修复依赖」列。识别了以下依赖组：T2↔T12（同一文件，userId 提取归一化）、T3↔T16（T3 修复后 T16 的测试内容将改变）、T4/T27↔T11（消息文本修改会使 T11 的字符串匹配静默失效）。建议执行者按依赖组修复。 |

## 修订说明（v3）

| 质询意见 | 回应 |
|---------|------|
| Q1：汇总表中 T27 重复出现，造成数据混乱 | 已删除 P5 行「T27文本对齐」条目。T27 的消息文本修正属于 P2（与 T4 同类偏差对齐），保留 P2 行并删除冗余的 P5 行。统计行数值维持「26/26 项」不变（T27 重复条目不计入独立项）。 |
| Q2：T14 优先级与正文严重性自相矛盾 | 已将汇总表中 T14 的优先级由 P2 降为 P3，与正文中 Low 严重性描述对齐。正文中「严重性为 Low」的独立标注保留作为分析上下文（已不再与优先级矛盾）。 |
| Q3：T23 修复建议不完整——未解决深层 N+1 链 | 已将 T23 修改建议扩展为两步修复方案：第一步替换 `findById` 为 `findWithDetailsById`；第二步在 `UserRepository` 中扩展 EntityGraph 或新增 `@Query` 方法彻底消除 `post.getFunctions()` 的懒加载。同时补充了数据量级较小时的简化决策依据说明。 |
| Q4：T2/T12 依赖组缺少显式同步修复指令 | 已在 T2 修改建议开头添加显式说明「T2 与 T12 必须在单次提交中同步修复」，并解释了操作顺序依赖关系（T12 的 userId 提取替换依赖于 T2 的注入切换）。 |
| Q5：T8 安全日志实现方案不满足审计要求 | 已替换 T8 修改建议：不再直接给出 `log.info()` 代码示例，改为前置调研提示 + 两个备选方案（复用现有 AuditLogger / 新增 SecurityAuditLogger 组件）。明确审计日志应满足可审计性要求（不可篡改、可追溯、结构化字段）。 |
| Q6：T3/T16 修复未评估对 API 消费者的响应契约影响 | 已在 T3「风险与注意事项」中补充 API 响应消息从模板原文变为插值文本的契约影响分析（确认前端是否依赖于消息文本格式）。T16 的测试建议已明确说明需同时验证 IP 锁定和用户名锁定两种插值结果。 |
| Q7：API 契约变更类修复的风险处理深度不一致 | 已统一 T6（路径变更）、T19（HTTP 方法变更）、T25（状态码变更）的风险评估模板，每项变更的「风险与注意事项」均包含三项分析：① 对前端的契约影响范围；② 是否需要版本发布协调；③ 是否存在灰度/兼容期处理方案。 |

## 修订说明（v4）

| 质询意见 | 回应 |
|---------|------|
| Q1：T23 N+1 查询次数公式 `1 + M + M×N` 存在事实错误 | 已修正。标准 JPA 懒加载下：`user.getPosts()` 为集合一次性加载（1 次查询），而非每个岗位 1 次（M 次），正确公式为 `1 + 1 + M = M + 2`。同时在分析中补充了 EntityGraph 和 `@Query JOIN FETCH` 两种修复后的查询模式说明。 |

## 修订说明（v5）

| 质询意见 | 回应 |
|---------|------|
| Q1：汇总表 T4 的「相关项/修复依赖」标注与正文分析相矛盾 | 已删除 T4 汇总表行中的 "T11（消息变更影响字符串匹配）" 依赖标记。T4 正文的「风险与注意事项」中已修改为：明确说明 T4 修复不影响 T11，但 T11 本身的字符串匹配方式存在设计级脆弱性，建议在 T11 修复中一并关注。 |
| Q2：汇总表 T11 的「相关项/修复依赖」标注方向倒置 | 已将 T11 汇总表的标注从 "T4/T27（消息变更使匹配静默失效）" 改为 "T4/T27（T11 修复前，文本变更会造成匹配静默失效）"，明确限定前提条件。 |
| Q3：T23 P0 优先级与 T26（启动阻塞）同级别缺乏可比性论证 | 已在 T23 分析中补充了数据量级估算（岗位数 1-5，功能数 10-50，查询次数 3-7 次），说明当前数据量级下的性能影响可控。据此将 T23 优先级从 P0 降为 P1（核心业务流程缺陷），与 T26 的 P0（启动阻塞）保持可区分度。P0 定义调整为仅覆盖"启动阻塞/数据安全性"类问题。 |
| Q4：T22 @Column 修改建议存在内部矛盾 | 已删除前半句"若列名为 component 则无需显式指定"，统一为"推荐显式添加 @Column(name = 'component') 以提高代码可读性并避免 Hibernate 元模型中的潜在歧义"。 |
| Q5：修复执行顺序缺乏批次编排建议 | 已新增「修复执行策略」章节，将 26 项按依赖关系打包为 7 个执行批次（阻塞修复 → 编码规范对齐 → JWT 认证重构 → 消息管线修复 → Controller 层修复 → 基础设施修复 → 测试补充），并明确批次间执行依赖和并行策略。 |
| Q6：T8 修改建议的前置调研可能阻塞执行启动 | 已在前置调研中补充了快速确认审计框架是否存在的三步检查方法（全局搜索类名 → 检查 common-module/framework → 默认选方案 B）。方案 B（新增 SecurityAuditLogger）标注为默认选择，即使项目后续发现已有框架，适配成本也很低。 |
| Q7：T13 虚拟哈希常量未给出具体值或生成指引 | 已提供验证可用的 BCrypt 哈希常量（对应明文 "dummy"，cost=10）：`$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy`，并附 Java 命令和在线生成器两种生成指引。 |

## 修订说明（v6）

| 质询意见 | 回应 |
|---------|------|
| Q1：T14 详细诊断标题的优先级标注与汇总表矛盾（标题标 P2，汇总表标 P3） | 已将第 319 行标题后缀 `（P2）` 改为 `（P3）`，与汇总表及 v3 修订说明中已做的降级保持一致。三者均已统一为 P3。 |
| Q2：修复执行策略对跨批次修改同一文件的串行依赖声明不足（批次 1/3 均改 SecurityConfigPhase1.java；批次 5 内 T6/T7 均改 AuthController.java） | 已在批次 3 说明中明确声明"本批次必须在批次 1 之后执行"（因同一文件修改冲突）。批次 5 说明中补充 T6/T7 需同一开发者顺序修改（同一文件），T19/T21（MenuController.java）同理。批次间执行依赖图已更新为：批次 1 → 批次 3（因冲突声明）→ 批次 2,4-6（可并行）。 |
| Q3：需求第 2 项"根因分析"中 OOD 设计文档维度未被有效响应（T3/T8/T9 的 OOD 粒度不足亦有贡献） | 已在「模式识别」章节新增"OOD 设计覆盖度评价"段落，对 T3（消息模板管线未设计插值机制）、T8（安全日志无存储抽象）、T9（UserConverter/UserFacadeImpl 职责划分无显式决策）逐一分析其 OOD 粒度不足。三者的根因分类已从"实现编码偏差"调整为"实现编码偏差（OOD 设计粒度不足亦有贡献）"。汇总表统计和概述统计同步更新（纯实现编码偏差 18 项 + 含 OOD 贡献 3 项）。 |
| Q4：T1 与 T10 的密钥校验职责划分方案缺少跨项协调指引（T1 已提及 T10 但 T10 未交叉引用 T1） | 已在 T10 的「风险与注意事项」中补充对 T1 的显式交叉引用，给出建议的职责划分方案：JwtConfig.validate() 负责配置层校验（存在性 + 基本长度），JwtTokenProvider.init() 负责密码学层校验（解码后字节长度）。 |
| Q5：T8 的修改建议中前置调研与默认方案的优先级不明确 | 已重构 T8 修改建议结构：将默认行动路径（方案 B：新增 SecurityAuditLogger）作为首要方案并标注"推荐"，原有三步调研步骤压缩为单行脚注作为备选方案的补充说明。 |
| Q6：批次 7（测试补充）的启动条件未明确规格化 | 已在批次 7 描述中补充启动条件："批次 1-6 涉及的全部单元测试和集成测试通过后，执行者方可开始批次 7 的测试补充"。 |
