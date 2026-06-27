# 诊断报告：OOD Phase1_B 实现审查问题分析（v7）

## 概述

基于审查报告 `Harness/reviews/202606261757_ood_phase1_B_code_review/todo.md` 所列 26 项问题（T1-T23, T25-T27），逐项核查代码实现与 OOD 设计文档 `Docs/05_ood_phase1_B.md` 的一致性。核查范围包括 `AIMedical/backend/modules/common-module/common-module-impl/` 下的实现代码及 `AIMedical/backend/common/` 下的基础设施代码，以及 `AIMedical/frontend/` 前端代码。

---

## 汇总

| 优先级 | 编号 | 判定 | 根因归属 | 影响模块 | 相关项/修复依赖 |
|--------|------|------|---------|---------|----------------|
| P0 | T26 | 真实缺陷 | Spring 配置冲突 | AuthModuleConfig / SecurityConfigPhase1 | - |
| P1 | T23 | 真实缺陷 | 实现编码偏差 | MenuServiceImpl | - |
| P1 | T1 | 真实缺陷 | 实现编码偏差 | JwtTokenProvider | - |
| P1 | T2 | 真实缺陷 | 实现编码偏差 | JwtAuthenticationFilter | T12（同一文件，userId提取） |
| P1 | T3 | 真实缺陷 | 实现编码偏差（OOD 设计粒度不足亦有贡献） | GlobalExceptionHandler / GlobalErrorCode | - |
| P1 | T6 | 真实缺陷 | 实现编码偏差 | AuthController | - |
| P1 | T7 | 真实缺陷 | 实现编码偏差 | AuthController | - |
| P1 | T8 | 真实缺陷 | 实现编码偏差（OOD 设计粒度不足亦有贡献） | AuthService / AuthServiceImpl | - |
| P1 | T22 | 真实缺陷 | 实现编码偏差（OOD 设计粒度不足亦有贡献） | PermissionFunction | - |
| P1 | T25 | 真实缺陷 | 实现编码偏差 | GlobalExceptionHandler | - |
| P2 | T4 | 真实缺陷 | 实现编码偏差 | GlobalErrorCode | T27（同类消息偏差） |
| P2 | T5 | 真实缺陷 | 实现编码偏差 | AuthServiceImpl | - |
| P2 | T9 | 真实缺陷 | 实现编码偏差（OOD 设计粒度不足亦有贡献） | UserConverter / UserFacadeImpl | - |
| P2 | T10 | 真实缺陷 | 实现编码偏差 | JwtConfig | T1（同为JWT启动验证） |
| P2 | T11 | 真实缺陷 | 实现编码偏差 | RestAuthenticationEntryPoint | T4/T27（T11 修复前，文本变更会造成匹配静默失效） |
| P2 | T12 | 真实缺陷 | 实现编码偏差 | JwtAuthenticationFilter | T2（同一文件，集中JwtTokenProvider） |
| P2 | T13 | 真实缺陷 | 实现编码偏差 | AuthServiceImpl | T3（同为消息管线）、T16（测试内容随T3修复变化） |
| P2 | T19 | 真实缺陷 | 实现编码偏差 | MenuController | - |
| P2 | T20 | 真实缺陷 | 实现编码偏差 | MenuServiceImpl | - |
| P2 | T21 | 真实缺陷 | 实现编码偏差 | MenuController | - |
| P2 | T27 | 真实缺陷 | 实现编码偏差 | GlobalErrorCode | T4（同类消息偏差） |
| P3 | T14 | 真实缺陷 | 实现与设计约定不一致（功能正确） | SlidingWindowCounter | - |
| P3 | T15 | 真实缺陷 | 测试覆盖不完整 | AuthServiceTest | - |
| P3 | T16 | 真实缺陷 | 测试覆盖不完整 | LoginAttemptTrackerTest / AuthServiceTest | T3（T3修复后T16的测试内容将改变） |
| P3 | T17 | 真实缺陷 | 测试覆盖不完整 | MenuServiceTest | - |
| P3 | T18 | 真实缺陷 | 测试覆盖不完整 | SecurityConfigPhase1Test | - |

