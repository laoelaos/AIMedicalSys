# 诊断质询报告（v1）

## 质询结果

LOCATED

## 逐维度审查

### 1. 证据充分性

**[通过]** 报告所有关键代码行为描述均经过源文件验证，包括：
- T1：`JwtTokenProvider.generateAccessToken()`（line 52-63）确无 type claim；`validateToken()`（line 96）确检查 `claims.get("type", String.class)`
- T2：`AuthServiceImpl.refresh()`（line 270-283）检测到异常后仅 `log.warn()` 并继续返回新 Token
- T8：代码 `"30分钟"`/`"15分钟"` 与代码实际一致
- T9：`logout()`（line 182-185）`validateToken` 返回 null 时直接 return 跳过审计日志
- T10：`logout()` line 182 和 line 187 两次完整 JWT 解析
- T11：`getCurrentUser(String token)`（line 303-304）重新解析 token 而非使用 SecurityContext
- T12：`JwtTokenProvider.init()` line 37 使用标准 Base64 正则 `^[A-Za-z0-9+/]+=*$`
- T13：`SlidingWindowCounter` line 14 `private final ReentrantLock lock` 全局锁
- T15：`recordUsernameFailure`/`recordIpFailure`（line 32-49）无窗口过期检查
- T16：`formatMessage`（line 39）先尝试 `MessageFormat.format`，对命名占位符触发异常
- T17：`RestAuthenticationEntryPoint.java:33` 直接 `Result.fail(errorCode)` 未经过插值
- T18：`refreshTimestamps`（line 68）为 `ConcurrentHashMap`，entry 永不被移除
- T3：路径偏差经 `glob` 确认，文件不在 `application/` 下

所有关键推断均有代码或 OOD 文档行号支撑。

### 2. 逻辑完整性

**[通过]** 每项问题从现象到根因的因果链完整，无逻辑跳跃：
- T1：缺失 type claim → validateToken 返回 null → Filter 清除 SecurityContext → 请求视为未认证
- T2：仅 log.warn 不阻断 → 攻击者可持续滥用泄露 token
- T13：OOD 文本内在矛盾（第433行"每IP独立锁" vs 第444行"单ReentrantLock"）→ 编码选择字面实现 → 全局锁性能问题
- 任务依赖图（line 575-588）合理反映了代码级依赖关系（T6→T2、T25→T13）
- 修复批次的分组逻辑（同文件合并、功能点数量约束、子任务间干扰评估、回滚影响分析）充分

### 3. 覆盖完备性

**[通过-轻微]** 所有 34 个待办事项均已分析（31 真实 + 1 其他 + 1 误报）。迭代需求中识别的 3 个问题（T13 分类矛盾、测试适配指引缺失、修复批 6 分组）已在 v8 修订中处理。

**[问题-轻微]** 概述汇总表（line 13）"OOD 文档缺陷"计数为 3（8.8%），但 OOD 缺陷汇总表（line 447-452）在 v8 加入 T13 后实际包含 4 个条目（T2、T8、T13、T17）。虽然 T13 是双归属分类（OOD 主因 + 编码次因），但 T13 在 OOD 汇总表中的出现与概述计数 3 存在矛盾——读者无法判断 T13 是否计入 OOD 分类。若 T13 计入 OOD 则概述应为 4 且编码缺陷相应调整为 10（保持总数 34）；若不计入则应从 OOD 汇总表移除或添加脚注说明。当前状态介于两者之间。

## 质询要点

- **问题**：概述汇总表 OOD 缺陷计数（3）与 OOD 缺陷汇总表条目数（4）不一致
- **原因**：v8 修订在 OOD 汇总表新增了 T13，但未同步更新概述计数或添加标注说明 T13 的双归属分类方式，导致概述与明细表矛盾
- **建议方向**：统一两者——若 T13 计入 OOD 概述计数则改为 4（编码缺陷相应调整为 10），若不计入则从 OOD 汇总表移除 T13 或添加脚注
