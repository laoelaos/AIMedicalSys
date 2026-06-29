根据以下审查结果，迭代上一轮的产出，形成新版的文件，从而更好地满足用户需求。

## 当前审查结果

### 问题 1：[中] T6"与被测代码独立"断言与 T2 行为变更存在矛盾
- 依赖图将 T5/T6/T7 一并标注为"与被测代码修复批独立，可先行或并行"，但 T6（异常刷新检测单元测试）的被测对象正是 AuthServiceImpl.refresh() 第 270-283 行的异常刷新检测逻辑
- T2（修复批 1）将更改此行为——从 log-only 改为拒绝请求并强制清除本地 token
- 若 T6 测试在 T2 之前编写并验证旧行为，T2 实施后需大量返工
- 改进建议：在依赖图中将 T6 从"独立"组中分离，标注 `T6 ──→ T2（测试对象行为变更依赖：T6 的检测条件验证可先行，但响应行为验证需在 T2 实施后更新）`

### 问题 2：[中] T11→T17 依赖箭头缺乏代码级依据
- 依赖图标注 `T11 ──→ T17`，解释为"同属 Filter/Handler 层"
- T11 修改对象：AuthServiceImpl.getCurrentUser()（Service 层），对 RestAuthenticationEntryPoint/RestAccessDeniedHandler 无任何引用
- T17 修改对象：RestAuthenticationEntryPoint、RestAccessDeniedHandler（Security 认证/授权异常出口），对 AuthServiceImpl 无任何引用
- "同属 Filter/Handler 层"描述的是架构层次上的相似性，而非代码依赖或执行时序依赖
- 改进建议：删除 T11→T17 箭头（两者完全独立，可并行实施）。如需保留层次关联信息，改用无箭头的分组标注

### 问题 3：[低] T18 内存估算术语"堆外"使用错误
- 原文："约 10MB 堆外"
- refreshTimestamps 声明为 ConcurrentHashMap<Long, Deque<Long>>（AuthServiceImpl.java:68），所有数据存储在 JVM 堆内内存中
- "堆外"特指通过 DirectByteBuffer、Unsafe.allocateMemory 等方式分配的本机内存，与 ConcurrentHashMap 的分配方式不符
- 改进建议：将"堆外"改为"堆内"或"内存"

### 新增评估结论
- 需求响应充分度：已充分满足用户需求（三项核心要求均已覆盖）
- 迭代修正确认：a_v3_iteration_requirement.md 要求的 4 项修正均已正确实施
- 可操作性系统性评估：整体可执行性较高，存在上述 2 处需优化的依赖/任务切分问题

## 历史迭代回顾

### 已解决的问题
- 迭代 1 的 6 个问题：汇总表计数矛盾、算术错误"23个测试"、T3/T4 严重度-优先级错位、T12 解码器遗漏、T13 cleanup 同步依赖 — 已在 v2~v3 中全部修正
- 迭代 2 的 3 个问题：汇总表分类计数仍不一致、T3 未纳入修复批次、T1→T2 依赖关系缺乏论证 — 已在 v3~v4 中全部修正
- 迭代 3 新增的 T22 误报判定不符代码事实 — 已在 v5 中修正

### 持续存在的问题（本轮重点）
以下 3 个问题自迭代 3 首次报告以来，经质询验证为有效，但在 a_v3_diag_v2 中仍未修正：
1. **T6 与 T2 的依赖关系错误** — 迭代 3 问题 1，本轮问题 1
2. **T11→T17 伪依赖箭头** — 迭代 3 问题 2，本轮问题 2
3. **T18"堆外"术语错误** — 迭代 3 即已报告（当时未记入历史，本轮问题 3）

### 新发现的问题
（无本轮首次识别的新问题——全部 3 个问题均为持续未修复项）

## 上一轮产出路径
Harness/redeliberations/202606270218_diagnose_todo_issues/a_v3_diag_v2.md

## 用户需求
Harness/redeliberations/202606270218_diagnose_todo_issues/requirement.md