**统计：** 26/26 项确认为真实缺陷（0 项误报）。其中 17 项根因归属于纯实现编码偏差，4 项归属于实现编码偏差（OOD 设计粒度不足亦有贡献）（T3/T8/T9/T22），1 项归属于设计与实现约定不一致（T14），4 项归属于测试覆盖不完整（T15/T16/T17/T18），1 项归属于 Spring 配置冲突（T26）。

**OOD 设计覆盖度评价（反事实分析全覆盖）：** 以下逐项分析 OOD 文档若更完整/精确，实现者是否能避免偏差：

### OOD 维度充分（纯实现编码偏差，17 项）

以下 17 项的根因完全归属于实现侧——OOD 文档提供了完整且精确的设计指引，实现者只需遵循即可避免偏差：

| 编号 | OOD 所在章节 | 设计指引充分性 |
|------|-------------|---------------|
| **T1** | 4.7 节 + 1.3 节 | OOD 4.7 明确规定了启动验证的三项检查（null/空值、解码后字节≥32、URL-safe 字符集）及对应的 IllegalStateException 消息；1.3 节定义 JwtTokenProvider 为集中提供者。OOD 提供了完整的验证契约。 |
| **T2** | 1.3 节 + 3.3 节 | OOD 1.3 明确 JwtTokenProvider 是"JWT 令牌生成、解析、验证的集中提供者"，3.3 节也使用 JwtTokenProvider 而非 JwtUtil。实现者选择依赖旧 JwtUtil 是编码决策偏差。 |
| **T4** | 10.2 节 + 3.3 节 | OOD 10.2 明确规定 UNAUTHORIZED 消息为"未认证或令牌已失效"。实现者仅写了"未认证"。OOD 指引完整。 |
| **T5** | 3.1.3 节步骤 7 | OOD 3.1.3 步骤 7 明确规定"若用户已被禁用或被删除，需递增 LoginAttemptTracker IP 维度的失败计数"。实现者遗漏此步骤。 |
| **T6** | 4.4 节 + 6.1 节 | OOD 4.4 保护清单和 6.1 接口清单均定义端点路径为 `PUT /api/auth/profile`。路径 `/me` 是旧代码残留，并非 OOD 文档未覆盖。 |
| **T7** | 3.1.6 节步骤 2 | OOD 3.1.6 步骤 2 要求 Controller 从 SecurityContext 获取用户 ID。实现者在 Controller 层直接调用 JwtTokenProvider 是架构性违规。 |
| **T10** | 4.7 节 | OOD 4.7 明确写"Base64 解码后的字节长度 ≥ 32"。实现者错误地检查了原始字符串长度而非解码后长度。 |
| **T11** | 3.3 节 | OOD 3.3 要求 AuthenticationEntryPoint 行为契约依赖错误码区分。实现者使用消息字符串匹配是检测机制错误。 |
| **T12** | 8.1 节 C3 | OOD 8.1 C3 明确要求抽取 `JwtTokenProvider.getUserIdFromClaims(Claims)` 消除重复。实现者未完成此重构。 |
| **T13** | 3.1.1 节步骤 5/6 | OOD 明确使用动词"比对"（matches），非"编码"（encode）。实现者使用了错误的方法。 |
| **T14** | 4.1 节 | OOD 4.1 明确要求 ReentrantLock 保护窗口集合。实现者仅部分遵循此要求。 |
| **T19** | 4.4 节 + 6.1 节 | OOD 明确写 PATCH。实现者用了 PUT。OOD 指引完整。 |
| **T20** | 10.1 节 + 6.1 节 | OOD 明确要求 CHILDREN_EXIST。实现者用了 PARAM_INVALID。 |
| **T21** | 5.2 节 | OOD 5.2 明确要求路径 id 与请求体 id 一致性校验。实现者完全未实现。 |
| **T23** | 7.1 节脚注 + 8.1 节 M9 | OOD 两处提及 @EntityGraph 避免 N+1。UserRepository 已有 `findWithDetailsById` 方法带 EntityGraph。实现者未使用。 |
| **T25** | 10.1 节 | OOD 10.1 明确写 RATE_LIMITED 和 ACCOUNT_LOCKED 应返回 HTTP 429。实现者均映射为 400。 |
| **T27** | 10.2 节 | OOD 10.2 明确写 FORBIDDEN 消息为"无权限访问"。实现者写为"无权限"。 |

