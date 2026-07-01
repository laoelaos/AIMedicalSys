根据以下审查结果，迭代上一轮的产出，形成新版的文件，从而更好地满足用户需求。

## 当前审查结果

### 问题 1（严重）：Phase4ServiceMetaProvider 存在并发安全设计缺陷
`Phase4ServiceMetaProvider` 接口（§3.1, lines 1050-1076）的三个方法定义在 Phase 4 服务的单例 Bean 实例级别，语义为返回"最近一次"内部调用使用的元数据。在并发环境中多个请求线程同时调用同一 Phase 4 服务时，`getRetryCount()` 等接口返回的数据可能属于不同线程的请求，导致 `AiCallRecord` 中的元数据跨请求污染。
- **所在位置**：§3.1 Phase4ServiceMetaProvider 接口定义（lines 1050-1076）、§4.2 薄适配器成功路径元数据提取（lines 3449-3459）
- **改进建议**：方案 A（推荐）：将元数据返回方式从"服务实例级接口"改为"请求/响应级绑定"——Phase 4 服务在响应 DTO 中内嵌元数据字段，薄适配器直接读取响应 DTO 的元数据。方案 B：将 `Phase4ServiceMetaProvider` 改为请求级上下文对象，通过方法参数传递。

### 问题 2（重要）：类图缺少 `doDegrade` 方法，且 v18 修订声明与实际内容不一致
v18 修订说明第 2 条声明"§2.3 类图 doDegrade 方法签名补充 sentinelReason 第 15 个参数"，但实际 §2.3 类图中 `AbstractCapabilityExecutor` 类节点没有 `doDegrade` 方法的任何声明。
- **所在位置**：§2.3 类图 AbstractCapabilityExecutor（lines 448-465）与 §4.1 `doDegrade()` 伪代码（lines 3276-3303）
- **改进建议**：在 §2.3 类图 `AbstractCapabilityExecutor` 类节点中新增 `doDegrade` 方法声明（与 §4.1 伪代码签名一致）。同步修正 v18 修订说明的表述。

### 问题 3（重要）：DiscussionConclusionCapabilityExecutor 前置 LLM 压缩调用缺少模型路由设计
§4.1 特化伪代码（lines 3330-3346）中前置压缩调用通过 `LlmChatService.chat(compressRequest)` 发送请求。但 `compressRequest` 的 `clientType` 字段仅标注注释"从 ModelRouter 获取或使用默认"。压缩调用发生在实验分流/模板渲染/模型路由之前，此时无法获知目标模型端点和 clientType。
- **所在位置**：§4.1 DiscussionConclusionCapabilityExecutor.doExecuteInternal() 特化伪代码（lines 3307-3388），特别是 line 3339
- **改进建议**：方案 A：为压缩调用引入固定的轻量模型配置（硬编码 endpoint + 低成本的摘要模型）。方案 B：在 `extractVariables()` 阶段之前允许一次独立的 `ModelRouter` 调用，结果缓存供压缩和主调用复用。

### 问题 4（重要）：`estimateTokens()` 方法未定义
§4.1 伪代码（line 3326）引用 `estimateTokens(transcripts)`，设计文档未给出使用的 Tokenizer、中文字符 Token 换算比例、角色标记开销等具体信息，实现者无法直接编码。
- **所在位置**：§4.1 DiscussionConclusionCapabilityExecutor.doExecuteInternal()（line 3326）
- **改进建议**：补充 `estimateTokens()` 的具体实现策略：(a) 明确 Tokenizer 方案；(b) 给出中文医疗文本保守换算比例；(c) 说明 >3000 阈值的决策依据。

### 问题 5（中等）：薄适配器异常分类的字符串匹配存在维护脆弱性
§4.2 薄适配器 catch 块（lines 3432-3433）通过字符串数组匹配 6 个已知 Phase 4 业务异常类名。若 Phase 4 模块重构时重命名异常类，薄适配器将静默将业务异常归类为基础设施异常。
- **所在位置**：§4.2 薄适配器特化管线伪代码（lines 3424-3445）
- **改进建议**：方案 A（推荐）：建立 Phase 4 业务异常的公共基类约定，薄适配器通过 `instanceof` 匹配。方案 B：通过 `Class.forName()` 加 `isInstance()` 实现可配置的匹配。

### 问题 6（中低）：`structuredChat`→`chat` 回退路径的 `retryCount` 覆盖不一致未声明
§4.1 `doExecuteInternal()` 中两条成功路径的 `retryCount` 含义不同——structuredChat 路径的计数包含 structuredChat 内部重试，chat 回退路径的计数仅包含 chat 层重试，导致回退场景下 retryCount 偏低，且未在任何文档位置说明此差异。
- **所在位置**：§4.1 doExecuteInternal() 伪代码（lines 3154-3155 structuredChat 成功路径，lines 3198-3199 chat 回退路径）
- **改进建议**：方案 A：在 chat 回退路径注释中补充说明 retryCount 仅反映 chat 层重试次数。方案 B：在 `StructuredOutputNotSupportedException` 中加入 `originalRetryCount` 字段，回退时累加此值。

## 历史迭代回顾

### 已解决的问题
- **Phase4ServiceMetaProvider 接口归属与模块依赖方向矛盾**（迭代 17 问题 1）：接口从 ai-impl/thin-adapter/ 迁移至 ai-api/dto/base/，依赖方向矛盾已解决，当前反馈（问题 1）聚焦于接口本身的并发安全设计缺陷，非同一问题
- **降级路径系统性双重计数**（迭代 16 问题 1）：已在 v16 中修复，降级记录统一由 doDegrade() 承担
- **parseTimeout 硬编码**（迭代 14 问题 7）：已抽取为可配置项
- **线程模型饥饿风险**（迭代 13 问题 2，迭代 14 问题 4）：讨论结论前置压缩调用的线程隔离方案已在 v15 定稿

### 持续存在的问题（在多轮反馈中反复出现）
- **问题 2（类图缺少 doDegrade 方法）**：迭代 17 问题 2 → 迭代 18 问题 2 → v18 声明修复但实际未修复，三连同类问题尚未解决
- **问题 3（讨论结论前置压缩缺少模型路由设计）**：迭代 13 问题 2（线程隔离）→ 迭代 18 问题 3（模型路由缺口）→ v18 未解决
- **问题 4（estimateTokens 未定义）**：迭代 13 问题 2 关联 → 迭代 18 问题 4 → v18 未解决
- **问题 1（Phase4ServiceMetaProvider 并发安全）**：迭代 18 问题 1 → v18 接口归属已修正但并发安全根本设计缺陷仍在

### 新发现的问题
- **问题 6（retryCount 覆盖不一致未声明）**：本轮新识别的可观测性数据完整性问题

## 上一轮产出路径
C:\Develop\Software\AIMedicalSys\Harness\redeliberations\202606281602_phase5_pkgG_ood\a_v18_copy_from_v17.md

## 用户需求
C:\Develop\Software\AIMedicalSys\Harness\redeliberations\202606281602_phase5_pkgG_ood\requirement.md
