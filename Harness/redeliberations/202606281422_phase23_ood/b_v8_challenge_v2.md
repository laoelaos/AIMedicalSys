# 质量质询报告（v2）

## 质询结果

CHALLENGED

## 逐维度审查

### 1. 证据充分性

**[通过]** 问题1（DoctorFacade 包名与 auth 语义不匹配）——经核实源码，common-module-api/.../auth/ 包下确仅有 UserFacade/CurrentUser/UserInfoResponse，DoctorFacade 放入 auth 包不合理，证据充分。

**[通过]** 问题3（合并症检查遗漏）——需求文档 §3.4.2 检查项 #2 原文确为"检查处方药品是否与患者的过敏史冲突；是否与患者的合并症冲突"（L848），设计文本 AllergyCheckRule 仅覆盖过敏史冲突，合并症-药品禁忌检查无对应规则实现，证据充分。

**[通过]** 问题5（TriageRecord 缺推荐医生快照）——需求文档 §5.1（L1287）"分诊记录"实体明确列有"推荐医生"字段，设计文本 TriageRecord 确无 recommendedDoctors 快照字段，证据充分。

**[通过]** 问题4（AiResult partialData 歧义）——AiResult.java 源码确仅有 success/data/errorCode/degraded/fallbackReason 五字段，degraded() 工厂方法确将 data 设为 null，设计文本"新增 partialData 或使用现有 data 字段"确为歧义描述，证据充分。

**[通过]** 问题15（TTL 清理竞态与规则快照失效）——设计文本 §6.1 确仅描述"每5分钟扫描清理"而未说明清理与并发访问的竞态处理；DialogueSession 快照版本查询无结果时的行为确未定义，证据充分。

**[问题-严重]** 问题14（错误码命名 _AI_ 中段）声称需求文档 §3.4 引言层约定"所有错误码必须保留 _AI_ 中段"，因此 RX_ASSIST_DOSE_STANDARD_NOT_FOUND 不含 _AI_ 中段违反约定。但审查报告忽略了关键上下文：需求文档 §3.4 引言（L814-818）的命名约定明确限定范围为"3.4.x 所有 AI 能力的错误码"——即 AI 调用过程产生的错误码。RX_ASSIST_DOSE_STANDARD_NOT_FOUND 是本地业务逻辑错误码（剂量标准不存在），并非 AI 调用超时/不可用/输入无效等 AI 能力错误，其语义与 AI 能力错误码根本不同。需求文档 L817 定义的错误类型枚举（TIMEOUT/UNAVAILABLE/INPUT_INVALID/NO_RECOMMENDATION/CONFLICT/OUTPUT_INCOMPLETE/MODEL_FORBIDDEN/INTERNAL_*_FAIL）全部是 AI 调用链路上的错误类型，不包含"业务数据缺失"类别。将非 AI 错误码强行套用 AI 命名约定属于过度泛化，问题14 的核心判定依据存在证据缺陷——错将 AI 能力错误码的命名约定覆盖到了本地业务错误码。

**[建议]** 撤回问题14，或重新定位为：设计文本 §5.1 需将 AI 能力错误码与本地业务错误码的命名规则分别说明（而非声称"非 AI 业务错误码违反了 AI 命名约定"）。

### 2. 逻辑完整性

**[通过]** 16个问题之间无矛盾，各问题定位到不同设计区域，独立性强。

**[通过]** 改进建议与问题一致且可行——问题3、4、5的改进建议明确具体，可直接指导修改。

**[问题-严重]** 问题9和问题16对 allergyHistory/allergyDetails 的数据来源分析存在内部逻辑不一致。问题9认定"需求文档 §3.1.6 规定 allergyHistory 应从健康档案的 allergen 列表以中文逗号拼接，即后端是实现者"，并建议"前端仅传 patientId，后端在 Service 层从健康档案自动提取并拼接"。但需求文档 §3.1.6（L396-398）的原文明确是过渡方案，规定了三层行为：(1) allergy_history 保持 string 由后端拼接；(2) allergy_details 为可选扩展容器，"默认缺省，业务方确认纳入后再由后端拼接传递"；(3) 前端在健康档案编辑界面填写 reaction_type/severity 时，将结构化数据"存入 allergy_details 扩展容器"。问题9建议"前端仅传 patientId"与需求文档第(3)层行为冲突——需求文档明确允许前端存入 allergy_details 数据。同时问题16建议"前端请求 DTO 中 allergyDetails 为可选字段（用于离线/缓存场景的临时覆盖）"，这与问题9的"前端仅传 patientId"直接矛盾——如果前端不传 patientInfo（仅传 patientId），则 allergyDetails 作为前端请求 DTO 的可选字段无从谈起。问题的改进建议自身存在逻辑矛盾。

