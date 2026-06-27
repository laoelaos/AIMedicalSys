# 诊断质询报告（v1）

## 质询结果

LOCATED

## 逐维度审查

### 1. 证据充分性

**[通过]** T23 查询次数公式已修正为 `1 + 1 + M = M + 2`，与实际 JPA 懒加载行为一致（`user.getPosts()` 为集合一次性加载，而非 M 次）。

**[通过]** 所有关键诊断结论均附有精确的代码文件路径和行号引用，经抽查确认与实际代码一致：
- `MenuServiceImpl.java:44` 使用 `userRepository.findById(userId)` 而非 `findWithDetailsById`
- `UserRepository.java:16-17` 存在 `findWithDetailsById` 带 `@EntityGraph(attributePaths = {"roles", "posts"})`
- `SlidingWindowCounter.java` 中 `ReentrantLock` 仅用于 `cleanup()` 方法，`tryAcquire()` 未使用
- `AuthModuleConfig.java:20` 与 `SecurityConfigPhase1.java:42` 均定义 `tokenBlacklist()` Bean
- `JwtAuthenticationFilter.java:5,34` 注入 `JwtUtil` 而非 `JwtTokenProvider`
- `GlobalExceptionHandler.java:38-57` `resolveHttpStatus()` 缺少 RATE_LIMITED/ACCOUNT_LOCKED/TOKEN_REFRESH_FAILED 映射
- `GlobalErrorCode.java:9` UNAUTHORIZED 消息为"未认证"（OOD 要求"未认证或令牌已失效"）
- `AuthController.java:70` `@PutMapping("/me")`（OOD 要求 `/profile`）
- OOD `05_ood_phase1_B.md:1087-1091` 确认 UNAUTHORIZED/FORBIDDEN/ACCOUNT_LOCKED 消息及 HTTP 状态码约定

**[通过]** 无未经验证的假设，所有根因判定基于代码和设计文档的双重确认。

### 2. 逻辑完整性

**[通过]** 逐 T 项形成完整因果链：现象描述 → 代码定位 → 设计/实现偏差对比 → 根因结论，无逻辑跳跃。

**[通过]** 依赖关系分析完整（T2↔T12 同文件同步修复、T3↔T16 消息插值影响测试断言、T4/T27 文本变更与 T11 字符串匹配的关系等）。

**[通过]** 影响范围判定合理，无矛盾线索遗漏。

### 3. 覆盖完备性

**[通过]** 迭代需求指定的 Q1（T23 公式错误）已在 v4 修订说明中明确响应，分析段落公式已更正，EntityGraph 和 `@Query JOIN FETCH` 两种修复后的查询模式说明已补充。

**[通过]** 所有 26 项问题（T1-T23, T25-T27）均逐项诊断，无遗漏。

**[通过]** 修订说明完整回溯了 v2/v3/v4 各轮反馈的响应情况，变更点可追溯。

## 质询要点

无（LOCATED）。
