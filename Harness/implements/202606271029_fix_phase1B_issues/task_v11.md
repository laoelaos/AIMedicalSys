# 任务指令（v11）

## 动作
NEW

## 任务描述
修复 `Docs/05_ood_phase1_B.md` 中 5 处 OOD 文档缺陷：T8（3.1.1/10.3 节 BusinessException args 示例值错误）、T2-OOD（4.2 节缺少异常刷新阻断逻辑定义）、T12-OOD（4.7 节密钥字符集迁移策略不明确）、T13-OOD（4.1 节锁粒度描述矛盾）、T17-OOD（10.3 节缺少 AuthenticationEntryPoint/AccessDeniedHandler 插值出口）。

## 选择理由
R7 已通过验证（628 tests, 0 failures）。OOD 文档更新是唯一待办的非代码任务，轻量可先行。后续 R9 测试增强轮次需要 OOD 文档正确性作为基准。

## 任务上下文

### 目标文件
`Docs/05_ood_phase1_B.md`（仅此一个文件，5 处局部修改）

### T8 — 修正 BusinessException args 示例值

**OOD 3.1.1 节（登录流程·ACCOUNT_LOCKED 消息插值机制段落）**：
- 当前第 155 行：`BusinessException(GlobalErrorCode.ACCOUNT_LOCKED, "请30分钟后重试")` 和 `"请15分钟后重试"`
- **问题**：模板是 `"账户已锁定，请{锁定时间}后重试"`，args 应为 `"30分钟"` 和 `"15分钟"`（仅时间短语，非完整消息），与 10.3 节模板设计一致
- **修复**：将 args 从 `"请30分钟后重试"` 改为 `"30分钟"`，`"请15分钟后重试"` 改为 `"15分钟"`

**OOD 10.3 节（错误消息模板插值管线·args 在 Service 层传入）**：
- 当前第 1221-1224 行：同样错误的 args 值
- **修复**：同上

### T2-OOD — 补充异常刷新检测阻断逻辑（OOD 4.2 节）

**OOD 4.2 节（Token 黑名单/轮换设计·Refresh Token 安全补偿策略）**：
- 当前第 502 行：仅定义了日志告警（`log.warn`），未定义阻断逻辑
- **需要修改**：
  1. 在异常刷新检测描述中增加阻断逻辑：检测到异常刷新后（5 秒窗口内超过 2 次请求），应拒绝本次刷新请求，返回 TOKEN_REFRESH_FAILED 错误并强制前端清除本地 token
  2. 新增 `ConcurrentHashMap<Long, Deque<Long>> refreshTimestamps` 的过期清理说明（T18 修复已在代码中实现，需文档对齐）

### T12-OOD — 明确 URL-safe 密钥字符集迁移策略（OOD 4.7 节）

**OOD 4.7 节（JWT 密钥配置与约束）**：
- 当前第 620 行已定义合法字符集为 URL-safe（`A-Z a-z 0-9 - _`），但未提及从标准 Base64 迁移的过渡策略
- **需要补充**：
  1. 启动验证逻辑顺序：先校验 URL-safe 字符集，再用 `Base64.getUrlDecoder()` 解码
  2. 若生产环境已有标准 Base64 密钥，需说明迁移方案（如同时支持两种格式的双密钥过渡，或一次性重新生成 URL-safe 密钥）
  3. 第 637 行启动验证逻辑描述应与实际代码一致

### T13-OOD — 澄清锁粒度描述（OOD 4.1 节）

**OOD 4.1 节（SlidingWindowCounter 契约）**：
- 当前第 433 行：`"确保每个 IP 的窗口对象独立加锁，细粒度锁减少竞争"` — 暗示每 key 独立锁
- 当前第 444 行：`"ReentrantLock 保护跨窗口操作的原子性"` — 描述全局单锁
- 两个表述矛盾。实际实现（T13 修复后）使用 `ConcurrentHashMap.compute` 原子操作，**不再使用全局 ReentrantLock**
- **需要修改**：
  1. 删除或修正第 444 行 "ReentrantLock 保护跨窗口操作的原子性"
  2. 统一描述为："每个 key 的 Deque 在 `ConcurrentHashMap.compute` 闭包内原子访问，无需额外锁"
  3. 同步更新第 433 行对应的"独立加锁"表述为"`ConcurrentHashMap.compute` 原子操作"

### T17-OOD — 补充 AuthenticationEntryPoint/AccessDeniedHandler 插值出口（OOD 10.3 节）

**OOD 10.3 节（错误消息模板插值管线）**：
- 当前流程仅覆盖 `GlobalExceptionHandler` 一个出口
- **需要补充**：
  1. 管线图中增加 `RestAuthenticationEntryPoint` 和 `RestAccessDeniedHandler` 分支
  2. 说明这两个出口在 `Result.fail(errorCode)` 前调用 `MessageInterpolator` 进行插值
  3. 明确 `MessageInterpolator` 作为独立组件的职责（接口 + 实现），与 `GlobalExceptionHandler` 共享

## 已有代码上下文
- `Docs/05_ood_phase1_B.md` — 1376 行，Phase 1 包 B 统一修复与包 B OOD 设计方案
- 诊断报告 `Docs/Diagnosis/impl/05_phase1B_report.md` 第 441-453 行汇总了 4 个 OOD 文档缺陷（T2/T8/T13/T17）
- 代码实现变更（T12、T13、T17 的编码修复）已在之前的实施轮次中完成，OOD 文档需与代码实现对齐