### OOD 维度粒度不足（4 项）

以下 4 项中，OOD 文档在核心设计方向上有指引，但关键细节未覆盖到实现者可直接遵循的程度：

| 编号 | OOD 覆盖了什么 | OOD 缺少了什么 | 若 OOD 更完整，实现偏差是否可避免 |
|------|---------------|---------------|--------------------------------|
| **T3** | 10.2 节定义了 ACCOUNT_LOCKED 的动态消息模板 `{锁定时间}` 和 args 传递语义。 | 未设计 BusinessException args → GlobalExceptionHandler → Result 的完整插值管线。OOD 缺少模板插值机制的实现路径设计（插值时机、插值方法、Result 是否支持动态消息）。 | ⚠ 部分可避免。如有 OOD 层级的插值管线设计，实现者会意识到 args 需被消费而非丢弃。但模板插值属于 cross-cutting 机制设计，在 OOD 阶段做出决策需要更细致的架构讨论。 |
| **T8** | 3.1.4 节定义登出"记录安全日志"和 refreshToken 处理流程。 | 未定义审计日志的存储机制（文件 vs 数据库 vs AuditLogger 框架）、接口抽象（是否应有 SecurityAuditLogger interface）、日志格式（结构化字段）。 | ⚠ 可避免。如有明确的审计日志接口和存储抽象，实现者不会完全忽略安全日志记录。 |
| **T9** | 5.2 节目录结构中列出了 UserConverter 和 UserFacadeImpl 两个类。 | 未明确指定 UserFacadeImpl.toUserInfoResponse() 应委托 UserConverter，两者职责划分缺少 OOD 层面的显式决策。 | ⚠ 可避免。如有显式的职责划分决策（如"UserFacadeImpl.toUserInfoResponse() 委托 UserConverter"），实现者不会在两处独立维护重复转换逻辑。 |
| **T22** | 5.2 节 MenuResponse 和 MenuUpdateRequest 均定义了 component 字段。 | 5.1 节 PermissionFunction 实体变更列表中未提及新增 component 字段映射——仅列出了类名重命名、enabled NOT NULL、visible NOT NULL，遗漏了 component 的实体级定义。 | ✅ 可避免。OOD 5.2 已在前端 DTO 中定义 component，但 5.1 实体变更表未将其包含在内，导致实现者遗漏实体的对应字段——这是 OOD 文档自身的不完整。 |

### OOD 不涉及（1 项）

| 编号 | 原因 |
|------|------|
| **T26** | 重复 Bean 定义属于 Spring 配置层面的重复声明，OOD 文档不涉及 Bean 装配细节。纯实现阶段的配置错误。 |

### 测试覆盖不完整（4 项）

T15/T16/T17/T18 四项为测试缺口，OOD 文档提供了完整的场景定义，测试覆盖不足是纯实现侧的质量管控问题。

---

## 修复执行策略

基于以上依赖关系分析，将 26 项修复整理为以下执行批次：

