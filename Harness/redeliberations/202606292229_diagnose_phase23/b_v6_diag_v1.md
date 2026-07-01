# 质量审查报告 — Phase 23 C/3/DE 诊断报告（v6）

审查时间：第 6 轮
审查视角：需求响应充分度 + 可操作性 + 深度完整性（侧重内部审议未覆盖维度）

---

## 问题 1（中等）— P01 和 A03 两个 P0 条目缺失修复建议

- **所在位置**：a_v6_diag_v1.md:58-63（P01）、a_v6_diag_v1.md:68-73（A03）
- **问题描述**：P01（异步 AI 调度机制未实现）和 A03（AiSuggestionResult 5 状态映射表全部未实现）均为 P0 条目，但只描述问题现象和引用证据，**未给出任何修复方向或实施策略**。同一报告中其他 P0 条目（如 C04 推荐了 TransactionTemplate 编程式事务方案、C14 给出了 5 步精确执行顺序、C06/E03 提供了合并修复策略）均有明确的可执行建议。P01 和 A03 的"缺什么"清楚但"怎么改"完全留白，执行者无法直接从报告中获取修复指导。
- **严重程度**：中等
- **改进建议**：为 P01 补充推荐实施路径，例如：在 DedupTaskScheduler 中增加 `@Async` 方法触发 `AiService.prescriptionAssist()` 调用，schedule() 返回后提交异步任务，或将 schedule() 拆分为"创建 PENDING 条目"和"启动异步回调"两个阶段由调用方编排。为 A03 补充状态机实现策略，例如：在 PrescriptionAssistServiceImpl 中增加异步回调处理器，schedule() 调用后监听/轮询 AiSuggestionResult 状态变更，按 OOD §3.4 的 PENDING→COMPLETED/FAILED 映射完成状态迁移。

## 问题 2（一般）— A02×A10 优先级划分与耦合分析隐含矛盾

- **所在位置**：a_v6_diag_v1.md:64-67（A02，P0）、a_v6_diag_v1.md:370-382（A10，P2）及耦合分析 A02×A10 节（行 444-448）
- **问题描述**：耦合分析约定 A10（application.yml 配置键）必须"先行或同期"于 A02（未来.get() 超时注入），否则 @Value 注入缺少配置来源。但 A02 是 P0（必须立即修复）而 A10 是 P2（可并行修复）。虽然报告中提到"可通过 `:` 默认值兜底避免 Spring 启动异常"，但此缓解方案意味着：若 A10 因属于 P2 被长期搁置，各 AI 调用的超时配置始终以源码 `@Value("${...:8s}")` 中的默认值运行，**配置键永远不会写入 application.yml**，A10 事实上不会被执行。报告既未警示此风险，也未建议将 A10 中的 ai.timeout.* 配置项提升为 P0 的配置子项或标注"建议与 A02 同迭代完成"。
- **严重程度**：一般
- **改进建议**：将 A10 中涉及 ai.timeout.* 和 facade.*.timeout 的配置键定义为 A02 的前置子任务，标注"与 A02 同迭代完成"或赋予独立 P1 优先级。或在耦合分析中增加风险警示段，说明 A10 若被搁置的后果及验收标准（application.yml 中存在对应配置键）。

## 问题 3（一般）— P01×A03 异步 AI 管线耦合未分析

- **所在位置**：a_v6_diag_v1.md:395-448（跨问题耦合副作用分析），缺少 P01×A03 条目
- **问题描述**：P01（异步 AI 调度未实现）和 A03（状态映射表未实现）共同指向 DedupTaskScheduler 的异步 AI 调度管线，是同一段缺失业务逻辑的两个视图。若只修复 P01（增加异步调用触发）而未实现 A03 的状态映射，则异步调用完成后结果无处回填，AiSuggestionResult 永远停在 PENDING；若只修复 A03（状态映射+consumed 标记）而未修复 P01，则状态机虽有定义但无触发入口。两者必须同步实施，否则任一单独修复都会导致运行时行为异常。当前耦合分析章节缺少该群组。
- **严重程度**：一般
- **改进建议**：新增 P01×A03 耦合条目，约定：(1) 同步实施不可拆分；(2) 建议在 DedupTaskScheduler 同一提交中完成调度触发 + 回调处理器 + 状态迁移的完整管线；(3) 修复验收标准为：schedule() 后 AiSuggestionResult 能从 PENDING 成功迁移至 COMPLETED/FAILED。

## 问题 4（一般）— A07×A11 重复修复未被关联

- **所在位置**：a_v6_diag_v1.md:360-364（A07）、a_v6_diag_v1.md:383-386（A11）、耦合分析 A07×A09 节（行 399-405）
- **问题描述**：A07（AiResult.success(data) 允许 data=null）和 A11（业务层防御性判空）的根因修复完全相同——"在 AiResult.success(data) 中增加 Objects.requireNonNull(data) 断言"。A07 和 A11 分别从契约违规和冗余检查两个视角描述同一问题。耦合分析只覆盖了 A07×A09（断言生效后 null-data 路径消除的时序问题），但未关联 A11。这可能导致：(a) 同一处代码修改被分配两次（A07 和 A11 分别派发修复任务）；(b) A11 中"移除业务层冗余判空"的步骤未被纳入 A09 的修复顺序考量——A09 的调用方前置检查依赖"业务层存在判空"这一前提，若 A11 先于 A09 移除判空而 A09 的降级路径尚未就绪，则 PrescriptionAuditServiceImpl 在 A07 断言生效前会短暂暴露 NPE 风险。
- **严重程度**：一般
- **改进建议**：将 A07 和 A11 合并在同一条目下，或明确标注"A11 是 A07 的衍生修复项"。在耦合分析中增加 A07×A09×A11 三向约束：(1) A09 降级路径先上；(2) A07 断言后上；(3) A11 移除冗余判空最后上——因为移除冗余判空前必须确保 A07 断言和 A09 降级路径均已就位。

## 问题 5（轻微）— C04 PESSIMISTIC_WRITE 建议缺少性能影响评估

- **所在位置**：a_v6_diag_v1.md:420-424（C04×E02×S04 耦合分析修复实施顺序第 3 步）
- **问题描述**：推荐在 `findBySessionId` 查询上加 `@Lock(PESSIMISTIC_WRITE)` 以防止并发 UPDATE 冲突，但未分析此悲观锁对分诊流程吞吐量的影响。`findBySessionId` 在 selectDepartment()（行 148）和 E02 修复后的 saveTriageRecord() 中均会被调用，添加 PESSIMISTIC_WRITE 意味着同一个 sessionId 的分诊和选科操作在事务提交前会阻塞其他线程读取该行。对于高并发的分诊场景，此锁策略可能导致吞吐量下降或死锁风险。报告未讨论替代方案（如 OPTIMISTIC 锁 + 重试、应用层队列串行化）。
- **严重程度**：轻微
- **改进建议**：在建议中添加性能影响评估段或锁策略的进一步考量：(a) 仅在 saveTriageRecord 路径上加锁而非全局 findBySessionId 方法上加锁（缩小锁范围）；(b) 评估 OPTIMISTIC 锁 + 重试在 E02 场景下的可行性（@Column(unique=true) 已提供最终保障，OPTIMISTIC 冲突时抛出 DataIntegrityViolationException 后可重试）；(c) 增加对高并发 TPS 场景下降级锁策略的引导（如信号量隔离）。
