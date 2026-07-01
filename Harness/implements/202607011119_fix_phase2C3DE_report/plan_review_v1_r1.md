# 计划审查报告（v1 r1）

## 审查结果
**REJECTED**

## 发现

### **[严重] R1 实施方式不明确，存在误导风险**

Plan R1 描述：`使用 CompletableFuture 或直接 RestTemplate 级别超时保护`。`或` 字表示两种完全不同的实施路径，Planner 未做技术决策。

- **CompletableFuture 方案**：在 TriageServiceImpl.findDoctorsForDepartments 调用层用 `CompletableFuture.supplyAsync(() → doctorFacade.findAvailableDoctorsByDepartment()).orTimeout(doctorFacadeTimeout, SECONDS)` 包装。此方案需使用线程池（当前未指定），引入额外线程切换开销，且 DoctorFacade 为同步接口（非 Future），`supplyAsync` 需借用公共线程池或自定义池。
- **RestTemplate 级别方案**：在 DoctorFacade 实现侧（doctor 模块）设置 Feign/RestTemplate 的 connectTimeout 和 readTimeout。此方案不需改动 TriageServiceImpl 调用逻辑（现有 catch Exception 已覆盖），但修改范围涉及 doctor 模块，且 `consultation.doctor-facade.timeout` 配置值需跨模块传递。

当前代码中 `doctorFacade` 接口为同步阻塞式（`List<AvailableDoctor> findAvailableDoctorsByDepartment(String departmentId)`），无异步返回——直接用 `CompletableFuture.supplyAsync` 包装会产生额外开销且需线程池。OOD 要求 `通过 @Value 或 feign/restTemplate 超时配置注入`，Plan 应选择一个具体方案并描述变更锚点和影响范围。

**修正方向**：移除 `或` 表述，指定唯一实施方案。推荐在 DoctorFacade 实现侧配置 RestTemplate 超时 + 将 `consultation.doctor-facade.timeout` 注入到 TriageServiceImpl 中使用 `CompletableFuture.supplyAsync(...).get(timeout, SECONDS)` 作为双重保障（与 task_v1.md 要求 4 `保持现有 try/catch 结构，将超时保护嵌入调用逻辑` 一致）。

### **[一般] R1 未体现 task_v1.md 详细要求**

Plan R1 仅写了 `注入超时配置`，缺少 task_v1.md 明确要求的 3 个行为细节：
1. **WARN 日志格式**：未提及记录调用耗时、异常类型和科室 ID（task_v1 要求 3）
2. **空列表兜底**：未提及超时时将 `TriageResponse.doctors` 置为空列表（task_v1 要求 3）
3. **保持现有结构**：未提及复用现有 try/catch 结构（task_v1 要求 4）

这些细节决定了实现正确性，Plan 过于简略可能导致细节遗漏。

**修正方向**：补充上述三项行为要求到 R1 描述中。

### **[一般] 任务 16「P1 其余低风险项批量修复」粒度过粗**

任务 16 涉及约 30 项跨 consultation/prescription/medical-record/application 四个模块的修复，工作量预估 8 人时。风险：
- 30+ 项无子项拆分清单，执行时无法追踪进度
- 跨模块耦合：T8/T9/T18/T19/T20/T21/T22/T35 等涉及不同模块的 Repository/Converter/Service，8 人时内完成 30 项过于乐观
- 无明确的验证标准，修复后无法逐项确认

**修正方向**：将任务 16 拆分为 3-4 个子任务（按模块或按修复类型分组），每项预估 ≤ 3 人时，提供子项清单。

### **[一般] P2 问题 T24 未被覆盖**

诊断报告 P2 项 T24（`ConcurrentHashMapStore 缺少 Spring 注解`，诊断报告行 297-300）未被分配至任何任务。ConcurrentHashMapStore 缺少 `@Service` 注解意味着不会被 Spring 自动扫描注册，可能导致 SuggestionStore 的依赖注入失败。虽然 DraftContextStoreImpl 有 `@Service` 注解，但 ConcurrentHashMapStore 作为三项 Store 接口的 Phase 2/3 默认实现，缺少注解会影响 SuggestionStore（被 DedupTaskScheduler 依赖）和 SessionStore（被 DialogueSessionManager 依赖）的注入。同时，代码中 ConcurrentHashMapStore 只 `implements SuggestionStore`，未实现 `SessionStore` 和 `DraftContextStore`，与 OOD 要求不符，需确认是否需要补充实现。

**修正方向**：在任务 17（或新增独立任务）中补充修复 T24——为 ConcurrentHashMapStore 添加 `@Service` 注解，并补充缺失的 `SessionStore`/`DraftContextStore` 实现（或创建专门 Store 实现类）。

### **[轻微] 任务 16 命名与内容不一致**

任务 16 标题为「P1 其余低风险项批量修复」，但列表中包含 P2 级别项（P09 `PrescriptionItem.unit 字段`、P12 `DrugInteractionRule Phase 4 预留`、P15 `降级 BLOCK 路径 reasons 固定字符串`、A08 `降级路径 fallback 文案硬编码英文`、A11 `个别业务处过度防御性检查`），与任务 17「P2 项批量修复」含义重叠。易导致 P2 项被重复修复或遗漏。

**修正方向**：将 P2 项从任务 16 移至任务 17，或统一任务标题为「低风险项批量修复」并明确包含 P1+P2 混合。