| 批次 | 包含项 | 策略说明 |
|------|--------|---------|
| **批次 1：阻塞修复** | T26 | 修复重复 Bean 定义使应用可启动。无前置依赖，优先执行。 |
| **批次 2：编码规范对齐** | T4, T27, T20, T14 | 纯文本修改/枚举值替换，无运行时行为变更风险，可并行修复。**T22 从本批次移出**——因其涉及 JPA 实体 schema 变更，归入批次 6。 |
| **批次 3：JWT 认证重构** | T2, T12, T1, T10 | T2↔T12 必须同步提交（同一文件的注入切换 + userId 提取归一化）；T1 和 T10 在该文件修改后独立修复。注意 T2 将 `JwtUtil` 替换为 `JwtTokenProvider` 后，SecurityConfigPhase1 的 Bean 组装方法签名需同步调整。**本批次必须在批次 1 之后执行**，因 T2/T12 修改的 `SecurityConfigPhase1.java` 在批次 1（T26，删除重复 Bean）中已被修改，若并行执行会产生同一文件修改冲突 |
| **批次 4：消息管线修复** | T3, T13, T5, T25 | T3（消息模板插值）与 T13（dummy BCrypt）均在 AuthServiceImpl 中修改，建议同一提交以减少合并冲突。T5（刷新流程 IP 计数）也在 AuthServiceImpl 中，可一并处理。T25（状态码映射）在 GlobalExceptionHandler 中，无依赖。 |
| **批次 5：Controller 层修复** | T6, T7, T19, T21 | T6（路径变更）和 T7（SecurityContext 重构）均修改 `AuthController.java`，**需同一开发者顺序修改（同一文件）**。T19 和 T21 均修改 `MenuController.java`，同理需顺序修改。T6/T7 修改涉及 API 契约变化，需前端同步配合 |
| **批次 6：基础设施修复** | T8, T9, T11, T22, T23 | T8 安全审计日志需先确认项目中的审计框架；T9 统一转换逻辑需确认不产生循环依赖且先修复 UserConverter 缺陷再委托；T11 需新增异常类型（AccountDisabledAuthenticationException）；T22 新增 JPA 实体字段需评估 schema 变更影响；T23 需扩展 Repository 查询方法。**T22 从批次 2 移入本批次**，因其涉及 JPA 实体字段新增，非纯文本修改。**注意 T8 与 T9 的隐含交叉**：T8 新建的 SecurityAuditLogger 若被 UserFacadeImpl 或 AuthService 引用记录用户转换操作的审计日志，T9 的 UserConverter 合并后需同步引入日志记录。批次排期时应将 T8/T9 分配给同一位开发者或至少互相同步接口设计。 |
| **批次 7：测试补充** | T15, T16, T17, T18 | **启动条件：批次 1-6 涉及的全部单元测试和集成测试通过后，执行者方可开始批次 7 的测试补充。** T16 与 T3 强依赖——需 T3 修复后再确认消息插值结果。 |

**批次间执行依赖：** 批次 1 → 批次 3（因 SecurityConfigPhase1.java 冲突声明）→ 批次 2, 4-6（可并行，但与批次 3 无依赖关系，批次 3 修复后其他批次可继续）→ 批次 7（功能修复验收标准满足后启动）。批次 2, 4, 5, 6 彼此无依赖，可并行执行以缩短修复周期。

**批次 6 隐含依赖声明（新增）：** T8 和 T9 在表面无直接 API 依赖，但存在日志逻辑交叉的可能场景：若 UserFacadeImpl.toUserInfoResponse() 或 UserConverter.toUserInfoResponse() 需要记录转换操作的审计日志（如管理员查询用户信息时的审计需求），T8 的 SecurityAuditLogger 接口将成为 T9 的依赖。建议批次 6 内部排期：T8 先完成（确定 SecurityAuditLogger 接口），T9 随后（必要时引入日志记录）。T22 和 T23 与 T8/T9 无交叉依赖。

---

## 逐项诊断

### T1: JwtTokenProvider@PostConstruct 缺少启动验证（P1）

