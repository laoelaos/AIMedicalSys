# 质量审查报告（v17）

审查时间：2026-06-29
审查轮次：第 17 轮
审查范围：Phase 5 包 G OOD 设计文档 v17（a_v17_copy_from_v16.md）

---

**审查结论：该产出经过 16 轮迭代审议，核心质量已大幅提升。本章检出 7 项问题，其中 1 项严重（架构级依赖矛盾），1 项重要（v17 版本回退导致的图-文不一致），5 项一般。**

---

### Q1 [严重] Phase4ServiceMetaProvider 接口归属与模块依赖方向存在架构矛盾

- **问题描述**：§3.1 定义 `Phase4ServiceMetaProvider` 接口，明确标注"归属：ai-impl/thin-adapter/ 包（底座侧定义，Phase 4 服务选择性实现）"。底座侧在 `doExecuteInternal()` 中通过 `instanceof` 检测后调用。但 ai-impl 对 Phase 4 模块的 Maven 依赖使用 `provided` 作用域，依赖方向为 `ai-impl → Phase 4`。Phase 4 服务若要实现此接口，必须在编译期依赖 ai-impl，这将导致依赖方向反转，违反 §2.2 定义的模块依赖规则。当前表述实际上要求 Phase 4 模块反向依赖 ai-impl，但文档未记录此依赖冲突的解决方案。

- **所在位置**：§3.1「Phase 4 服务可选元数据接口契约」段（约行 1049-1082）；§2.2「模块依赖方向」（约行 393-398）；§8.3 `provided` 作用域说明

- **严重程度**：严重

- **改进建议**：
  方案 A（推荐）：将 `Phase4ServiceMetaProvider` 接口移至 `ai-api/dto/base/` 包（与 `AiRequestBase` 同包），Phase 4 模块已依赖 ai-api，改造代价最低。底座侧通过 `phase4ServiceDelegate instanceof Phase4ServiceMetaProvider` 检查保持不变。
  方案 B：底座侧放弃 `instanceof` 检测方式，改用反射 SPI 机制——Phase 4 模块在 `com.aimedical.modules.*.service` 中定义同名接口方法，底座通过反射安全调用。此方案无需修改 Maven 依赖，但牺牲编译期类型安全。
  方案 C：保留接口在 ai-impl 中，但需在 §2.2 中明确增加一条依赖规则例外，说明 Phase 4 模块可选择性地在 `pom.xml` 中声明对 ai-impl 的 `optional` 依赖以启用元数据暴露功能，并评估是否产生循环依赖风险。

---

### Q2 [重要] §2.3 类图 doDegrade 方法签名缺少 sentinelReason 参数（v17 版本回归）

- **问题描述**：§2.3 类图中 `AbstractCapabilityExecutor` 的 `doDegrade` 方法签名有 14 个参数（行 455），不含 `sentinelReason`。但在 v17 中，§3.4/§3.5/§4.1 统一新增了 `@Nullable String sentinelReason` 参数（如 §4.1 行 3260 的 15 参数签名），§4.1 中所有 `doDegrade()` 调用点均已传入 `sentinelReason`（实验分流异常时传入 `"EXPERIMENT_ASSIGN_ERROR"`，其他场景传入 `null`）。类图未同步更新，导致图-文不一致。实现者若依据类图编码将产生编译错误。

- **所在位置**：§2.3 类图行 455（`AbstractCapabilityExecutor` 的 `doDegrade` 方法签名）；对比 §4.1 行 3260（15 参数方法实现）；对比 §4.1 行 2999/3015/3049/3069/3116/3149/3161/3171/3197/3200/3206/3215/3220/3230/3237（所有 15 参数调用点）

- **严重程度**：重要

- **改进建议**：在 `doDegrade` 方法签名末尾追加 `sentinelReason: String` 参数，使其与 §4.1 伪代码定义一致。同时一并检查 §2.3 中 `AiCallRecord` 的工厂方法是否需要在类图中体现 `sentinelReason` 参数（若类图仅展示字段而非工厂方法则无需修改，当前类图行为正确）。

---

### Q3 [一般] 伪代码中 experimentAssignFailed 变量未声明类型

- **问题描述**：§4.1 `doExecuteInternal()` 伪代码中（行 3033、行 3352），变量 `experimentAssignFailed` 直接以赋值形式出现（`experimentAssignFailed = false`），缺少类型声明。真实 Java 代码中该变量需显式声明为 `boolean`（或在作用域外声明）。当前形式在编码阶段可能引发编译错误或让实现者不确定声明位置。

- **所在位置**：§4.1 行 3033（`experimentAssignFailed = false`）、行 3352（`experimentAssignFailed = true`）

- **严重程度**：一般

- **改进建议**：在行 3033 的 `experimentAssignFailed = false` 前补充 `boolean experimentAssignFailed = false` 声明，使其与 `Object parsedResult = null`（行 3103）的模式一致。

