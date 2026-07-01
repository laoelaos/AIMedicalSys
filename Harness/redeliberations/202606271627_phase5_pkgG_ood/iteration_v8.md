# 再审议判定报告（v8）

## 判定结果

RETRY

## 判定理由

诊断报告共识别 11 项问题，其中严重等级 2 项（P1 异步线程上下文传播未定义、P2 LlmCallExecutor 拒绝策略未定义），重要等级 4 项（P3-P6），中等等级 2 项（P7-P8），一般等级 3 项（P9-P11）。质询报告结论为 LOCATED，全部问题经质询确认成立，实际轮次（1）小于最大轮次（12）说明审查提前终止且结论被接受。因存在严重及重要等级问题，满足 RETRY 条件。

## 需要解决的问题（仅 RETRY 时存在）

- **问题描述**：异步线程上下文传播未定义 — SecurityContext 和 RequestContext 在 supplyAsync 线程池中丢失
- **所在位置**：§4.1 AbstractCapabilityExecutor.execute() 模板方法（第 1144-1157 行）+ §3.1 UserId/SessionId 上下文来源（第 609-612 行）+ §3.5 字段填充策略（第 903 行）+ §3.1 薄适配器 departmentId 提取（第 647 行）
- **严重程度**：严重
- **改进建议**：在 §6 并发设计中补充异步上下文传播机制，推荐方案 (c) 在 execute() 入口处提取完毕再传入 lambda，或方案 (b) 将异步边界下移到 LLM 调用层面

- **问题描述**：LlmCallExecutor 线程池拒绝策略未定义 — 高并发下线程池满载后的行为不可控
- **所在位置**：§3.2 LlmClient 线程模型（第 735 行）
- **严重程度**：严重
- **改进建议**：在 §3.2 或 §6.1 中为 LlmCallExecutor 显式定义拒绝策略，建议 CallerRunsPolicy 或 DiscardPolicy + 降级，同步补充 queueCapacity 定义

- **问题描述**：doDegrade() 方法签名缺少 departmentId 参数 — 降级路径下 AiCallRecord 构造缺少科室标识
- **所在位置**：§4.1 第 1153、1170、1176、1183 行调用点；§4.1 第 1189-1200 行方法体
- **严重程度**：重要
- **改进建议**：将 doDegrade() 方法签名扩展为 doDegrade(startTime, degradeReason, request, capabilityId, departmentId)，相应更新所有调用点

- **问题描述**：AiCallRecord 工厂方法签名不完整 — failure() 和 degraded() 签名未覆盖 callerRole/callerId 字段
- **所在位置**：§3.5 工厂方法签名（第 884-898 行）
- **严重程度**：重要
- **改进建议**：在所有三个工厂方法签名中增加 callerRole 和 callerId 参数（推荐方案 a）

- **问题描述**：薄适配器 departmentId 提取在 supplyAsync 异步上下文中无效
- **所在位置**：§3.1 第 647 行 doExtractDepartmentId() 伪代码；§4.1 第 1149-1150 行调用位置
- **严重程度**：重要
- **改进建议**：在 AiOrchestrator.handle() 的委托入口处提取 departmentId 并注入到 AiRequestBase.departmentId

- **问题描述**：ModelEndpointHealthManager 状态机缺少 UNAVAILABLE→DEGRADED 和 CONNECTED→UNAVAILABLE 的直接转换路径
- **所在位置**：§3.2 第 750-756 行
- **严重程度**：重要
- **改进建议**：补充端点健康管理器的状态转换表，明确 CONNECTED → UNAVAILABLE（连续 N 次调用失败）和 UNAVAILABLE → CONNECTED（探测调用成功）路径

- **问题描述**：熔断器与端点健康管理器独立探测可能产生冲突
- **所在位置**：§3.2 第 756 行 + 第 763-766 行
- **严重程度**：中等
- **改进建议**：统一探测机制，将端点健康探测决策权归并到一个组件

- **问题描述**：AiCallRecord 未记录 Prompt 版本号 — A/B 实验效果分析缺少关键维度
- **所在位置**：§3.5 AiCallRecord 字段表（第 860-882 行）
- **严重程度**：中等
- **改进建议**：在 AiCallRecord 和 AiCallLogEntity 中补充 promptVersion 字段

- **问题描述**：AiCallRecord.degraded() 工厂方法缺少 outputSummary 参数 — 本地规则降级结果无法记录
- **所在位置**：§3.5 第 895-898 行
- **严重程度**：一般
- **改进建议**：在 degraded() 工厂方法签名中补充 String outputSummary 参数

- **问题描述**：未显式验证与 Phase0/Phase1ABD OOD 设计风格的一致性
- **所在位置**：全局
- **严重程度**：一般
- **改进建议**：在 §1 概述部分增加对 Phase0/Phase1ABD 设计风格和结构的引用说明

- **问题描述**：ModelRoute 缺少端点级认证和超时配置定义
- **所在位置**：§3.2 ModelRoute 定义（第 787 行）
- **严重程度**：一般
- **改进建议**：扩展到 ModelRoute 值对象中增加 authentication、timeout 字段