- **判定：** 真实缺陷
- **根因：** 实现编码偏差（OOD 充分：4.7 节完整定义了验证契约）
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
- **根因：** 实现编码偏差（OOD 充分：1.3 节定义 JwtTokenProvider 为集中提供者）
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
- **根因：** 实现编码偏差（OOD 充分：10.2 节明确规定了消息）
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
- **根因：** 实现编码偏差（OOD 充分：3.1.3 步骤 7 写明了要求）
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
- **根因：** 实现编码偏差（OOD 充分：4.4 节和 6.1 节均明确定义路径为 `/profile`）
- **分析：**
  - `AuthController`（`controller/AuthController.java:70`）使用 `@PutMapping("/me")`，实际映射为 `PUT /api/auth/me`。
  - OOD 4.4 保护清单和 6.1 接口清单均定义资料更新端点为 `PUT /api/auth/profile`。
  - 路径与设计契约不一致。OOD 文档未变更过此路径定义，实现侧使用了旧代码路径。
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
- **根因：** 实现编码偏差（OOD 充分：3.1.6 步骤 2 明确要求）
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
  - 修改方式：
    - **不可直接委托 UserConverter**：UserConverter 在三处关键逻辑上存在缺陷（无 getEnabled 过滤、sort NPE 风险、无 getEnabled 权限过滤），若简单委托 `UserFacadeImpl.toUserInfoResponse()` → `userConverter.toUserInfoResponse(user)`，将**丢失 UserFacadeImpl 已正确的 null-safe 行为**。正确的修复路径：
    - **推荐方案：以 UserFacadeImpl 的实现为正确基准**，将 UserConverter 的三处不一致修正（补充 Role::getEnabled 过滤、改用 Comparator.nullsLast、补充 PermissionFunction::getEnabled 过滤），然后废弃 UserFacadeImpl 中的私有转换方法，统一委托 UserConverter（因 UserConverter 是专司转换的 @Component，职责更单一）。
    - 或方案 B：直接废弃 UserConverter，将 `toUserInfoResponse()` 及三个辅助方法统一保留在 UserFacadeImpl 中，删除 UserConverter。
  - 涉及的相关文件：`UserConverter.java`、`UserFacadeImpl.java`
  - 验证方式：对 `UserConverter` 补充测试，覆盖已禁用角色、已禁用权限、Role.sort 为 null 的场景，验证转换结果与 `UserFacadeImpl` 一致。
- **风险与注意事项：**
  - 若最终选择 `UserFacadeImpl` 委托 `UserConverter`，需在 `UserFacadeImpl` 中注入 `UserConverter` 可能形成循环依赖 —— 检查两个类的依赖关系。当前 `UserFacadeImpl` 和 `UserConverter` 均仅依赖 UserRepository 和 User 实体，无交叉依赖，可安全注入。
  - **OOD 维度分析（补充）：** 若 OOD 在 5.2 节中明确指定 `UserFacadeImpl.toUserInfoResponse()` 应委托 `UserConverter`，实现者本可在一处完成转换逻辑，避免两处独立维护。当前 OOD 只列出了两个类，未定义其协作关系。

### T10: JwtConfig.validate() 检查的是原始字符串长度而非解码后字节长度（P2）

- **判定：** 真实缺陷
- **根因：** 实现编码偏差（OOD 充分：4.7 节明确写了"解码后的字节长度"）
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
- **根因：** 实现编码偏差（OOD 充分：3.3 节要求基于错误码区分）
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
  - **选项 B 的测试成本评估：** 若采用 `AccountDisabledAuthenticationException`，需要测试它在 Spring Security Filter Chain 中的行为——JwtAuthenticationFilter 在 doFilterInternal 中抛出 AuthenticationException（当前在第 139 行通过匿名类抛出），Spring Security 的 ExceptionTranslationFilter 会捕获它并调用 AuthenticationEntryPoint.commence()。要验证这一完整链路，需要较重的测试基础设施（Mock HttpServletRequest/Response + FilterChain + 模拟 ExceptionTranslationFilter 行为，或使用 `@WebMvcTest` + `@Import(SecurityConfigPhase1.class)` 做完整集成测试）。纯单元测试可以验证 `RestAuthenticationEntryPoint.commence()` 被调用时传入 AccountDisabledAuthenticationException 返回正确结果，但无法覆盖 Filter 层抛出到 EntryPoint 的异常传递路径。建议的测试策略：`RestAuthenticationEntryPointTest` 覆盖 EntryPoint 层的正确性（单元测试）；`SecurityConfigPhase1Test` 或集成测试覆盖 Filter→EntryPoint 的异常传递（集成测试，测试成本与当前已有的测试基础设施一致——现有测试已使用 `@AutoConfigureMockMvc` 模式）。
  - 若采用选项 B，需新增一个异常类，此类应放在 exception 包中。注意 JwtAuthenticationFilter 目前在第 138-140 行匿名创建 AuthenticationException 子类，改为抛出具体类型后，这一匿名类可以删除。
  - 与 T4/T27 联动：T4/T27 的消息文本变更不影响此处的 instanceof 判断逻辑，因此修复 T11 后 T4/T27 的变更不会导致此处静默失效。

