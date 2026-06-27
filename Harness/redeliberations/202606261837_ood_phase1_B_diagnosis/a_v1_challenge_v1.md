# 诊断质询报告（v1）

## 质询结果

LOCATED

## 逐维度审查

### 1. 证据充分性

**[通过]** 每个诊断点均提供了具体文件路径和行号引用，经逐一交叉验证与实际代码一致：
- T1: JwtTokenProvider.init() (行31-36) 确未做 null/空值/Base64长度/URL-safe校验
- T2: JwtAuthenticationFilter (行5,34,60) 确注入 JwtUtil 而非 JwtTokenProvider
- T3: BusinessException args 被存储 (行14-18) 但 GlobalExceptionHandler (行27) 调 Result.fail(errorCode) 仅取 errorCode.getMessage() 模板原文，args 未被消费
- T4/T27: GlobalErrorCode 消息分别为"未认证"、"无权限"，与 OOD 10.2 要求的"未认证或令牌已失效"、"无权限访问"不符
- T5: refreshToken() (行180-184) 禁用/删除用户场景确未调 recordIpFailure
- T6: AuthController (行70) @PutMapping("/me") vs OOD 要求的 /api/auth/profile
- T7: AuthController.changePassword() (行82-91) 确直接调 JwtTokenProvider 而非从 SecurityContext 获取
- T8: AuthService.logout(String) (行13,147-164) 无 refreshToken 参数，Controller 接收后未传递
- T9: UserConverter (行41) 用 comparingInt (NPE风险、无Role.getEnabled过滤、无f.getEnabled过滤); UserFacadeImpl (行62-63) 用 nullsLast 且过滤了 enabled
- T10: JwtConfig.validate() (行55) 确检查 secret.length() < 32 而非解码后字节长度
- T11: RestAuthenticationEntryPoint (行16,27) 确用 message.contains() 字符串匹配识别 ACCOUNT_DISABLED
- T12: JwtAuthenticationFilter.extractUserId() (行142-150) 与 JwtTokenProvider.getUserIdFromClaims() (行93-101) 方法体完全一致
- T13: AuthServiceImpl.login() (行105) 确用 encode("dummy") 而非 matches()
- T14: SlidingWindowCounter (行14) lock 仅在 cleanup() (行54-60) 使用，tryAcquire() (行28-52) 未使用
- T19: MenuController (行117) 确用 @PutMapping 而非 @PatchMapping
- T20: MenuServiceImpl.deleteMenu() (行165) 确用 PARAM_INVALID 而非 CHILDREN_EXIST
- T21: MenuController.update() (行117-125) 确未校验 path id 与 request id 一致性
- T22: PermissionFunction 确无 component 字段映射; MenuServiceImpl (行187) component 硬编码 null
- T23: getUserMenuTree() (行44) 确用 findById 而非 findWithDetailsById
- T25: GlobalExceptionHandler.resolveHttpStatus() (行38-57) 确未映射 RATE_LIMITED/ACCOUNT_LOCKED/TOKEN_REFRESH_FAILED
- T26: AuthModuleConfig (行19-22, 无profile) 与 SecurityConfigPhase1 (行41-44, @Profile("phase1")) 确有重复 TokenBlacklist bean
- T27: GlobalErrorCode.FORBIDDEN (行10) 确为"无权限"而非 OOD 要求的"无权限访问"

**[通过]** 所有代码行为描述均与实际代码一致，无推测性断言。

### 2. 逻辑完整性

**[通过]** 每个诊断点均形成完整的因果链：现象描述 → 代码位置与行为 → OOD设计对照 → 根因判定 → 影响范围。无逻辑跳跃。

**[通过]** 未发现与诊断结论矛盾的线索。诊断对 T14 的正确性补充说明（ConcurrentHashMap.compute 已提供原子性）体现了对矛盾线索的主动处理。

**[通过]** 影响范围判断合理、明确，未遗漏相关模块。

### 3. 覆盖完备性

**[通过]** 诊断覆盖了审查报告中 T1-T23 及 T25-T27 所有 26 项问题，无遗漏。

**[轻微]** 汇总统计（行323）存在数值不一致：
- "27/27 项" → 实际审查报告及诊断表中均为 26 项（T1-T23、T25-T27，无 T24）
- "24 项根因归属于实现编码偏差" → 实际诊断表统计为 21 项
- "3 项归属于测试覆盖不完整（T15/T16/T17/T18）" → 列表 4 项但表述为 3 项

此问题不影响各诊断点的结论准确性，仅属表述精确性瑕疵。

**[通过]** 诊断结论完整回答了"问题是什么"（各审查项的真实性判定）和"为什么发生"（根因分析），涵盖 OOD 设计文档偏差与实现编码偏差两个维度。

## 质询要点

（无严重/一般问题，仅存在轻微表述精确性瑕疵）
