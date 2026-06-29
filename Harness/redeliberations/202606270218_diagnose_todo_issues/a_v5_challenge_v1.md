# 诊断质询报告（v5）

## 质询结果

LOCATED

## 逐维度审查

### 1. 证据充分性

**[通过]** T1 关于 Access Token 无 type claim 的根因判定准确，代码验证 `JwtTokenProvider.java:52-63` 确实无 type claim，Filter 验证路径一致
**[通过]** T8 关于 ACCOUNT_LOCKED args 的根因判定准确，代码验证 `AuthServiceImpl.java:104,110` 传参 "30分钟"/"15分钟"，OOD 示例确实使用不同值
**[通过]** T12 关于 Base64 字符集的证据充分，代码确认 `JwtTokenProvider.java:37` 使用标准 Base64 正则且 `Base64.getDecoder()` 解码
**[通过]** T13 关于全局锁的证据准确，代码确认 `SlidingWindowCounter.java:14` 使用全局 `ReentrantLock`，`tryAcquire()` 与 `cleanup()` 均在全局锁保护下
**[通过]** T14 关于 JwtUtil 遗留 claims 的判定准确，代码确认 `JwtUtil.java:75-77` 含 role/position claims 且无 jti
**[通过]** T15 关于 record* 方法无窗口过期防御的判定准确，代码确认 `LoginAttemptTracker.java:32-49` 仅递增计数不检查窗口
**[通过]** T16 关于 MessageFormat 异常开销的描述准确，代码确认 `GlobalExceptionHandler.java:38-44` 先尝试 MessageFormat 后 catch 降级
**[通过]** T18 关于 refreshTimestamps 无过期清理的判定准确，代码确认 `AuthServiceImpl.java:271` 的 compute 闭包仅惰性清理过期条目但从不移除整个 entry

### 2. 逻辑完整性

**[通过]** T1-T34 的根因因果链完整，从代码行为到根因的推导无逻辑跳跃
**[通过]** T13 根因归属（OOD 文本内在模糊 + 编码实现跟随）正确反映了第 433 行"每 IP 独立加锁"与第 444 行"ReentrantLock 保护原子性"的内在矛盾
**[通过]** T17 根因分类统一（OOD 文档遗漏主因 + 编码未覆盖次因），逻辑自洽
**[通过]** T6→T2 依赖关系已修正（检测条件验证可先行，响应行为验证需在 T2 后），符合代码实际依赖
**[通过]** 汇总表分类计数与逐项分析一致：编码缺陷 11 项、OOD 缺陷 3 项、测试覆盖不足 12 项、测试设计/实现质量 6 项、其他 1 项、误报 1 项 = 34 项

### 3. 覆盖完备性

**[通过]** 全部 34 个待办事项均已完成根因分析
**[通过]** 每个问题均判断了真实/误报属性
**[通过]** 已完成 OOD 缺陷与编码缺陷的区分
**[通过]** 修复批次划分合理，依赖图清晰
**[通过]** 各批次风险标注完整（批 3 子任务间干扰评估、批 4 回滚影响范围）

## 质询要点（CHALLENGED 时存在）

（无）
