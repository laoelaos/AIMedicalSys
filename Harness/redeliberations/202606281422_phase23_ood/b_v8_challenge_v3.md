# 质量质询报告（v3）

## 质询结果

LOCATED

## 逐维度审查

### 1. 证据充分性

**[通过]** 问题1（DoctorFacade 包名不匹配）已验证源码——auth 包下确为 UserFacade/CurrentUser/UserInfoResponse，DoctorFacade 不在其中，证据确凿。

**[通过]** 问题3（合并症检查遗漏）直接引用需求文档 §3.4.2 检查项 #2 原文"是否与患者的合并症冲突"，与设计 §3.2 AllergyCheckRule 仅实现过敏史检查形成明确对照，证据充分。

**[通过]** 问题4（AiResult 超时路径歧义）已验证 AiResult.java 源码——failure() 和 degraded() 工厂方法均将 data 设为 null，且需求文档 §3.4.3 L871-883 明确超时需同时传 errorCode+partial_content，证据链完整。v3 修订补充了 failure() 路径，逻辑自洽。

**[通过]** 问题5（TriageRecord 缺推荐医生快照）引用需求文档 §5.1 分诊记录实体明确列出"推荐医生"字段，与设计 §3.1 TriageRecord 字段列表缺少该字段形成直接对照，证据充分。

**[通过]** 问题9（allergyHistory/allergyDetails 数据来源语义不一致）v3 版本合并了 v2 矛盾的两个问题，基于需求文档 §3.1.6 三层行为原文重新定位，统一为后端优先+前端补充的双通道语义，消除了内部矛盾，证据充分。

**[通过]** 问题16（错误码表遗漏）逐条对照需求文档 §3.4.x 错误码定义与设计 §5.1 表，列出 RX_ASSIST_AI_NO_RECOMMENDATION、RX_AUDIT_AI_INPUT_INVALID、MR_GEN_AI_INPUT_INVALID、MR_GEN_AI_OUTPUT_INCOMPLETE 四项遗漏，证据确凿。

**[通过]** 其余问题（2/6/7/8/10/11/12/13/14/15）均基于设计文档原文与需求文档原文的对照分析，证据充分。

### 2. 逻辑完整性

**[通过]** 16 个问题之间不存在逻辑矛盾。问题9 和问题10 虽然均涉及过敏信息流转，但问题9 关注数据来源语义（allergyHistory/allergyDetails 的填充优先级），问题10 关注辅助开方过敏告警与处方审核过敏检查的关系与 severity 映射，二者视角不同、不矛盾。

**[通过]** 各问题改进建议与问题定位一致且可行。问题3 建议新增 ContraindicationCheckRule 并明确数据来源实体，问题4 建议明确使用 AiResult.data 并补齐两条路径重载，问题5 建议增加 recommendedDoctors JSON TEXT 字段，均具体可执行。

**[通过]** v3 修订说明正确回应了此前质询意见——问题14 从"命名违规"重新定位为"分类规则不明确"，问题9 合并统一，问题4 补充 failure() 路径——修订逻辑自洽。

### 3. 覆盖完备性

**[通过]** 任务描述要求审查维度为需求响应充分度、整体深度和完整性，以及从实际落地视角评估设计可编码性、接口完整性、异常场景覆盖。审查报告的问题覆盖了：

- **需求响应充分度**：问题3（合并症检查遗漏）、问题5（推荐医生字段缺失）、问题16（错误码遗漏）
- **接口完整性**：问题6（AuditAlert.severity 类型未定义）、问题7（DosageAlert 无错误码字段）、问题11（RegistrationEvent 事件契约未定义）
- **异常/边界场景覆盖**：问题4（AiResult 超时路径二义性）、问题8（处方一致性比较语义未定义）、问题15（TTL 清理竞态/规则快照失效）
- **设计可编码性**：问题2（matched_rules 子字段设计决策缺失）、问题9（allergyDetails 数据来源优先级不清）、问题10（辅助开方过敏告警与处方审核关系不明确）、问题14（错误码分类命名规则不明确）

**[问题-轻微]** 审查报告未覆盖一个潜在质量问题：设计文档 §3.4 PrescriptionAssistService 声明 check-dose 端点接收 frequency 参数用于日剂量校验，但 DosageCheckRequest 的字段定义（§4.4）仅列 drugCode/dosage/unit/routeOfAdministration/patientAge/patientWeight/frequency，而 DosageThresholdService 的匹配优先级策略（§3.4）描述四级均未命中时的降级行为，但未明确当 frequency 不传（可选场景）且 DosageStandard 存在 dailyMax 时日剂量校验是否跳过。此参数可选性对编码者而言存在歧义，但严重程度较低（可通过合理默认值处理）。

**[建议]** 此为轻微遗漏，不构成 CHALLENGED 条件。若后续修订可补充 frequency 可选性与日剂量校验的关系说明。

### 4. 报告必要性

**[通过]** 16 个问题均关注需求响应、接口完整性、异常场景、编码可行性等任务核心维度，未陷入文档校对、行号统计、版本号等无关细节。

**[通过]** 问题1（DoctorFacade 包名不匹配）虽涉及目录结构，但核心关注点是"接口的语义归属影响编码者的模块定位判断"，属于设计可编码性维度，合理。

---

## 总结

审查报告的 16 个问题（含 3 项严重）证据充分、逻辑自洽、覆盖任务要求的审查维度。v3 修订正确回应了此前质询意见，消除了 v2 的内部矛盾和证据不足。存在的轻微覆盖遗漏（DosageCheckRequest.frequency 可选性与日剂量校验的关系）不影响审查结论的可信度。产出作者可据此报告改进设计文档。
