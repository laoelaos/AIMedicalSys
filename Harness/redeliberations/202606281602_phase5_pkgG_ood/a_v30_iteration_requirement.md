根据以下审查结果，迭代上一轮的产出，形成新版的文件，从而更好地满足用户需求。

## 当前审查结果

### 问题1：[重要] §4.2 薄适配器超时 WARN 日志使用全局默认值而非有效超时值
薄适配器 `doExecuteInternal()` 伪代码中，委托调用使用 `effectiveThinAdapterTimeout`（支持 per-capability 覆盖）作为实际超时值（第 3927、3932 行），但超时降级的 WARN 日志（第 3934 行）却使用了 `thinAdapterTimeout`（全局默认值）记录超时阈值。当 per-capability 超时覆盖生效时日志显示默认值，严重误导运维排障。
- 所在位置：§4.2 薄适配器特化管线伪代码，第 3927 行 vs 第 3934 行
- 严重程度：重要
- 改进建议：将第 3934 行的 `thinAdapterTimeout.toMillis()` 替换为 `effectiveThinAdapterTimeout.toMillis()`

### 问题2：[重要] LlmChatOptions 阶段一填充伪代码与参数白名单声明不一致
§3.2 第 1845 行声明参数映射白名单包含 six 个 key，但 §4.1 伪代码的阶段一填充（第 3538-3544 行）仅映射了前三个（`temperature`、`maxTokens`、`stopSequences`），后三个（`topP`、`frequencyPenalty`、`presencePenalty`）缺失，导致路由配置值被忽略而使用 SDK 默认值。
- 所在位置：§3.2 第 1845 行（白名单）vs §4.1 第 3538-3544 行（伪代码）
- 严重程度：重要
- 改进建议：在 §4.1 伪代码阶段一填充中补充 `topP`、`frequencyPenalty`、`presencePenalty` 三个字段的映射逻辑

### 问题3：[一般] PrescriptionAssist 变量提取策略与 DTO 嵌套结构不匹配
`PrescriptionAssistCapabilityExecutor` 特化设计表（§3.11.4 第 3147 行）声明使用 `ObjectMapper.convertValue` 从 DTO 映射为扁平键值对，但 `PrescriptionAssistRequest` 将患者信息封装为 `PatientInfo` 内嵌值对象，`convertValue` 对嵌套对象产生嵌套 key（如 `patientInfo.age`）而非扁平 key（如 `patientAge`），导致模板变量无法正确填充。而 `PrescriptionCheckCapabilityExecutor`（第 3119 行）对相同结构使用自定义手工展开方式，两者不一致。
- 所在位置：§3.11.4 第 3145 行（模板变量）、第 3147 行（策略）；§3.11.2 第 3119 行（对比）
- 严重程度：一般
- 改进建议：统一 PrescriptionAssist 和 PrescriptionCheck 的患者数据建模方式和变量提取策略——推荐改为方式 B 自定义展开手工将 `PatientInfo` 展开为扁平变量，或改为扁平字段结构

### 问题4：[一般] AiCallRecord.capabilityName 字段无填充来源
§3.5 `AiCallRecord` 字段定义表（第 2207-2208 行）列出 `capabilityName: String` 字段，但三个工厂方法签名（`success()`/`failure()`/`degraded()`，第 2232-2252 行）的入参中均不包含该参数，§4.1 所有调用点均未传入，实施者无法获知填充来源。
- 所在位置：§3.5 第 2207-2208 行（字段定义）、第 2232-2252 行（工厂方法签名）；§4.1 各调用点
- 严重程度：一般
- 改进建议：方案 A（推荐）：在工厂方法签名中补充 `String capabilityName` 参数，同步更新所有调用点；方案 B：在字段定义注释中说明由 `capabilityId` 从配置映射表自动补全

### 问题5：[一般] 精确 Tokenizer 路径与字符估算回退路径共用同一 3000 阈值判断，v27 修订意图未落实
§4.1 `DiscussionConclusionCapabilityExecutor.doExecuteInternal()` 伪代码中，精确 Tokenizer 路径（第 3817-3827 行）和字符估算回退路径（第 3828-3842 行）各自计算 `estimatedTokens` 后，在第 3846 行共用同一 `> 3000` 阈值判断来决定是否触发前置压缩。v27 修订说明第 9 条（第 4704 行）明确要求将估算回退路径的触发阈值提升至 4000，但伪代码仅添加了 WARN 日志未改变实际触发阈值。
- 所在位置：§4.1 第 3817-3846 行；v27 修订说明第 9 条（第 4704 行）
- 严重程度：一般
- 改进建议：按路径区分阈值——精确路径使用 3000、估算路径使用 4000（或等价于校正后实际 Token 为 3000 的字符数阈值）

## 历史迭代回顾

### 已解决的问题（出现在历史反馈但当前反馈中不再提及）
- 降级策略注入机制的两套并行描述冲突（v3 问题 1、3 → v7 问题 4 → 已消除）
- 修订说明与设计正文混合问题（v4 问题 1 → 已剥离归档）
- 文档内部迭代标记残留（v4 问题 2 → 已清理）
- §3.11 节编号不连续及章节位置异常（v4 问题 3 → 已重新编号）
- 多实例行为约束缺失（v2 问题 2 → 已新增 §1.5）
- API Surface 状态表缺失（v2 问题 3 → 已新增 §1.6）
- 降级路径系统性双重计数（v16 问题 1 → 已修复）
- CallContext 参数签名迁移不一致（v21~v24 连续多轮 → 已落地迁移计划）
- 薄适配器构造器 super() 参数不匹配（v24 问题 1 → 已修复）
- Phase4ServiceMetaProvider 接口归属矛盾（v18 问题 1 → 已迁移至 ai-api）
- SlidingWindowMetricsStore @RefreshScope 清空窗口数据（v20 问题 1 → 已移除）
- doDegrade() 参数签名四态并存（v28 问题 1 → 已有迁移计划）

### 持续存在的问题（在多轮反馈中反复出现，需重点解决）
- **薄适配器超时 WARN 日志使用全局默认值**（v1 诊断问题 3 → v29 诊断问题 1）：首轮诊断已提出，v29 修订未涉及此区域，仍需修正
- **LlmChatOptions 参数映射不完整**（v3 问题 10 → v6 问题 2 → v29 问题 2）：从最初类图缺失 topP/frequencyPenalty/presencePenalty 字段逐步演进，v29 发现伪代码阶段一填充仍缺失这三个字段的映射逻辑
- **PrescriptionAssist/PrescriptionCheck DTO 建模方式不一致**（v11 问题 7 → v29 问题 3）：持续多轮未统一
- **Tokenizer 精确/估算路径阈值未区分**（v25 问题 2 → v26 问题 4 → v29 问题 5）：v27 修订意图要求估算路径阈值提升至 4000，但伪代码仍未落实

### 新发现的问题（本轮新识别的问题）
- **AiCallRecord.capabilityName 字段无填充来源**（v29 问题 4）：此前 29 轮迭代均未发现此设计缺口

## 上一轮产出路径
C:\Develop\Software\AIMedicalSys\Harness\redeliberations\202606281602_phase5_pkgG_ood\a_v29_copy_from_v28.md

## 用户需求
C:\Develop\Software\AIMedicalSys\Harness\redeliberations\202606281602_phase5_pkgG_ood\requirement.md