### T12: userId 提取逻辑重复（C3 修复未完成）（P2）

- **判定：** 真实缺陷
- **根因：** 实现编码偏差（OOD 充分：8.1 节 C3 明确要求抽取）
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
- **根因：** 实现编码偏差（OOD 充分：3.1.1 节明确使用"比对"语义）
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
- **根因：** 实现与设计约定不一致（功能正确）（OOD 充分：4.1 节明确要求 ReentrantLock 策略）
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
- **根因：** 测试覆盖不完整（OOD 充分：3.1.1 节步骤 6 明确了场景）
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
- **根因：** 测试覆盖不完整（OOD 充分：10.2 节定义了消息格式）
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
- **根因：** 测试覆盖不完整（OOD 充分：5.2 节定义递归结构，6.1 节定义树形 API）
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
- **根因：** 测试覆盖不完整（OOD 充分：3.3 节明确规定了顺序）
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
- **根因：** 实现编码偏差（OOD 充分：4.4 节和 6.1 节均规定 PATCH）
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
- **根因：** 实现编码偏差（OOD 充分：10.1 节和 6.1 节规定 CHILDREN_EXIST）
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
- **根因：** 实现编码偏差（OOD 充分：5.2 节明确要求此项校验）
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
- **根因：** 实现编码偏差（OOD 设计粒度不足亦有贡献——5.2 节 DTO 定义了 component 但 5.1 节实体变更表遗漏了此字段映射）
- **分析：**
  - `PermissionFunction` 实体（`permission/PermissionFunction.java`）未声明 `component` 字段。
  - `sys_function` 表存在 `component` 列（存储前端组件路径如 `Layout`、`system/user/index`），但实体未映射。
  - 导致：
    - `MenuResponse` 中的 `component` 始终为 `null`（`MenuServiceImpl.convertToMenuResponse()` 硬编码 `null`）
    - `MenuUpdateRequest` 中的 `component` 变更无法持久化
    - `data.sql` 中写入的 `component` 值无法被业务代码读取
- **修改建议：**
  - 代码位置：
    - `permission/PermissionFunction.java`：新增 `private String component;` 字段，添加 @Column 注解
    - `MenuServiceImpl.java:182-192`：`convertToMenuResponse()` 中将 `null` 替换为 `function.getComponent()`
    - `MenuUpdateRequest` 确认已有 component 字段及 setter，若无则补充
  - 涉及的相关文件：`PermissionFunction.java`、`MenuServiceImpl.java`、`MenuResponse.java`、`MenuUpdateRequest.java`
  - 验证方式：集成测试 —— 创建 PermissionFunction 并设置 component 值，通过 `MenuServiceImpl.getMenuById()` 验证返回的 MenuResponse.component 不为 null。
- **风险与注意事项：**
  - **JPA schema 变更风险评估：**
    - `application-dev.yml` 中 `spring.jpa.hibernate.ddl-auto=update`：dev 环境下 Hibernate 会自动在 `sys_function` 表增加 `component` 列，无启动风险。但需注意 dev 数据库中已存在的 `component` 列不会被覆盖。
    - `application-prod.yml` 未设置 ddl-auto，采用默认值。Spring Boot 3.x 对非嵌入式数据库的默认行为是不执行 DDL 自动变更，因此生产环境不会发生 Hibernate 自动修改表结构的情况。
    - 但需通过 schema.sql 或手动 DDL 脚本在生产环境显式添加 `component` 列，否则实体字段与表结构不匹配将导致查询异常。
    - 新增字段后需确认初始数据（data.sql）中的 Function 数据是否已包含 component 值；若已有 INSERT 语句中未包含 component 列，新增字段不影响已有数据（数据库列已有默认值或允许 NULL）。
    - `sys_function` 表的 `component` 列可能已有数据（从 schema/data.sql 查看）。新增实体字段后需确保 JPA 查询不因 mapping 缺失而忽略该列数据。
  - 新增字段后需添加 getter/setter 方法。`PermissionFunction` 当前未使用 Lombok，需手动添加。推荐显式添加 `@Column(name = "component")` 以提高代码可读性并避免 Hibernate 元模型中的潜在歧义。