**[建议]** 统一问题9和问题16的数据来源分析：allergyHistory 确认由后端拼接（与 §3.1.6 第1层一致），allergyDetails 按 §3.1.6 过渡方案第3层——允许前端存入但后端从健康档案实体自动提取为 single source of truth。改进建议应统一为：AuditRequest/PrescriptionAssistRequest 中 allergyDetails 字段保留（对齐过渡方案的前端存入场景），但后端 Service 层优先从健康档案实体自动生成 allergyDetails，前端传入值仅作为 fallback/离线场景覆盖。需在 §3.2 和 §3.4 补充来源优先级说明。

### 3. 覆盖完备性

**[通过]** 审查维度覆盖充分——需求响应充分度（问题3合并症遗漏、问题5推荐医生缺失）、整体深度和完整性（问题4歧义、问题8比较语义、问题10/11跨模块关系）、异常场景和边界条件（问题15竞态、问题7错误码传递）均有覆盖。

**[通过]** 审查整体评价中对3项严重问题的识别准确——问题3（合并症检查遗漏）、问题4（AiResult partialData 歧义）、问题5（TriageRecord 缺推荐医生快照）确为影响编码实现的核心问题。

**[问题-严重]** 需求文档 §3.4.3（L865-883）对病历生成定义了完整的超时降级服务质量要求，包括非流式和流式两种模式的超时阈值和部分内容保留策略。审查报告问题4聚焦于 AiResult 的实现路径歧义（data 字段 vs partialData 字段），但遗漏了一个更根本的设计完整性问题：设计文本 §3.3 MedicalRecordService 的"非流式超时降级路径"仅描述了超时后提取部分结果的行为，但需求文档 §3.4.3（L883）明确定义了"错误码补齐：MR_GEN_AI_TIMEOUT 适用于非流式超时、流式首字超时、流式过程总时长超时、流式分片间隔超时等所有超时场景，并保留已生成的部分内容"——即 MR_GEN_AI_TIMEOUT 错误码需要在 AiResult 中传递，但 AiResult.failure() 工厂方法将 data 设为 null，无法同时携带错误码和部分数据。这意味着设计文本需要修改的是 AiResult 的 failure 路径（而非仅 degraded 路径），问题4的改进建议仅覆盖了 degraded() 工厂方法的重载，未覆盖 failure 路径——超时是 failure+errorCode+partialData 的组合，不是 degraded+fallbackReason+partialData 的组合。此遗漏影响了问题4改进建议的完整性。

**[建议]** 问题4改进建议补充：AiResult.failure() 也需新增重载 `failure(String errorCode, T partialData)`，或在超时场景由 AI 实现直接构造 `AiResult(success=false, partialData, errorCode, degraded=false, fallbackReason=null)`，不使用现有 failure() 工厂方法。与 degraded 重载建议合并，统一说明 AiResult 在超时场景如何同时携带错误码和部分数据。

**[问题-一般]** 审查报告在"需求响应充分度"维度下遗漏了一项关键覆盖检查：需求文档 §3.4.10（L994）辅助开方的输出契约中错误码包括 `RX_ASSIST_AI_NO_RECOMMENDATION`，设计文本 §3.4 虽定义了此场景的行为（AI 返回空处方草案），但在 §5.1 错误码表中"开方辅助"类别未列出该错误码——表中仅有 `RX_ASSIST_DOSE_STANDARD_NOT_FOUND` 和 `RX_ASSIST_AI_TIMEOUT`/`RX_ASSIST_AI_UNAVAILABLE`，缺 `RX_ASSIST_AI_NO_RECOMMENDATION`。同样，分诊模块错误码表缺少需求 §3.4.1 定义的 `TRIAGE_AI_INPUT_INVALID`。这些错误码是需求文档明确定义的输出契约字段，遗漏影响编码实现的错误码定义完整性。

