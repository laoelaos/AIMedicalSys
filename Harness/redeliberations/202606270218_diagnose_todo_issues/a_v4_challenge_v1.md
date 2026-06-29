# 诊断质询报告（v1）

## 质询结果

LOCATED

## 逐维度审查

### 1. 证据充分性

**[通过]** T1：`JwtTokenProvider.java:52-63` 的 `generateAccessToken()` 确无 `type` claim（仅含 `sub`/`userId`/`userType`/`jti`/`iat`/`exp`），`JwtTokenProvider.java:88-105` 的 `validateToken()` 在 `expectedType="access"` 时执行 `claims.get("type", String.class)`，`JwtAuthenticationFilter.java:61` 传入 `"access"`— 三者吻合。

**[通过]** T2：`AuthServiceImpl.java:270-283` 检测到异常刷新后仅 `log.warn(...)`（第 280 行），继续执行第 285-288 行生成新 Token。OOD `05_ood_phase1_B.md:502` 确未定义阻断逻辑。

**[通过]** T5：`UserFacadeImplTest.java:24` 确为 `mock(UserConverter.class)`，全文 5 处 `when(userConverter.toUserInfoResponse(...)).thenReturn(...)` 确认 mock。

**[通过]** T12：`JwtTokenProvider.java:37-38` 正则 `^[A-Za-z0-9+/]+=*$` 确为标准 Base64，`JwtTokenProvider.java:42` 确用 `Base64.getDecoder()` 而非 `Base64.getUrlDecoder()`。

**[通过]** T13：`SlidingWindowCounter.java:14` 确为 `private final ReentrantLock lock;`，全局单锁。

**[通过]** T15：`LoginAttemptTracker.java:32-39` 的 `recordUsernameFailure()` 和 `LoginAttemptTracker.java:42-49` 的 `recordIpFailure()` 确无窗口过期检查。

**[通过]** T18：`AuthServiceImpl.java:68` 的 `refreshTimestamps` 为 `ConcurrentHashMap<Long, Deque<Long>>`，`compute` 闭包只惰性清理单 key 内过期条目，不删除整个 entry，确为堆内内存。

**[通过]** T3：三条路径在 `application/` 下均不存在。`GlobalExceptionHandlerTest.java` 在 `backend/common/`，`GlobalErrorCodeTest.java` 在 `backend/common/`，`EntityMappingIT.java` 在 `backend/integration/`。

### 2. 逻辑完整性

**[通过]** 因果链完整：T1 从"不写 type claim → validate 失败 → 返回 null → 清除 context → 401"、T9 从"过期 token 提前 return → 跳过 audit log"等，均形成闭合因果链，无逻辑跳跃。

**[通过]** 汇总表计数自查一致：编码缺陷 11 项、OOD 缺陷 3 项、测试覆盖不足 12 项、测试设计/实现质量 6 项、其他 1 项、误报 1 项，合计 34 项。逐项核对无误。

**[通过]** 依赖图修正：T6→T2 依赖已正确标注、T11→T17 伪依赖箭头已删除、T18 术语"堆外"已更正为"堆内"。三项修正均反映在修订说明 v6 及正文中。

### 3. 覆盖完备性

**[通过]** 全部 34 个待办事项（T1-T34）均已逐一分析，每项包含真实/误报判定和根因分类。

**[通过]** 任务要求的三个核心问题均被覆盖：① 是否真实存在 ② 根因是 OOD 缺陷还是编码缺陷 ③ 修改流程建议（修复批次/依赖图/单次任务大小指导）。

**[通过]** 迭代需求 `a_v4_iteration_requirement.md` 中要求的 3 项持续问题均已正确修正：
- T6/T2 依赖分离 — 已调整
- T11→T17 伪依赖箭头 — 已删除
- T18"堆外"→"堆内" — 已更正

## 质询要点

无。