### T23: getUserMenuTree 未使用 @EntityGraph，存在 N+1 查询风险（P1）

- **判定：** 真实缺陷
- **根因：** 实现编码偏差（OOD 充分：7.1 节脚注和 8.1 节 M9 均要求 EntityGraph 避免 N+1）
- **分析：**
  - `MenuServiceImpl.getUserMenuTree()`（`service/impl/MenuServiceImpl.java:44`）使用 `userRepository.findById(userId)`（无 EntityGraph），随后遍历 `user.getPosts()`（触发懒加载查询）和 `post.getFunctions()`（触发额外 M 次查询）。
  - `UserRepository`（`permission/UserRepository.java:16-17`）已定义 `findWithDetailsById` 方法带有 `@EntityGraph(attributePaths = {"roles", "posts"})`，但代码未使用。
  - 对于一个用户有 M 个岗位、每个岗位有 N 个功能的场景，查询次数分析如下：
    - 标准 JPA 懒加载下：`userRepository.findById(userId)` 1 次 → `user.getPosts()` 1 次（Hibernate 一次性加载整个集合，而非 M 次）→ 每个 `post.getFunctions()` M 次，合计 `1 + 1 + M = M + 2`
    - 使用 `findWithDetailsById`（EntityGraph 覆盖 `roles`、`posts`）修复后：1 次关联查询加载 user+posts → M 次 `post.getFunctions()` 懒加载，合计 `1 + M`
    - 完整 `@Query JOIN FETCH` 修复后：1 次 JOIN 查询加载全部三层
  - **并发场景分析：** 单请求场景下查询次数为 `M + 2`（M 为岗位数，通常 1-5），在当前数据量级下为 3-7 次 SQL。但在管理后台并发场景下（如 50 用户同时加载菜单树），瞬时查询量放大为 `50 × (M+2) = 150-350` 次，对数据库连接池产生瞬时压力。以 HikariCP 默认连接池大小 10 为例，350 次查询若集中在同一秒内，查询排队等待时间可能达到数十毫秒至数百毫秒。需关注以下风险点：
    - 若 `post.getFunctions()` 的 `FetchType.LAZY` 未配置批量抓取（`@BatchSize`），M 个岗位将触发 M 次独立 SQL 查询
    - 若多个管理员用户同时操作菜单管理页面，并发查询压力叠加
    - 系统启动后首次加载（缓存未预热）时，N+1 影响最明显
    - 优先级判定修正：**维持 P1（核心业务流程缺陷），非 P0**，因 N+1 在当前数据量级下不导致功能性错误或启动阻塞。
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
- **根因：** 实现编码偏差（OOD 充分：10.1 节规定了 429 映射）
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
    ① **前端 429 处理现状核查：** 前端 axios 响应拦截器（`shared/src/api/index.ts:37-55`）对 HTTP 状态码的处理逻辑如下：status 401 → 返回 UNAUTHORIZED；status 403 → 返回 FORBIDDEN；其他非 2xx 状态码 → 先尝试从响应体提取业务 code 和 message，若存在则返回业务错误，否则返回 `HTTP_ERROR` + `"请求失败（{status}）"`。429 状态码会走通用分支——因 `GlobalExceptionHandler` 返回的 429 响应体包含业务错误码（RATE_LIMITED / ACCOUNT_LOCKED）和对应消息，前端将收到 `BusinessError{ code: "RATE_LIMITED" | "ACCOUNT_LOCKED", message: "..." }`。用户不会看到"请求失败（429）"的通用信息，而是看到后端返回的业务消息（如"登录尝试过于频繁，请稍后重试"）。当前诊断（v6 版）声称"若前端已有 429 处理逻辑（如显示'请稍后重试'）则可正常适配"存在事实偏差——前端**没有** 429 专用处理路径，但通用分支仍能正确消费业务消息。
    ② 版本发布协调：需确保前端已适配 429 后再部署后端，否则用户可能看到未处理的异常提示。
    ③ 灰度/兼容期处理：若前端无法同步更新，可临时将 RATE_LIMITED/ACCOUNT_LOCKED 保留 400 状态码，在后续版本再切换至 429。

