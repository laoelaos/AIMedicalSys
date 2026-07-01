# 再审议判定报告（v5）

## 判定结果

RETRY

## 判定理由

组件B诊断报告共识别出 8 个问题，其中严重问题 3 个、一般问题 4 个、轻微问题 1 个。质询结论为 LOCATED（审查被组件B确认），实际轮次 1 未达到最大轮次 12，提前终止。由于诊断报告中存在 3 个严重问题和 4 个一般问题，不符合 PASS 的任一条件（不含严重/一般、未 LOCATED、全轻微），因此判定需重新运行组件A以修复这些问题。

## 需要解决的问题（仅 RETRY 时存在）

- **问题描述**：§3.1 DialogueSessionManager 详细设计与 §1.1/§6.1 Store 抽象层强制约束矛盾
- **所在位置**：§3.1 DialogueSessionManager 描述；§1.1 设计目标 Store 抽象层段；§6.1 部署约束段
- **严重程度**：严重
- **改进建议**：将内部存储引用替换为 SessionStore 接口（注入 ConcurrentHashMapStore 实现），删除或改写否定 interface 抽象价值的论证，同步补充 Store 接口及实现类的包路径

- **问题描述**：异步 AI 建议流程缺少 AiResult → AiSuggestionResult 的映射逻辑定义
- **所在位置**：§2.3 AiService.prescriptionAssist() 定义；§3.4 AiSuggestionResult 条目；§6.3 异步 AI 建议与去重段；§4.4 异步 AI 调用完成后更新状态段
- **严重程度**：严重
- **改进建议**：新增明确的 AiResult → AiSuggestionResult 映射规则表，覆盖 COMPLETED/FAILED/DEGRADED 三路径，同步补充 partialData 写入时机和 JSON 格式定义

- **问题描述**：产出未参考需求中列明的 Phase 23 已有 OOD 草案，设计连续性无法验证
- **所在位置**：全文均未引用，尤其应在 §1 概述或 §7 设计决策中有追迹说明
- **严重程度**：严重
- **改进建议**：在 §1 概述中增加对 Phase 23 已有 OOD 草案的追迹说明，或在 §7 设计决策表中为关键决策标注与草案的一致/差异关系

- **问题描述**：DosageCheckRequest.prescriptionId 的"必填"约束与 §3.4 后端自动生成 fallback 路径矛盾
- **所在位置**：§1.3 DosageCheckRequest 条目；§3.4 "prescriptionId 分配时机"段；§4.4 check-dose 请求参数描述
- **严重程度**：一般
- **改进建议**：将约束改为"主路径必填，空值时由后端自动生成并回写"，或拆分为两个子场景明确定义，并在 §3.4 中明确 API 验证层应跳过 @NotNull 校验

- **问题描述**：所有 API 端点缺少结构化契约定义，前端/QA 无法直接消费
- **所在位置**：§1.3 核心抽象一览（各 DTO 条目）；§4.1~§4.4 关键行为契约；全文无 OpenAPI/Swagger 章节
- **严重程度**：一般
- **改进建议**：在 §4 后或新增章节集中给出各端点的完整请求/响应 JSON 示例，明确定义错误响应包装格式，明确前端/QA 获取完整 API 契约的方式

- **问题描述**：AiSuggestionResult TTL 与 PrescriptionDraftContext TTL 一致性问题已修复，但 DedupTaskScheduler 的去重判定仍可能因 TTL 差异导致逻辑残差
- **所在位置**：§3.4 AiSuggestionResult "TTL（60 分钟）"描述；§3.4 异步 AI 调用去重策略段；§6.3 prescriptionId 级去重段
- **严重程度**：一般
- **改进建议**：定义 TTL 过期与 consumed 标记的协调策略——前端查询 TTL 过期时返回特定错误码触发重试，或延长 TTL 以 consumed 标记为唯一释放条件

- **问题描述**：§5.1 错误码表缺少若干边界场景的错误码
- **所在位置**：§5.1 模块级错误码表
- **严重程度**：一般
- **改进建议**：补充 TRIAGE_DOCTOR_FACADE_UNAVAILABLE、MR_GEN_TEMPLATE_LOAD_FAILED、RX_ASSIST_AI_SUGGEST_CREATE_FAILED、RX_AUDIT_CONCURRENT_SUBMIT 等错误码