**[建议]** 在问题列表中补充：§5.1 错误码表需补齐需求文档 §3.4.x 明确定义的全部错误码，至少包括 RX_ASSIST_AI_NO_RECOMMENDATION 和 TRIAGE_AI_INPUT_INVALID。

### 4. 报告必要性

**[通过]** 问题1（DoctorFacade 包名位置）虽为包结构细节，但 auth 包语义冲突会导致编码时误导开发者，对可落地性有实际影响，不构成过度细节。

**[通过]** 问题14（错误码命名）作为轻微问题列出是合理的严重度判定，但核心证据存在缺陷（详见证据充分性维度），并非不必要的细节问题。

**[通过]** 问题12（降级前端 UI 行为）、问题13（底座落地论证）均为轻微级别且对前端/后端实现者有参考价值，报告必要性成立。

## 质询要点

### 要点1

- **问题**：问题14将 RX_ASSIST_DOSE_STANDARD_NOT_FOUND 不含 _AI_ 中段判定为"违反需求文档 §3.4 引言命名约定"，但需求文档 §3.4 引言（L814-817）的命名约定限定范围为"3.4.x 所有 AI 能力的错误码"，错误类型枚举（TIMEOUT/UNAVAILABLE/INPUT_INVALID 等）均为 AI 调用链路上的错误类型，不包含"本地业务数据缺失"类别。RX_ASSIST_DOSE_STANDARD_NOT_FOUND 是本地业务逻辑错误码，不属于 AI 能力错误码命名约定覆盖范围。
- **原因**：问题14的核心判定证据存在过度泛化——将 AI 能力错误码的命名规则错误覆盖到本地业务错误码，导致审查结论不可信。如果设计者据此将所有非 AI 错误码也加上 _AI_ 中段（如 RX_ASSIST_AI_DOSE_STANDARD_NOT_FOUND），将造成语义混乱——_AI_ 中段的本意是非 AI 错误码区分标记，非 AI 错误码携带此标记反而破坏区分语义。
- **建议方向**：撤回问题14或重新定位——问题应改为"设计文本 §5.1 需将 AI 能力错误码与本地业务错误码的命名规则分别说明，而非将二者混合在同一前缀下不加区分"。

### 要点2

- **问题**：问题9建议"前端仅传 patientId，后端从健康档案自动提取"，问题16建议"前端请求 DTO 中 allergyDetails 为可选字段用于离线/缓存场景临时覆盖"，二者在同一数据来源维度上给出相互矛盾的改进建议。
- **原因**：如果前端仅传 patientId（问题9），则 AuditRequest/PrescriptionAssistRequest 作为前端请求 DTO 中的 allergyDetails 字段无意义（问题16的"前端传入"场景不成立）。反之如果 allergyDetails 作为前端请求 DTO 字段保留（问题16），则"前端仅传 patientId"不成立。两问题的改进建议在数据流方向上存在排斥关系，产出作者无法同时执行二者。
- **建议方向**：合并问题9和问题16，统一给出一致的改进建议：allergyHistory 由后端从健康档案拼接、allergyDetails 按 §3.1.6 过渡方案"后端优先拼接、前端存入作为补充"的双通道语义，在 Service 层定义来源优先级规则。

### 要点3

- **问题**：问题4聚焦于 AiResult degraded() 工厂方法的改进建议，但遗漏了 AiResult failure() 路径的同类问题——超时场景需要同时传递 errorCode（如 MR_GEN_AI_TIMEOUT）和 partialData，而 failure() 工厂方法将 data 设为 null。
- **原因**：需求文档 §3.4.3（L883）明确 MR_GEN_AI_TIMEOUT 适用于所有超时场景并保留部分内容，这意味着超时返回是 failure+errorCode+partialData 的组合（不是 degraded+fallbackReason+partialData）。仅覆盖 degraded() 重载无法解决编码实现者面临的 AiResult 构造路径歧义。
- **建议方向**：问题4改进建议补充 failure() 路径处理——AiResult.failure() 需新增重载 `failure(String errorCode, T partialData)`，或明确超时场景不使用任何现有工厂方法而直接构造 AiResult 对象，确保超时降级路径的 errorCode 和 partialData 可同时传递。