### T26: AuthModuleConfig 与 SecurityConfigPhase1 存在重复的 TokenBlacklist Bean 定义（P0）

- **判定：** 真实缺陷
- **根因：** Spring 配置冲突（OOD 不涉及）
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
- **根因：** 实现编码偏差（OOD 充分：10.2 节明确规定了消息）
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

## 修订说明（v7）

| 质询意见 | 回应 |
|---------|------|
| Q1（严重）：OOD 文档维度响应不充分——需对全部 26 项做反事实分析而非仅 T3/T8/T9 | 已重构「OOD 设计覆盖度评价」小节，按三段式（OOD 充分 / OOD 粒度不足 / OOD 不涉及）对全部 26 项逐一分析。新分类：17 项纯实现偏差（OOD 完全充分），4 项含 OOD 贡献（T3/T8/T9/T22——T22 新增，因其 5.1 节实体变更表遗漏了 component 字段映射），1 项 Spring 配置冲突（T26，OOD 不涉及），4 项测试覆盖。总计统计同步修正为 17+4+1+4=26 项，并在汇总表「根因归属」列中同步更新 T22 的根因为含 OOD 贡献。 |
| Q2（中）：T25 前端 429 处理事实偏差 | 已修正 T25「风险与注意事项」中前端 429 处理现状的描述。实际前端代码（`shared/src/api/index.ts:37-55`）的 error 拦截器仅对 401 和 403 有专用处理路径，429 走通用分支。但通用分支能正确提取后端响应体中的业务 code 和 message，因此用户不会看到通用错误信息，而是看到业务错误消息。修正后的描述准确反映了"无专用 429 处理但有通用分支兜底"的现状。 |
| Q3（中）：T9 委托逻辑矛盾——UserConverter 缺陷致直接委托丢失 null-safe 行为 | 已修正 T9「修改建议」：移除"推荐 UserFacadeImpl 直接委托 UserConverter"的表述，改为"以 UserFacadeImpl 为正确基准，先修正 UserConverter 的三处不一致，再废弃重复代码"。明确说明直接委托会导致丢失三个正确行为（Role::getEnabled 过滤、sort nullsLast、PermissionFunction::getEnabled 过滤）。 |
| Q4（中）：T22 批次风险被低估——JPA 实体 schema 变更非纯文本修改 | 已将 T22 从批次 2「编码规范对齐」移入批次 6「基础设施修复」。新增 JPA schema 变更风险评估（ddl-auto 在 dev 为 update 无风险，prod 无显式 ddl-auto 需通过 DDL 脚本手动加列）。 |
| Q5（低）：T11 备选方案 B 的测试成本未评估 | 已在 T11「风险与注意事项」中新增对选项 B 的测试成本评估。分析结论：RestAuthenticationEntryPoint 层的单元测试成本低，Filter→EntryPoint 异常传递链的集成测试需较重的测试基础设施（MockMvc），但该测试成本与项目现有测试基础设施一致（已有 `@AutoConfigureMockMvc` 测试）。 |
| Q6（低）：T23 并发场景分析缺失 | 已在 T23 分析中新增「并发场景分析」子章节，量化评估 50 用户并发加载菜单时的连接池压力（150-350 次/秒）。优先级维持 P1 不变（非功能性错误，非启动阻塞）。 |
| Q7（低）：批次 6 未声明 T8 与 T9 的隐含依赖 | 已在「修复执行策略」批次 6 描述中新增隐含依赖声明段落，说明 T8（SecurityAuditLogger）若被 UserFacadeImpl 引用日志记录，将成为 T9 的依赖。建议批次 6 内部排期：T8 先完成接口定义，T9 随后。 |
