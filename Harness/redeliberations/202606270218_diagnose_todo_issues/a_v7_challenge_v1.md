# 诊断质询报告（v1）

## 质询结果

LOCATED

## 逐维度审查

### 1. 证据充分性

**[通过]** T1 根因已通过实际代码验证：`JwtTokenProvider.java:52-63`（generateAccessToken 无 type claim）、`JwtTokenProvider.java:88-105`（validateToken 检查 type）、`JwtAuthenticationFilter.java:61`（传入 "access"）三处均与诊断描述一致。

**[通过]** T2 根因已确认：`AuthServiceImpl.java:270-283` 异常刷新仅 log.warn 不阻断，OOD 4.2 节（`Docs/05_ood_phase1_B.md` 第 620-621、637-638 行）确实未定义阻断逻辑。诊断结论"代码符合 OOD 但 OOD 设计不充分"证据充分。

**[通过]** T8 根因已验证：`GlobalExceptionHandler.formatMessage()`（`GlobalExceptionHandler.java:38-48`）的插值行为与诊断一致——"30分钟"作为 args 经 replaceFirst 正确插值；若使用 OOD 示例的"请30分钟后重试"会得到"请请30分钟后重试后重试"的双重替换结果。

**[通过]** T12 根因已验证：`JwtTokenProvider.java:37` 使用标准 Base64 正则 `^[A-Za-z0-9+/]+=*$`，`JwtTokenProvider.java:42` 使用 `Base64.getDecoder()`；OOD 4.7 节（`Docs/05_ood_phase1_B.md:620`）明确要求 "Base64 URL-safe 字符集（A-Z a-z 0-9 - _）"。连带影响分析（不解配套改解码器则抛出 IllegalArgumentException）准确。

**[通过]** T13 根因已验证：`SlidingWindowCounter.java:14` 全局 `ReentrantLock`，`tryAcquire()`（第 36-54 行）和 `cleanup()`（第 59-66 行）均在全局锁保护下执行。OOD 文本矛盾分析（第 433 行"每 IP 独立加锁" vs 第 444 行"ReentrantLock 保护原子性"）代码可验证。

**[通过]** T14 证据：`JwtUtil.java:75`（`claims.put("role", role)`）、第 77 行（`claims.put("position", position)`）、无 jti。`JwtAuthenticationFilter.java:111` 引用 `JwtUtil.extractToken`（静态方法）。`generateToken()` 在 Java 源文件中无生产代码调用方——该 claim 准确。

**[通过]** T17 证据：`RestAuthenticationEntryPoint.java:33` 和 `RestAccessDeniedHandler.java:34` 均直接 `Result.fail(errorCode)`，未经过 `formatMessage()` 插值管线。OOD 确未显式覆盖此路径。

**[通过]** T22 误报判断准确：`AuthController.java:69` 使用 `@PutMapping("/profile")`，`AuthControllerTest.java:225` `@DisplayName("PUT /api/auth/profile")`，测试直接调用 Controller 方法无 HTTP 路径参数。

### 2. 逻辑完整性

**[通过]** 从 34 个待办项到总览分类的因果链完整。各 T 项的"现象→代码定位→根因判定"推理链条清晰，无逻辑跳跃。

**[通过]** 汇总表计数经逐项核对一致：编码缺陷 11（T1/T9-T19 共 11 项）、OOD 缺陷 3（T2/T8/T17）、测试覆盖不足 12（T4/T6/T7/T20/T23/T26-T29/T31/T32/T34）、测试设计/实现质量 6（T5/T21/T24/T25/T30/T33）、其他 1（T3）、误报 1（T22）。合计 34。OOD 表（T2/T8/T17）与编码缺陷表（含 T13 带 OOD 主因标注）的计数一致。

**[通过]** 修复批次依赖分析合理（任务依赖图、子任务间干扰评估、回滚影响范围），存量测试影响已逐项标注。

**[通过]** T17 的"推荐技术路线"（抽取 MessageInterpolator）逻辑清晰，明确排除方案 A/B 的理由。

### 3. 覆盖完备性

**[通过]** 所有 34 个待办项均已覆盖，每个判定为"真实存在"或"误报"。

**[通过]** 迭代要求中列出的 4 项问题均已修正：
- [严重] 汇总表 OOD 计数 3 与详细表一致（OOD 表 3 项 + 注释说明 T13 双归属在编码缺陷表呈现）
- [一般] T13 双归属已在编码缺陷表条目中标注「↗ 主因为OOD文本模糊性」
- [一般] T14 已从 P1 移至 P2
- [轻微] 修复批 6 子任务 D 已标注 T20 建议在 T17 之后实施

**[通过]** 完整回答了"问题是什么"（每个 T 项的描述和影响范围）和"为什么发生"（根因分析，区分 OOD 缺陷 vs 编码缺陷）。

## 质询要点

无。所有维度评估均通过，根因已准确定位，证据链完整，修复者可据此采取行动。