---

### Q4 [一般] 文档头部版本说明未同步更新至 v17

- **问题描述**：文档头部（行 3）称"v7~v16 修订说明保留于尾部作为变更追踪参考"，但尾部实际包含 v7~v17 共计 11 个版本的修订说明（行 4148 至行 4262）。头部版本范围未更新为"v7~v17"，版本同步轻微滞后。

- **所在位置**：行 3（文档头部变更摘要）；行 4253（修订说明（v17））

- **严重程度**：一般

- **改进建议**：将头部说明中的"v7~v16 修订说明保留于尾部"更新为"v7~v17 修订说明保留于尾部作为变更追踪参考"。

---

### Q5 [一般] §2.1 目录结构缺少 Phase4ServiceMetaProvider.java

- **问题描述**：§3.1 文本中明确定义了 `Phase4ServiceMetaProvider` 接口（完整方法签名，约行 1051-1073），并标注归属 `ai-impl/thin-adapter/` 包（行 1053）。但 §2.1 目录结构的 `thin-adapter/` 子段（行 308-314）仅列出 6 个 CapabilityExecutor 实现，未包含此接口文件。

- **所在位置**：§2.1 行 308-314（`thin-adapter/` 子目录）；§3.1 行 1051-1073（接口定义）

- **严重程度**：一般

- **改进建议**：在 §2.1 目录结构的 `thin-adapter/` 子段中补入 `Phase4ServiceMetaProvider.java` 条目，与 §3.1 文本一致。

---

### Q6 [一般] AiCallRecord 工厂方法 callTime 参数类型与伪代码使用方式无矛盾但缺少时间来源说明

- **问题描述**：§3.5 中三个 `AiCallRecord` 工厂方法的 `callTime` 参数类型统一为 `LocalDateTime`，管线伪代码中传入 `LocalDateTime.now()`（行 2103）。但 `LocalDateTime.now()` 获取的是系统默认时区的当前时间，而 JPA 中的 `call_time` 字段（`DATETIME(3)`）在不同数据库（MySQL/PostgreSQL/时区配置）下的存储和读取行为不同。当前文档未说明 `callTime` 应使用系统本地时间还是 UTC 时间，也未记录时区策略。虽然不是代码错误，但属于实现细节遗留。

- **所在位置**：§3.5 行 2103（参数类型说明）；§3.5 行 2196（`call_time` 字段定义 `DATETIME(3)`）

- **严重程度**：一般

- **改进建议**：在 §3.5 工厂方法说明段中补充一句话，明确 `callTime` 的时间来源约定（推荐统一使用 UTC，或在 AiPlatformConfig 中配置 JVM 时区 + 数据库连接时区），使时间存储策略对实现者透明。

---

### Q7 [一般] 热加载定时刷新与事件驱动刷新之间的一致性保证未定义

- **问题描述**：§3.9 定义了四个配置组的热加载机制（自定义定时刷新、@RefreshScope、事件驱动），但未定义定时刷新和事件驱动之间的优先级和冲突解决规则。例如：`ModelRouter` 同时支持 `@Scheduled(fixedDelay=60000)` 定时轮询和 `RouteConfigChangedEvent` 事件驱动两种触发方式（行 3566），文档说"三种触发方式均复用同一刷新方法，通过 synchronized 互斥防止并发刷新"，但未说明：
  - 轮询周期（60 秒）和事件触发之间的延迟差是否可接受
  - 事件丢失后的兜底时间窗口是否过长
  - 定时刷新发现变更与事件刷新发现变更，若出现同一变更被处理两次是否有副作用

- **所在位置**：§3.9「运行时配置热加载机制」（约行 2539-2556）；§6.1 `ModelRouter` 刷新策略（行 3566）

- **严重程度**：一般

- **改进建议**：在 §3.9 热加载机制段落中补充"双机制协同规则"说明——事件驱动为主动刷新路径（毫秒级生效），定时轮询为兜底路径（最长 60 秒后自动感知）。两者刷新同一 `AtomicReference`，执行全量替换，幂等操作，重复触发无副作用。

---

## 综述

产出 v17 在经历了 16 轮迭代审议后，核心设计质量已达到较高水平。本次审查发现的最严重问题是**Phase4ServiceMetaProvider 接口归属与模块依赖方向的架构矛盾**——接口放在 ai-impl 中但需要 Phase 4 模块实现它，这与 ai-impl 对 Phase 4 的 `provided` 单向依赖矛盾。此问题虽不影响底座自身编码，但将导致 Phase 4 模块无法实际实现该接口，薄适配器的 modelId/promptVersion/retryCount 三维度分析缺口将无法按预期补齐。建议优先按方案 A（迁移至 ai-api）修复。

其余问题中，类图 `doDegrade` 签名遗漏 `sentinelReason` 属于 v17 新增功能但未同步更新类图的回归问题，若直接按类图编码将编译失败，建议同步修正。
