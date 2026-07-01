# 再审议判定报告（v11）

## 判定结果

RETRY

## 判定理由

诊断报告共识别 10 项问题（2 严重 + 4 一般 + 4 轻微），质询报告确认（LOCATED）全部问题证据充分、逻辑完整、覆盖完备、必要性强。其中 P1（check-dose 请求参数/行为矛盾）和 P2（AdditionalResponse 字段缺失）为严重等级，P3/P4/P5/P6 为一般等级，均未达到 PASS 标准。内部循环实际轮次 1 < 最大轮次 12，提前终止系因质询确认为 LOCATED，满足重新运行条件。

## 需要解决的问题（仅 RETRY 时存在）

- **问题描述**：check-dose 端点请求参数缺少 prescriptionId，无法支撑 PrescriptionDraftContext 写入行为
- **所在位置**：§4.4 "即时校验子端点"段落
- **严重程度**：严重
- **改进建议**：在 check-dose 请求参数中增加 prescriptionId（必填，String），同步更新 §一.3 包E DosageCheckRequest 字段说明；若设计为无状态端点则从行为描述中删除 CRITICAL 写入逻辑

- **问题描述**：AdditionalResponse 业务层 DTO 缺少字段定义
- **所在位置**：§3.1 DialogueCreateRequest 字段表——additionalResponses
- **严重程度**：严重
- **改进建议**：在 §3.1 或 §1.3 包C 核心抽象中补充 AdditionalResponse 字段定义（question、answer、answeredAt），与 ai-api 层 AdditionalResponseItem 保持映射一致

- **问题描述**：TriageRule 实体 conditions 字段 JSON 结构未定义
- **所在位置**：§3.1 TriageRule（JPA @Entity）——conditions 字段
- **严重程度**：一般
- **改进建议**：给出 conditions 的 JSON schema 示例（如 List\<ConditionItem\> 含 keyword、weight、matchType），在 §7 设计决策中补充结构选择理由

- **问题描述**：全量拼接策略在多轮长对话场景下的 token 超限风险未评估
- **所在位置**：§3.1 TriageService——"AI 调用上下文传递策略"
- **严重程度**：一般
- **改进建议**：补充 token 超限风险评估，选择截断/摘要/标注延期三种策略之一写入设计决策

- **问题描述**：错误码 RX_ASSIST_AI_SUGGESTION_NOT_FOUND 命名违反自身定义的命名规则
- **所在位置**：§5.1 错误码表——"开方辅助（非AI）"行
- **严重程度**：一般
- **改进建议**：将命名改为 RX_ASSIST_SUGGESTION_NOT_FOUND（去掉 _AI_）并修正引用处，或改移至"开方辅助（AI）"行

- **问题描述**：全量降级路径中前端无法区分"AI 完全不可用"与"AI 明确返回空"
- **所在位置**：§4.3 病历生成场景——完全降级路径描述；§3.3 RecordGenerateResponse 字段
- **严重程度**：一般
- **改进建议**：在 RecordGenerateResponse 中增加 fallbackReason 可选字段，或定义更细粒度降级错误码
