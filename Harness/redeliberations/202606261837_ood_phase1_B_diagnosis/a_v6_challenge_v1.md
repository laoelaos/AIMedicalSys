# 诊断质询报告（v1）

## 质询结果

LOCATED

## 逐维度审查

### 1. 证据充分性

**[通过]** T1（JwtTokenProvider.init() 缺少启动验证）：代码确认 `init()` 仅执行 `Base64.getDecoder().decode(secret)` 和 `hmacShaKeyFor(keyBytes)`，无 null/空值检查及解码后字节长度检查，诊断准确。

**[通过]** T2（JwtAuthenticationFilter 依赖 JwtUtil）：代码确认构造注入 `private final JwtUtil jwtUtil`，`doFilterInternal()` 调用 `jwtUtil.validateTokenAndGetClaims(token)`，与诊断一致。

**[通过]** T3（ACCOUNT_LOCKED 模板未解析）：代码确认 `GlobalExceptionHandler.handleBusinessException()` 第27行调用 `Result.fail(errorCode)` 直接返回模板原文，BusinessException 的 args 未被消费，诊断准确。

**[通过]** T4/T27（消息与设计不一致）：代码确认 `UNAUTHORIZED` 消息为"未认证"、`FORBIDDEN` 消息为"无权限"，诊断准确。

**[通过]** T5（刷新流程未递增 IP 失败计数）：代码确认 `refreshToken()` 第180-184行在用户禁用/删除时直接抛异常，未调用 `recordIpFailure()`，诊断准确。

**[通过]** T6（端点路径不一致）：代码确认 `@PutMapping("/me")` 映射为 `PUT /api/auth/me`，与 OOD 定义的 `/api/auth/profile` 不一致，诊断准确。

**[通过]** T7（changePassword 跳过 SecurityContext）：代码确认 Controller 层第87-88行直接调用 `jwtTokenProvider.validateToken()` 和 `getUserIdFromClaims()` 获取用户 ID，未使用 SecurityContext，诊断准确。

**[通过]** T8（logout 忽略 refreshToken）：代码确认 `AuthService.logout(String token)` 签名仅接收 token，Controller 虽接收 `RefreshTokenRequest` 但未传递给 AuthService，诊断准确。

**[通过]** T9（重复转换逻辑及三处不一致）：代码确认 UserConverter 和 UserFacadeImpl 均有 `User→UserInfoResponse` 转换逻辑，三者不一致（Role::getEnabled 过滤、sort 空值处理、权限 enabled 过滤）均与代码一致，诊断准确。

**[通过]** T10（JwtConfig 检查原始字符串长度而非解码后字节）：代码确认 `secret.length() < 32` 检查的是原始 Base64 字符串，诊断准确。

**[通过]** T11（消息字符串匹配识别禁用账户）：代码确认 `RestAuthenticationEntryPoint.commence()` 使用 `message.contains(ACCOUNT_DISABLED_MESSAGE)` 判断，`JwtAuthenticationFilter.throwAccountDisabled()` 通过 `AuthenticationException` 消息传递状态，诊断准确。

**[通过]** T12（extractUserId 重复）：代码确认 `JwtAuthenticationFilter.extractUserId()`（第142-150行）与 `JwtTokenProvider.getUserIdFromClaims()`（第93-101行）方法体完全一致，诊断准确。

**[通过]** T13（encode 替代 matches）：代码确认 `AuthServiceImpl.login()` 第105行和第113行使用 `passwordEncoder.encode("dummy")` 而非 `matches()`，诊断准确。

**[通过]** T14（ReentrantLock 作用域不完整）：代码确认 `tryAcquire()` 未使用 `lock` 字段，仅在 `cleanup()` 中加锁，诊断准确。

**[通过]** T20（删除菜单使用 PARAM_INVALID 而非 CHILDREN_EXIST）：代码确认 `deleteMenu()` 第165行使用 `GlobalErrorCode.PARAM_INVALID`，诊断准确。

**[通过]** T23/N+1（findById 未使用 EntityGraph）：代码确认 `getUserMenuTree()` 第44行使用 `userRepository.findById(userId)` 而非 `findWithDetailsById`，`user.getPosts()` 和 `post.getFunctions()` 均为懒加载，诊断准确。

**[通过]** T25（状态码映射缺失 429）：代码确认 `resolveHttpStatus()` 仅映射 UNAUTHORIZED/FORBIDDEN/NOT_FOUND/PARAM_INVALID/SYSTEM_ERROR，RATE_LIMITED/ACCOUNT_LOCKED/TOKEN_REFRESH_FAILED 映射为默认 400，诊断准确。

**[通过]** T26（重复 TokenBlacklist Bean 定义）：代码确认 `AuthModuleConfig` 和 `SecurityConfigPhase1` 均定义 `@Bean TokenBlacklist tokenBlacklist()`，诊断准确。

### 2. 逻辑完整性

**[通过]** 从问题现象到根因形成了完整因果链，T1-T27 各条目的分析逻辑清晰，无逻辑跳跃。

**[通过]** 批次的依赖关系（批次1→批次3、批次5内 T6/T7 顺序修改）已明确声明，批次间执行依赖图完整。

**[通过]** 影响范围判定合理：T23 的 N+1 查询公式 `M+2` 已在 v4 中修正并验证正确；优先级 P0-P3 分级有明确区分标准。

### 3. 覆盖完备性

**[通过]** 全部 26 项（T1-T23、T25-T27）均有覆盖，0 项误报。

**[通过]** 迭代要求中的 6 项反馈（T14 优先级标注、跨批次串行依赖、OOD 设计维度补充、T1/T10 交叉引用、T8 默认路径优先级、批次7启动条件）已在 v6 中全部响应。

**[通过]** OOD 设计覆盖度评价已补充（第49行），T3/T8/T9 的根因分类已调整为含 OOD 贡献标记。

**[通过]** 诊断结论完整回答了"问题是什么"（26 项真实缺陷）和"为什么发生"（每项有根因分析）。

**[问题-轻微]** 报告标题标注为 `（v5）`（第1行），但文件名为 `a_v6_diag_v1.md` 且内容已包含 v6 修订，标题版本号应同步更新为 `（v6）`。此问题不影响诊断准确性，但可能对读者造成版本混淆。

## 质询要点

无严重/一般问题。
