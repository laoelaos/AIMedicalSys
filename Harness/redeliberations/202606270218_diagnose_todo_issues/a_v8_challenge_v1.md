# 诊断质询报告（v1）

## 质询结果

LOCATED

## 逐维度审查

### 1. 证据充分性

**[通过]** T1-T34 所有根因判定均具备充分的代码/OOD 文档证据支撑，关键推断已逐一核实：
- T1：`JwtTokenProvider.generateAccessToken()`（文件第52-63行）确实不含 `type` claim，`validateToken()`（第96行）校验 `type`，`JwtAuthenticationFilter`（第61行）传入 `"access"`，三者一致
- T2：OOD 4.2 节（第502行）仅定义 `log.warn` 告警，无阻断逻辑，代码实现完全跟随 OOD
- T3：三条测试文件路径经 glob 确认不在 `application/` 下，真实路径与 scope.md 范畴描述一致
- T8：OOD 3.1.1 节（第155行）和 10.3 节（第1222-1224行）示例使用 `"请30分钟后重试"` 作为 args，与 OOD 10.3 表格（第1253行）定义的 `"30分钟"` 不一致；代码第104行使用 `"30分钟"`，与表格一致
- T9：`logout()` 第182-184行在 claims 为 null 时直接 return，跳过第202行审计日志
- T10：第182行 `validateToken` 解析 token，第187行 `getJtiFromToken` 重新解析同一 token
- T11：OOD 3.1.5 节（第220行）规定从 SecurityContext 获取 user ID，代码第303-314行直接接收 String token 参数
- T12：第37行正则 `^[A-Za-z0-9+/]+=*$` 为标准 Base64 字符集，第42行 `Base64.getDecoder()` 为标准解码器
- T13：第14行 `ReentrantLock` 为全局单锁；OOD 第433行与第444行存在文本模糊性
- T14：`JwtUtil.generateToken()` 第75行 `claims.put("role", role)`、第77行 `claims.put("position", position)`，无 jti
- T15：`recordUsernameFailure()` 第32-39行、`recordIpFailure()` 第42-49行不检查窗口过期
- T16：`formatMessage()` 第39行先尝试 `MessageFormat.format`，对命名占位符 `{锁定时间}` 触发异常
- T17：`RestAuthenticationEntryPoint.java:33` 直接 `Result.fail(errorCode)`，未经过插值管线
- T18：`refreshTimestamps` 第68行 `ConcurrentHashMap` 无过期清理机制
- T19：`MenuController.getCurrentUserId()` 第152-161行直接操作 `SecurityContextHolder`
- T21：`AuthServiceTest.java:268` 冗余 stub 经验证存在

### 2. 逻辑完整性

**[通过]** 从现象到根因的因果链完整，无逻辑跳跃或矛盾线索：
- T1 因果链：缺 type claim → validateToken 返回 null → Filter 清除 SecurityContext → 全部未认证处理
- T2 因果链：OOD 仅定义告警 → 代码跟随 → 攻击者可在告警窗口内持续滥用
- T8 因果链：OOD 示例代码与表格模板定义冲突 → 代码选择了表格定义的一致行为
- T13 因果链：OOD 文本内在矛盾 → 编码跟随其中一种解读（全局锁）→ 非最优实现
- T17 因果链：OOD 10.3 节仅覆盖 GlobalExceptionHandler 出口 → 编码自然未在 EntryPoint/Handler 路径应用插值
- 依赖图 P2 区块已补充 T20→T17 和 T25→T13 两条依赖边（v10 修订确认），与子任务表一致

### 3. 覆盖完备性

**[通过]** 所有 34 个待办事项（T1-T34）均完成根因分析，判定真实/误报，分类明确：
- 编码缺陷 11 项、OOD 文档缺陷 3 项（T2/T8/T17，T13 双归属）、测试覆盖不足 12 项、测试设计/实现质量 6 项、其他 1 项（T3）、误报 1 项（T22）
- 迭代需求中 4 个问题 + 1 个补充审查发现均在 v10 修订中完整响应：
  - 问题1（依赖图）：P2 区块补充两条依赖边，内部不一致已消除
  - 问题2（T3 目标文件）：子任务 A 明确指定 `review_v2_D.md`
  - 问题3（T12/T13 冲突）：确认无交叉影响，补充风险说明
  - 问题4（T6 依赖模糊）：补充 @Disabled/assumeTrue 具体指引
  - 补充审查（P1 边界争议）：补充注释说明 T5/T6/T7 归入 P1 的依据
