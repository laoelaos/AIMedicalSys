# Phase 5 包G OOD 设计质量审查报告（v1）

审查对象：a_v7_design_v2.md  
审查视角：需求响应充分度、整体深度和完整性、事实错误/逻辑矛盾（侧重内部审议未充分覆盖的维度）  
迭代轮次：第 7 轮

---

## 发现的问题

### 问题 1：[事实错误] §4.1 伪代码中结构化输出解析失败缺少异常处理路径，与 §5.1 错误分类承诺矛盾

**所在位置**：§4.1 CapabilityExecutor.execute() 伪代码第 1172-1177 行；§5.1 错误分类表第 1271 行

**问题描述**：§5.1 错误分类表明确声明"结构化输出解析失败 → 提取 JSON 片段重试 → 仍失败降级 → AiResult.degraded()"。但 §4.1 伪代码中 `structuredOutputParser.parse()` 调用无 try-catch 包裹，紧随其后直接进入 success 指标采集和 `AiResult.success()` 路径。若 `parse()` 抛 ParseException，异常将逃逸出 `supplyAsync` lambda，表现为 CompletionException，而非按文档预期进入降级路径。尽管设计正文声称"管线内部的预期异常已在 execute() 内部捕获并以降级路径处理"，但伪代码并未体现这一捕获逻辑，实现者对此无参照依据。

**严重程度**：严重

**改进建议**：在 §4.1 伪代码 `parse()` 调用外围添加 try-catch，捕获 ParseException → 记录降级指标 → 返回 `doDegrade()`；或在 §3.1 AbstractCapabilityExecutor 模板方法注释中明确阐明此流程，并在 §4.1 伪代码中标注"实际实现由模板方法 doExecuteInternal 内部处理"，以消除伪代码与正文的矛盾。

---

### 问题 2：[事实错误] §3.1 userId 提取方式在未认证上下文中存在 NPE 风险

**所在位置**：§3.1 UserId 与 SessionId 的上下文来源段第 607 行；§4.1 伪代码第 1158 行

**问题描述**：`SecurityContextHolder.getContext().getAuthentication().getName()` 链式调用未处理 `getAuthentication()` 返回 null 的情况。在以下场景中将触发 NullPointerException：(1) 定时任务/批处理作业中执行 AI 调用（无认证上下文）；(2) 匿名访问端点；(3) SecurityContext 未建立或已被清除。§5.1 错误分类表未将此类场景纳入任何错误类别，管线也未提供 fallback userId（如 "SYSTEM" 或 "ANONYMOUS" 占位值）。经过 7 轮迭代，此边界条件始终未覆盖。

**严重程度**：重要

**改进建议**：在 §3.1 userId 来源段补充 null 安全处理策略：明确约定使用 `Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication()).map(Authentication::getName).orElse("SYSTEM")` 或其他占位值；或在 §4.1 伪代码中体现 null 检查与 fallback 逻辑；相应将此类异常归属到 §5.1 错误分类表。

---

### 问题 3：[逻辑矛盾] §2.3 类图 AbstractCapabilityExecutor 方法集与 §3.1 模板方法描述不一致

**所在位置**：§2.3 AbstractCapabilityExecutor 类图第 214-218 行；§3.1 AbstractCapabilityExecutor 模板方法第 696-724 行；修订说明 v8 第 1615 行

**问题描述**：§3.1 模板方法描述中清晰定义了 `doExecuteInternal()`（抽象方法，子类特化正常管线）和 `doExtractDepartmentId()`（可选重写，departmentId 提取方式）两个方法。但 §2.3 类图中 AbstractCapabilityExecutor 仅声明了 `execute()` 和 `doDegrade()` 两个方法，`doExecuteInternal()` 和 `doExtractDepartmentId()` 均缺失。v8 修订说明第 10 条记录了 AbstractCapabilityExecutor 的引入，但类图同步不完整。实现者仅查看类图会误以为只需实现已列出的方法，而实际还需要实现 `doExecuteInternal()`。

**严重程度**：重要

**改进建议**：在 §2.3 类图中为 AbstractCapabilityExecutor 补充 `#doExecuteInternal(startTime, request, capabilityId, departmentId) AiResult<R>`（抽象方法） 和 `#doExtractDepartmentId(request) String`（可重写方法）的声明。

---

### 问题 4：[逻辑矛盾] §3.1 模板方法委托模式与 §4.1 内联伪代码的归属关系不明确

**所在位置**：§3.1 AbstractCapabilityExecutor 模板方法第 696-724 行；§4.1 CapabilityExecutor.execute() 伪代码第 1145-1191 行

**问题描述**：§3.1 的抽象骨架设计采用模板方法模式：`execute()`（final）→ 降级预检 → 委托 `doExecuteInternal()`（抽象）。但 §4.1 的 CapabilityExecutor.execute() 伪代码展示的是完整的管线内联实现（降级预检 → 变量提取 → 实验分流 → 模板渲染 → 模型路由 → 健康检查 → LLM 调用 → 解析 → 指标采集），未区分哪些步骤属于骨架公共部分（模板方法本身）、哪些属于 `doExecuteInternal()` 子类特化。读者无法判定 §4.1 伪代码究竟对应 `execute()` 模板方法的完整实现还是 `doExecuteInternal()` 的示例实现。这是 v8 引入 AbstractCapabilityExecutor 后留下的结构性矛盾。

