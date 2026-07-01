# 再审议判定报告（v2）

## 判定结果

RETRY

## 判定理由

组件B的诊断报告识别出 5 个有效问题，其中 2 个严重（问题1：BLOCK 强制阻断机制缺失、问题2：AuditRecord 缺少业务关联标识）、3 个一般（问题3：AiSuggestionResult 内存存储重启丢失、问题4：DosageStandard 无年龄/体重分级支持、问题5：病历生成降级策略不合理）。质询报告确认了这些问题的有效性（LOCATED），实际轮次（1）小于最大轮次（12），说明提前终止且审查结论成立。根据判定标准，存在严重和一般等级问题，应重新运行组件A。

## 需要解决的问题

- **问题描述**：BLOCK 风险等级缺少后端强制阻断执行机制，处方审核安全性依赖前端履约
- **所在位置**：§3.2 AuditRiskLevel 职责定位；§4.2 审核场景流程
- **严重程度**：严重
- **改进建议**：在业务层定义独立强制阻断切入机制，推荐路径 A（预检/提交端点分离）或路径 B（Controller 层拦截返回 403/422）

- **问题描述**：AuditRecord 缺少处方级关联标识（prescriptionOrderId、doctorId、patientId），审计追踪能力受限
- **所在位置**：§3.2 AuditRecord 协作描述
- **严重程度**：严重
- **改进建议**：在 AuditRecord entity 中增加 prescriptionOrderId、doctorId、patientId 必填字段

- **问题描述**：AiSuggestionResult 使用 ConcurrentHashMap 暂存，服务重启后所有结果丢失且无错误提示
- **所在位置**：§6.3 "包E 的异步 AI 建议"
- **严重程度**：一般
- **改进建议**：参照 §3.1 findOrCreate 三分支模式，补充建议不存在时的明确错误码及 TTL 说明

- **问题描述**：DosageStandard 未定义支持年龄/体重分级的剂量存储结构，影响剂量校验实现
- **所在位置**：§2.1 common/entity/DosageStandard.java；§3.4 DosageThresholdService
- **严重程度**：一般
- **改进建议**：明确定义年龄/体重分级支持方式（方式A：添加年龄体重范围字段；方式B：拆分 AgeBandedDosage 子实体）

- **问题描述**：病历生成降级策略为"返回空病历框架 + 全字段缺失"，丢弃已提取的结构化字段
- **所在位置**：§4.3 病历生成场景——降级流程
- **严重程度**：一般
- **改进建议**：改为"分层保护"策略，保留已提取字段，仅标记缺失字段
