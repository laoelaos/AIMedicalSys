# 诊断质询报告（v1）

## 质询结果

LOCATED

## 逐维度审查

### 1. 证据充分性

**[通过]** T1：JwtTokenProvider.generateAccessToken()（L52-63）确无 `type` claim，validateToken()（L88-105）L96 执行 `claims.get("type", String.class)` 比对，JwtAuthenticationFilter（L61）传入 `"access"`，验证路径完整。

**[通过]** T2：AuthServiceImpl.refresh()（L270-283）仅 L280 log.warn，后续 L285-288 继续生成并返回新 Token，无阻断逻辑。

**[通过]** T9：AuthServiceImpl.logout()（L177-206）L182-184 在 claims==null 时直接 return，跳过 L202-203 的审计日志记录。

**[通过]** T10：logout() L182 调用 validateToken 解析一次，L187 调用 getJtiFromToken（JwtTokenProvider L117-127）再次独立解析同一 token。

**[通过]** T12：JwtTokenProvider.init() L37 正则 `^[A-Za-z0-9+/]+=*$` 确为标准 Base64 字符集，L42 使用 `Base64.getDecoder()`。

**[通过]** T13：SlidingWindowCounter.java L14 `private final ReentrantLock lock` 确为全局单锁；tryAcquire() L36-54 在全局锁保护下执行；cleanup() L59-66 同样依赖全局锁。

**[通过]** T15：LoginAttemptTracker.recordUsernameFailure()（L32-39）和 recordIpFailure()（L42-49）均无条件递增计数，无窗口过期检查；窗口清除仅在 isLocked() 方法中惰性处理。

**[通过]** T16：GlobalExceptionHandler.formatMessage()（L34-48）L38-39 优先尝试 MessageFormat.format，对 `{锁定时间}` 这类非数字占位符会抛出 IllegalArgumentException，L42-44 降级为 replaceFirst。

**[通过]** T17：RestAuthenticationEntryPoint.java L33 和 RestAccessDeniedHandler.java L34 均使用 `Result.fail(errorCode)` 直接输出错误码消息，未经过 formatMessage 插值管线。

**[通过]** T19：MenuController.java L152-161 直接调用 `SecurityContextHolder.getContext().getAuthentication()`，未使用 CurrentUser 接口。

**[通过]** T21：AuthServiceTest.java L268 确存在 `when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true)` 冗余 stub；todo.md 标注位置（186-209）与实际位置（262-268）偏差已正确识别。

**[通过]** T3：三条测试文件路径在 `application/` 下均不存在，实际位置已正确指向 common/ 和 integration/ 模块。

### 2. 逻辑完整性

**[通过]** T1/T2/T9/T15/T16 各条因果链完整无跳跃，从代码行为到问题影响形成闭环。

**[通过]** 影响范围判定合理（T18 内存泄漏有定量估算，T2 安全隐患分析到位）。

**[通过]** 迭代需求的 8 项审查意见均已闭环回应，其中问题 1-6（高/中）全部实质修复，问题 7-8（低）已补充。

### 3. 覆盖完备性

**[通过]** 全部 34 个待办事项均完成根因分析，每项给出真实/误报判定并分类。

**[通过]** 迭代需求中的所有问题现象（汇总表矛盾、23→20 算术错误、T3/T4 严重度矛盾、T12/T13 连带影响遗漏、副作用风险评估缺失、T8 章节引用错误）均有覆盖和修正。

**[通过-轻微]** 汇总章节（L452-453）将 T3、T33 列在"测试设计/实现质量问题"下（共 8 项），而主汇总表（L16）显示测试设计/实现质量为 6 项（T3 和 T33 归入"其他"类别）。两处分类口径存在细微差异，但不影响根因定位的可理解性。

## 质询要点

无严重/一般问题。仅一处表述口径微调建议：汇总章节文本（L452-453）建议对齐主汇总表的分类口径，避免读者对 T3/T33 的归类产生困惑。