**严重程度**：重要

**改进建议**：将 §4.1 的 CapabilityExecutor 伪代码按模板方法模式重构为分层结构：标注骨架公共步骤（降级预检、departmentId 提取、doDegrade）与 `doExecuteInternal()` 的分界，或在伪代码开头用注释明确"以下代码对应 AbstractCapabilityExecutor.doExecuteInternal() 的实现模板"。

---

### 问题 5：[完整性缺失] extractVariables() 在 AbstractCapabilityExecutor 的方法契约中未定义

**所在位置**：§3.1 变量提取约定段第 633-636 行；§3.1 AbstractCapabilityExecutor 模板方法第 696-724 行；§4.1 伪代码第 1157 行

**问题描述**：变量提取是完整管线中的关键步骤（将业务 DTO 字段映射为 Prompt 模板变量），§4.1 伪代码第 1157 行明确调用 `extractVariables(request)`，§3.1 描述了两种实现方式（ObjectMapper 转换 vs 自定义方法）。但 `extractVariables()` 既未出现在 AbstractCapabilityExecutor 的类图中，也未出现在模板方法的接口定义中——它不是抽象方法、默认方法、也不是模板方法参数。实现者无契约可循：不知道应该重写哪个方法、在哪个时机调用、返回值格式要求。当前设计将变量提取作为"约定"而非"契约"，对于 7 项底座能力的实现者而言缺少强制性指导。

**严重程度**：重要

**改进建议**：在 AbstractCapabilityExecutor 中显式定义 `doExtractVariables(T request) Map<String, Object>`（protected，默认实现使用 ObjectMapper.convertValue），模板方法在模型路由之前调用此方法。同步更新类图和方法签名。

---

### 问题 6：[事实错误] §3.1 薄适配器文本声称含模型路由检查但伪代码未实现

**所在位置**：§3.1 薄适配器型 CapabilityExecutor 的管线行为第 638-643 行；薄适配器伪代码第 646-683 行

**问题描述**：§3.1 文本描述薄适配器管线"包含：降级预检、**模型路由空值检查后直接委托 Phase 4 业务服务**"，但薄适配器伪代码中在降级预检通过后直接调用 `phase4ServiceDelegate.execute(request)`，未出现任何模型路由步骤或空值检查。文本与伪代码矛盾，实现者不确定是否需要在委托前执行模型路由检查。

**严重程度**：中等

**改进建议**：统一二选一：(a) 若薄适配器确实不需要模型路由（直接委托 Phase 4，模型已在 Phase 4 内部选择），修改文本删除"模型路由空值检查"表述；(b) 若需要，在薄适配器伪代码中补充模型路由步骤和空值判断。

---

### 问题 7：[完整性缺失] LocalRuleFallback 接口返回 raw AiResult，降级路径存在 unchecked 类型转换

**所在位置**：§2.3 类图 LocalRuleFallback 第 430-435 行；§4.1 doDegrade() 伪代码第 1183-1187 行

**问题描述**：`LocalRuleFallback.fallback(Object request)` 返回 raw `AiResult`（无泛型参数），但 CapabilityExecutor 管线期望 `AiResult<R>`。§4.1 `doDegrade()` 伪代码中 `localRuleFallback.fallback(request)` 的结果被直接 `return` 作为 `AiResult<R>`，存在未检查的 unchecked 类型转换。对于仅处方审核一个实现（`PrescriptionLocalRuleFallback`）的场景，当前设计可通过运行时类型擦除正常运行，但未来新增 LocalRuleFallback 实现时，类型安全性无编译时保障。此外接口参数使用 `Object` 类型消除了编译期类型检查。

**严重程度**：中等

**改进建议**：将 LocalRuleFallback 接口泛型化为 `LocalRuleFallback<T, R>`，`fallback(T request) AiResult<R>`，并使 `PrescriptionLocalRuleFallback implements LocalRuleFallback<PrescriptionCheckRequest, PrescriptionCheckResponse>`；关联 CapabilityExecutor 中对 LocalRuleFallback 的引用也使用泛型参数。

---

## 整体质量评价

本产出经过 7 轮迭代修订，整体成熟度较高，覆盖了类图、核心职责、协作关系、关键接口、状态模型等 OOD 核心要素，技术设计深度和完整性在多数维度上满足编码实施需要。当前存在的主要问题集中于：(1) 伪代码实现与设计文本/错误分类承诺之间的不一致（问题 1、4、6），此类问题在编码实施阶段将直接导致实现偏离设计；(2) 新增抽象骨架 AbstractCapabilityExecutor（v8）引入后，类图、模板方法与伪代码之间的同步遗留问题（问题 3、4）；(3) 边界条件和异常场景覆盖仍有缺口（问题 2、1）。以上问题修复后可作为编码实施的充分设计依据。
