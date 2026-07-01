# 再审议判定报告（v29）

## 判定结果

RETRY

## 判定理由

组件B诊断报告（v2）经质询确认（LOCATED），实际轮次2 < 最大轮次12，质询提前终止且审查结论被确认。报告识别出5个问题，其中问题1、2为事实错误/逻辑矛盾（严重等级），问题3、4、5为影响准确性与完整度的非致命问题（一般等级）。根据判定标准，审查报告包含严重及一般等级的问题，需要重新运行组件A进行修复。

## 需要解决的问题（仅 RETRY 时存在）

- **问题描述**：薄适配器超时WARN日志使用全局默认值（`thinAdapterTimeout`）而非实际生效的per-capability覆盖值（`effectiveThinAdapterTimeout`），误导运维排障
- **所在位置**：§4.2 薄适配器特化管线伪代码，第3927行 vs 第3934行
- **严重程度**：严重
- **改进建议**：将第3934行 `thinAdapterTimeout.toMillis()` 替换为 `effectiveThinAdapterTimeout.toMillis()`

- **问题描述**：LlmChatOptions阶段一填充伪代码仅映射白名单中6个key的前3个（temperature、maxTokens、stopSequences），遗漏topP、frequencyPenalty、presencePenalty，导致SDK默认值覆盖路由配置值
- **所在位置**：§3.2 第1845行（白名单声明）vs §4.1 第3538-3544行（伪代码映射）
- **严重程度**：严重
- **改进建议**：在§4.1伪代码阶段一填充中补充topP、frequencyPenalty、presencePenalty的映射逻辑

- **问题描述**：PrescriptionAssist变量提取策略（方式A：ObjectMapper.convertValue）与嵌套DTO结构（PatientInfo内嵌值对象）不匹配，模板变量{{patientAge}}等无法正确填充，且与PrescriptionCheck同类结构的处理方式（方式B：自定义展开）不一致
- **所在位置**：§3.11.4 第3145行（模板变量）、第3147行（变量提取策略）
- **严重程度**：一般
- **改进建议**：统一PrescriptionAssist与PrescriptionCheck的患者数据建模方式和变量提取策略（推荐改为方式B自定义展开，或改为扁平字段结构）

- **问题描述**：AiCallRecord.capabilityName字段在字段定义表中存在，但三个工厂方法签名（success()/failure()/degraded()）均不包含该参数，管线中所有调用点均未传入，实施者无法获知填充来源
- **所在位置**：§3.5 第2207-2208行（字段定义）、第2232-2252行（工厂方法签名）；§4.1各工厂方法调用点
- **严重程度**：一般
- **改进建议**：方案A（推荐）：在工厂方法签名中补充String capabilityName参数并更新调用点；方案B：在字段定义注释中说明由capabilityId自动映射补全

- **问题描述**：精确Tokenizer路径与字符估算回退路径在§4.1第3846行共用同一>3000阈值判断，但v27修订说明要求估算路径阈值提升至4000，修订意图未落实
- **所在位置**：§4.1 第3817-3846行；v27修订说明第9条（第4704行）
- **严重程度**：一般
- **改进建议**：按路径区分阈值——精确路径使用3000，估算路径使用4000（或等价于校正后实际Token为3000的字符数阈值）
